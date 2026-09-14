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
 * Web tests for {@code POST /assess}: the seven-stage assessment the result page renders, its verdict copy, the
 * sample households' assigned stages, stage 6's current-payment-versus-snowball dates, and hostile input. The form
 * itself — the blank form, debt rows, sample loading, and {@code /edit} — is {@link FormPageTests}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockTestConfig.class)
class AssessPageTests {

    @Autowired
    private MockMvc mockMvc;

    /** Returns the markup for the single stage row whose title is {@code title}, up to (not including) the next
     *  stage row, so assertions can target one stage's disc and chip instead of matching page-wide. */
    private static String stageBlock(String body, String title) {
        int titleIndex = body.indexOf(title);
        assertThat(titleIndex).describedAs("expected to find stage titled '%s'", title).isGreaterThan(-1);
        int start = body.lastIndexOf("<div class=\"stage\"", titleIndex);
        int nextStart = body.indexOf("<div class=\"stage\"", titleIndex + title.length());
        int end = nextStart == -1 ? body.length() : nextStart;
        return body.substring(start, end);
    }

    /** Returns the markup for the current stage's open card, up through (not including) the next stage row. */
    private static String openCardHtml(String body) {
        int start = body.indexOf("<div class=\"stage-open\">");
        assertThat(start).describedAs("expected an open stage card").isGreaterThan(-1);
        int nextStart = body.indexOf("<div class=\"stage\"", start);
        int end = nextStart == -1 ? body.length() : nextStart;
        return body.substring(start, end);
    }

    /** Returns {@code text} with its apostrophes as Thymeleaf's escaped {@code th:text} renders them, so assertions
     *  can read the plain copy while matching the page's actual HTML source. */
    private static String escaped(String text) {
        return text.replace("'", "&#39;");
    }

    @Test
    void assessRunsTheRealSevenStageAssessmentForTheBoardHousehold() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("6200")
                                .monthlyEssentialExpenses("2100")
                                .starterEmergencyFund("1000")
                                .fullEmergencyFund("0")
                                .retirementContributionPercent("6")
                                .rent("1400")
                                .debt(0, "Store card", "CREDIT_CARD", "840", "26.99", "35")
                                .debt(1, "Car loan", "AUTO", "9140", "6.40", "312")
                                .debt(2, "Student loan", "STUDENT_LOAN", "2500", "4.50", "110")
                                .values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("class=\"banner\"");
        assertThat(body).doesNotContain("Smallest first");
        assertThat(body).doesNotContain("41% paid down");
        assertThat(body).doesNotContain("See the payoff plan");

        assertThat(body).contains(escaped("You're on stage 2 of 7"));
        assertThat(body).contains("$12,480 stands between you and debt freedom.");
        assertThat(body).contains(escaped(
                "With the debt snowball you're free of everything but the house by January 2027, "
                        + "about 5 months from now."));
        assertThat(body).contains("$2,243");
        assertThat(body).contains("$12,480");
        assertThat(body).contains("1 of 7");
        assertThat(body).contains("Save $1,000 to start");
        assertThat(body).contains("Pay off everything but the house");
        assertThat(body).contains("Save three months of expenses");
        assertThat(body).contains("Put 15% toward retirement");
        assertThat(body).contains("Save for college");
        assertThat(body).contains("Pay off the house");
        assertThat(body).contains("Build wealth and give");
        assertThat(body).contains("$1,000 of $1,000.");
        assertThat(body).contains("Three debts remain, totalling <b>$12,480</b>.");
        assertThat(body).contains("$0 of $10,500.");
        assertThat(body).contains(escaped("You're at 6%. Nine points to go."));
        assertThat(body).contains(escaped("You told us you're not funding college."));
        assertThat(body).contains(escaped("You rent, so there's no mortgage to pay down."));
        assertThat(body).contains("Opens once stages 1 through 6 are done or skipped.");
        assertThat(body).contains("Done");
        assertThat(body).contains("In progress");
        assertThat(body).contains("Not started");
        assertThat(body).contains("Skipped");
        assertThat(body).contains("width:40%");

