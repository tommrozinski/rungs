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
    void healthEndpointReturnsOnlyUpStatus() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .hasBodyTextEqualTo("{\"status\":\"UP\"}");
    }

    @Test
    void envEndpointIsNotExposed() {
        assertThat(mvc.get().uri("/actuator/env")).hasStatus(404);
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
