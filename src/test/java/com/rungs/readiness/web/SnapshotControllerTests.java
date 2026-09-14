package com.rungs.readiness.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SnapshotControllerTests {

    /** Pins {@code asOf} to September 2026 (design.md 039 section 4's sample households are computed against it). */
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
        assertThat(body).doesNotContain("name=\"rents\"");
        assertThat(body).contains("name=\"housing\" value=\"MORTGAGE\"");
        assertThat(body).contains("id=\"housing-mortgage\"");
        assertThat(body).contains("name=\"mortgageBalance\"");
        assertThat(body).contains("name=\"mortgageRate\"");
        assertThat(body).contains("name=\"mortgagePayment\"");
        assertThat(body).doesNotContain("name=\"annualPropertyTaxes\"");
        assertThat(body).contains("Your full monthly payment, including taxes and insurance.");
        assertThat(body).contains("Paid off? Enter 0 for the remaining balance, and enter your property taxes "
                + "and insurance as the monthly payment, since those still come out of your take-home pay each "
                + "month.");
        assertThat(body).doesNotContain("name=\"maturityMonth\"");
        assertThat(body).doesNotContain("name=\"maturityYear\"");
        assertThat(body).contains("name=\"monthlyRent\"");
        assertThat(body).contains("name=\"intendsToFundCollege\"");
        assertThat(body).contains("name=\"retired\"");
        assertThat(body).contains("debts[0].label");
        assertThat(body).doesNotContain("debts[1].label");
        assertThat(body).doesNotContain("<script");

        // The mortgage radio is checked by default, and the mortgage fields are empty (no mortgage yet entered).
        int mortgageRadioIndex = body.indexOf("id=\"housing-mortgage\"");
        int mortgageRadioEnd = body.indexOf(">", mortgageRadioIndex);
        assertThat(body.substring(mortgageRadioIndex, mortgageRadioEnd)).contains("checked");
        assertThat(body).contains("name=\"mortgageBalance\" value=\"\"");

        assertThat(body).contains("<select name=\"sample\"");
        assertThat(body).contains("Choose a sample household");
        assertThat(body).contains("Buried in debt");
        assertThat(body).contains("Paying down debt");
        assertThat(body).contains("Debt-free, building savings");
        assertThat(body).contains("Paying off the house");
        assertThat(body).contains("Paid-off home, no debt");
        assertThat(body).contains("formaction=\"/sample\" formnovalidate");
        assertThat(body).contains("Load sample data");
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
    void editWithLargeDebtIndexDoesNotFail() throws Exception {
        mockMvc.perform(post("/edit")
                        .param("debts[99999].label", "Anything"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void breakdownWithLargeDebtIndexDoesNotFail() throws Exception {
        mockMvc.perform(post("/breakdown")
                        .param("debts[99999].label", "Anything"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void addDebtAtTwentyRowsDoesNotAddAFurtherRow() throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request = post("/debts/add");
        for (int i = 0; i < 20; i++) {
            request.param("debts[" + i + "].label", "Debt " + i);
        }

        String body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("debts[19].label");
        assertThat(body).doesNotContain("debts[20].label");
    }

    @Test
    void assessRunsTheRealSevenStageAssessmentForTheBoardHousehold() throws Exception {
        String body = mockMvc.perform(post("/assess")
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
        assertThat(body).contains(
                "With the debt snowball you're free of everything but the house by January 2027, "
                        + "about 5 months from now.");
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
    void resultPageHasBothButtonsAndAHiddenCopyOfEveryFormField() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "6200")
                        .param("monthlyEssentialExpenses", "2100")
                        .param("starterEmergencyFund", "1000")
                        .param("fullEmergencyFund", "0")
                        .param("retirementContributionPercent", "6")
                        .param("housing", "RENT")
                        .param("monthlyRent", "1400")
                        .param("intendsToFundCollege", "true")
                        .param("retired", "true")
                        .param("debts[0].label", "Store card")
                        .param("debts[0].type", "CREDIT_CARD")
                        .param("debts[0].balance", "840")
                        .param("debts[0].annualInterestRate", "26.99")
                        .param("debts[0].minimumPayment", "35"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("formaction=\"/edit\"");
        assertThat(body).contains("Change my numbers");
        assertThat(body).contains("formaction=\"/assess#breakdown\"");
        assertThat(body).contains("name=\"breakdown\" value=\"true\"");
        assertThat(body).contains("Show payoff schedule");

        assertThat(body).contains("name=\"monthlyTakeHomeIncome\" value=\"6200\"");
        assertThat(body).contains("name=\"monthlyEssentialExpenses\" value=\"2100\"");
        assertThat(body).contains("name=\"starterEmergencyFund\" value=\"1000\"");
        assertThat(body).contains("name=\"fullEmergencyFund\" value=\"0\"");
        assertThat(body).contains("name=\"retirementContributionPercent\" value=\"6\"");
        assertThat(body).contains("name=\"housing\" value=\"RENT\"");
        assertThat(body).contains("name=\"monthlyRent\" value=\"1400\"");
        assertThat(body).contains("name=\"mortgageBalance\"");
        assertThat(body).contains("name=\"mortgageRate\"");
        assertThat(body).contains("name=\"mortgagePayment\"");
        assertThat(body).doesNotContain("name=\"maturityMonth\"");
        assertThat(body).doesNotContain("name=\"maturityYear\"");
        assertThat(body).contains("name=\"intendsToFundCollege\" value=\"true\"");
        assertThat(body).contains("name=\"retired\" value=\"true\"");
        assertThat(body).contains("name=\"debts[0].label\" value=\"Store card\"");
        assertThat(body).contains("name=\"debts[0].type\" value=\"CREDIT_CARD\"");
        assertThat(body).contains("name=\"debts[0].balance\" value=\"840\"");
        assertThat(body).contains("name=\"debts[0].annualInterestRate\" value=\"26.99\"");
        assertThat(body).contains("name=\"debts[0].minimumPayment\" value=\"35\"");

        assertThat(body).doesNotContain("id=\"breakdown\"");
    }

    @Test
    void resultPageOmitsUncheckedCheckboxHiddenFields() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "6200")
                        .param("housing", "RENT"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("name=\"intendsToFundCollege\" value=\"true\"");
        assertThat(body).doesNotContain("name=\"retired\" value=\"true\"");
    }

    @Test
    void editRestoresEveryValueIncludingHousingModeAndDebtRows() throws Exception {
        String body = mockMvc.perform(post("/edit")
                        .param("monthlyTakeHomeIncome", "7800")
                        .param("housing", "MORTGAGE")
                        .param("mortgageBalance", "285000")
                        .param("mortgageRate", "6.5")
                        .param("mortgagePayment", "2269")
                        .param("intendsToFundCollege", "true")
                        .param("debts[0].label", "Store card")
                        .param("debts[0].balance", "840"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("<form");
        assertThat(body).contains("action=\"/assess\"");
        assertThat(body).contains("name=\"monthlyTakeHomeIncome\" value=\"7800\"");
        assertThat(body).contains("name=\"mortgageBalance\" value=\"285000\"");
        assertThat(body).contains("name=\"mortgagePayment\" value=\"2269\"");
        assertThat(body).doesNotContain("name=\"maturityMonth\"");
        assertThat(body).doesNotContain("name=\"maturityYear\"");
        assertThat(body).contains("value=\"Store card\"");
        int mortgageRadioIndex = body.indexOf("id=\"housing-mortgage\"");
        int mortgageRadioEnd = body.indexOf(">", mortgageRadioIndex);
        assertThat(body.substring(mortgageRadioIndex, mortgageRadioEnd)).contains("checked");
        int collegeCheckboxIndex = body.indexOf("id=\"intendsToFundCollege\"");
        int collegeCheckboxEnd = body.indexOf(">", collegeCheckboxIndex);
        assertThat(body.substring(collegeCheckboxIndex, collegeCheckboxEnd)).contains("checked");
    }

    @Test
    void editWithHostileHiddenValuesNeverFails() throws Exception {
        mockMvc.perform(post("/edit")
                        .param("monthlyTakeHomeIncome", "1e-999999999")
                        .param("debts[0].balance", "not-a-number"))
                .andExpect(status().isOk());
    }

    @Test
    void assessShowsTheProgressBarInsideTheOpenCardForAnInProgressCurrentStage() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "4000")
                        .param("monthlyEssentialExpenses", "2000")
                        .param("starterEmergencyFund", "250")
                        .param("fullEmergencyFund", "0")
                        .param("retirementContributionPercent", "0")
                        .param("housing", "RENT"))
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
                        .param("housing", "RENT"))
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

    @Test
    void addDebtKeepsTheHousingModeAndMortgageValues() throws Exception {
        String body = mockMvc.perform(post("/debts/add")
                        .param("housing", "MORTGAGE")
                        .param("mortgageBalance", "285000")
                        .param("mortgageRate", "6.5")
                        .param("mortgagePayment", "2269"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("name=\"mortgageBalance\" value=\"285000\"");
        assertThat(body).contains("name=\"mortgageRate\" value=\"6.5\"");
        assertThat(body).contains("name=\"mortgagePayment\" value=\"2269\"");
        assertThat(body).doesNotContain("name=\"maturityMonth\"");
        assertThat(body).doesNotContain("name=\"maturityYear\"");
        int mortgageRadioIndex = body.indexOf("id=\"housing-mortgage\"");
        int mortgageRadioEnd = body.indexOf(">", mortgageRadioIndex);
        assertThat(body.substring(mortgageRadioIndex, mortgageRadioEnd)).contains("checked");
    }

    @Test
    void removeDebtKeepsTheHousingModeAndRentValue() throws Exception {
        String body = mockMvc.perform(post("/debts/remove/0")
                        .param("housing", "RENT")
                        .param("monthlyRent", "1400")
                        .param("debts[0].label", "Store card")
                        .param("debts[1].label", "Car loan"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("name=\"monthlyRent\" value=\"1400\"");
        int rentRadioIndex = body.indexOf("id=\"housing-rent\"");
        int rentRadioEnd = body.indexOf(">", rentRadioIndex);
        assertThat(body.substring(rentRadioIndex, rentRadioEnd)).contains("checked");
    }

    @Test
    void loadSampleFillsTheFormWithTheBuriedHousehold() throws Exception {
        String body = mockMvc.perform(post("/sample").param("sample", "buried"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("name=\"monthlyTakeHomeIncome\" value=\"3800\"");
        assertThat(body).contains("name=\"monthlyRent\" value=\"1250\"");
        assertThat(body).contains("value=\"Credit card\"");
        assertThat(body).contains("<option value=\"buried\" selected");
    }

    @Test
    void loadSampleFillsTheFormWithTheBoardHousehold() throws Exception {
        String body = mockMvc.perform(post("/sample").param("sample", "board"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("name=\"monthlyTakeHomeIncome\" value=\"6200\"");
        assertThat(body).contains("name=\"monthlyRent\" value=\"1400\"");
        assertThat(body).contains("value=\"Student loan\"");
        assertThat(body).contains("<option value=\"board\" selected");
    }

    @Test
    void loadSampleFillsTheFormWithTheDebtFreeHousehold() throws Exception {
        String body = mockMvc.perform(post("/sample").param("sample", "debt-free"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("name=\"mortgageBalance\" value=\"285000\"");
        assertThat(body).contains("<option value=\"debt-free\" selected");
    }

    @Test
    void loadSampleFillsTheFormWithTheHomestretchHousehold() throws Exception {
        String body = mockMvc.perform(post("/sample").param("sample", "homestretch"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("name=\"mortgageBalance\" value=\"198000\"");
        assertThat(body).contains("<option value=\"homestretch\" selected");
    }

    @Test
    void loadSampleFillsTheFormWithThePaidOffHousehold() throws Exception {
        String body = mockMvc.perform(post("/sample").param("sample", "paid-off"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("name=\"mortgageBalance\" value=\"0\"");
        assertThat(body).contains("name=\"mortgagePayment\" value=\"650\"");
        assertThat(body).contains("<option value=\"paid-off\" selected");
    }

    @Test
    void loadUnknownSampleReRendersThePostedFormUnchanged() throws Exception {
        String body = mockMvc.perform(post("/sample")
                        .param("sample", "not-a-real-sample")
                        .param("monthlyTakeHomeIncome", "4321"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("name=\"monthlyTakeHomeIncome\" value=\"4321\"");
    }

    @Test
    void loadBlankSampleReRendersThePostedFormUnchangedAndNeverFails() throws Exception {
        mockMvc.perform(post("/sample").param("monthlyTakeHomeIncome", "4321"))
                .andExpect(status().isOk());
    }

    @Test
    void assessAssignsTheBuriedSampleHouseholdToStageOne() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "3800")
                        .param("monthlyEssentialExpenses", "2100")
                        .param("starterEmergencyFund", "150")
                        .param("fullEmergencyFund", "0")
                        .param("retirementContributionPercent", "0")
                        .param("housing", "RENT")
                        .param("monthlyRent", "1250")
                        .param("debts[0].label", "Credit card")
                        .param("debts[0].type", "CREDIT_CARD")
                        .param("debts[0].balance", "6400")
                        .param("debts[0].annualInterestRate", "24.9")
                        .param("debts[0].minimumPayment", "190")
                        .param("debts[1].label", "Car loan")
                        .param("debts[1].type", "AUTO")
                        .param("debts[1].balance", "14200")
                        .param("debts[1].annualInterestRate", "9.5")
                        .param("debts[1].minimumPayment", "385")
                        .param("debts[2].label", "Medical bill")
                        .param("debts[2].type", "MEDICAL")
                        .param("debts[2].balance", "2300")
                        .param("debts[2].annualInterestRate", "0")
                        .param("debts[2].minimumPayment", "75")
                        .param("debts[3].label", "Personal loan")
                        .param("debts[3].type", "PERSONAL")
                        .param("debts[3].balance", "4800")
                        .param("debts[3].annualInterestRate", "17")
                        .param("debts[3].minimumPayment", "160"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("You're on stage 1 of 7");
    }

    @Test
    void assessAssignsTheDebtFreeSampleHouseholdToStageThree() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "7800")
                        .param("monthlyEssentialExpenses", "2600")
                        .param("starterEmergencyFund", "1000")
                        .param("fullEmergencyFund", "6000")
                        .param("retirementContributionPercent", "4")
                        .param("housing", "MORTGAGE")
                        .param("mortgageBalance", "285000")
                        .param("mortgageRate", "6.5")
                        .param("mortgagePayment", "2269"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("You're on stage 3 of 7");
    }

    @Test
    void assessAssignsTheHomestretchSampleHouseholdToStageSixAndShowsTheSnowballsEarlierPayoffDate() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "11500")
                        .param("monthlyEssentialExpenses", "3400")
                        .param("starterEmergencyFund", "1000")
                        .param("fullEmergencyFund", "17000")
                        .param("retirementContributionPercent", "15")
                        .param("housing", "MORTGAGE")
                        .param("mortgageBalance", "198000")
                        .param("mortgageRate", "3.25")
                        .param("mortgagePayment", "1900"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("You're on stage 6 of 7");
        assertThat(body).contains("At your current payment it's paid off in");
        assertThat(body).contains(
                "With the debt snowball it's paid off in October 2028, 8 years 2 months sooner than your payment "
                        + "alone.");
    }

    @Test
    void assessAssignsThePaidOffSampleHouseholdToStageSeven() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "9200")
                        .param("monthlyEssentialExpenses", "3100")
                        .param("starterEmergencyFund", "1000")
                        .param("fullEmergencyFund", "18000")
                        .param("retirementContributionPercent", "15")
                        .param("housing", "MORTGAGE")
                        .param("mortgageBalance", "0")
                        .param("mortgageRate", "0")
                        .param("mortgagePayment", "0"))
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
                        .param("monthlyTakeHomeIncome", "9200")
                        .param("monthlyEssentialExpenses", "3100")
                        .param("starterEmergencyFund", "1000")
                        .param("fullEmergencyFund", "18000")
                        .param("retirementContributionPercent", "15")
                        .param("housing", "MORTGAGE")
                        .param("mortgageBalance", "92233720368547748.07")
                        .param("mortgageRate", "5")
                        .param("mortgagePayment", "92233720368547748.07"))
                .andExpect(status().isOk());
    }

    @Test
    void assessShowsBothMortgagePayoffDatesAndSoonerForAHomeownerWithDebts() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "6450")
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
                        .param("debts[2].minimumPayment", "110"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // asOf is fixed to September 2026 (FixedClockConfig); the domain computes the current-payment payoff at
        // August 2039 and the debt snowball's mortgage payoff at November 2030, 8 years 9 months sooner.
        assertThat(body).contains("At your current payment it's paid off in August 2039.");
        assertThat(body).contains(
                "With the debt snowball it's paid off in November 2030, 8 years 9 months sooner.");
    }

    @Test
    void assessShowsTheSnowballOnlyDateWhenTheCurrentPaymentDoesNotCoverInterest() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "9000")
                        .param("monthlyEssentialExpenses", "2000")
                        .param("starterEmergencyFund", "1000")
                        .param("fullEmergencyFund", "7200")
                        .param("retirementContributionPercent", "15")
                        .param("housing", "MORTGAGE")
                        .param("mortgageBalance", "300000")
                        .param("mortgageRate", "6")
                        .param("mortgagePayment", "400"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("You're on stage 6 of 7");
        assertThat(body).contains("Your $400.00 payment doesn't cover the $1,500.00 monthly interest.");
        assertThat(body).contains("With the debt snowball it's paid off in September 2030.");
        assertThat(body).doesNotContain("sooner");
    }

    @Test
    void assessForARenterShowsNoMortgagePayoffDates() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "6200")
                        .param("monthlyEssentialExpenses", "2100")
                        .param("starterEmergencyFund", "1000")
                        .param("fullEmergencyFund", "0")
                        .param("retirementContributionPercent", "6")
                        .param("housing", "RENT")
                        .param("monthlyRent", "1400"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("You rent, so there's no mortgage to pay down.");
        assertThat(body).doesNotContain("With the debt snowball it's paid off");
        assertThat(body).doesNotContain("At your current payment it's paid off");
    }
}
