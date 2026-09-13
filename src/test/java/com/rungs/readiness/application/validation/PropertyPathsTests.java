package com.rungs.readiness.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.application.DebtInput;
import com.rungs.readiness.application.MortgageInput;
import com.rungs.readiness.application.SnapshotInput;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/**
 * The part of {@code JsonPointers} that turns a Jakarta Validation property path into a pointer, shared by the API
 * ({@code adapter.in.web.api.JsonPointers}) and {@link SnapshotValidator} so both report the same pointer for the
 * same violation.
 */
class PropertyPathsTests {

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
    void pointersSortSegmentBySegmentWithIndexesInNumericOrder() {
        List<String> pointers = new ArrayList<>(List.of("/monthlyTakeHomeIncome", "/debts/10/balance", "/debts",
                "/debts/2/type", "/debts/2/balance", "/debts/02x", "/emergencyFundMonths", "/debts/100", "/debts/9",
                "/mortgage/balance"));

        pointers.sort(PropertyPaths.ORDER);

        assertThat(pointers).containsExactly("/debts", "/debts/2/balance", "/debts/2/type", "/debts/9",
                "/debts/10/balance", "/debts/100", "/debts/02x", "/emergencyFundMonths", "/monthlyTakeHomeIncome",
                "/mortgage/balance");
    }

    private static List<String> pointers(SnapshotInput input, String prefix) {
        return FACTORY.getValidator().validate(input, FieldsThenWholeForm.class).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(path -> PropertyPaths.fromViolation(prefix, path))
                .toList();
    }

    private static SnapshotInput withDebts(List<DebtInput> debts) {
        return new SnapshotInput(null, null, null, null, null, null, null, debts, null, null);
    }
}
