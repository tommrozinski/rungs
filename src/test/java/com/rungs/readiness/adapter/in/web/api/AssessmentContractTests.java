package com.rungs.readiness.adapter.in.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Contract tests for {@code POST /v1/assessments} (story 007): exact response bodies through the real HTTP layer and
 * the application's own Jackson configuration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AssessmentContractTests {

    private static final String URI = "/v1/assessments";

    /**
     * The AC-1.2 response, stored compact exactly as the server writes it and with no trailing newline, so the
     * comparison is byte-for-byte; any change to the response shape shows up as a diff of this file.
     */
    private static final String GOLDEN_AC_1_2 = "/golden/assessment-ac-1-2.json";

    private static final String AC_1_2_REQUEST = """
            {"monthlyTakeHomeIncome": "5000", "monthlyEssentialExpenses": "3000", "starterEmergencyFund": "1000",
             "fullEmergencyFund": "9000", "retirementContributionPercent": "15", "emergencyFundMonths": 3,
             "debts": [], "mortgage": null,
             "householdFlags": {"rents": true, "retired": false, "intendsToFundCollege": false}}
            """;

    private final MockMvcTester mvc;

    @Autowired
    AssessmentContractTests(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void ac11FiveHundredSavedAndCreditCardDebtIsStageOne() {
        String request = """
                {"starterEmergencyFund": "500",
                 "debts": [{"label": "Credit card", "type": "CREDIT_CARD", "balance": "12000",
                            "annualInterestRate": "22.9", "minimumPayment": "300"}],
                 "mortgage": null}
                """;

        assertThat(post(request))
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON)
                .hasBodyTextEqualTo("{\"currentStage\":1,\"stages\":["
                        + "{\"stage\":1,\"status\":\"IN_PROGRESS\",\"percentComplete\":\"50.0\","
                        + "\"remainingAmount\":{\"amount\":\"500.00\",\"display\":\"$500.00\"},"
                        + "\"blocker\":\"Your starter emergency fund is $500.00 short of $1,000.00.\",\"skipReason\":null},"
                        + "{\"stage\":2,\"status\":\"IN_PROGRESS\",\"percentComplete\":\"0.0\","
                        + "\"remainingAmount\":{\"amount\":\"12000.00\",\"display\":\"$12,000.00\"},"
                        + "\"blocker\":\"You have $12,000.00 of debt across 1 debt.\",\"skipReason\":null},"
                        + satisfied(3)
                        + ",{\"stage\":4,\"status\":\"NOT_STARTED\",\"percentComplete\":\"0.0\",\"remainingAmount\":null,"
                        + "\"blocker\":\"You're contributing 0% of income to retirement, short of 15%.\",\"skipReason\":null},"
                        + "{\"stage\":5,\"status\":\"SKIPPED\",\"percentComplete\":\"0.0\",\"remainingAmount\":null,"
                        + "\"blocker\":null,\"skipReason\":\"You don't plan to fund college, so there's nothing to save for.\"},"
                        + satisfied(6)
                        + ",{\"stage\":7,\"status\":\"IN_PROGRESS\",\"percentComplete\":\"50.0\",\"remainingAmount\":null,"
                        + "\"blocker\":\"3 of the 6 earlier stages are complete.\",\"skipReason\":null}],"
                        + "\"monthlySurplus\":{\"amount\":\"-300.00\",\"display\":\"-$300.00\"},\"warnings\":[]}");
    }

    @Test
    void ac12RenterWithNoCollegePlansMeetingStagesOneToFourMatchesTheGoldenFile() throws IOException {
        MvcTestResult result = post(AC_1_2_REQUEST);

        assertThat(result).hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
        assertThat(result).bodyJson().extractingPath("$.currentStage").isEqualTo(7);
        assertThat(result).bodyJson().extractingPath("$.stages[*].status").asArray()
                .containsExactly("SATISFIED", "SATISFIED", "SATISFIED", "SATISFIED", "SKIPPED", "SKIPPED", "SATISFIED");
        assertThat(result).bodyJson().extractingPath("$.stages[4].skipReason")
                .isEqualTo("You don't plan to fund college, so there's nothing to save for.");
        assertThat(result).bodyJson().extractingPath("$.stages[5].skipReason")
                .isEqualTo("You rent, so there's no mortgage to pay down.");
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(golden());
    }

    @Test
    void ac13InProgressStagesHaveBlockersAndSkippedStagesHaveOnlySkipReasons() {
        String mixedHousehold = """
                {"monthlyTakeHomeIncome": "3000", "monthlyEssentialExpenses": "1000", "starterEmergencyFund": "500",
                 "debts": [{"label": "Card", "type": "CREDIT_CARD", "balance": "2000", "annualInterestRate": "19",
                            "minimumPayment": "50"}],
                 "mortgage": {"balance": "100000", "annualInterestRate": "6", "monthlyPayment": "900",
                              "homeValue": "250000"},
                 "householdFlags": {"rents": false, "retired": true, "intendsToFundCollege": true}}
                """;

        MvcTestResult result = post(mixedHousehold);

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.stages[*].status").asArray().containsExactly(
                "IN_PROGRESS", "IN_PROGRESS", "NOT_STARTED", "SKIPPED", "SKIPPED", "IN_PROGRESS", "IN_PROGRESS");
        assertThat(result).bodyJson()
                .extractingPath("$.stages[?(@.status == 'IN_PROGRESS' && @.blocker == null)]").asArray().isEmpty();
        assertThat(result).bodyJson()
                .extractingPath("$.stages[?(@.status == 'SKIPPED' && (@.skipReason == null || @.blocker != null))]")
                .asArray().isEmpty();
        assertThat(result).bodyJson().extractingPath("$.stages[3].skipReason")
                .isEqualTo("You're retired, so you're no longer saving for retirement from income.");
        assertThat(result).bodyJson().extractingPath("$.stages[5].blocker")
                .isEqualTo("You have $100,000.00 left on your mortgage.");
        assertThat(result).bodyJson().extractingPath("$.stages[6].percentComplete").isEqualTo("33.3");
    }

    @Test
    void ac14SatisfiedIsExactlyOneHundredAndAZeroDenominatorIsZeroWithoutError() {
        String request = """
                {"mortgage": {"balance": "100000", "annualInterestRate": "6", "monthlyPayment": "900",
                              "homeValue": ""}}
                """;

        MvcTestResult result = post(request);

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.stages[1]").asMap().containsExactly(
                entry("stage", 2), entry("status", "SATISFIED"), entry("percentComplete", "100.0"),
                entry("remainingAmount", null), entry("blocker", null), entry("skipReason", null));
        assertThat(result).bodyJson().extractingPath("$.stages[5]").asMap().containsExactly(
                entry("stage", 6), entry("status", "NOT_STARTED"), entry("percentComplete", "0.0"),
                entry("remainingAmount", Map.of("amount", "100000.00", "display", "$100,000.00")),
                entry("blocker", "You have $100,000.00 left on your mortgage."), entry("skipReason", null));
    }

    @Test
    void anEmptySnapshotIsEveryValueZeroWithThreeEmergencyFundMonths() {
        assertThat(post("{}"))
                .hasStatusOk()
                .hasBodyTextEqualTo("{\"currentStage\":1,\"stages\":["
                        + "{\"stage\":1,\"status\":\"NOT_STARTED\",\"percentComplete\":\"0.0\","
                        + "\"remainingAmount\":{\"amount\":\"1000.00\",\"display\":\"$1,000.00\"},"
                        + "\"blocker\":\"Your starter emergency fund is $1,000.00 short of $1,000.00.\",\"skipReason\":null},"
                        + satisfied(2) + "," + satisfied(3)
                        + ",{\"stage\":4,\"status\":\"NOT_STARTED\",\"percentComplete\":\"0.0\",\"remainingAmount\":null,"
                        + "\"blocker\":\"You're contributing 0% of income to retirement, short of 15%.\",\"skipReason\":null},"
                        + "{\"stage\":5,\"status\":\"SKIPPED\",\"percentComplete\":\"0.0\",\"remainingAmount\":null,"
                        + "\"blocker\":null,\"skipReason\":\"You don't plan to fund college, so there's nothing to save for.\"},"
                        + satisfied(6)
                        + ",{\"stage\":7,\"status\":\"IN_PROGRESS\",\"percentComplete\":\"66.6\",\"remainingAmount\":null,"
                        + "\"blocker\":\"4 of the 6 earlier stages are complete.\",\"skipReason\":null}],"
                        + "\"monthlySurplus\":{\"amount\":\"0.00\",\"display\":\"$0.00\"},\"warnings\":[]}");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "monthlyTakeHomeIncome         | MISSING",
        "monthlyTakeHomeIncome         | null",
        "monthlyTakeHomeIncome         | \"\"",
        "monthlyTakeHomeIncome         | \"  \"",
        "monthlyEssentialExpenses      | MISSING",
        "monthlyEssentialExpenses      | null",
        "monthlyEssentialExpenses      | \"\"",
        "monthlyEssentialExpenses      | \"  \"",
        "starterEmergencyFund          | MISSING",
        "starterEmergencyFund          | null",
        "starterEmergencyFund          | \"\"",
        "starterEmergencyFund          | \"  \"",
        "fullEmergencyFund             | MISSING",
        "fullEmergencyFund             | null",
        "fullEmergencyFund             | \"\"",
        "fullEmergencyFund             | \"  \"",
        "monthlyCollegeContribution    | MISSING",
        "monthlyCollegeContribution    | null",
        "monthlyCollegeContribution    | \"\"",
        "monthlyCollegeContribution    | \"  \"",
        "retirementContributionPercent | MISSING",
        "retirementContributionPercent | null",
        "retirementContributionPercent | \"\"",
        "retirementContributionPercent | \"  \""
    })
    void blankTopLevelMoneyAndPercentageAreZero(String field, String blankValue) {
        Map<String, String> blank = household();
        Map<String, String> zero = household();
        setOrRemove(blank, field, blankValue);
        zero.put(field, "\"0\"");

        assertThat(bodyOf(json(blank))).isEqualTo(bodyOf(json(zero)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "annualInterestRate | MISSING", "annualInterestRate | null", "annualInterestRate | \"\"",
        "annualInterestRate | \"  \"",
        "minimumPayment     | MISSING", "minimumPayment     | null", "minimumPayment     | \"\"",
        "minimumPayment     | \"  \""
    })
    void blankDebtRateAndMinimumPaymentAreZero(String field, String blankValue) {
        Map<String, String> blank = debt();
        Map<String, String> zero = debt();
        setOrRemove(blank, field, blankValue);
        zero.put(field, "\"0\"");

        assertThat(bodyOf(withDebts(json(blank)))).isEqualTo(bodyOf(withDebts(json(zero))));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "balance            | MISSING", "balance            | null", "balance            | \"\"",
        "balance            | \"  \"",
        "annualInterestRate | MISSING", "annualInterestRate | null", "annualInterestRate | \"\"",
        "annualInterestRate | \"  \"",
        "monthlyPayment     | MISSING", "monthlyPayment     | null", "monthlyPayment     | \"\"",
        "monthlyPayment     | \"  \"",
        "homeValue          | MISSING", "homeValue          | null", "homeValue          | \"\"",
        "homeValue          | \"  \""
    })
    void blankMortgageValuesAreZero(String field, String blankValue) {
        Map<String, String> blank = mortgage();
        Map<String, String> zero = mortgage();
        setOrRemove(blank, field, blankValue);
        zero.put(field, "\"0\"");

        assertThat(bodyOf(withMortgage(json(blank)))).isEqualTo(bodyOf(withMortgage(json(zero))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "null"})
    void missingEmergencyFundMonthsIsThree(String months) {
        Map<String, String> defaulted = household();
        Map<String, String> three = household();
        Map<String, String> six = household();
        setOrRemove(defaulted, "emergencyFundMonths", months);
        three.put("emergencyFundMonths", "3");
        six.put("emergencyFundMonths", "6");

        String body = bodyOf(json(defaulted));

        assertThat(body).isEqualTo(bodyOf(json(three))).isNotEqualTo(bodyOf(json(six)))
                .contains("3-month target of $6,000.00");
    }

    @Test
    void blankDebtBalanceRemovesTheDebt() {
        Map<String, String> blankBalance = debt();
        blankBalance.put("balance", "\"\"");

        assertThat(bodyOf(withDebts(json(blankBalance)))).isEqualTo(bodyOf(json(household())));
    }

    @Test
    void aZeroBalanceDebtChangesNoFigureInTheResponse() {
        Map<String, String> paidOff = debt();
        paidOff.put("label", "\"Paid off\"");
        paidOff.put("balance", "\"0.00\"");
        paidOff.put("minimumPayment", "\"450\"");

        String withoutIt = bodyOf(withDebts(json(debt())));

        assertThat(bodyOf(withDebts(json(paidOff) + "," + json(debt())))).isEqualTo(withoutIt);
        assertThat(bodyOf(withDebts(json(debt()) + "," + json(paidOff)))).isEqualTo(withoutIt);
    }

    @Test
    void debtLabelsAppearInTheStageTwoBlockerOnlyAsACount() {
        String request = """
                {"debts": [{"label": "", "type": "AUTO", "balance": "100", "annualInterestRate": "1", "minimumPayment": "1"},
                           {"label": "Visa", "type": "CREDIT_CARD", "balance": "200.50", "annualInterestRate": "1",
                            "minimumPayment": "1"}]}
                """;

        MvcTestResult result = post(request);

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.stages[1].blocker")
                .isEqualTo("You have $300.50 of debt across 2 debts.");
        assertThat(result).bodyText().doesNotContain("Visa").doesNotContain("Debt 1").doesNotContain("debt-");
    }

    @Test
    void identicalRequestsProduceByteIdenticalBodies() {
        String request = withDebts(json(debt()));

        byte[] first = post(request).getResponse().getContentAsByteArray();
        byte[] second = post(request).getResponse().getContentAsByteArray();

        assertThat(first).isNotEmpty().isEqualTo(second);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "1000 | 1050    | -50.00     | -$50.00",
        "5000 | 1234.56 | 3765.44    | $3,765.44",
        "0    | 0       | 0.00       | $0.00"
    })
    void monthlySurplusIsAMoneyObject(String income, String expenses, String amount, String display) {
        String request = "{\"monthlyTakeHomeIncome\": \"" + income + "\", \"monthlyEssentialExpenses\": \"" + expenses
                + "\"}";

        assertThat(post(request)).hasStatusOk().bodyJson().extractingPath("$.monthlySurplus").asMap()
                .containsExactly(entry("amount", amount), entry("display", display));
    }

    @Test
    void rejectedBodiesAreNotWrittenToTheLog(CapturedOutput output) {
        assertThat(post("{\"monthlyTakeHomeIncomeTypo\": \"5000\"}")).hasStatus(400);
        assertThat(post("{\"monthlyTakeHomeIncome\": 987654}")).hasStatus(400);
        assertThat(post("{\"emergencyFundMonths\": 3.25}")).hasStatus(400);
        assertThat(post("{\"monthlyTakeHomeIncome\": \"123abc\"}")).hasStatus(400);

        assertThat(output).doesNotContain("monthlyTakeHomeIncomeTypo").doesNotContain("987654")
                .doesNotContain("3.25").doesNotContain("123abc");
    }

    @Test
    void aMisspeltPropertyIsRejectedNotIgnored() {
        assertThat(post("{\"monthlyTakeHomeIncom\": \"5000\"}")).hasStatus(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"monthlyTakeHomeIncome\": 5000}",
        "{\"monthlyTakeHomeIncome\": 50.25}",
        "{\"starterEmergencyFund\": true}",
        "{\"retirementContributionPercent\": 15}",
        "{\"debts\": [{\"type\": \"AUTO\", \"balance\": 100}]}",
        "{\"emergencyFundMonths\": 3.5}",
        "{\"emergencyFundMonths\": 4.0}"
    })
    void jsonScalarsAreNotCoercedIntoTextOrWholeNumbers(String request) {
        assertThat(post(request)).hasStatus(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"monthlyTakeHomeIncome\": \"abc\"}",
        "{\"monthlyEssentialExpenses\": \"-5\"}",
        "{\"retirementContributionPercent\": \"fifteen\"}",
        "{\"retirementContributionPercent\": \"101\"}",
        "{\"emergencyFundMonths\": 2}",
        "{\"debts\": [{\"type\": \"MORTGAGE\", \"balance\": \"100\"}]}",
        "{\"debts\": [{\"type\": \"\", \"balance\": \"100\"}]}",
        "{\"mortgage\": {\"balance\": \"1,00\"}}",
        "not json"
    })
    void inputThatCannotBeNormalizedIsABadRequestWithoutEchoingIt(String request) {
        MvcTestResult result = post(request);

        assertThat(result).hasStatus(400);
        assertThat(result).bodyText().doesNotContain("abc").doesNotContain("fifteen").doesNotContain("MORTGAGE")
                .doesNotContain("Exception");
    }

    private MvcTestResult post(String body) {
        return mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .content(body).exchange();
    }

    private String bodyOf(String request) {
        MvcTestResult result = post(request);
        assertThat(result).hasStatusOk();
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private static byte[] golden() throws IOException {
        try (InputStream in = AssessmentContractTests.class.getResourceAsStream(GOLDEN_AC_1_2)) {
            assertThat(in).as("golden file %s", GOLDEN_AC_1_2).isNotNull();
            return in.readAllBytes();
        }
    }

    private static String satisfied(int stage) {
        return "{\"stage\":" + stage + ",\"status\":\"SATISFIED\",\"percentComplete\":\"100.0\","
                + "\"remainingAmount\":null,\"blocker\":null,\"skipReason\":null}";
    }

    /** A household where every top-level value is non-zero, so a value wrongly read as non-zero shows in the body. */
    private static Map<String, String> household() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("monthlyTakeHomeIncome", "\"4000\"");
        fields.put("monthlyEssentialExpenses", "\"2000\"");
        fields.put("starterEmergencyFund", "\"700\"");
        fields.put("fullEmergencyFund", "\"1500\"");
        fields.put("monthlyCollegeContribution", "\"100\"");
        fields.put("retirementContributionPercent", "\"6\"");
        fields.put("emergencyFundMonths", "4");
        fields.put("householdFlags", "{\"rents\": false, \"retired\": false, \"intendsToFundCollege\": true}");
        return fields;
    }

    private static Map<String, String> debt() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("label", "\"Card\"");
        fields.put("type", "\"CREDIT_CARD\"");
        fields.put("balance", "\"2500\"");
        fields.put("annualInterestRate", "\"21.5\"");
        fields.put("minimumPayment", "\"75\"");
        return fields;
    }

    private static Map<String, String> mortgage() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("balance", "\"120000\"");
        fields.put("annualInterestRate", "\"5.25\"");
        fields.put("monthlyPayment", "\"950\"");
        fields.put("homeValue", "\"300000\"");
        return fields;
    }

    private static void setOrRemove(Map<String, String> fields, String field, String value) {
        if ("MISSING".equals(value)) {
            fields.remove(field);
        } else {
            fields.put(field, value);
        }
    }

    private static String withDebts(String debts) {
        Map<String, String> fields = household();
        fields.put("debts", "[" + debts + "]");
        return json(fields);
    }

    private static String withMortgage(String mortgage) {
        Map<String, String> fields = household();
        fields.put("mortgage", mortgage);
        return json(fields);
    }

    private static String json(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(field -> "\"" + field.getKey() + "\": " + field.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
