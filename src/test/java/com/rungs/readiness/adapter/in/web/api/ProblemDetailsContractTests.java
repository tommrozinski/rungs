package com.rungs.readiness.adapter.in.web.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

/** Contract tests for the RFC 9457 problem details of {@code POST /v1/assessments} (story 011). */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ProblemDetailsContractTests {

    private static final String URI = "/v1/assessments";

    private static final String VALID_DEBT =
            "{\"label\": \"Card\", \"type\": \"CREDIT_CARD\", \"balance\": \"100\", \"annualInterestRate\": \"19.9\","
                    + " \"minimumPayment\": \"25\"}";

    private static final String ZERO_BALANCE_DEBT = "{\"type\": \"AUTO\", \"balance\": \"0.00\"}";

    private final MockMvcTester mvc;

    @Autowired
    ProblemDetailsContractTests(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void aFieldViolationIsAValidationProblemWithOneError() {
        assertThat(post("{\"monthlyTakeHomeIncome\": \"-1\"}"))
                .hasStatus(400)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .hasBodyTextEqualTo("{\"type\":\"urn:rungs:problem:validation\",\"title\":\"Some fields need attention.\","
                        + "\"status\":400,\"detail\":\"1 field needs attention.\",\"errors\":["
                        + "{\"pointer\":\"/monthlyTakeHomeIncome\",\"code\":\"NEGATIVE\","
                        + "\"message\":\"Enter zero or more.\"}]}");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "{\"monthlyEssentialExpenses\": \"-5\"}                   | /monthlyEssentialExpenses      | NEGATIVE"
            + "         | Enter zero or more.",
        "{\"mortgage\": {\"balance\": \"1,00\"}}                  | /mortgage/balance              | INVALID_AMOUNT"
            + "   | Enter an amount like 1,234.56.",
        "{\"fullEmergencyFund\": \"1000000000.01\"}               | /fullEmergencyFund             | AMOUNT_TOO_LARGE"
            + " | Enter an amount of $1,000,000,000.00 or less.",
        "{\"retirementContributionPercent\": \"1E2\"}             | /retirementContributionPercent | INVALID_PERCENT"
            + "  | Enter a percentage like 7.25.",
        "{\"mortgage\": {\"annualInterestRate\": \"101\"}}        | /mortgage/annualInterestRate   | PERCENT_RANGE"
            + "    | Enter a percentage from 0 to 100.",
        "{\"retirementContributionPercent\": \"15.125\"}          | /retirementContributionPercent | TOO_PRECISE"
            + "      | Use at most 2 decimal places.",
        "{\"debts\": [{\"type\": \"AUTO\", \"balance\": \"1\", \"annualInterestRate\": \"7.1255\"}]}"
            + " | /debts/0/annualInterestRate | TOO_PRECISE | Use at most 3 decimal places.",
        "{\"emergencyFundMonths\": 2}                             | /emergencyFundMonths           | MONTHS_MIN"
            + "       | Enter 3 months or more.",
        "{\"debts\": [{\"type\": \"MORTGAGE\", \"balance\": \"1\"}]} | /debts/0/type               | MORTGAGE_AS_DEBT"
            + " | Enter your mortgage in the mortgage section, not as a debt.",
        "{\"debts\": [{\"type\": \"BOAT\", \"balance\": \"1\"}]}  | /debts/0/type                  | INVALID_CHOICE"
            + "   | Choose a debt type from the list.",
        "{\"debts\": [{\"balance\": \"1\"}]}                      | /debts/0/type                  | INVALID_CHOICE"
            + "   | Choose a debt type from the list.",
        "{\"debts\": [{\"type\": \"AUTO\", \"balance\": \"1\"}, null]} | /debts/1                  | INVALID_TYPE"
            + "     | Send this value as the JSON type the field expects."
    })
    void eachFieldRuleIsAValidationProblemWithItsPointerCodeAndMessage(String request, String pointer, String code,
            String message) {
        assertThat(post(request))
                .hasStatus(400)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .hasBodyTextEqualTo(oneError(pointer, code, message));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "{\"monthlyTakeHomeIncom\": \"5000\"}                                      | /monthlyTakeHomeIncom",
        "{\"debts\": [{\"id\": \"debt-1\", \"type\": \"AUTO\", \"balance\": \"1\"}]} | /debts/0/id",
        "{\"mortgage\": {\"balance\": \"1\", \"extra/~\": 1}}                      | /mortgage/extra~1~0"
    })
    void anUnknownPropertyIsAValidationProblemAtItsPointer(String request, String pointer) {
        assertThat(post(request))
                .hasStatus(400)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .hasBodyTextEqualTo(oneError(pointer, "UNKNOWN_FIELD", "Remove this field; it isn't part of the request."));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "{\"monthlyTakeHomeIncome\": 5000}                                 | /monthlyTakeHomeIncome",
        "{\"monthlyTakeHomeIncome\": 50.25}                                | /monthlyTakeHomeIncome",
        "{\"starterEmergencyFund\": true}                                  | /starterEmergencyFund",
        "{\"retirementContributionPercent\": 15}                           | /retirementContributionPercent",
        "{\"emergencyFundMonths\": 3.5}                                    | /emergencyFundMonths",
        "{\"emergencyFundMonths\": \"3\"}                                  | /emergencyFundMonths",
        "{\"emergencyFundMonths\": true}                                   | /emergencyFundMonths",
        "{\"debts\": {}}                                                   | /debts",
        "{\"debts\": [{\"type\": \"AUTO\", \"balance\": \"1\"}, {\"type\": \"AUTO\", \"balance\": 100}]} | /debts/1/balance",
        "{\"mortgage\": [\"1\"]}                                           | /mortgage",
        "{\"householdFlags\": {\"rents\": [true]}}                         | /householdFlags/rents"
    })
    void aValueOfTheWrongJsonTypeIsAnInvalidTypeAtItsPointer(String request, String pointer) {
        assertThat(post(request))
                .hasStatus(400)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .hasBodyTextEqualTo(oneError(pointer, "INVALID_TYPE",
                        "Send this value as the JSON type the field expects."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not json", "{\"monthlyTakeHomeIncome\": ", "", "[]", "null", "\"text\"", "42",
        "{\"a\" \"b\"}", "{} {}"})
    void aBodyThatIsNotASnapshotObjectIsAMalformedRequestWithoutErrors(String request) {
        assertThat(post(request))
                .hasStatus(400)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .hasBodyTextEqualTo("{\"type\":\"urn:rungs:problem:malformed-request\","
                        + "\"title\":\"The request body isn't valid JSON.\",\"status\":400,"
                        + "\"detail\":\"Send a JSON object that matches the request format.\"}");
    }

    @Test
    void aMisspeltNumericPropertyIsRejectedNotReadAsZero() {
        MvcTestResult misspelt = post("{\"monthlyTakeHomeIncom\": \"5000\", \"monthlyEssentialExpenses\": \"1\"}");

        assertThat(misspelt).hasStatus(400).bodyJson().extractingPath("$.errors[0].code").isEqualTo("UNKNOWN_FIELD");
        assertThat(misspelt).bodyText().doesNotContain("monthlySurplus");
    }

    @Test
    void everyViolationIsReportedInPointerOrderWithIndexesComparedNumerically() {
        List<String> debts = new ArrayList<>(Collections.nCopies(11, VALID_DEBT));
        debts.set(10, "{\"type\": \"AUTO\", \"balance\": \"-1\"}");
        debts.set(2, "{\"type\": \"BOAT\", \"balance\": \"x\"}");
        String request = "{\"monthlyTakeHomeIncome\": \"x\", \"emergencyFundMonths\": 1, \"debts\": ["
                + String.join(", ", debts) + "]}";

        MvcTestResult result = post(request);

        assertThat(result).hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("5 fields need attention.");
        assertThat(result).bodyJson().extractingPath("$.errors[*].pointer").asArray().containsExactly(
                "/debts/2/balance", "/debts/2/type", "/debts/10/balance", "/emergencyFundMonths",
                "/monthlyTakeHomeIncome");
        assertThat(result).bodyJson().extractingPath("$.errors[*].code").asArray().containsExactly(
                "INVALID_AMOUNT", "INVALID_CHOICE", "NEGATIVE", "MONTHS_MIN", "INVALID_AMOUNT");
    }

    @Test
    void noSubmittedValueAppearsInAProblemBodyOrTheLog(CapturedOutput output) {
        List<String> requests = List.of(
                "{\"monthlyTakeHomeIncome\": \"-987654.32\", \"retirementContributionPercent\": \"98765.4321\","
                        + " \"debts\": [{\"label\": \"Zyxwvut\", \"type\": \"QWERTYBOAT\", \"balance\": \"987654.321\"}]}",
                "{\"monthlyTakeHomeIncome\": 987654.32}",
                "{\"emergencyFundMonths\": \"987654\"}",
                "{\"extraField\": \"987654.32\"}",
                "{\"monthlyTakeHomeIncome\": \"987654.32\" \"Zyxwvut\"}");

        for (String request : requests) {
            assertThat(post(request)).hasStatus(400).bodyText().doesNotContain("987654").doesNotContain("98765")
                    .doesNotContain("Zyxwvut").doesNotContain("QWERTYBOAT").doesNotContain("Exception");
        }
        assertThat(output).doesNotContain("987654").doesNotContain("98765").doesNotContain("Zyxwvut")
                .doesNotContain("QWERTYBOAT").doesNotContain("WARN");
    }

    @Test
    void fiftyOneDebtsWithABalanceAreTooMany() {
        assertThat(post(withDebts(Collections.nCopies(51, VALID_DEBT))))
                .hasStatus(400)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .hasBodyTextEqualTo(oneError("/debts", "TOO_MANY_DEBTS", "Enter 50 debts or fewer."));
    }

    @Test
    void sixtyDebtsOfWhichTenHaveNoBalanceAreAssessed() {
        List<String> debts = new ArrayList<>(Collections.nCopies(50, VALID_DEBT));
        debts.addAll(5, Collections.nCopies(10, ZERO_BALANCE_DEBT));

        assertThat(post(withDebts(debts))).hasStatusOk().bodyJson().extractingPath("$.stages[1].blocker")
                .isEqualTo("You have $5,000.00 of debt across 50 debts.");
    }

    @Test
    void aTopLevelFieldViolationHidesTheWholeFormRule() {
        String request = "{\"monthlyTakeHomeIncome\": \"x\", \"debts\": ["
                + String.join(", ", Collections.nCopies(51, VALID_DEBT)) + "]}";

        assertThat(post(request)).hasStatus(400).hasBodyTextEqualTo(
                oneError("/monthlyTakeHomeIncome", "INVALID_AMOUNT", "Enter an amount like 1,234.56."));
    }

    @Test
    void aDebtFieldViolationHidesTheWholeFormRule() {
        List<String> debts = new ArrayList<>(Collections.nCopies(51, VALID_DEBT));
        debts.set(50, "{\"type\": \"AUTO\", \"balance\": \"100\", \"annualInterestRate\": \"x\"}");

        assertThat(post(withDebts(debts))).hasStatus(400).hasBodyTextEqualTo(
                oneError("/debts/50/annualInterestRate", "INVALID_PERCENT", "Enter a percentage like 7.25."));
    }

    @Test
    void aNegativeBalanceIsRejectedBeforeZeroBalanceDebtsAreRemoved() {
        assertThat(post(withDebts(List.of("{\"type\": \"OTHER\", \"balance\": \"-5\"}")))).hasStatus(400)
                .hasBodyTextEqualTo(oneError("/debts/0/balance", "NEGATIVE", "Enter zero or more."));
    }

    @Test
    void aBadTypeOnAZeroBalanceDebtIsRejectedBeforeTheDebtIsRemoved() {
        assertThat(post(withDebts(List.of("{\"type\": \"BOAT\", \"balance\": \"0\"}")))).hasStatus(400)
                .hasBodyTextEqualTo(oneError("/debts/0/type", "INVALID_CHOICE", "Choose a debt type from the list."));
    }

    @Test
    void theLargestAllowedValuesAreAssessedWithoutOverflow() {
        String billion = "\"1,000,000,000.00\"";
        String debt = "{\"type\": \"PERSONAL\", \"balance\": " + billion + ", \"annualInterestRate\": \"100\","
                + " \"minimumPayment\": " + billion + "}";
        String request = "{\"monthlyTakeHomeIncome\": " + billion + ", \"monthlyEssentialExpenses\": " + billion
                + ", \"starterEmergencyFund\": " + billion + ", \"fullEmergencyFund\": " + billion
                + ", \"monthlyCollegeContribution\": " + billion + ", \"retirementContributionPercent\": \"100\""
                + ", \"emergencyFundMonths\": 2147483647, \"debts\": [" + String.join(", ", Collections.nCopies(50, debt))
                + "], \"mortgage\": {\"balance\": " + billion + ", \"annualInterestRate\": \"100\", \"monthlyPayment\": "
                + billion + ", \"homeValue\": " + billion + "}, \"householdFlags\": {\"intendsToFundCollege\": true}}";

        MvcTestResult result = post(request);

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.monthlySurplus.amount")
                .isEqualTo("-50000000000.00");
    }

    private static String withDebts(List<String> debts) {
        return "{\"debts\": [" + String.join(", ", debts) + "]}";
    }

    private static String oneError(String pointer, String code, String message) {
        return "{\"type\":\"urn:rungs:problem:validation\",\"title\":\"Some fields need attention.\",\"status\":400,"
                + "\"detail\":\"1 field needs attention.\",\"errors\":[{\"pointer\":\"" + pointer + "\",\"code\":\""
                + code + "\",\"message\":\"" + message + "\"}]}";
    }

    private MvcTestResult post(String body) {
        return mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .content(body).exchange();
    }
}
