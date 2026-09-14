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
 * from September 2026, the debt-free and homestretch samples' rendered maturity year still lands exactly where each
 * sample's own payoff computation (from this clock's assessment month) puts it.
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

        assertThat(body).contains("value=\"2048\" aria-label=\"Maturity year\"");
    }

    @Test
    void loadSampleComputesTheHomestretchMaturityRelativeToThisClocksAssessmentMonth() throws Exception {
        String body = mockMvc.perform(post("/sample").param("sample", "homestretch"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("value=\"2042\" aria-label=\"Maturity year\"");
    }
}
