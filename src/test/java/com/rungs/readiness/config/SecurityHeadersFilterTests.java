package com.rungs.readiness.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 034 REQ-002/REQ-007: every response carries the strict Content-Security-Policy plus the two supporting
 * security headers, whatever the request path or the eventual response status.
 */
class SecurityHeadersFilterTests {

    private static final String EXPECTED_CSP = "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "img-src 'self' data:; font-src 'self'; connect-src 'self'; form-action 'self'; "
            + "frame-ancestors 'none'; base-uri 'self'";

    @Test
    void addsTheContentSecurityPolicyToEveryResponse() throws Exception {
        MockHttpServletResponse response = filterAndCapture("/");

        assertThat(response.getHeader("Content-Security-Policy")).isEqualTo(EXPECTED_CSP);
    }

    @Test
    void addsNosniffAndReferrerPolicy() throws Exception {
        MockHttpServletResponse response = filterAndCapture("/");

        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
    }

    @Test
    void stillInvokesTheRestOfTheChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/anything");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] chainCalled = {false};
        FilterChain chain = (req, res) -> chainCalled[0] = true;

        new SecurityHeadersFilter().doFilter(request, response, chain);

        assertThat(chainCalled[0]).isTrue();
    }

    private static MockHttpServletResponse filterAndCapture(String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        new SecurityHeadersFilter().doFilter(request, response, chain);
        return response;
    }
}
