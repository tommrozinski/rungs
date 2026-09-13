package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.application.validation.FieldRejection;
import com.rungs.readiness.application.validation.Rejection;
import com.rungs.readiness.application.validation.ValidationCode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@link FormErrors}: turns {@link FieldRejection}s (pointer-keyed, as {@code SnapshotValidator} reports them) into
 * the form-field-name-keyed lookup the templates use, and the C-09 summary text (story 027 REQ-006).
 */
class FormErrorsTests {

    private static final Rejection NEGATIVE = new Rejection(ValidationCode.NEGATIVE, "Enter zero or more.");

    private static final Rejection TOO_MANY =
            new Rejection(ValidationCode.TOO_MANY_DEBTS, "Enter 50 debts or fewer.");

    @Test
    void anEmptyListOfViolationsHasNoSummary() {
        assertThat(FormErrors.summary(List.of())).isEmpty();
    }

    @Test
    void aSummaryUsesTheSameWordingAsTheApi() {
        List<FieldRejection> violations = List.of(new FieldRejection("/monthlyTakeHomeIncome", NEGATIVE));

        assertThat(FormErrors.summary(violations)).contains("1 field needs attention.");
    }

    @Test
    void aTopLevelPointerBecomesItsOwnFieldName() {
        Map<String, Rejection> byName =
                FormErrors.byName(List.of(new FieldRejection("/monthlyTakeHomeIncome", NEGATIVE)));

        assertThat(byName).containsEntry("monthlyTakeHomeIncome", NEGATIVE);
    }

    @Test
    void aDebtPointerBecomesBracketedIndexNotation() {
        Map<String, Rejection> byName = FormErrors.byName(List.of(new FieldRejection("/debts/2/balance", NEGATIVE)));

        assertThat(byName).containsEntry("debts[2].balance", NEGATIVE);
    }

    @Test
    void aMortgagePointerBecomesDottedNotation() {
        Map<String, Rejection> byName = FormErrors.byName(List.of(new FieldRejection("/mortgage/balance", NEGATIVE)));

        assertThat(byName).containsEntry("mortgage.balance", NEGATIVE);
    }

    @Test
    void theWholeFormDebtsPointerIsAvailableSeparatelyForTheTable() {
        List<FieldRejection> violations = List.of(new FieldRejection("/debts", TOO_MANY));

        Optional<Rejection> tableError = FormErrors.forTable(violations);

        assertThat(tableError).contains(TOO_MANY);
        assertThat(FormErrors.byName(violations)).doesNotContainKey("debts");
    }

    @Test
    void noTableErrorWhenThereIsNoWholeFormViolation() {
        assertThat(FormErrors.forTable(List.of(new FieldRejection("/monthlyTakeHomeIncome", NEGATIVE)))).isEmpty();
    }
}
