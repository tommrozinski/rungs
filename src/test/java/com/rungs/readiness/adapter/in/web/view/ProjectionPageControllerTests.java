package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.rungs.readiness.application.DebtInput;
import com.rungs.readiness.application.HouseholdFlagsInput;
import com.rungs.readiness.application.MortgageInput;
import com.rungs.readiness.application.SnapshotInput;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** S-03, the projection screen (story 030). */
@SpringBootTest
@AutoConfigureMockMvc
class ProjectionPageControllerTests {

    private static final SnapshotInput THREE_DEBT_HOUSEHOLD = new SnapshotInput("5000.00", "4350.00", "1000.00",
            null, null, null, null,
            List.of(
                    new DebtInput("Credit card", "CREDIT_CARD", "3500.00", "23.99", "90.00"),
                    new DebtInput("Medical bill", "MEDICAL", "900.00", "0", "50.00"),
                    new DebtInput("Car loan", "AUTO", "7200.00", "6.9", "210.00")),
            null, null);

    private static final String THREE_DEBT_HOUSEHOLD_JSON = """
            {"monthlyTakeHomeIncome": "5000.00", "monthlyEssentialExpenses": "4350.00",
             "starterEmergencyFund": "1000.00",
             "debts": [{"label": "Credit card", "type": "CREDIT_CARD", "balance": "3500.00",
                         "annualInterestRate": "23.99", "minimumPayment": "90.00"},
                       {"label": "Medical bill", "type": "MEDICAL", "balance": "900.00",
                         "annualInterestRate": "0", "minimumPayment": "50.00"},
                       {"label": "Car loan", "type": "AUTO", "balance": "7200.00",
                         "annualInterestRate": "6.9", "minimumPayment": "210.00"}]}
            """;

    private final MockMvcTester mvc;

