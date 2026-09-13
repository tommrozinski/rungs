package com.rungs.readiness.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.application.DebtInput;
import com.rungs.readiness.application.ProjectionInput;
import com.rungs.readiness.application.SnapshotInput;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/**
 * {@link SnapshotValidator#validate(ProjectionInput)} (story 010, 011 REQ-011): validates the nested snapshot with
 * the same {@link FieldsThenWholeForm} sequence as {@link SnapshotValidator#validate(SnapshotInput)}, with every
 * pointer prefixed by {@code /snapshot}.
 */
class ProjectionInputValidationTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();

    private static final Validator VALIDATOR = FACTORY.getValidator();

    private final SnapshotValidator snapshotValidator = new SnapshotValidator(VALIDATOR);

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    private static SnapshotInput emptySnapshot() {
        return new SnapshotInput(null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void aValidProjectionInputHasNoViolations() {
        ProjectionInput input = new ProjectionInput(emptySnapshot(), "300.00", "SNOWBALL", 360);

        assertThat(snapshotValidator.validate(input)).isEmpty();
    }

    @Test
    void aMissingSnapshotIsInvalidTypeAtSnapshot() {
        ProjectionInput input = new ProjectionInput(null, null, null, null);

        List<FieldRejection> violations = snapshotValidator.validate(input);

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst().pointer()).isEqualTo("/snapshot");
        assertThat(violations.getFirst().rejection().code()).isEqualTo(ValidationCode.INVALID_TYPE);
    }

    @Test
    void aNestedSnapshotFieldViolationIsPrefixedWithSnapshot() {
        SnapshotInput snapshot = new SnapshotInput("-1", null, null, null, null, null, null, null, null, null);
        ProjectionInput input = new ProjectionInput(snapshot, null, null, null);

        List<FieldRejection> violations = snapshotValidator.validate(input);

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst().pointer()).isEqualTo("/snapshot/monthlyTakeHomeIncome");
        assertThat(violations.getFirst().rejection())
                .isEqualTo(new Rejection(ValidationCode.NEGATIVE, "Enter zero or more."));
    }

    @Test
    void aNestedDebtFieldViolationIsPrefixedWithSnapshot() {
        SnapshotInput snapshot = new SnapshotInput(null, null, null, null, null, null, null,
                List.of(new DebtInput(null, "AUTO", "-1", null, null)), null, null);
        ProjectionInput input = new ProjectionInput(snapshot, null, null, null);

        assertThat(snapshotValidator.validate(input)).extracting(FieldRejection::pointer)
                .containsExactly("/snapshot/debts/0/balance");
    }

    @Test
    void anInvalidExtraMonthlyPaymentIsReported() {
        ProjectionInput input = new ProjectionInput(emptySnapshot(), "-5", null, null);

        List<FieldRejection> violations = snapshotValidator.validate(input);

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst().pointer()).isEqualTo("/extraMonthlyPayment");
        assertThat(violations.getFirst().rejection().code()).isEqualTo(ValidationCode.NEGATIVE);
    }

    @Test
    void anUnknownStrategyIsReported() {
        ProjectionInput input = new ProjectionInput(emptySnapshot(), null, "FASTEST", null);

        List<FieldRejection> violations = snapshotValidator.validate(input);

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst().pointer()).isEqualTo("/strategy");
        assertThat(violations.getFirst().rejection().code()).isEqualTo(ValidationCode.INVALID_STRATEGY);
    }

    @Test
    void aHorizonOutsideOneToSixHundredIsReported() {
        ProjectionInput input = new ProjectionInput(emptySnapshot(), null, null, 601);

        List<FieldRejection> violations = snapshotValidator.validate(input);

        assertThat(violations).hasSize(1);
        assertThat(violations.getFirst().pointer()).isEqualTo("/horizonMonths");
        assertThat(violations.getFirst().rejection().code()).isEqualTo(ValidationCode.HORIZON_RANGE);
    }

    @Test
    void theWholeFormFiftyDebtRuleAppliesUnderSnapshotDebts() {
        SnapshotInput snapshot = new SnapshotInput(null, null, null, null, null, null, null,
                Collections.nCopies(51, new DebtInput(null, "AUTO", "100", null, null)), null, null);
        ProjectionInput input = new ProjectionInput(snapshot, null, null, null);

        List<FieldRejection> violations = snapshotValidator.validate(input);

        assertThat(violations).extracting(FieldRejection::pointer).containsExactly("/snapshot/debts");
        assertThat(violations.getFirst().rejection().code()).isEqualTo(ValidationCode.TOO_MANY_DEBTS);
    }
}
