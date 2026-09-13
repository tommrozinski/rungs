package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.rungs.readiness.application.DebtInput;
import com.rungs.readiness.application.ExampleHouseholds;
import com.rungs.readiness.application.HouseholdFlagsInput;
import com.rungs.readiness.application.SnapshotInput;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

/** S-02, the assessment screen (story 029). */
@SpringBootTest
@AutoConfigureMockMvc
class AssessmentPageControllerTests {

    private final MockMvcTester mvc;

    @Autowired
    AssessmentPageControllerTests(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void everyFigurePercentageBlockerAndSkipReasonMatchesTheApiForEachExampleHousehold()
            throws java.io.UnsupportedEncodingException {
        for (String id : ExampleHouseholds.ids()) {
            SnapshotInput input = ExampleHouseholds.byId(id).orElseThrow();

            MvcTestResult api = mvc.post().uri("/v1/assessments").contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(input)).exchange();
            assertThat(api).as("household %s", id).hasStatusOk();
            DocumentContext json = JsonPath.parse(api.getResponse().getContentAsString(StandardCharsets.UTF_8));

            Document page = document(submit(input));
            String context = "household " + id;

            String surplusDisplay = json.read("$.monthlySurplus.display", String.class);
            assertThat(page.select(".stat-figure").get(0).text()).as(context + " surplus").isEqualTo(surplusDisplay);

            int currentStage = json.read("$.currentStage", Integer.class);
            List<Element> rows = page.select("ol.ladder > li");
            assertThat(rows).as(context + " ladder size").hasSize(7);

            for (int stage = 1; stage <= 7; stage++) {
                String status = json.read("$.stages[" + (stage - 1) + "].status", String.class);
                Element row = rows.get(stage - 1);
                String rowContext = context + " stage " + stage;
                if ("SKIPPED".equals(status)) {
                    String skipReason = json.read("$.stages[" + (stage - 1) + "].skipReason", String.class);
                    assertThat(row.selectFirst(".stage-reason").text()).as(rowContext).isEqualTo(skipReason);
                } else if (stage == currentStage) {
                    String blocker = json.read("$.stages[" + (stage - 1) + "].blocker", String.class);
                    if (blocker != null) {
                        assertThat(row.selectFirst(".blocker").text()).as(rowContext).isEqualTo(blocker);
                    }
                    String percent = json.read("$.stages[" + (stage - 1) + "].percentComplete", String.class);
                    assertThat(row.selectFirst("[role=progressbar]").attr("aria-valuenow")).as(rowContext)
                            .isEqualTo(percent);
                } else if ("IN_PROGRESS".equals(status) || "NOT_STARTED".equals(status)) {
                    String blocker = json.read("$.stages[" + (stage - 1) + "].blocker", String.class);
                    assertThat(row.selectFirst(".stage-reason").text()).as(rowContext).isEqualTo(blocker);
                }
            }
        }
    }

    @Test
    void documentTitleNamesTheCurrentStageOfSeven() {
        SnapshotInput input = ExampleHouseholds.byId("deep-in-debt").orElseThrow();

        Document page = document(submit(input));

        assertThat(page.selectFirst("title").text()).isEqualTo("Your stage: 1 of 7 · Rungs");
        assertThat(page.selectFirst("h1").text()).isEqualTo("Your stage");
    }

    @Test
    void aValidationFailureReRendersTheFormInsteadOfThePage() {
        MvcTestResult result = mvc.post().uri("/assessment").param("monthlyTakeHomeIncome", "-1").exchange();

        assertThat(result).hasStatus(200);
        Document page = document(result);
        assertThat(page.selectFirst("h1").text()).isEqualTo("Check your stage");
        Element input = page.selectFirst("input#monthlyTakeHomeIncome");
        assertThat(input.attr("aria-invalid")).isEqualTo("true");
    }

    @ParameterizedTest
    @ValueSource(ints = {256, 99999})
    void assessNeverFiveHundredsOnAnOutOfRangeDebtIndex(int index) {
        MvcTestResult result = mvc.post().uri("/assessment").param("debts[" + index + "].balance", "100").exchange();

        assertThat(result).hasStatus(400);
    }