    @Autowired
    ProjectionPageControllerTests(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void postProjectionRendersTheWholePage() {
        Document page = document(submit("/projection", THREE_DEBT_HOUSEHOLD, "300", "SNOWBALL"));

        assertThat(page.selectFirst("h1").text()).isEqualTo("Your payoff plan");
        assertThat(page.selectFirst("title").text()).isEqualTo("Your payoff plan · Rungs");
        assertThat(page.selectFirst("#extra-payment-value").text()).isEqualTo("$300 extra a month");
        assertThat(page.select("table.schedule")).hasSize(1);
    }

    @Test
    void postProjectionControlFormCarriesTheStaleResponseHtmxMarkup() {
        Document page = document(submit("/projection", THREE_DEBT_HOUSEHOLD, "0", "SNOWBALL"));

        Element form = page.selectFirst("#projection-controls");
        assertThat(form.attr("hx-post")).isEqualTo("/projection/results");
        assertThat(form.attr("hx-trigger")).isEqualTo("input delay:250ms");
        assertThat(form.attr("hx-target")).isEqualTo("#results");
        assertThat(form.attr("hx-sync")).isEqualTo("this:replace");
    }

    @Test
    void postProjectionResultsRendersTheResultsFragmentWithTheOutOfBandLabel() {
        MvcTestResult result = submit("/projection/results", THREE_DEBT_HOUSEHOLD, "300", "SNOWBALL");

        assertThat(result).hasStatusOk();
        Document fragment = document(result);
        Element label = fragment.selectFirst("#extra-payment-value");
        assertThat(label.attr("hx-swap-oob")).isEqualTo("true");
        assertThat(label.text()).isEqualTo("$300 extra a month");
        assertThat(fragment.select(".stat-label").eachText())
                .containsExactly("Debt-free in", "Total interest", "First debt paid off");
        assertThat(fragment.selectFirst("h1")).isNull();
    }

    @Test
    void aValidationFailureOnProjectionReRendersTheSnapshotForm() {
        MvcTestResult result = mvc.post().uri("/projection").param("monthlyTakeHomeIncome", "-1").exchange();

        assertThat(result).hasStatus(200);
        Document page = document(result);
        assertThat(page.selectFirst("h1").text()).isEqualTo("Check your stage");
        assertThat(page.selectFirst("input#monthlyTakeHomeIncome").attr("aria-invalid")).isEqualTo("true");
    }

    @Test
    void aValidationFailureOnResultsIsABadRequestWithACautionFragment() {
        MvcTestResult result = mvc.post().uri("/projection/results").param("monthlyTakeHomeIncome", "-1").exchange();

        assertThat(result).hasStatus(400);
        Document fragment = document(result);
        assertThat(fragment.selectFirst(".banner-message").text()).isEqualTo("Check your numbers and try again.");
    }

    @Test
    void aValidationFailureOnScheduleIsABadRequest() {
        MvcTestResult result = mvc.post().uri("/projection/schedule").param("monthlyTakeHomeIncome", "-1").exchange();

        assertThat(result).hasStatus(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/projection", "/projection/results", "/projection/schedule"})
    void getIsMethodNotAllowed(String path) {
        assertThat(mvc.get().uri(path)).hasStatus(405);
    }

    @ParameterizedTest
    @MethodSource("hostilePathsAndIndices")
    void neverFiveHundredsOnAnOutOfRangeDebtIndex(String path, int index) {
        MvcTestResult result = mvc.post().uri(path).param("debts[" + index + "].balance", "100").exchange();

        assertThat(result).hasStatus(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/projection", "/projection/results", "/projection/schedule"})
    void neverFiveHundredsOnASparseDebtIndex(String path) {
        MvcTestResult result = mvc.post().uri(path).param("debts[200].balance", "5").exchange();

        assertThat(result.getResponse().getStatus()).isLessThan(500);
    }

    @ParameterizedTest
    @ValueSource(strings = {"X", ""})
    void aTamperedStrategyOnProjectionReRendersTheSnapshotFormInsteadOfFailing(String strategy) {
        MvcTestResult result = submit("/projection", THREE_DEBT_HOUSEHOLD, "0", strategy);

        assertThat(result.getResponse().getStatus()).isLessThan(500);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/projection/results", "/projection/schedule"})
    void aTamperedStrategyOnAFragmentRouteIsABadRequestInsteadOfFailing(String path) {
        MvcTestResult result = submit(path, THREE_DEBT_HOUSEHOLD, "0", "X");

        assertThat(result).hasStatus(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-5", "abc"})
    void aTamperedExtraMonthlyPaymentOnProjectionReRendersTheSnapshotFormInsteadOfFailing(String extraMonthlyPayment) {
        MvcTestResult result = submit("/projection", THREE_DEBT_HOUSEHOLD, extraMonthlyPayment, "SNOWBALL");

        assertThat(result.getResponse().getStatus()).isLessThan(500);
    }

    @ParameterizedTest
    @MethodSource("tamperedExtraMonthlyPaymentAndFragmentPaths")
    void aTamperedExtraMonthlyPaymentOnAFragmentRouteIsABadRequestInsteadOfFailing(
            String path, String extraMonthlyPayment) {
        MvcTestResult result = submit(path, THREE_DEBT_HOUSEHOLD, extraMonthlyPayment, "SNOWBALL");

        assertThat(result).hasStatus(400);
    }

    private static Stream<Arguments> hostilePathsAndIndices() {
        return Stream.of("/projection", "/projection/results", "/projection/schedule")
                .flatMap(path -> Stream.of(256, 99999).map(index -> Arguments.of(path, index)));
    }

    private static Stream<Arguments> tamperedExtraMonthlyPaymentAndFragmentPaths() {
        return Stream.of("/projection/results", "/projection/schedule")
                .flatMap(path -> Stream.of("-5", "abc")
                        .map(extraMonthlyPayment -> Arguments.of(path, extraMonthlyPayment)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/projection", "/projection/results", "/projection/schedule"})
    void securityHeadersArePresent(String path) {
        MvcTestResult result = submit(path, THREE_DEBT_HOUSEHOLD, "0", "SNOWBALL");

        assertThat(result.getResponse().getHeader("Content-Security-Policy")).isEqualTo(
                "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self'; "
                        + "connect-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'self'");
        assertThat(result.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(result.getResponse().getHeader("Referrer-Policy")).isEqualTo("no-referrer");
    }

    @Test
    void warningsRenderAsCautionBannersWhenTheHouseholdHasThem() {
        SnapshotInput household = new SnapshotInput("3000", "3200", null, null, null, null, 9,
                List.of(new DebtInput("Card", "CREDIT_CARD", "10000", "24", "1")), null, null);

        Document page = document(submit("/projection", household, "0", "SNOWBALL"));

        assertThat(page.select("#warnings-heading")).hasSize(1);
        assertThat(page.select(".banner-message").eachText()).contains(
                "The minimum payment on Card doesn't cover its monthly interest, so the balance grows.");
    }

    @Test
    void scheduleCapsAtTwentyFourMonthsWithAShowAllButtonBeyondThatAndTheScheduleEndpointReturnsEveryMonth() {
        SnapshotInput household = new SnapshotInput("3000", "2850", null, null, null, null, null,
                List.of(new DebtInput("Card", "CREDIT_CARD", "9000", "18", "150")), null, null);

        DocumentContext api = json(mvc.post().uri("/v1/projections").contentType(MediaType.APPLICATION_JSON)
                .content(apiJson(household, "0", "SNOWBALL")).exchange());
        int totalMonths = api.read("$.schedule.length()", Integer.class);
        assertThat(totalMonths).isGreaterThan(24);

        Document page = document(submit("/projection", household, "0", "SNOWBALL"));
        assertThat(page.select("#schedule-container tbody tr")).hasSize(24);
        Element button = page.selectFirst("#schedule-container button");
        assertThat(button.text()).isEqualTo("Show all " + totalMonths + " months");
        assertThat(button.attr("hx-post")).isEqualTo("/projection/schedule");

        Document scheduleFragment = document(submit("/projection/schedule", household, "0", "SNOWBALL"));
        assertThat(scheduleFragment.select("tbody tr")).hasSize(totalMonths);
        assertThat(scheduleFragment.selectFirst("button")).isNull();
    }

    @Test
    void noShowAllButtonAtOrBelowTwentyFourMonths() {
        Document page = document(submit("/projection", THREE_DEBT_HOUSEHOLD, "0", "SNOWBALL"));

        assertThat(page.selectFirst("#schedule-container button")).isNull();
    }

    @Test
    void truncatedPlanNamesTheHorizonAtTheFootOfTheTable() {
        SnapshotInput household = new SnapshotInput("10000", "3000", null, null, null, null, null,
                List.of(new DebtInput("Card", "CREDIT_CARD", "1000000000.00", "100", "0")), null, null);

        Document page = document(submit("/projection", household, "0", "SNOWBALL"));

        assertThat(page.select("#schedule-container").text())
                .contains("This plan doesn't finish within 360 months.");
    }

    @Test
    void the029StageTwoCallToActionLandsOnTheProjectionPage() {
        MvcTestResult assessed = submit("/assessment", THREE_DEBT_HOUSEHOLD, null, null);
        Document assessmentPage = document(assessed);
        Element cta = assessmentPage.selectFirst("li.is-expanded form");
        assertThat(cta.attr("action")).isEqualTo(StageRowViews.PAYOFF_PLAN_ACTION);

        MockMvcRequestBuilder request = mvc.post().uri(cta.attr("action"));
        for (Element hidden : cta.select("input[type=hidden]")) {
            request = request.param(hidden.attr("name"), hidden.attr("value"));
        }
        Document projectionPage = document(request.exchange());

        assertThat(projectionPage.selectFirst("h1").text()).isEqualTo("Your payoff plan");
        assertThat(projectionPage.selectFirst("#extra-payment-value").text()).isEqualTo("$0 extra a month");
        assertThat(projectionPage.select("input[name=strategy][value=SNOWBALL]").attr("checked"))
                .isEqualTo("checked");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SNOWBALL", "AVALANCHE"})
    void everyScheduleFigureAndStatMatchesTheApiForTheAc21Household(String strategy) {
        DocumentContext api = json(mvc.post().uri("/v1/projections").contentType(MediaType.APPLICATION_JSON)
                .content(apiJson(THREE_DEBT_HOUSEHOLD, "0", strategy)).exchange());

        Document page = document(submit("/projection", THREE_DEBT_HOUSEHOLD, "0", strategy));

        List<String> debtIds = List.of("debt-1", "debt-2", "debt-3");
        int months = api.read("$.schedule.length()", Integer.class);
        Element table = page.selectFirst("table.schedule");
        List<Element> rows = table.select("tbody tr");
        assertThat(rows).hasSize(months);

        for (int monthIndex = 0; monthIndex < months; monthIndex++) {
            String base = "$.schedule[" + monthIndex + "]";
            List<String> retired = api.read(base + ".retiredDebtIds", List.class);
            Element row = rows.get(monthIndex);
            List<String> cells = row.select("td").eachText();
            for (int debtIndex = 0; debtIndex < debtIds.size(); debtIndex++) {
                String debtId = debtIds.get(debtIndex);
                String linePath = base + ".lines[?(@.debtId=='" + debtId + "')]";
                List<Map<String, Object>> lines = api.read(linePath, List.class);
                String expectedPayment = "—";
                String expectedBalance = "—";
                if (!lines.isEmpty()) {
                    Map<String, Object> line = lines.get(0);
                    expectedPayment = (String) ((Map<String, Object>) line.get("payment")).get("display");
                    expectedBalance = (String) ((Map<String, Object>) line.get("closingBalance")).get("display");
                }
                assertThat(cells.get(debtIndex * 2)).as("month %d %s payment", monthIndex + 1, debtId)
                        .isEqualTo(expectedPayment);
                assertThat(cells.get(debtIndex * 2 + 1)).as("month %d %s balance", monthIndex + 1, debtId)
                        .isEqualTo(expectedBalance);
            }
            boolean expectRetired = !retired.isEmpty();
            assertThat(row.hasClass("is-retired")).as("month %d retired", monthIndex + 1).isEqualTo(expectRetired);
        }

        Integer monthsToDebtFreedom = api.read("$.monthsToDebtFreedom", Integer.class);
        String debtFreeFigure = page.select(".stat-figure").get(0).text();
        assertThat(debtFreeFigure).isEqualTo(monthsToDebtFreedom + (monthsToDebtFreedom == 1 ? " month" : " months"));

        String totalInterestDisplay = api.read("$.totalInterestPaid.display", String.class);
        assertThat(page.select(".stat-figure").get(1).text()).isEqualTo(totalInterestDisplay);

        Integer firstRetirementMonth = null;
        for (int monthIndex = 0; monthIndex < months && firstRetirementMonth == null; monthIndex++) {
            List<String> retired = api.read("$.schedule[" + monthIndex + "].retiredDebtIds", List.class);
            if (!retired.isEmpty()) {
                firstRetirementMonth = api.read("$.schedule[" + monthIndex + "].month", Integer.class);
            }
        }
        String expectedFirstDebt = firstRetirementMonth == null ? "Not within 30 years" : "Month " + firstRetirementMonth;
        assertThat(page.select(".stat-figure").get(2).text()).isEqualTo(expectedFirstDebt);
    }

    private MvcTestResult submit(String path, SnapshotInput input, String extraMonthlyPayment, String strategy) {
        MockMvcRequestBuilder request = mvc.post().uri(path);
        for (HiddenField field : HiddenSnapshotFields.of(input.forForm())) {
            request = request.param(field.name(), field.value());
        }
        if (extraMonthlyPayment != null) {
            request = request.param("extraMonthlyPayment", extraMonthlyPayment);
        }
        if (strategy != null) {
            request = request.param("strategy", strategy);
        }
        return request.exchange();
    }

    private static DocumentContext json(MvcTestResult result) {
        try {
            return JsonPath.parse(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static Document document(MvcTestResult result) {
        try {
            return Jsoup.parse(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String apiJson(SnapshotInput input, String extraMonthlyPayment, String strategy) {
        String snapshot = input == THREE_DEBT_HOUSEHOLD ? THREE_DEBT_HOUSEHOLD_JSON : toJson(input);
        return "{\"snapshot\": " + snapshot + ", \"extraMonthlyPayment\": \"" + extraMonthlyPayment
                + "\", \"strategy\": \"" + strategy + "\", \"horizonMonths\": 360}";
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
        MortgageInput mortgage = input.mortgage();
        String mortgageJson = mortgage == null
                ? "null"
                : "{\"balance\":" + quote(mortgage.balance()) + ",\"annualInterestRate\":"
                        + quote(mortgage.annualInterestRate()) + ",\"monthlyPayment\":"
                        + quote(mortgage.monthlyPayment()) + ",\"homeValue\":" + quote(mortgage.homeValue()) + '}';
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
                + ",\"debts\":" + debtsJson + ",\"mortgage\":" + mortgageJson + ",\"householdFlags\":{\"rents\":"
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
