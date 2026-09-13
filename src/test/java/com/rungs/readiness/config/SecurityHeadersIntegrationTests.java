package com.rungs.readiness.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * 034 REQ-002, REQ-006, REQ-007: the security headers land on every response this server sends, whatever the
 * route or the eventual status, and no route sets a cookie.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityHeadersIntegrationTests {

    private final MockMvcTester mvc;

    @Autowired
    SecurityHeadersIntegrationTests(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void getSlashCarriesTheSecurityHeadersAndNoCookie() {
        MvcTestResult result = mvc.get().uri("/").exchange();

        assertSecurityHeaders(result);
        assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
    }

    @Test
    void postAssessmentCarriesTheSecurityHeadersAndNoCookie() {
        MvcTestResult result = mvc.post().uri("/assessment").param("monthlyTakeHomeIncome", "5000").exchange();

        assertSecurityHeaders(result);
        assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
    }

    @Test
    void postV1AssessmentsCarriesTheSecurityHeaders() {
        MvcTestResult result = mvc.post().uri("/v1/assessments").contentType(MediaType.APPLICATION_JSON)
                .content("{}").exchange();

        assertSecurityHeaders(result);
    }

    @Test
    void aNotFoundResponseStillCarriesTheSecurityHeaders() {
        MvcTestResult result = mvc.get().uri("/no-such-route").exchange();

        assertThat(result).hasStatus(404);
        assertSecurityHeaders(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/assessment", "/snapshot/field"})
    void resultsRoutesRejectGetWithMethodNotAllowed(String path) {
        assertThat(mvc.get().uri(path)).hasStatus(405);
    }

    private static void assertSecurityHeaders(MvcTestResult result) {
        assertThat(result.getResponse().getHeader("Content-Security-Policy")).isEqualTo(
                "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self'; "
                        + "connect-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'self'");
        assertThat(result.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(result.getResponse().getHeader("Referrer-Policy")).isEqualTo("no-referrer");
    }
}
