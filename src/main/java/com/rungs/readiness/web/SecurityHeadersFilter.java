package com.rungs.readiness.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds browser security headers to every response this app renders, including a 404 or other error dispatch: a
 * strict {@code Content-Security-Policy} (the srcdoc payoff-schedule iframe is same-origin, so its stylesheet and
 * font still load under {@code 'self'}; {@code style-src-attr 'unsafe-inline'} is the one loosening, for the
 * progress bar's {@code th:style} width), {@code X-Content-Type-Options}, {@code Referrer-Policy}, and
 * {@code Cache-Control: no-store} on every response except a static asset under {@code /css/} or {@code /fonts/},
 * which should still be cacheable.
 */
class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String CONTENT_SECURITY_POLICY = "default-src 'none'; style-src 'self'; "
            + "style-src-attr 'unsafe-inline'; font-src 'self'; img-src 'self'; frame-src 'self'; "
            + "form-action 'self'; base-uri 'none'; frame-ancestors 'self'";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        if (!isStaticAsset(request)) {
            response.setHeader("Cache-Control", "no-store");
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Returns whether {@code request} targets a static asset the browser should be free to cache. Built from
     * {@link HttpServletRequest#getServletPath()} plus any {@link HttpServletRequest#getPathInfo()} — the
     * container's normalized path, split across the two depending on the servlet mapping in play — rather than
     * {@link HttpServletRequest#getRequestURI()}, the raw, unnormalized path a request like {@code /css/..;/assess}
     * could use to look like a static asset without actually being routed as one.
     */
    private static boolean isStaticAsset(HttpServletRequest request) {
        String path = request.getServletPath() + (request.getPathInfo() == null ? "" : request.getPathInfo());
        return path.startsWith("/css/") || path.startsWith("/fonts/");
    }

    /**
     * Runs again on an error dispatch (e.g. a 404 forwarded to {@code /error}) instead of being skipped, so an
     * error response still carries these headers.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
