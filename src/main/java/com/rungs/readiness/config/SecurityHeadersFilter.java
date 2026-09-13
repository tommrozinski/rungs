package com.rungs.readiness.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 034 REQ-002 and REQ-007: adds a strict Content-Security-Policy, {@code X-Content-Type-Options: nosniff}, and
 * {@code Referrer-Policy: no-referrer} to every response, API and view routes alike (harmless for JSON, and
 * simpler than matching routes). Registered for both the ordinary request dispatch and the error dispatch (034
 * design decision), so the headers are present on error responses too.
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

    /** REQ-002's exact policy string. */
    static final String CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "img-src 'self' data:; font-src 'self'; connect-src 'self'; form-action 'self'; "
            + "frame-ancestors 'none'; base-uri 'self'";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
