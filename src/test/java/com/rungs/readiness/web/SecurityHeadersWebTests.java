package com.rungs.readiness.web;

import static com.rungs.readiness.web.FormParams.form;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@link SecurityHeadersFilter}'s response headers, present on every response this app renders: a normal page, a
 * result page, a 404, and a static asset — the last minus {@code Cache-Control: no-store}, which would defeat
 * caching a file that never changes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockTestConfig.class)
class SecurityHeadersWebTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getRootHasSecurityHeaders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(result -> {
                    var response = result.getResponse();
                    assertThat(response.getHeader("Content-Security-Policy")).isEqualTo(
                            "default-src 'none'; style-src 'self'; style-src-attr 'unsafe-inline'; "
                                    + "font-src 'self'; img-src 'self'; frame-src 'self'; form-action 'self'; "
                                    + "base-uri 'none'; frame-ancestors 'self'");
                    assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
                    assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
                    assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
                });
    }

    @Test
    void postAssessHasSecurityHeaders() throws Exception {
        mockMvc.perform(post("/assess").params(form().monthlyTakeHomeIncome("6200").values()))
                .andExpect(result -> {
                    var response = result.getResponse();
                    assertThat(response.getHeader("Content-Security-Policy")).isNotBlank();
                    assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
                });
    }

    @Test
    void notFoundStillHasSecurityHeaders() throws Exception {
        mockMvc.perform(get("/nope"))
                .andExpect(result -> {
                    var response = result.getResponse();
                    assertThat(response.getHeader("Content-Security-Policy")).isNotBlank();
                    assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
                });
    }

    @Test
    void cssResponseHasHeadersButNotNoStore() throws Exception {
        mockMvc.perform(get("/css/rungs.css"))
                .andExpect(result -> {
                    var response = result.getResponse();
                    assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
                    assertThat(response.getHeader("Cache-Control")).isNotEqualTo("no-store");
                });
    }

    @Test
    void resultPageIframeIsSandboxed() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .params(form().monthlyTakeHomeIncome("6200").param("breakdown", "true").values()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("sandbox=\"allow-same-origin\"");
    }
}
