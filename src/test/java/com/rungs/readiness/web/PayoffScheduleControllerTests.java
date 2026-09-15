package com.rungs.readiness.web;

import static com.rungs.readiness.web.FormParams.form;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.HtmlUtils;

/**
 * Web tests for the "Show payoff schedule" flow: the result page's two inline breakdown iframes, "Your loans" and
 * "Payoff schedule".
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockTestConfig.class)
class PayoffScheduleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    private static MockHttpServletRequestBuilder boardWithMortgage(String path) {
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

    /** Extracts the raw (still HTML-attribute-escaped) {@code srcdoc} of the {@code <iframe title="title">} tag,
     * and everything in the page after it, from {@code body}. */
    private static String extractSrcdoc(String body, String title) {
        int titleIndex = body.indexOf("title=\"" + title + "\"");
        assertThat(titleIndex).describedAs("expected an iframe titled '%s'", title).isGreaterThan(-1);
        String fromTitle = body.substring(titleIndex);
        int srcdocStart = fromTitle.indexOf("srcdoc=\"") + "srcdoc=\"".length();
        int srcdocEnd = fromTitle.indexOf("\"></iframe>", srcdocStart);
        return fromTitle.substring(srcdocStart, srcdocEnd);
    }

    private static int extractLoansIframeHeightPx(String body) {
        int titleIndex = body.indexOf("title=\"Your loans\"");
        assertThat(titleIndex).isGreaterThan(-1);
        // The rendered attribute is a plain style="height:NNpx" — find it within this <iframe ...> tag.
        int iframeStart = body.lastIndexOf("<iframe", titleIndex);
        int iframeEnd = body.indexOf(">", titleIndex);
        String iframeTag = body.substring(iframeStart, iframeEnd);
        int heightIndex = iframeTag.indexOf("height:");
        assertThat(heightIndex).describedAs("expected a height:NNpx style on the loans iframe: %s", iframeTag)
                .isGreaterThan(-1);
        int pxIndex = iframeTag.indexOf("px", heightIndex);
        return Integer.parseInt(iframeTag.substring(heightIndex + "height:".length(), pxIndex));
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
    void assessWithBreakdownTrueRendersTwoSeparateIframesForLoansAndSchedule() throws Exception {
        String body = mockMvc.perform(boardWithMortgage("/assess").param("breakdown", "true"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("title=\"Your loans\"");
        assertThat(body).contains("title=\"Payoff schedule\"");

        int loansTitleIndex = body.indexOf("title=\"Your loans\"");
        int scheduleTitleIndex = body.indexOf("title=\"Payoff schedule\"");
        assertThat(loansTitleIndex).isGreaterThan(-1);
        assertThat(scheduleTitleIndex).isGreaterThan(loansTitleIndex);

        String loansSrcdoc = extractSrcdoc(body, "Your loans");
        assertThat(loansSrcdoc).contains("Min payment");
        assertThat(loansSrcdoc).contains("Store card");
        assertThat(loansSrcdoc).contains("Mortgage");
        assertThat(loansSrcdoc).doesNotContain("Cum int");
        assertThat(loansSrcdoc).doesNotContain("Cascade");

        String scheduleSrcdoc = extractSrcdoc(body, "Payoff schedule");
        assertThat(scheduleSrcdoc).contains("Cum int");
        assertThat(scheduleSrcdoc).contains("Cascade");
        assertThat(scheduleSrcdoc).doesNotContain("Min payment");
    }

    @Test
    void loansIframeHeightForOneLoanIsExactlyThreeRowsPlusChrome() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "6200")
                        .param("housing", "RENT")
                        .param("monthlyRent", "1400")
                        .param("debts[0].label", "Store card")
                        .param("debts[0].balance", "840")
                        .param("debts[0].minimumPayment", "35")
                        .param("breakdown", "true"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 1 loan: header + loan + total = 3 rows.
        assertThat(extractLoansIframeHeightPx(body)).isEqualTo(3 * 29 + 28);
    }

    @Test
    void scheduleIframeFitsAShortScheduleAndCapsALongOneAtSeventyPercentOfTheWindow() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "6200")
                        .param("housing", "RENT")
                        .param("monthlyRent", "1400")
                        .param("debts[0].label", "Store card")
                        .param("debts[0].balance", "840")
                        .param("debts[0].minimumPayment", "35")
                        .param("breakdown", "true"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // The surplus clears $840 in its first month: header + 1 month + totals = 3 rows.
        assertThat(body).contains("height: min(70vh, " + (3 * 29 + 48) + "px)");
    }

    @Test
    void loansIframeHeightForFourLoansIsExactlySixRowsPlusChrome() throws Exception {
        String body = mockMvc.perform(boardWithMortgage("/assess").param("breakdown", "true"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 3 debts + the mortgage = 4 loans: header + 4 loan rows + total = 6 rows.
        assertThat(extractLoansIframeHeightPx(body)).isEqualTo(6 * 29 + 28);
    }

    @Test
    void assessWithBreakdownTrueRendersTheIframeSrcdocsWithTheSchedule() throws Exception {
        String body = mockMvc.perform(boardWithMortgage("/assess").param("breakdown", "true"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("id=\"breakdown\"");
        assertThat(body).contains("class=\"breakdown-frame\"");
        assertThat(body).contains("srcdoc=\"");
        assertThat(body).contains("Store card");
        assertThat(body).contains("Car loan");
        assertThat(body).contains("Student loan");
        assertThat(body).contains("Mortgage");
        assertThat(body).doesNotContain("<script");

        String scheduleSrcdoc = extractSrcdoc(body, "Payoff schedule");
        assertThat(scheduleSrcdoc).contains("Cum int");
        assertThat(scheduleSrcdoc).contains("Sep 2026");
    }

    @Test
    void breakdownTableHeadersHaveScopeAttributes() throws Exception {
        String body = mockMvc.perform(boardWithMortgage("/assess").param("breakdown", "true"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String loansSrcdoc = HtmlUtils.htmlUnescape(extractSrcdoc(body, "Your loans"));
        String scheduleSrcdoc = HtmlUtils.htmlUnescape(extractSrcdoc(body, "Payoff schedule"));

        assertThat(loansSrcdoc).contains("<th scope=\"col\">Loan</th>");
        assertThat(loansSrcdoc).contains("<th scope=\"row\">TOTAL</th>");
        assertThat(scheduleSrcdoc).contains("<th scope=\"col\">Month</th>");
        assertThat(scheduleSrcdoc).contains("<th scope=\"row\">TOTAL</th>");
    }

    @Test
    void resultPageNoLongerRepeatsTheScheduleTitleAndShowsTheSubtitleOnce() throws Exception {
        String body = mockMvc.perform(boardWithMortgage("/assess").param("breakdown", "true"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("Your surplus goes to the smallest debt");
        assertThat(body).doesNotContain("Payoff schedule — debt snowball");

        String scheduleSrcdoc = extractSrcdoc(body, "Payoff schedule");
        assertThat(scheduleSrcdoc).doesNotContain("debt snowball");
        assertThat(scheduleSrcdoc).doesNotContain("Your surplus goes to the smallest debt");
    }

    @Test
    void breakdownRendersTheFirstMonthFiguresAndLoanNamesInPayoffOrder() throws Exception {
        String body = mockMvc.perform(boardWithMortgage("/assess").param("breakdown", "true"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // The result page's hidden form fields echo the debts in submitted order, so the payoff order below is
        // checked within the "Your loans" iframe document (which lists loans in payoff order), not the whole page.
        String loansDoc = HtmlUtils.htmlUnescape(extractSrcdoc(body, "Your loans"));
        String scheduleDoc = HtmlUtils.htmlUnescape(extractSrcdoc(body, "Payoff schedule"));

        // Debt snowball order: smallest starting balance first — Store card $840, Student loan $2,500,
        // Car loan $9,140 — then the mortgage last.
        int storeIndex = loansDoc.indexOf("Store card");
        int studentIndex = loansDoc.indexOf("Student loan");
        int carIndex = loansDoc.indexOf("Car loan");
        int mortgageIndex = loansDoc.indexOf(">Mortgage<");
        assertThat(storeIndex).isGreaterThan(-1);
        assertThat(storeIndex).isLessThan(studentIndex);
        assertThat(studentIndex).isLessThan(carIndex);
        assertThat(carIndex).isLessThan(mortgageIndex);

        assertThat(loansDoc).contains("$840.00");
        assertThat(scheduleDoc).contains("$18.89");
        assertThat(scheduleDoc).contains("$858.89");
    }

    @Test
    void renterHouseholdHasNoMortgageColumn() throws Exception {
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
                        .param("breakdown", "true"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("Mortgage");
    }

    @Test
    void noLoansRendersTheEmptyMessageAndNoIframes() throws Exception {
        String body = mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "6200")
                        .param("housing", "RENT")
                        .param("breakdown", "true"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("id=\"breakdown\"");
        assertThat(body).contains("No debts or mortgage to pay down.");
        assertThat(body).doesNotContain("<iframe");
        assertThat(body).doesNotContain("srcdoc=");
    }

    @Test
    void hostileDebtLabelIsEscapedEverywhereAndNeverProducesAnExecutableScriptTag() throws Exception {
        String hostileLabel = "\"><script>alert(1)</script>&quot;";

        String body = mockMvc.perform(post("/assess")
                        .params(form()
                                .monthlyTakeHomeIncome("6200")
                                .debtLabel(0, hostileLabel)
                                .debtBalance(0, "840")
                                .param("breakdown", "true")
                                .values()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body.toLowerCase(Locale.ROOT)).doesNotContain("<script");

        String loansDoc = HtmlUtils.htmlUnescape(extractSrcdoc(body, "Your loans"));
        String scheduleDoc = HtmlUtils.htmlUnescape(extractSrcdoc(body, "Payoff schedule"));

        assertThat(loansDoc.toLowerCase(Locale.ROOT)).doesNotContain("<script");
        assertThat(scheduleDoc.toLowerCase(Locale.ROOT)).doesNotContain("<script");
        // The label reaches the inner document with its own dangerous characters still escaped as element text.
        assertThat(loansDoc).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    // 20 debts (the row cap), each with a minimum a little below its own monthly interest (like the
    // calculator's own 600-cap test), so all 20 stay open and the schedule renders the full 600 months — the
    // largest a result page can be, now that it carries both breakdown iframes (each escaped once more as an
    // attribute value) alongside the seven-stage ladder and the hidden form copy.
    @Test
    void theTwentyDebtRowCapKeepsTheSixHundredMonthResponseSizeBounded() throws Exception {
        MockHttpServletRequestBuilder request = post("/assess")
                .param("monthlyTakeHomeIncome", "0")
                .param("housing", "RENT")
                .param("monthlyRent", "0")
                .param("breakdown", "true");
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

        assertThat(new String(body, StandardCharsets.UTF_8))
                .contains("This schedule stops at 600 months.");
        assertThat(body.length).isLessThan(2_600_000);
    }

    @Test
    void breakdownWithHostileHiddenValuesNeverFails() throws Exception {
        mockMvc.perform(post("/assess")
                        .param("monthlyTakeHomeIncome", "1e-999999999")
                        .param("debts[0].balance", "not-a-number")
                        .param("mortgageBalance", "not-a-number")
                        .param("housing", "MORTGAGE")
                        .param("breakdown", "true"))
                .andExpect(status().isOk());
    }
}
