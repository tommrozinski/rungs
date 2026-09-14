package com.rungs.readiness.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proves {@link SnapshotController#loadSample} feeds {@link SampleHouseholds#byId} the running {@link Clock}'s own
 * assessment month rather than a value calibrated for a single month (design.md 039 review): at a fixed clock far
 * from September 2026, the debt-free and homestretch samples' rendered maturity still sits exactly 27 and 16 years
 * (324 and 192 months — both whole multiples of 12) ahead of that month, so the month component matches the
 * assessment month and only the year moves.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SampleHouseholdMaturityAcrossClocksTests {

    /** Pins {@code asOf} to February 2031, deliberately unlike {@link SnapshotControllerTests}'s September 2026. */
    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2031-02-15T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loadSampleComputesTheDebtFreeMaturityRelativeToThisClocksAssessmentMonth() throws Exception {
        String body = mockMvc.perform(post("/sample").param("sample", "debt-free"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("value=\"2058\" aria-label=\"Maturity year\"");
    }

    @Test
    void loadSampleComputesTheHomestretchMaturityRelativeToThisClocksAssessmentMonth() throws Exception {
        String body = mockMvc.perform(post("/sample").param("sample", "homestretch"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("value=\"2047\" aria-label=\"Maturity year\"");
    }
}
