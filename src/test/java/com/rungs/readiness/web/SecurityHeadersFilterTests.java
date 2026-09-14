package com.rungs.readiness.web;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

/**
 * {@link SecurityHeadersFilter}'s static-asset check reads {@link HttpServletRequest#getServletPath()} (the
 * container's normalized path) rather than {@link HttpServletRequest#getRequestURI()} (the raw, unnormalized path),
 * so a request whose raw URI merely starts with {@code /css/} or {@code /fonts/} — e.g. a
 * {@code /css/..;/assess}-style path-parameter trick a container may resolve to a different servlet path — is never
 * mistaken for a cacheable static asset.
 */
class SecurityHeadersFilterTests {

    private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

    private static HttpServletRequest requestWith(String servletPath, String requestUri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        lenient().when(request.getServletPath()).thenReturn(servletPath);
        lenient().when(request.getRequestURI()).thenReturn(requestUri);
        return request;
    }

    @Test
    void aRealStaticAssetServletPathHasNoNoStoreHeader() throws Exception {
        HttpServletRequest request = requestWith("/css/rungs.css", "/css/rungs.css");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(response, org.mockito.Mockito.never()).setHeader("Cache-Control", "no-store");
        verify(chain).doFilter(request, response);
    }

    @Test
    void aRawUriLookingLikeAStaticAssetButNormalizingToARealPathStillGetsNoStore() throws Exception {
        // The raw requestURI starts with "/css/", as a naive prefix check on getRequestURI() alone would see it;
        // the container's own normalized servletPath is "/assess", which is what the filter must actually decide on.
        HttpServletRequest request = requestWith("/assess", "/css/..;/assess");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader("Cache-Control", "no-store");
        verify(chain).doFilter(request, response);
    }
}
