package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** S-01, the snapshot form screen (story 027). */
@SpringBootTest
@AutoConfigureMockMvc
class SnapshotFormControllerTests {

    private final MockMvcTester mvc;

    @Autowired
    SnapshotFormControllerTests(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void getRendersAnEmptyFormWithSubheadAndButtonCopy() {
        MvcTestResult result = mvc.get().uri("/").exchange();

        assertThat(result).hasStatusOk().hasContentTypeCompatibleWith(MediaType.TEXT_HTML);
        Document page = document(result);
        assertThat(page.selectFirst("h1").text()).isEqualTo("Check your stage");
        assertThat(page.body().text()).contains("Nothing you enter is stored.");
        assertThat(page.select("button:contains(Check my stage)")).isNotEmpty();
        assertThat(page.select("button:contains(Add a debt)")).isNotEmpty();
        assertThat(page.select("label[for=monthlyTakeHomeIncome]")).hasSize(1);
        assertThat(page.select("input#monthlyTakeHomeIncome").attr("value")).isEqualTo("0");
        assertThat(page.select("input#emergencyFundMonths").attr("value")).isEqualTo("3");
        assertThat(page.select("script[src=/js/vendor/htmx.min.js]")).hasSize(1);
        assertThat(page.selectFirst("#mortgage-section").hasClass("is-hidden")).isFalse();
    }

    @Test
    void postReRendersWithAnErrorTiedByAriaDescribedbyAndAC09Summary() {
        MvcTestResult result = mvc.post().uri("/").param("monthlyTakeHomeIncome", "-1").exchange();

        assertThat(result).hasStatus(200);
        Document page = document(result);
        Element input = page.selectFirst("input#monthlyTakeHomeIncome");
        assertThat(input.attr("aria-invalid")).isEqualTo("true");
        String errorId = input.attr("aria-describedby");
        assertThat(errorId).isNotBlank();
        assertThat(page.getElementById(errorId).text()).isEqualTo("Enter zero or more.");
        assertThat(page.body().text()).contains("1 field needs attention.");
    }

    @Test
    void postWithNoViolationsHasNoSummaryOrErrorState() {
        MvcTestResult result = mvc.post().uri("/").param("monthlyTakeHomeIncome", "5000").exchange();

        Document page = document(result);
        assertThat(page.selectFirst("input#monthlyTakeHomeIncome").attr("aria-invalid")).isEmpty();
        assertThat(page.body().text()).doesNotContain("field needs attention").doesNotContain("fields need attention");
    }

    @Test
    void postNormalizesBlankFieldsAndKeepsAZeroBalanceDebtRowVisibleWithAPositionalLabel() {
        MvcTestResult result = mvc.post().uri("/")
                .param("monthlyTakeHomeIncome", "")
                .param("debts[0].label", "")
                .param("debts[0].type", "CREDIT_CARD")
                .param("debts[0].balance", "")
                .exchange();

        Document page = document(result);
        assertThat(page.select("input#monthlyTakeHomeIncome").attr("value")).isEqualTo("0");
        assertThat(page.select("input#debts-0-label").attr("value")).isEqualTo("Debt 1");
        assertThat(page.select("input#debts-0-balance").attr("value")).isEqualTo("0");
        assertThat(page.select("table.schedule tbody tr")).hasSize(1);
    }

    @Test
    void addDebtAppendsABlankRowToTheSwappedTable() {
        MvcTestResult result = mvc.post().uri("/snapshot/debts/add")
                .param("debts[0].label", "Card")
                .param("debts[0].type", "CREDIT_CARD")
                .param("debts[0].balance", "100")
                .exchange();

        assertThat(result).hasStatusOk();
        Document fragment = document(result);
        assertThat(fragment.select("table.schedule tbody tr")).hasSize(2);
        assertThat(fragment.select("input#debts-1-balance").attr("value")).isEqualTo("0");
    }

    @Test
    void addingA51stRealDebtIsRefusedWithTheStory011MessageAndNoRowIsAdded() {
        // A blank added row never counts toward the 50-debt limit (011 REQ-003), so the table already carries 51
        // real debts here (as it would once a previously added blank row is later given a real balance); adding a
        // 52nd is refused with the same TOO_MANY_DEBTS message the API uses, and the table still shows only 51 rows.
        MockMvcRequestBuilder request = mvc.post().uri("/snapshot/debts/add");
        for (int index = 0; index < 51; index++) {
            request = request.param("debts[" + index + "].type", "AUTO")
                    .param("debts[" + index + "].balance", "100");
        }

        MvcTestResult result = request.exchange();

        assertThat(result).hasStatusOk();
        Document fragment = document(result);
        assertThat(fragment.body().text()).contains("Enter 50 debts or fewer.");
        assertThat(fragment.select("table.schedule tbody tr")).hasSize(51);
    }

    @Test
    void removeDebtDropsTheRowAtItsIndexFromTheSwappedTable() {
        MvcTestResult result = mvc.post().uri("/snapshot/debts/remove/0")
                .param("debts[0].label", "First")
                .param("debts[0].type", "AUTO")
                .param("debts[0].balance", "100")
                .param("debts[1].label", "Second")
                .param("debts[1].type", "AUTO")
                .param("debts[1].balance", "200")
                .exchange();

        assertThat(result).hasStatusOk();
        Document fragment = document(result);
        assertThat(fragment.select("table.schedule tbody tr")).hasSize(1);
        assertThat(fragment.select("input#debts-0-label").attr("value")).isEqualTo("Second");
    }

    @Test
    void onBlurWithAValidValueSwapsInTheFieldWithoutAnError() {
        MvcTestResult result = mvc.post().uri("/snapshot/field").param("monthlyTakeHomeIncome", "5000").exchange();

        assertThat(result).hasStatusOk();
        Document fragment = document(result);
        Element input = fragment.selectFirst("input#monthlyTakeHomeIncome");
        assertThat(input.attr("value")).isEqualTo("5000");
        assertThat(input.attr("aria-invalid")).isEmpty();
    }

    @Test
    void onBlurWithAnInvalidValueSwapsInTheFieldWithItsError() {
        MvcTestResult result = mvc.post().uri("/snapshot/field").param("monthlyTakeHomeIncome", "-1").exchange();

        Document fragment = document(result);
        Element input = fragment.selectFirst("input#monthlyTakeHomeIncome");
        assertThat(input.attr("aria-invalid")).isEqualTo("true");
        assertThat(fragment.getElementById(input.attr("aria-describedby")).text())
                .isEqualTo("Enter zero or more.");
    }

    @ParameterizedTest
    @ValueSource(ints = {256, 99999})
    void postToTheWholeFormNeverFiveHundredsOnAnOutOfRangeDebtIndex(int index) {
        MvcTestResult result = mvc.post().uri("/").param("debts[" + index + "].balance", "100").exchange();

        assertThat(result).hasStatus(400);
    }

    @ParameterizedTest
    @ValueSource(ints = {256, 99999})
    void addDebtNeverFiveHundredsOnAnOutOfRangeDebtIndex(int index) {
        MvcTestResult result = mvc.post().uri("/snapshot/debts/add").param("debts[" + index + "].balance", "100").exchange();

        assertThat(result).hasStatus(400);
    }

    @ParameterizedTest
    @ValueSource(ints = {256, 99999})
    void removeDebtNeverFiveHundredsOnAnOutOfRangeDebtIndex(int index) {
        MvcTestResult result = mvc.post().uri("/snapshot/debts/remove/0").param("debts[" + index + "].balance", "100").exchange();

        assertThat(result).hasStatus(400);
    }

    @Test
    void postToTheWholeFormNeverFiveHundredsOnASparseDebtIndex() {
        MvcTestResult result = mvc.post().uri("/").param("debts[200].balance", "5").exchange();

        assertThat(result.getResponse().getStatus()).isLessThan(500);
    }

    @Test
    void addDebtNeverFiveHundredsOnASparseDebtIndex() {
        MvcTestResult result = mvc.post().uri("/snapshot/debts/add").param("debts[200].balance", "5").exchange();

        assertThat(result.getResponse().getStatus()).isLessThan(500);
    }

    @Test
    void removeDebtNeverFiveHundredsOnASparseDebtIndex() {
        MvcTestResult result = mvc.post().uri("/snapshot/debts/remove/0").param("debts[200].balance", "5").exchange();

        assertThat(result.getResponse().getStatus()).isLessThan(500);
    }

    @Test
    void onBlurWithAnUnknownFieldNameIs400() {
        assertThat(mvc.post().uri("/snapshot/field").param("income", "5000")).hasStatus(400);
    }

    @Test
    void anUnknownExampleIdIs404() {
        assertThat(mvc.post().uri("/snapshot/example/not-a-real-id")).hasStatus(404);
    }

    @Test
    void loadingTheDeepInDebtExampleShowsItsValues() {
        MvcTestResult result = mvc.post().uri("/snapshot/example/deep-in-debt").exchange();

        Document page = document(result);
        assertThat(page.select("input#monthlyTakeHomeIncome").attr("value")).isEqualTo("4200");
        assertThat(page.select("input#debts-0-label").attr("value")).isEqualTo("Store card");
        assertThat(page.select("input[name=householdFlags.rents]").attr("checked")).isEqualTo("checked");
    }

    @Test
    void loadingTheMidJourneyExampleShowsItsMortgage() {
        MvcTestResult result = mvc.post().uri("/snapshot/example/mid-journey").exchange();

        Document page = document(result);
        assertThat(page.selectFirst("#mortgage-section").hasClass("is-hidden")).isFalse();
        assertThat(page.select("input#mortgage-balance").attr("value")).isEqualTo("186000");
    }

    @Test
    void loadingTheNearlyDoneExampleShowsItsMortgage() {
        MvcTestResult result = mvc.post().uri("/snapshot/example/nearly-done").exchange();

        Document page = document(result);
        assertThat(page.select("input#mortgage-balance").attr("value")).isEqualTo("42000");
    }

    @Test
    void theRenderedErrorMessageMatchesTheApiForTheSameInput() {
        MvcTestResult api = mvc.post().uri("/v1/assessments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"monthlyTakeHomeIncome\": \"-1\"}").exchange();
        assertThat(api).hasStatus(400);

        Document formPage = document(mvc.post().uri("/").param("monthlyTakeHomeIncome", "-1").exchange());
        Element input = formPage.selectFirst("input#monthlyTakeHomeIncome");
        String formMessage = formPage.getElementById(input.attr("aria-describedby")).text();

        assertThat(api).bodyJson().extractingPath("$.errors[0].message").isEqualTo(formMessage);
    }

    private static Document document(MvcTestResult result) {
        try {
            return Jsoup.parse(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
