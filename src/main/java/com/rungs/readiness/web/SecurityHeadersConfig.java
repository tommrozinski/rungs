package com.rungs.readiness.web;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers {@link SecurityHeadersFilter} for both the normal request dispatch and the error dispatch a 404 or
 * other error status forwards to (a plain {@code @Component} filter would default to the request dispatch only),
 * and orders it first, so no other filter runs before its headers are set.
 */
@Configuration(proxyBeanMethods = false)
class SecurityHeadersConfig {

    @Bean
    FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registration =
                new FilterRegistrationBean<>(new SecurityHeadersFilter());
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