        // Stage 7 is IN_PROGRESS in the domain (2 of 6 earlier stages done: stage 1 and stage 6, a renter with no
        // mortgage), but the board shows it as Not started until every earlier stage is done or skipped.
        String stage7 = stageBlock(body, "Build wealth and give");
        assertThat(stage7).contains("chip-waiting\">Not started</span>");
        assertThat(stage7).doesNotContain("disc active");

        // The current stage (2) has no bar, so its open card's blocker sits flush with the card's bottom edge.
        String openCard = openCardHtml(body);
        assertThat(openCard).doesNotContain("class=\"bar\"");
        assertThat(openCard).contains("blocker last");
    }

    @Test
    void assessShowsTheProgressBarInsideTheOpenCardForAnInProgressCurrentStage() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("4000")
                                .monthlyEssentialExpenses("2000")
                                .starterEmergencyFund("250")
                                .fullEmergencyFund("0")
                                .retirementContributionPercent("0")
                                .housing("RENT")
                                .values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String openCard = openCardHtml(body);
        assertThat(openCard).contains("class=\"bar\"");
        assertThat(openCard).contains("width:25%");
        assertThat(openCard).doesNotContain("blocker last");
    }

    @Test
    void assessReportsStageOneForAHouseholdThatHasNotYetSavedTheStarterFund() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("4000")
                                .monthlyEssentialExpenses("2500")
                                .starterEmergencyFund("400")
                                .fullEmergencyFund("0")
                                .retirementContributionPercent("0")
                                .values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains(escaped("You're on stage 1 of 7"));
        assertThat(body).contains("$600 more and your starter fund is done.");
        assertThat(body).contains("Save $1,000 before anything else.");
    }

    @Test
    void assessReportsAllStagesDoneForAHouseholdThatHasClearedEveryStage() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("6000")
                                .monthlyEssentialExpenses("3000")
                                .starterEmergencyFund("1000")
                                .fullEmergencyFund("9000")
                                .retirementContributionPercent("15")
                                .housing("RENT")
                                .values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("Every stage that applies to you is done");
        assertThat(body).contains("Build wealth and give.");
        assertThat(body).contains("5 of 7");
    }

    @Test
    void assessWithHostileScientificNotationInputsStillReturnsOk() throws Exception {
        mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("6200")
                                .monthlyEssentialExpenses("3500")
                                .retirementContributionPercent("1e-999999999")
                                .debtLabel(0, "Store card")
                                .debtBalance(0, "840")
                                .param("debts[0].annualInterestRate", "1e-999999999")
                                .param("debts[0].minimumPayment", "35")
                                .values()))
                .andExpect(status().isOk());
    }

    @Test
    void assessAssignsTheBuriedSampleHouseholdToStageOne() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("3800")
                                .monthlyEssentialExpenses("2100")
                                .starterEmergencyFund("150")
                                .fullEmergencyFund("0")
                                .retirementContributionPercent("0")
                                .rent("1250")
                                .debt(0, "Credit card", "CREDIT_CARD", "6400", "24.9", "190")
                                .debt(1, "Car loan", "AUTO", "14200", "9.5", "385")
                                .debt(2, "Medical bill", "MEDICAL", "2300", "0", "75")
                                .debt(3, "Personal loan", "PERSONAL", "4800", "17", "160")
                                .values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains(escaped("You're on stage 1 of 7"));
    }

    @Test
    void assessAssignsTheDebtFreeSampleHouseholdToStageThree() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("7800")
                                .monthlyEssentialExpenses("2600")
                                .starterEmergencyFund("1000")
                                .fullEmergencyFund("6000")
                                .retirementContributionPercent("4")
                                .mortgage("285000", "6.5", "2269")
                                .values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains(escaped("You're on stage 3 of 7"));
    }

    @Test
    void assessAssignsTheHomestretchSampleHouseholdToStageSixAndShowsTheSnowballsEarlierPayoffDate() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("11500")
                                .monthlyEssentialExpenses("3400")
                                .starterEmergencyFund("1000")
                                .fullEmergencyFund("17000")
                                .retirementContributionPercent("15")
                                .mortgage("198000", "3.25", "1900")
                                .values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains(escaped("You're on stage 6 of 7"));
        assertThat(body).contains(escaped("At your current payment it's paid off in"));
        assertThat(body).contains(escaped(
                "With the debt snowball it's paid off in October 2028, 8 years 1 month sooner than your payment "
                        + "alone."));
    }

    @Test
    void assessAssignsThePaidOffSampleHouseholdToStageSeven() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("9200")
                                .monthlyEssentialExpenses("3100")
                                .starterEmergencyFund("1000")
                                .fullEmergencyFund("18000")
                                .retirementContributionPercent("15")
                                .mortgage("0", "0", "0")
                                .values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("Every stage that applies to you is done");
        assertThat(body).contains("Your house is paid off.");
    }

    @Test
    void assessWithANearOverflowMortgageBalanceAndPaymentDoesNotFail() throws Exception {
        mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("9200")
                                .monthlyEssentialExpenses("3100")
                                .starterEmergencyFund("1000")
                                .fullEmergencyFund("18000")
                                .retirementContributionPercent("15")
                                .mortgage("92233720368547748.07", "5", "92233720368547748.07")
                                .values()))
                .andExpect(status().isOk());
    }

    @Test
    void assessShowsBothMortgagePayoffDatesAndSoonerForAHomeownerWithDebts() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("6450")
                                .monthlyEssentialExpenses("2100")
                                .starterEmergencyFund("1000")
                                .fullEmergencyFund("0")
                                .retirementContributionPercent("6")
                                .mortgage("185000", "5.25", "1650")
                                .debt(0, "Store card", "CREDIT_CARD", "840", "26.99", "35")
                                .debt(1, "Car loan", "AUTO", "9140", "6.40", "312")
                                .debt(2, "Student loan", "STUDENT_LOAN", "2500", "4.50", "110")
                                .values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // asOf is fixed to September 2026 (FixedClockTestConfig); the domain computes the current-payment payoff at
        // July 2039 and the debt snowball's mortgage payoff at November 2030, 8 years 8 months sooner.
        assertThat(body).contains(escaped("At your current payment it's paid off in July 2039."));
        assertThat(body).contains(escaped(
                "With the debt snowball it's paid off in November 2030, 8 years 8 months sooner."));
    }

    @Test
    void assessShowsTheSnowballOnlyDateWhenTheCurrentPaymentDoesNotCoverInterest() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("9000")
                                .monthlyEssentialExpenses("2000")
                                .starterEmergencyFund("1000")
                                .fullEmergencyFund("7200")
                                .retirementContributionPercent("15")
                                .mortgage("300000", "6", "400")
                                .values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains(escaped("You're on stage 6 of 7"));
        assertThat(body).contains(escaped("Your $400.00 payment doesn't cover the $1,500.00 monthly interest."));
        assertThat(body).contains(escaped("With the debt snowball it's paid off in September 2030."));
        assertThat(body).doesNotContain("sooner");
    }

    @Test
    void assessForARenterShowsNoMortgagePayoffDates() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("6200")
                                .monthlyEssentialExpenses("2100")
                                .starterEmergencyFund("1000")
                                .fullEmergencyFund("0")
                                .retirementContributionPercent("6")
                                .rent("1400")
                                .values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains(escaped("You rent, so there's no mortgage to pay down."));
        assertThat(body).doesNotContain(escaped("With the debt snowball it's paid off"));
        assertThat(body).doesNotContain(escaped("At your current payment it's paid off"));
    }
}
