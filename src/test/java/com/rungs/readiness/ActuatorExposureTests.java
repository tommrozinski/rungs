package com.rungs.readiness;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorExposureTests {

    private final MockMvcTester mvc;

    @Autowired
    ActuatorExposureTests(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void healthEndpointReturnsUpStatusAndTheProbeGroupNames() {
        // With readiness/liveness probes enabled (story 037), Boot always lists the configured group names
        // alongside the aggregate status; this is separate from show-details, which stays "never".
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("UP");
        assertThat(mvc.get().uri("/actuator/health"))
                .bodyJson()
                .extractingPath("$.groups")
                .asList()
                .containsExactlyInAnyOrder("liveness", "readiness");
    }

    @Test
    void envEndpointIsNotExposed() {
        assertThat(mvc.get().uri("/actuator/env")).hasStatus(404);
    }

    @Test
    void readinessProbeReturnsOnlyUpStatus() {
        assertThat(mvc.get().uri("/actuator/health/readiness"))
                .hasStatusOk()
                .hasBodyTextEqualTo("{\"status\":\"UP\"}");
    }

    @Test
    void livenessProbeReturnsOnlyUpStatus() {
        assertThat(mvc.get().uri("/actuator/health/liveness"))
                .hasStatusOk()
                .hasBodyTextEqualTo("{\"status\":\"UP\"}");
    }

    @Test
    void discoveryPageListsOnlyHealthLinks() {
        // Boot adds a separate "health-path" link for the health operation that takes a path selector.
        assertThat(mvc.get().uri("/actuator"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$._links")
                .asMap()
                .containsOnlyKeys("self", "health", "health-path");
    }
}
