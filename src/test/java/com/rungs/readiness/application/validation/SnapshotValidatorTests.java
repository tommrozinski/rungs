package com.rungs.readiness.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.application.DebtInput;
import com.rungs.readiness.application.HouseholdFlagsInput;
import com.rungs.readiness.application.MortgageInput;
import com.rungs.readiness.application.SnapshotInput;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/**
 * {@link SnapshotValidator}: the one application-layer entry point that validates a {@link SnapshotInput}. It always
 * validates with {@link FieldsThenWholeForm}, so a plain {@code @Valid} could never silently skip the whole-form
 * 50-debt rule the way it could if callers ran their own validation (story 011 review finding).
 */
class SnapshotValidatorTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();

    private static final Validator VALIDATOR = FACTORY.getValidator();

    private final SnapshotValidator snapshotValidator = new SnapshotValidator(VALIDATOR);

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    @Test
    void aValidSnapshotHasNoViolations() {
        SnapshotInput input = new SnapshotInput("5000", "3000", "1000", "9000", "0", "15", 3, List.of(), null,
                new HouseholdFlagsInput(true, false, false));

        assertThat(snapshotValidator.validate(input)).isEmpty();
    }

    @Test
    void aFieldViolationCarriesItsPointerAndRejection() {
        SnapshotInput input = new SnapshotInput("-1", null, null, null, null, null, null, null, null, null);

        List<FieldRejection> violations = snapshotValidator.validate(input);

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst().pointer()).isEqualTo("/monthlyTakeHomeIncome");
        assertThat(violations.getFirst().rejection())
                .isEqualTo(new Rejection(ValidationCode.NEGATIVE, "Enter zero or more."));
    }

    @Test
    void violationsAreOrderedByPointer() {
        SnapshotInput input = withDebts(List.of(new DebtInput(null, "AUTO", "-1", null, null)));

        List<FieldRejection> violations = snapshotValidator.validate(
                new SnapshotInput("x", null, null, null, null, null, null, input.debts(), null, null));

        assertThat(violations).extracting(FieldRejection::pointer)
                .containsExactly("/debts/0/balance", "/monthlyTakeHomeIncome");
    }

    @Test
    void theWholeFormRuleRunsOnlyWhenEveryFieldRulePasses() {
        SnapshotInput input =
                withDebts(Collections.nCopies(51, new DebtInput(null, "AUTO", "100", null, null)));

        List<FieldRejection> violations = snapshotValidator.validate(input);

        assertThat(violations).extracting(FieldRejection::pointer).containsExactly("/debts");
        assertThat(violations.getFirst().rejection().code()).isEqualTo(ValidationCode.TOO_MANY_DEBTS);
    }

    @Test
    void validateDebtCountIgnoresFieldViolationsAndChecksOnlyTheWholeFormRule() {
        // The 51st debt's blank type would fail field validation and hide the whole-form rule under
        // validate()/FieldsThenWholeForm; validateDebtCount checks the debt count on its own regardless. Its balance
        // still counts, since AtMostFiftyDebts looks only at balance, not type.
        List<DebtInput> debts = new ArrayList<>(Collections.nCopies(50, new DebtInput(null, "AUTO", "100", null, null)));
        debts.add(new DebtInput(null, null, "100", null, null));

        List<FieldRejection> violations = snapshotValidator.validateDebtCount(withDebts(debts));

        assertThat(violations).extracting(FieldRejection::pointer).containsExactly("/debts");
        assertThat(violations.getFirst().rejection().code()).isEqualTo(ValidationCode.TOO_MANY_DEBTS);
    }

    @Test
    void validateDebtCountIsEmptyWhenFiftyOrFewerRealDebtsArePresent() {
        List<DebtInput> debts = new ArrayList<>(Collections.nCopies(50, new DebtInput(null, "AUTO", "100", null, null)));

        assertThat(snapshotValidator.validateDebtCount(withDebts(debts))).isEmpty();
    }

    @Test
    void aMortgageFieldPointerIsNestedUnderTheMortgage() {
        SnapshotInput input = new SnapshotInput(null, null, null, null, null, null, null, null,
                new MortgageInput(null, "101", null, null), null);

        assertThat(snapshotValidator.validate(input)).extracting(FieldRejection::pointer)
                .containsExactly("/mortgage/annualInterestRate");
    }

    @Test
    void validatingASingleTopLevelPropertyChecksOnlyThatField() {
        SnapshotInput input = new SnapshotInput("-1", "x", null, null, null, null, null, null, null, null);

        Optional<Rejection> rejection = snapshotValidator.validateProperty(input, "monthlyTakeHomeIncome");

        assertThat(rejection).contains(new Rejection(ValidationCode.NEGATIVE, "Enter zero or more."));
    }

    @Test
    void validatingAValidSinglePropertyReturnsEmpty() {
        SnapshotInput input = new SnapshotInput("100", null, null, null, null, null, null, null, null, null);

        assertThat(snapshotValidator.validateProperty(input, "monthlyTakeHomeIncome")).isEmpty();
    }

    @Test
    void validatingADebtPropertyChecksTheDebtInputDirectly() {
        DebtInput debt = new DebtInput(null, "BOAT", "1", null, null);

        Optional<Rejection> rejection = snapshotValidator.validateProperty(debt, "type");

        assertThat(rejection).contains(new Rejection(ValidationCode.INVALID_DEBT_TYPE,
                "Choose a debt type from the list."));
    }

    @Test
    void validatingAMortgagePropertyChecksTheMortgageInputDirectly() {
        MortgageInput mortgage = new MortgageInput(null, "101", null, null);

        Optional<Rejection> rejection = snapshotValidator.validateProperty(mortgage, "annualInterestRate");

        assertThat(rejection)
                .contains(new Rejection(ValidationCode.PERCENT_RANGE, "Enter a percentage from 0 to 100."));
    }

    @Test
    void constructorRejectsANullValidator() {
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> new SnapshotValidator(null)))
                .isInstanceOf(NullPointerException.class);
    }

    private static SnapshotInput withDebts(List<DebtInput> debts) {
        return new SnapshotInput(null, null, null, null, null, null, null, debts, null, null);
    }
}
