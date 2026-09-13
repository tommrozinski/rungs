package com.rungs.readiness.adapter.in.web.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.application.DebtInput;
import com.rungs.readiness.application.MortgageInput;
import com.rungs.readiness.application.SnapshotInput;
import com.rungs.readiness.application.validation.FieldsThenWholeForm;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.core.JacksonException;

class JsonPointersTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    @Test
    void aTopLevelPropertyIsOneSegment() {
        SnapshotInput input = new SnapshotInput("x", null, null, null, null, null, null, null, null, null);

        assertThat(pointers(input, "")).containsExactly("/monthlyTakeHomeIncome");
    }

    @Test
    void aDebtFieldCarriesItsIndexBeforeItsName() {
        SnapshotInput input = withDebts(Arrays.asList(
                new DebtInput(null, "AUTO", "1", null, null), new DebtInput(null, "AUTO", "1", null, null),
                new DebtInput(null, "AUTO", "-1", null, null)));

        assertThat(pointers(input, "")).containsExactly("/debts/2/balance");
    }

    @Test
    void aMissingDebtEntryIsItsIndex() {
        SnapshotInput input = withDebts(Arrays.asList(new DebtInput(null, "AUTO", "1", null, null), null));

        assertThat(pointers(input, "")).containsExactly("/debts/1");
    }

    @Test
    void aMortgageFieldIsNestedUnderTheMortgage() {
        SnapshotInput input = new SnapshotInput(null, null, null, null, null, null, null, null,
                new MortgageInput(null, "101", null, null), null);

        assertThat(pointers(input, "")).containsExactly("/mortgage/annualInterestRate");
    }

    @Test
    void theWholeFormRulePointsAtTheDebts() {
        SnapshotInput input = withDebts(Collections.nCopies(51, new DebtInput(null, "AUTO", "1", null, null)));

        assertThat(pointers(input, "")).containsExactly("/debts");
    }

    @Test
    void theSnapshotPrefixIsPrependedForNestedSnapshots() {
        SnapshotInput input = withDebts(List.of(new DebtInput(null, "AUTO", "-1", null, null)));

        assertThat(pointers(input, "/snapshot")).containsExactly("/snapshot/debts/0/balance");
    }

    @Test
    void aJacksonPathBecomesNamesAndIndexes() {
        List<JacksonException.Reference> path = List.of(new JacksonException.Reference(null, "debts"),
                new JacksonException.Reference(null, 10), new JacksonException.Reference(null, "id"));

        assertThat(JsonPointers.fromJackson("", path)).isEqualTo("/debts/10/id");
        assertThat(JsonPointers.fromJackson("/snapshot", path)).isEqualTo("/snapshot/debts/10/id");
    }

    @Test
    void anEmptyPathIsTheRootOrThePrefix() {
        assertThat(JsonPointers.fromJackson("", List.of())).isEmpty();
        assertThat(JsonPointers.fromJackson("/snapshot", List.of())).isEqualTo("/snapshot");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "a/b      | /a~1b",
        "a~b      | /a~0b",
        "~1       | /~01",
        "/~       | /~1~0",
        "plain    | /plain"
    })
    void tildeAndSlashAreEscapedInNames(String name, String pointer) {
        assertThat(JsonPointers.fromJackson("", List.of(new JacksonException.Reference(null, name))))
                .isEqualTo(pointer);
    }

    @Test
    void pointersSortSegmentBySegmentWithIndexesInNumericOrder() {
        List<String> pointers = new ArrayList<>(List.of("/monthlyTakeHomeIncome", "/debts/10/balance", "/debts",
                "/debts/2/type", "/debts/2/balance", "/debts/02x", "/emergencyFundMonths", "/debts/100", "/debts/9",
                "/mortgage/balance", "/debts/00000000000000000000000000000003", "/debts/99999999999999999999999"));

        pointers.sort(JsonPointers.ORDER);

        assertThat(pointers).containsExactly("/debts", "/debts/2/balance", "/debts/2/type",
                "/debts/00000000000000000000000000000003", "/debts/9", "/debts/10/balance", "/debts/100", "/debts/99999999999999999999999",
                "/debts/02x", "/emergencyFundMonths", "/monthlyTakeHomeIncome", "/mortgage/balance");
    }

    private static List<String> pointers(SnapshotInput input, String prefix) {
        return FACTORY.getValidator().validate(input, FieldsThenWholeForm.class).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(path -> JsonPointers.fromViolation(prefix, path))
                .toList();
    }

    private static SnapshotInput withDebts(List<DebtInput> debts) {
        return new SnapshotInput(null, null, null, null, null, null, null, debts, null, null);
    }
}
