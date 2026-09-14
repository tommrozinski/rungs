package com.rungs.readiness.web;

import static com.rungs.readiness.web.FormParams.form;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Web tests for the snapshot form itself: the blank form, adding and removing debt rows, loading sample households,
 * and the {@code /edit} round trip back from the result page. Assessment and its result-page copy are
 * {@link AssessPageTests}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockTestConfig.class)
class FormPageTests {

    @Autowired
    private MockMvc mockMvc;

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
        assertThat(body).contains("name=\"housing\" value=\"MORTGAGE\"");
        assertThat(body).contains("id=\"housing-mortgage\"");
        assertThat(body).contains("name=\"mortgageBalance\"");
        assertThat(body).contains("name=\"mortgageRate\"");
        assertThat(body).contains("name=\"mortgagePayment\"");
        assertThat(body).contains("Your full monthly payment, including taxes and insurance.");
        assertThat(body).contains("Paid off? Enter 0 for the remaining balance");
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

        // The domain keeps the retirement percent to 2 decimals and a debt's rate to 3; a coarser step blocks the
        // browser from ever submitting e.g. 6.25% retirement or 6.125% debt rate.
        assertThat(body).contains("id=\"retirementContributionPercent\"");
        int retirementIndex = body.indexOf("id=\"retirementContributionPercent\"");
        int retirementStart = body.lastIndexOf("<input", retirementIndex);
        int retirementEnd = body.indexOf(">", retirementIndex);
        assertThat(body.substring(retirementStart, retirementEnd)).contains("step=\"0.01\"");

        int debtRateIndex = body.indexOf("debts[0].annualInterestRate");
        int debtRateStart = body.lastIndexOf("<input", debtRateIndex);
        int debtRateEnd = body.indexOf(">", debtRateIndex);
        assertThat(body.substring(debtRateStart, debtRateEnd)).contains("step=\"0.001\"");

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
    void sampleHouseholdControlsSitInsideALabeledBoxSeparateFromCheckMyStage() throws Exception {
        String body = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("Try a sample household: pick one, then load it.");

        int boxStart = body.indexOf("class=\"sample-box\"");
        assertThat(boxStart).describedAs("expected a sample-box container").isGreaterThan(-1);
        int checkStageIndex = body.indexOf("Check my stage");
        int selectIndex = body.indexOf("name=\"sample\"", boxStart);
        int loadButtonIndex = body.indexOf("formaction=\"/sample\"", boxStart);

        assertThat(checkStageIndex).isLessThan(boxStart);
        assertThat(selectIndex).isGreaterThan(boxStart);
        assertThat(loadButtonIndex).isGreaterThan(selectIndex);
    }

    @Test
    void numberInputsHaveNoMaxlengthButTheTextLabelInputStillDoes() throws Exception {
        String body = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int incomeIndex = body.indexOf("id=\"monthlyTakeHomeIncome\"");
        int incomeStart = body.lastIndexOf("<input", incomeIndex);
        int incomeEnd = body.indexOf(">", incomeIndex);
        assertThat(body.substring(incomeStart, incomeEnd)).contains("type=\"number\"").doesNotContain("maxlength");

        int labelIndex = body.indexOf("debts[0].label");
        int labelStart = body.lastIndexOf("<input", labelIndex);
        int labelEnd = body.indexOf(">", labelIndex);
        assertThat(body.substring(labelStart, labelEnd)).contains("type=\"text\"").contains("maxlength=\"60\"");
    }

    @Test
    void debtRowAndTableAccessibilityAttributesAreOnFirstRow() throws Exception {
        String body = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("aria-label=\"Debt 1 name\"");
        assertThat(body).contains("aria-label=\"Debt 1 type\"");
        assertThat(body).contains("aria-label=\"Debt 1 balance\"");
        assertThat(body).contains("aria-label=\"Debt 1 rate\"");
        assertThat(body).contains("aria-label=\"Debt 1 minimum payment\"");
        assertThat(body).contains("aria-label=\"Remove debt 1\"");
        assertThat(body).contains("<th scope=\"col\">Name</th>");
        assertThat(body).contains("aria-label=\"Choose a sample household\"");
    }

    @Test
    void addDebtKeepsTypedValuesAndAddsARow() throws Exception {
        String body = mockMvc.perform(post("/debts/add")
                        .params(form()
                                .monthlyTakeHomeIncome("6200")
                                .debt(0, "Store card", "CREDIT_CARD", "840", "26.99", "35")
                                .values()))
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
                        .params(form()
                                .debtLabel(0, "Store card")
                                .debtLabel(1, "Car loan")
                                .values()))
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
                        .params(form().debtLabel(0, "Store card").values()))
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
        MockHttpServletRequestBuilder request = post("/debts/add");
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
    void resultPageHasBothButtonsAndAHiddenCopyOfEveryFormField() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("6200")
                                .monthlyEssentialExpenses("2100")
                                .starterEmergencyFund("1000")
                                .fullEmergencyFund("0")
                                .retirementContributionPercent("6")
                                .rent("1400")
                                .intendsToFundCollege()
                                .retired()
                                .debt(0, "Store card", "CREDIT_CARD", "840", "26.99", "35")
                                .values()))
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
                        .params(form().monthlyTakeHomeIncome("6200").housing("RENT").values()))
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
                        .params(form()
                                .monthlyTakeHomeIncome("7800")
                                .mortgage("285000", "6.5", "2269")
                                .intendsToFundCollege()
                                .debtLabel(0, "Store card")
                                .debtBalance(0, "840")
                                .values()))
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
    void cssIsServedAtItsPath() throws Exception {
        mockMvc.perform(get("/css/rungs.css"))
                .andExpect(status().isOk());
    }

    @Test
    void tokensCssIsServedAndDeclaresTheInterFontFace() throws Exception {
        String body = mockMvc.perform(get("/css/tokens.css"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("@font-face");
        assertThat(body).contains("font-family: \"Inter\"");
        assertThat(body).contains("/fonts/InterVariable.woff2");
        assertThat(body).contains("font-display: swap");
    }

    @Test
    void formPageLinksTokensCssBeforeItsOwnStylesheet() throws Exception {
        String body = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        int tokensIndex = body.indexOf("/css/tokens.css");
        int rungsIndex = body.indexOf("/css/rungs.css");
        assertThat(tokensIndex).isGreaterThan(-1);
        assertThat(tokensIndex).isLessThan(rungsIndex);
    }

    @Test
    void addDebtKeepsTheHousingModeAndMortgageValues() throws Exception {
        String body = mockMvc.perform(post("/debts/add")
                        .params(form().mortgage("285000", "6.5", "2269").values()))
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
                        .params(form()
                                .rent("1400")
                                .debtLabel(0, "Store card")
                                .debtLabel(1, "Car loan")
                                .values()))
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
}
