package com.rungs.readiness.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Proves the request-size and header guarantees hold against a real embedded server, not just {@code MockMvc}'s
 * simulated dispatch: {@link SecurityHeadersFilter}'s headers on an error response, the request-size and
 * parameter-count limits from {@code application.properties}, and that multipart is disabled so it cannot bypass
 * those limits. Every hostile request here gets a 4xx, never a 500.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(FixedClockTestConfig.class)
class RealServerLimitsTests {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    @Test
    void aNotFoundResponseHasTheSecurityHeaders() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri("/nope")).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Security-Policy")).isPresent();
        assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
    }

    @Test
    void aMethodNotAllowedResponseHasTheSecurityHeaders() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri("/assess")).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(response.headers().firstValue("Content-Security-Policy")).isPresent();
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
    }

    @Test
    void aBadRequestResponseHasTheSecurityHeaders() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri("/debts/remove/abc"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Security-Policy")).isPresent();
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
    }

    @Test
    void aUrlEncodedBodyOverSixtyFourKilobytesIsRejectedNotServerError()
            throws IOException, InterruptedException {
        String hugeValue = "9".repeat(70 * 1024);
        String body = "monthlyTakeHomeIncome=" + hugeValue;
        HttpRequest request = HttpRequest.newBuilder(uri("/assess"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(413);
    }

    @Test
    void moreThanThreeHundredParametersIsBadRequestNotServerError() throws IOException, InterruptedException {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 310; i++) {
            if (i > 0) {
                body.append('&');
            }
            body.append("field").append(i).append("=x");
        }
        HttpRequest request = HttpRequest.newBuilder(uri("/assess"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    void aMultipartPostToAssessNeverBindsAndNeverFails() throws IOException, InterruptedException {
        String boundary = "----RungsTestBoundary";
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"monthlyTakeHomeIncome\"\r\n\r\n"
                + "6200\r\n"
                + "--" + boundary + "--\r\n";
        HttpRequest request = HttpRequest.newBuilder(uri("/assess"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isLessThan(500);
        if (response.statusCode() == 200) {
            assertThat(response.body()).doesNotContain("6200");
        } else {
            assertThat(response.statusCode()).isBetween(400, 499);
        }
    }
}
