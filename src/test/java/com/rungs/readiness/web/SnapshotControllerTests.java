package com.rungs.readiness.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SnapshotControllerTests {

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

    @Test
    void getRootRendersEmptyFormWithOneDebtRow() throws Exception {
        String body = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("<form");
        assertThat(body).contains("action=\"/assess\"");
        assertThat(body).contains("name=\"monthlyTakeHomeIncome\"");
        assertThat(body).contains("name=\"monthlyEssentialExpenses\"");
        assertThat(body).contains("name=\"starterEmergencyFund\"");
        assertThat(body).contains("name=\"fullEmergencyFund\"");
        assertThat(body).contains("name=\"retirementContributionPercent\"");
        assertThat(body).contains("name=\"rents\"");
        assertThat(body).contains("name=\"intendsToFundCollege\"");
        assertThat(body).contains("name=\"retired\"");
        assertThat(body).contains("debts[0].label");
        assertThat(body).doesNotContain("debts[1].label");
        assertThat(body).doesNotContain("<script");
    }

    @Test
    void addDebtKeepsTypedValuesAndAddsARow() throws Exception {
        String body = mockMvc.perform(post("/debts/add")
                        .param("monthlyTakeHomeIncome", "6200")
                        .param("debts[0].label", "Store card")
                        .param("debts[0].type", "CREDIT_CARD")
                        .param("debts[0].balance", "840")
                        .param("debts[0].annualInterestRate", "26.99")
                        .param("debts[0].minimumPayment", "35"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("value=\"6200\"");
        assertThat(body).contains("value=\"Store card\"");
        assertThat(body).contains("value=\"840\"");
        assertThat(body).contains("value=\"26.99\"");
        assertThat(body).contains("value=\"35\"");
        assertThat(body).contains("debts[1].label");
    }

    @Test
    void removeDropsTheRightRowAndKeepsOthers() throws Exception {
        String body = mockMvc.perform(post("/debts/remove/0")
                        .param("debts[0].label", "Store card")
                        .param("debts[1].label", "Car loan"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("value=\"Car loan\"");
        assertThat(body).doesNotContain("value=\"Store card\"");
        assertThat(body).contains("debts[0].label");
        assertThat(body).doesNotContain("debts[1].label");
    }

    @Test
    void removeWithOutOfRangeIndexDoesNotFail() throws Exception {
        mockMvc.perform(post("/debts/remove/99")
                        .param("debts[0].label", "Store card"))
                .andExpect(status().isOk());
    }

    @Test
    void largeDebtIndexDoesNotFail() throws Exception {
        mockMvc.perform(post("/assess")
                        .param("debts[99999].label", "Anything"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void assessRunsTheRealSevenStageAssessmentForTheBoardHousehold() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "6200")
                        .param("monthlyEssentialExpenses", "3500")
                        .param("starterEmergencyFund", "1000")
                        .param("fullEmergencyFund", "0")
                        .param("retirementContributionPercent", "6")
                        .param("rents", "true")
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
                        .param("debts[2].minimumPayment", "110"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("class=\"banner\"");
        assertThat(body).doesNotContain("Smallest first");
        assertThat(body).doesNotContain("41% paid down");
        assertThat(body).doesNotContain("See the payoff plan");

        assertThat(body).contains("You're on stage 2 of 7");
        assertThat(body).contains("$12,480 stands between you and debt freedom.");
        assertThat(body).contains("At your current surplus of $2,243 a month, that's about 6 months of work.");
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
        assertThat(body).contains("You're at 6%. Nine points to go.");
        assertThat(body).contains("You told us you're not funding college.");
        assertThat(body).contains("You rent, so there's no mortgage to pay down.");
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
                        .param("monthlyTakeHomeIncome", "4000")
                        .param("monthlyEssentialExpenses", "2000")
                        .param("starterEmergencyFund", "250")
                        .param("fullEmergencyFund", "0")
                        .param("retirementContributionPercent", "0")
                        .param("rents", "true"))
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
                        .param("monthlyTakeHomeIncome", "4000")
                        .param("monthlyEssentialExpenses", "2500")
                        .param("starterEmergencyFund", "400")
                        .param("fullEmergencyFund", "0")
                        .param("retirementContributionPercent", "0"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("You're on stage 1 of 7");
        assertThat(body).contains("$600 more and your starter fund is done.");
        assertThat(body).contains("Save $1,000 before anything else.");
    }

    @Test
    void assessReportsAllStagesDoneForAHouseholdThatHasClearedEveryStage() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "6000")
                        .param("monthlyEssentialExpenses", "3000")
                        .param("starterEmergencyFund", "1000")
                        .param("fullEmergencyFund", "9000")
                        .param("retirementContributionPercent", "15")
                        .param("rents", "true"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("All seven stages are done or skipped");
        assertThat(body).contains("Build wealth and give.");
        assertThat(body).contains("5 of 7");
    }

    @Test
    void assessWithHostileScientificNotationInputsStillReturnsOk() throws Exception {
        mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "6200")
                        .param("monthlyEssentialExpenses", "3500")
                        .param("retirementContributionPercent", "1e-999999999")
                        .param("debts[0].label", "Store card")
                        .param("debts[0].balance", "840")
                        .param("debts[0].annualInterestRate", "1e-999999999")
                        .param("debts[0].minimumPayment", "35"))
                .andExpect(status().isOk());
    }

    @Test
    void cssIsServedAtItsPath() throws Exception {
        mockMvc.perform(get("/css/rungs.css"))
                .andExpect(status().isOk());
    }
}
