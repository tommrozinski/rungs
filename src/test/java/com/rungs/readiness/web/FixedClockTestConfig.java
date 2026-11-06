package com.rungs.readiness.web;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Pins {@code asOf} to September 2026, matching the sample households' and other web tests' expected figures.
 *  Shared by every {@code @SpringBootTest} in this package via {@code @Import(FixedClockTestConfig.class)}. */
@TestConfiguration
class FixedClockTestConfig {

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), ZoneOffset.UTC);
    }
}