    @Test
    void assessNeverFiveHundredsOnASparseDebtIndex() {
        MvcTestResult result = mvc.post().uri("/assessment").param("debts[200].balance", "5").exchange();

        assertThat(result.getResponse().getStatus()).isLessThan(500);
    }

    @Test
    void changeMyNumbersReproducesTheSameFormItCameFrom() {
        SnapshotInput input = ExampleHouseholds.byId("mid-journey").orElseThrow();
        Document assessmentPage = document(submit(input));

        Element cta = assessmentPage.selectFirst("li.is-expanded form");
        assertThat(cta.attr("action")).isEqualTo(StageRowViews.CHANGE_NUMBERS_ACTION);

        MockMvcRequestBuilder changeNumbers = mvc.post().uri(cta.attr("action"));
        for (Element hidden : cta.select("input[type=hidden]")) {
            changeNumbers = changeNumbers.param(hidden.attr("name"), hidden.attr("value"));
        }

        Document form = document(changeNumbers.exchange());

        assertThat(form.selectFirst("h1").text()).isEqualTo("Check your stage");
        assertThat(form.select("input#monthlyTakeHomeIncome").attr("value")).isEqualTo("6800");
        assertThat(form.select("input#mortgage-balance").attr("value")).isEqualTo("186000");
        assertThat(form.body().text()).doesNotContain("field needs attention").doesNotContain("fields need attention");
    }

    private MvcTestResult submit(SnapshotInput input) {
        MockMvcRequestBuilder request = mvc.post().uri("/assessment");
        for (HiddenField field : HiddenSnapshotFields.of(input.forForm())) {
            request = request.param(field.name(), field.value());
        }
        return request.exchange();
    }

    private static Document document(MvcTestResult result) {
        try {
            return Jsoup.parse(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String toJson(SnapshotInput input) {
        List<DebtInput> debts = input.debts() == null ? List.of() : input.debts();
        StringBuilder debtsJson = new StringBuilder("[");
        for (int index = 0; index < debts.size(); index++) {
            if (index > 0) {
                debtsJson.append(',');
            }
            DebtInput debt = debts.get(index);
            debtsJson.append("{\"label\":").append(quote(debt.label())).append(",\"type\":")
                    .append(quote(debt.type())).append(",\"balance\":").append(quote(debt.balance()))
                    .append(",\"annualInterestRate\":").append(quote(debt.annualInterestRate()))
                    .append(",\"minimumPayment\":").append(quote(debt.minimumPayment())).append('}');
        }
        debtsJson.append(']');
        String mortgage = input.mortgage() == null
                ? "null"
                : "{\"balance\":" + quote(input.mortgage().balance()) + ",\"annualInterestRate\":"
                        + quote(input.mortgage().annualInterestRate()) + ",\"monthlyPayment\":"
                        + quote(input.mortgage().monthlyPayment()) + ",\"homeValue\":"
                        + quote(input.mortgage().homeValue()) + '}';
        HouseholdFlagsInput flags = input.householdFlags() == null
                ? new HouseholdFlagsInput(null, null, null)
                : input.householdFlags();
        return "{\"monthlyTakeHomeIncome\":" + quote(input.monthlyTakeHomeIncome()) + ",\"monthlyEssentialExpenses\":"
                + quote(input.monthlyEssentialExpenses()) + ",\"starterEmergencyFund\":"
                + quote(input.starterEmergencyFund()) + ",\"fullEmergencyFund\":" + quote(input.fullEmergencyFund())
                + ",\"monthlyCollegeContribution\":" + quote(input.monthlyCollegeContribution())
                + ",\"retirementContributionPercent\":" + quote(input.retirementContributionPercent())
                + ",\"emergencyFundMonths\":" + (input.emergencyFundMonths() == null ? "null"
                        : input.emergencyFundMonths())
                + ",\"debts\":" + debtsJson + ",\"mortgage\":" + mortgage + ",\"householdFlags\":{\"rents\":"
                + bool(flags.rents()) + ",\"retired\":" + bool(flags.retired()) + ",\"intendsToFundCollege\":"
                + bool(flags.intendsToFundCollege()) + "}}";
    }

    private static String quote(String text) {
        return text == null ? "null" : "\"" + text + "\"";
    }

    private static String bool(Boolean flag) {
        return String.valueOf(Boolean.TRUE.equals(flag));
    }
}
