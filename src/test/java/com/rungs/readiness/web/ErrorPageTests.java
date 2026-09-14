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
 * The custom {@code templates/error.html} page a real request's error status renders instead of Boot's whitelabel
 * page: only a real HTTP request through the embedded container triggers the container's error-page forward, so
 * this is a full {@code webEnvironment = RANDOM_PORT} test using the JDK's own HTTP client, rather than a
 * {@code MockMvc} one. The page shows the status but never an exception class name or stack trace.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(FixedClockTestConfig.class)
class ErrorPageTests {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private HttpResponse<String> request(String method, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "text/html")
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void getAssessIsMethodNotAllowedAndRendersTheCustomErrorPage() throws Exception {
        HttpResponse<String> response = request("GET", "/assess");

        assertThat(response.statusCode()).isEqualTo(405);
        String body = response.body();
        assertThat(body).contains("405");
        assertThat(body).doesNotContain("Whitelabel Error Page");
        assertThat(body).doesNotContain("Exception");
        assertThat(body).doesNotContain("java.lang");
        assertThat(body).doesNotContain("org.springframework");
        assertThat(body).contains("href=\"/\"");
    }

    @Test
    void removeWithNonNumericIndexIsBadRequestAndRendersTheCustomErrorPage() throws Exception {
        HttpResponse<String> response = request("POST", "/debts/remove/abc");

        assertThat(response.statusCode()).isEqualTo(400);
        String body = response.body();
        assertThat(body).contains("400");
        assertThat(body).doesNotContain("Whitelabel Error Page");
        assertThat(body).doesNotContain("Exception");
        assertThat(body).doesNotContain("java.lang");
        assertThat(body).doesNotContain("org.springframework");
    }

    @Test
    void unknownPathIsNotFoundAndRendersTheCustomErrorPage() throws Exception {
        HttpResponse<String> response = request("GET", "/nope");

        assertThat(response.statusCode()).isEqualTo(404);
        String body = response.body();
        assertThat(body).contains("404");
        assertThat(body).doesNotContain("Whitelabel Error Page");
        assertThat(body).doesNotContain("Exception");
    }
}
