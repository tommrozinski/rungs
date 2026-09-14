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
 * Web tests for the "Show payoff schedule" flow (design.md 040 section 4): the result page's inline breakdown and
 * the standalone {@code /breakdown} page.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PayoffScheduleControllerTests {

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder boardWithMortgage(
            String path) {
        return post(path)
                .param("monthlyTakeHomeIncome", "9500")
                .param("monthlyEssentialExpenses", "2100")
                .param("starterEmergencyFund", "1000")
                .param("fullEmergencyFund", "0")
                .param("retirementContributionPercent", "6")
                .param("housing", "MORTGAGE")
                .param("mortgageBalance", "185000")
                .param("mortgageRate", "5.25")
                .param("mortgagePayment", "1650")
                .param("debts[0].label", "Store card")
                .param("debts[0].type", "CREDIT_CARD")
                .param("debts[0].balance", "840")
                .param("debts[0].annualInterestRate", "26.99")
                .param("debts[0].minimumPayment", "35")
                .param("debts[1].label", "Car loan")
                .param("debts[1].type", "AUTO")
                .param("debts[1].balance", "9140")
                .param("debts[1].annualInterestRate", "6.40")
                .param("debts[1].minimumPayment", "312")
                .param("debts[2].label", "Student loan")
                .param("debts[2].type", "STUDENT_LOAN")
                .param("debts[2].balance", "2500")
                .param("debts[2].annualInterestRate", "4.50")
                .param("debts[2].minimumPayment", "110");
    }

    @Test
    void assessWithoutBreakdownHasNoBreakdownSection() throws Exception {
        String body = mockMvc.perform(boardWithMortgage("/assess"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("id=\"breakdown\"");
    }

    @Test
    void assessWithBreakdownTrueRendersTheIframeSrcdocWithTheSchedule() throws Exception {
        String body = mockMvc.perform(boardWithMortgage("/assess").param("breakdown", "true"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("id=\"breakdown\"");
        assertThat(body).contains("class=\"breakdown-frame\"");
        assertThat(body).contains("srcdoc=\"");
        assertThat(body).contains("PAYOFF SCHEDULE");
        assertThat(body).contains("Store card");
        assertThat(body).contains("Car loan");
        assertThat(body).contains("Student loan");
        assertThat(body).contains("Mortgage");
        assertThat(body).contains("Sep 2026");
        assertThat(body).doesNotContain("<script");
    }

    @Test
    void breakdownRendersTheFirstMonthFiguresAndLoanNamesInPayoffOrder() throws Exception {
        String body = mockMvc.perform(boardWithMortgage("/breakdown"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("PAYOFF SCHEDULE");
        int storeIndex = body.indexOf("Store card");
        int carIndex = body.indexOf("Car loan");
        int studentIndex = body.indexOf("Student loan");
        int mortgageIndex = body.indexOf(">Mortgage<");
        assertThat(storeIndex).isGreaterThan(-1);
        assertThat(storeIndex).isLessThan(carIndex);
        assertThat(carIndex).isLessThan(studentIndex);
        assertThat(studentIndex).isLessThan(mortgageIndex);

        assertThat(body).contains("$840.00");
        assertThat(body).contains("$18.89");
        assertThat(body).contains("$858.89");
    }

    @Test
    void renterHouseholdHasNoMortgageColumn() throws Exception {
        String body = mockMvc.perform(post("/breakdown")
                        .param("monthlyTakeHomeIncome", "6200")
                        .param("monthlyEssentialExpenses", "2100")
                        .param("starterEmergencyFund", "1000")
                        .param("fullEmergencyFund", "0")
                        .param("retirementContributionPercent", "6")
                        .param("housing", "RENT")
                        .param("monthlyRent", "1400")
                        .param("debts[0].label", "Store card")
                        .param("debts[0].type", "CREDIT_CARD")
                        .param("debts[0].balance", "840")
                        .param("debts[0].annualInterestRate", "26.99")
                        .param("debts[0].minimumPayment", "35"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("Mortgage");
    }

    @Test
    void noLoansRendersTheEmptyMessage() throws Exception {
        String body = mockMvc.perform(post("/breakdown")
                        .param("monthlyTakeHomeIncome", "6200")
                        .param("housing", "RENT"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("No debts or mortgage to pay down.");
    }

    // 20 debts (the row cap), each with a minimum a little below its own monthly interest (like the
    // calculator's own 600-cap test), so all 20 stay open and the schedule renders the full 600 months — the
    // largest a breakdown page can be, and the same shape as the pre-fix bug report (50 debts x 600 months,
    // 6.6 MB). Capped at 20 rows, this worst case measures ~2.0 MB (well under 6.6 MB, and under the 2.64 MB a
    // straight 50-to-20 scaling of the old figure would predict); the remaining size is the template's own
    // per-cell markup/whitespace, unrelated to the row-count bug this task fixes.
    @Test
    void twentyDebtsAtTheSixHundredMonthCapStayWellUnderTheOldFiftyDebtSize() throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request = post("/breakdown")
                .param("monthlyTakeHomeIncome", "0")
                .param("housing", "RENT")
                .param("monthlyRent", "0");
        for (int i = 0; i < 20; i++) {
            request.param("debts[" + i + "].label", "Debt " + i)
                    .param("debts[" + i + "].balance", "10000")
                    .param("debts[" + i + "].annualInterestRate", "2.4")
                    .param("debts[" + i + "].minimumPayment", "19.00");
        }

        byte[] body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(new String(body, java.nio.charset.StandardCharsets.UTF_8))
                .contains("This schedule stops at 600 months.");
        assertThat(body.length).isLessThan(2_200_000);
    }

    @Test
    void breakdownWithHostileHiddenValuesNeverFails() throws Exception {
        mockMvc.perform(post("/breakdown")
                        .param("monthlyTakeHomeIncome", "1e-999999999")
                        .param("debts[0].balance", "not-a-number")
                        .param("mortgageBalance", "not-a-number")
                        .param("housing", "MORTGAGE"))
                .andExpect(status().isOk());
    }
}
