package com.rungs.readiness.adapter.in.web.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
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
 * Contract tests for {@code POST /v1/projections} (story 010): exact response bodies through the real HTTP layer,
 * defaults, edge cases, and validation (story 011).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ProjectionContractTests {

    private static final String URI = "/v1/projections";

    /** design.md §3: the fixed three-debt scenario, with a snapshot sized to a $300 monthly surplus at stage 2. */
    private static final String THREE_DEBT_SNAPSHOT = """
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
    ProjectionContractTests(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void ac21ThreeDebtSnowballMatchesTheGoldenFile() throws IOException {
        String request = "{\"snapshot\": " + THREE_DEBT_SNAPSHOT
                + ", \"extraMonthlyPayment\": \"0.00\", \"strategy\": \"SNOWBALL\", \"horizonMonths\": 360}";

        MvcTestResult result = post(request);

        assertThat(result).hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
        assertThat(result.getResponse().getContentAsByteArray())
                .isEqualTo(golden("/golden/projection-three-debts-snowball.json"));
    }

    @Test
    void ac21ThreeDebtAvalancheMatchesTheGoldenFile() throws IOException {
        String request = "{\"snapshot\": " + THREE_DEBT_SNAPSHOT
                + ", \"extraMonthlyPayment\": \"0.00\", \"strategy\": \"AVALANCHE\", \"horizonMonths\": 360}";

        MvcTestResult result = post(request);

        assertThat(result).hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
        assertThat(result.getResponse().getContentAsByteArray())
                .isEqualTo(golden("/golden/projection-three-debts-avalanche.json"));
    }

    @Test
    void omittingEveryOptionUsesTheDocumentedDefaults() {
        String withDefaults = "{\"snapshot\": " + THREE_DEBT_SNAPSHOT + "}";
        String explicit = "{\"snapshot\": " + THREE_DEBT_SNAPSHOT
                + ", \"extraMonthlyPayment\": \"0\", \"strategy\": \"SNOWBALL\", \"horizonMonths\": 360}";

        byte[] withDefaultsBody = post(withDefaults).getResponse().getContentAsByteArray();
        byte[] explicitBody = post(explicit).getResponse().getContentAsByteArray();

        assertThat(withDefaultsBody).isNotEmpty().isEqualTo(explicitBody);
    }

    @Test
    void monthsToNextStageIsNullWhenTheCurrentStageIsNotTwo() {
        String request = "{\"snapshot\": {\"debts\": []}}";

        assertThat(post(request)).hasStatusOk().bodyJson().extractingPath("$.monthsToNextStage").isEqualTo(null);
    }

    @Test
    void noDebtsMeansZeroMonthsToDebtFreedomAndAnEmptySchedule() {
        String request = "{\"snapshot\": {\"debts\": []}}";

        MvcTestResult result = post(request);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.monthsToDebtFreedom").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.schedule").asArray().isEmpty();
    }

    @Test
    void aShortHorizonTruncatesTheScheduleWithNullMonths() {
        String snapshot = "{\"debts\": [{\"type\": \"CREDIT_CARD\", \"balance\": \"100000\","
                + " \"annualInterestRate\": \"6\", \"minimumPayment\": \"50\"}],"
                + " \"monthlyTakeHomeIncome\": \"5000\", \"monthlyEssentialExpenses\": \"4350\"}";
        String request = "{\"snapshot\": " + snapshot + ", \"horizonMonths\": 3}";

        MvcTestResult result = post(request);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.truncated").isEqualTo(true);
        assertThat(result).bodyJson().extractingPath("$.monthsToDebtFreedom").isEqualTo(null);
        assertThat(result).bodyJson().extractingPath("$.monthsToNextStage").isEqualTo(null);
        assertThat(result).bodyJson().extractingPath("$.schedule").asArray().hasSize(3);
    }

    @Test
    void aNegativeAmortizingDebtIsUnpayableButStillReturnsOk() {
        String snapshot = "{\"debts\": [{\"type\": \"CREDIT_CARD\", \"balance\": \"10000\","
                + " \"annualInterestRate\": \"100\", \"minimumPayment\": \"1\"}],"
                + " \"monthlyTakeHomeIncome\": \"1\", \"monthlyEssentialExpenses\": \"0\"}";
        String request = "{\"snapshot\": " + snapshot + ", \"horizonMonths\": 12}";

        MvcTestResult result = post(request);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.unpayableDebtIds").asArray().containsExactly("debt-1");
    }

    @Test
    void aRunawayBalanceIsTruncatedAndUnpayableRatherThanA500() {
        // Code review finding: the largest valid single debt at 100% APR with no minimum compounds past the
        // simulator's runaway ceiling long before the horizon, and previously overflowed a long (500).
        String snapshot = "{\"debts\": [{\"type\": \"CREDIT_CARD\", \"balance\": \"1000000000.00\","
                + " \"annualInterestRate\": \"100\", \"minimumPayment\": \"0\"}],"
                + " \"monthlyTakeHomeIncome\": \"10000\", \"monthlyEssentialExpenses\": \"3000\"}";
        String request = "{\"snapshot\": " + snapshot + ", \"horizonMonths\": 600}";

        MvcTestResult result = post(request);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.truncated").isEqualTo(true);
        assertThat(result).bodyJson().extractingPath("$.unpayableDebtIds").asArray().containsExactly("debt-1");
    }

    @Test
    void identicalRequestsProduceByteIdenticalBodies() {
        String request = "{\"snapshot\": " + THREE_DEBT_SNAPSHOT + "}";

        byte[] first = post(request).getResponse().getContentAsByteArray();
        byte[] second = post(request).getResponse().getContentAsByteArray();

        assertThat(first).isNotEmpty().isEqualTo(second);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 601})
    void horizonMonthsOutsideOneToSixHundredIsHorizonRange(int horizon) {
        String request = "{\"snapshot\": {}, \"horizonMonths\": " + horizon + "}";

        assertThat(post(request)).hasStatus(400).bodyJson().extractingPath("$.errors[0]").asMap()
                .containsExactly(
                        org.assertj.core.api.Assertions.entry("pointer", "/horizonMonths"),
                        org.assertj.core.api.Assertions.entry("code", "HORIZON_RANGE"),
                        org.assertj.core.api.Assertions.entry("message", "Enter a horizon from 1 to 600 months."));
    }

    @Test
    void anUnknownStrategyIsInvalidChoice() {
        String request = "{\"snapshot\": {}, \"strategy\": \"FASTEST\"}";

        assertThat(post(request)).hasStatus(400).bodyJson().extractingPath("$.errors[0]").asMap()
                .containsExactly(
                        org.assertj.core.api.Assertions.entry("pointer", "/strategy"),
                        org.assertj.core.api.Assertions.entry("code", "INVALID_CHOICE"),
                        org.assertj.core.api.Assertions.entry("message", "Choose snowball or avalanche."));
    }

    @Test
    void aNestedSnapshotFieldViolationIsPrefixedWithSnapshot() {
        String request = "{\"snapshot\": {\"debts\": [{\"type\": \"AUTO\", \"balance\": \"-1\"}]}}";

        assertThat(post(request)).hasStatus(400).bodyJson().extractingPath("$.errors[0].pointer")
                .isEqualTo("/snapshot/debts/0/balance");
    }

    @Test
    void aMissingSnapshotIsInvalidTypeAtSnapshot() {
        assertThat(post("{}")).hasStatus(400).bodyJson().extractingPath("$.errors[0]").asMap()
                .containsExactly(
                        org.assertj.core.api.Assertions.entry("pointer", "/snapshot"),
                        org.assertj.core.api.Assertions.entry("code", "INVALID_TYPE"),
                        org.assertj.core.api.Assertions.entry("message",
                                "Send this value as the JSON type the field expects."));
    }

    @Test
    void fiftyOneDebtsUnderSnapshotIsTooManyAtSnapshotDebts() {
        String debt = "{\"type\": \"AUTO\", \"balance\": \"1\"}";
        StringBuilder debts = new StringBuilder("[");
        for (int i = 0; i < 51; i++) {
            if (i > 0) {
                debts.append(", ");
            }
            debts.append(debt);
        }
        debts.append(']');
        String request = "{\"snapshot\": {\"debts\": " + debts + "}}";

        assertThat(post(request)).hasStatus(400).bodyJson().extractingPath("$.errors[0]").asMap()
                .containsExactly(
                        org.assertj.core.api.Assertions.entry("pointer", "/snapshot/debts"),
                        org.assertj.core.api.Assertions.entry("code", "TOO_MANY_DEBTS"),
                        org.assertj.core.api.Assertions.entry("message", "Enter 50 debts or fewer."));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "{\"snapshot\": {}, \"extraField\": \"1\"}         | /extraField",
        "{\"snapshot\": {\"debts\": [{\"id\": \"debt-1\", \"type\": \"AUTO\", \"balance\": \"1\"}]}} | /snapshot/debts/0/id"
    })
    void anUnknownFieldIsRejectedAtItsPointer(String request, String pointer) {
        assertThat(post(request)).hasStatus(400).bodyJson().extractingPath("$.errors[0]").asMap()
                .containsExactly(
                        org.assertj.core.api.Assertions.entry("pointer", pointer),
                        org.assertj.core.api.Assertions.entry("code", "UNKNOWN_FIELD"),
                        org.assertj.core.api.Assertions.entry("message",
                                "Remove this field; it isn't part of the request."));
    }

    @Test
    void noSubmittedValueAppearsInAProblemBodyOrTheLog(CapturedOutput output) {
        String request = "{\"snapshot\": {\"debts\": [{\"label\": \"Zyxwvut\", \"type\": \"QWERTYBOAT\","
                + " \"balance\": \"987654.321\"}]}}";

        assertThat(post(request)).hasStatus(400).bodyText().doesNotContain("Zyxwvut").doesNotContain("QWERTYBOAT")
                .doesNotContain("Exception");
        assertThat(output).doesNotContain("Zyxwvut").doesNotContain("QWERTYBOAT").doesNotContain("WARN");
    }

    private MvcTestResult post(String body) {
        return mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .content(body).exchange();
    }

    private static byte[] golden(String path) throws IOException {
        try (InputStream in = ProjectionContractTests.class.getResourceAsStream(path)) {
            assertThat(in).as("golden file %s", path).isNotNull();
            return in.readAllBytes();
        }
    }
}
