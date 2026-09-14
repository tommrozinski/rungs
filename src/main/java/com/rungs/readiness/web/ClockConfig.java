package com.rungs.readiness.web;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supplies the {@link Clock} the controller uses to derive the assessment month, so the domain never reads the
 * time itself. Tests substitute a fixed clock.
 */
@Configuration
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
