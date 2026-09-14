package com.rungs.readiness.web;

import static com.rungs.readiness.web.FormParams.form;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end proof that {@link SnapshotForm}'s length caps keep a hostile submission's oversized fields from
 * ballooning the page they're echoed back into, however many times (form re-render, result page's hidden copy,
 * inline payoff schedule breakdown) a value is copied.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockTestConfig.class)
class InputCapsWebTests {

    @Autowired
    private MockMvc mockMvc;

    private static final String HUGE_LABEL = "A".repeat(10_000);

    @Test
    void assessRendersAtMostSixtyCharactersOfAHugeDebtLabel() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("6200")
                                .debtLabel(0, HUGE_LABEL)
                                .debtBalance(0, "840")
                                .values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("A".repeat(60));
        assertThat(body).doesNotContain("A".repeat(61));
    }

    @Test
    void editRendersAtMostSixtyCharactersOfAHugeDebtLabel() throws Exception {
        String body = mockMvc.perform(post("/edit")
                        .params(form()
                                .debtLabel(0, HUGE_LABEL)
                                .debtBalance(0, "840")
                                .values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("A".repeat(60));
        assertThat(body).doesNotContain("A".repeat(61));
    }

    @Test
    void twentyDebtsWithHugeLabelsAndTheBreakdownOpenStayUnderASaneResponseSize() throws Exception {
        FormParams params = form().monthlyTakeHomeIncome("6200").param("breakdown", "true");
        for (int index = 0; index < 20; index++) {
            params.debt(index, HUGE_LABEL, "CREDIT_CARD", "840", "10", "35");
        }

        String body = mockMvc.perform(post("/assess").params(params.values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body.length()).isLessThan(2_500_000);
    }
}
