package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.application.DebtInput;
import com.rungs.readiness.application.HouseholdFlagsInput;
import com.rungs.readiness.application.MortgageInput;
import com.rungs.readiness.application.SnapshotInput;
import com.rungs.readiness.application.validation.FieldRejection;
import com.rungs.readiness.application.validation.Rejection;
import com.rungs.readiness.application.validation.ValidationCode;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@link SnapshotFormViews}: builds the template-ready view model from a {@code forForm()}-normalized snapshot. */
class SnapshotFormViewsTests {

    private static final Rejection NEGATIVE = new Rejection(ValidationCode.NEGATIVE, "Enter zero or more.");

    @Test
    void topLevelFieldsCarryTheirNameIdAndValue() {
        SnapshotInput normalized = blank().forForm();

        SnapshotFormView view = SnapshotFormViews.of(normalized, List.of());

        assertThat(view.income().name()).isEqualTo("monthlyTakeHomeIncome");
        assertThat(view.income().id()).isEqualTo("monthlyTakeHomeIncome");
        assertThat(view.income().value()).isEqualTo("0");
        assertThat(view.income().rejection()).isEmpty();
    }

    @Test
    void aTopLevelViolationMarksItsFieldAsAnError() {
        List<FieldRejection> violations = List.of(new FieldRejection("/monthlyTakeHomeIncome", NEGATIVE));

        SnapshotFormView view = SnapshotFormViews.of(blank().forForm(), violations);

        assertThat(view.income().state()).isEqualTo("error");
        assertThat(view.income().errorMessage()).isEqualTo("Enter zero or more.");
    }

    @Test
    void everyDebtRowCarriesItsIndexedNamesIdsAndRemoveAction() {
        SnapshotInput normalized = withDebts(new DebtInput("Card", "CREDIT_CARD", "100", "5", "10")).forForm();

        SnapshotFormView view = SnapshotFormViews.of(normalized, List.of());

        assertThat(view.debts()).hasSize(1);
        DebtRowView row = view.debts().getFirst();
        assertThat(row.label().name()).isEqualTo("debts[0].label");
        assertThat(row.label().id()).isEqualTo("debts-0-label");
        assertThat(row.label().value()).isEqualTo("Card");
        assertThat(row.balance().name()).isEqualTo("debts[0].balance");
        assertThat(row.balance().value()).isEqualTo("100");
        assertThat(row.rate().name()).isEqualTo("debts[0].annualInterestRate");
        assertThat(row.minimumPayment().name()).isEqualTo("debts[0].minimumPayment");
        assertThat(row.typeName()).isEqualTo("debts[0].type");
        assertThat(row.typeValue()).isEqualTo("CREDIT_CARD");
        assertThat(row.removeAction()).isEqualTo("/snapshot/debts/remove/0");
    }

    @Test
    void aDebtFieldViolationMarksThatRowsFieldAsAnError() {
        SnapshotInput normalized =
                withDebts(new DebtInput("Card", "CREDIT_CARD", "-1", "5", "10")).forForm();
        List<FieldRejection> violations = List.of(new FieldRejection("/debts/0/balance", NEGATIVE));

        SnapshotFormView view = SnapshotFormViews.of(normalized, violations);

        assertThat(view.debts().getFirst().balance().state()).isEqualTo("error");
    }

    @Test
    void everyDebtTypeExceptMortgageIsOffered() {
        SnapshotFormView view = SnapshotFormViews.of(blank().forForm(), List.of());

        assertThat(view.debtTypes()).containsExactly(
                "STUDENT_LOAN", "AUTO", "CREDIT_CARD", "MEDICAL", "PERSONAL", "OTHER");
    }

    @Test
    void theMortgageSectionIsVisibleWhenTheHouseholdDoesNotRent() {
        SnapshotInput normalized = new SnapshotInput(null, null, null, null, null, null, null, List.of(),
                new MortgageInput("100000", "5", "900", "250000"), new HouseholdFlagsInput(false, false, false))
                .forForm();

        SnapshotFormView view = SnapshotFormViews.of(normalized, List.of());

        assertThat(view.mortgage().visible()).isTrue();
        assertThat(view.mortgage().balance().name()).isEqualTo("mortgage.balance");
        assertThat(view.mortgage().balance().value()).isEqualTo("100000");
    }

    @Test
    void theMortgageSectionIsHiddenWhenTheHouseholdRents() {
        SnapshotInput normalized = new SnapshotInput(null, null, null, null, null, null, null, List.of(), null,
                new HouseholdFlagsInput(true, false, false)).forForm();

        assertThat(SnapshotFormViews.of(normalized, List.of()).mortgage().visible()).isFalse();
    }

    @Test
    void aMortgageFieldViolationMarksItsFieldAsAnError() {
        SnapshotInput normalized = blank().forForm();
        List<FieldRejection> violations = List.of(new FieldRejection("/mortgage/annualInterestRate", NEGATIVE));

        assertThat(SnapshotFormViews.of(normalized, violations).mortgage().rate().state()).isEqualTo("error");
    }

    @Test
    void theTableErrorAndSummaryComeFromTheViolationList() {
        Rejection tooMany = new Rejection(ValidationCode.TOO_MANY_DEBTS, "Enter 50 debts or fewer.");
        List<FieldRejection> violations = List.of(new FieldRejection("/debts", tooMany));

        SnapshotFormView view = SnapshotFormViews.of(blank().forForm(), violations);

        assertThat(view.tableError()).contains(tooMany);
        assertThat(view.summary()).contains("1 field needs attention.");
    }

    @Test
    void noTableErrorOrSummaryWhenThereAreNoViolations() {
        SnapshotFormView view = SnapshotFormViews.of(blank().forForm(), List.of());

        assertThat(view.tableError()).isEmpty();
        assertThat(view.summary()).isEmpty();
    }

    private static SnapshotInput blank() {
        return new SnapshotInput(null, null, null, null, null, null, null, null, null, null);
    }

    private static SnapshotInput withDebts(DebtInput... debts) {
        return new SnapshotInput(null, null, null, null, null, null, null, List.of(debts), null, null);
    }
}
