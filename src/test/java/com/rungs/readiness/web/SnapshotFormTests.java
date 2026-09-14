package com.rungs.readiness.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.Money;
import org.junit.jupiter.api.Test;

/**
 * The length caps {@link SnapshotForm} and {@link SnapshotForm.DebtRow} setters apply to every echoed string field,
 * so an oversized submission cannot balloon into an oversized response when the value is echoed back into the form
 * or the result page's hidden fields.
 */
class SnapshotFormTests {

    @Test
    void debtRowLabelIsCappedAtSixtyCharacters() {
        SnapshotForm.DebtRow row = new SnapshotForm.DebtRow();

        row.setLabel("A".repeat(1000));

        assertThat(row.getLabel()).hasSize(60);
        assertThat(row.getLabel()).isEqualTo("A".repeat(60));
    }

    @Test
    void debtRowLabelSetterIsNullSafe() {
        SnapshotForm.DebtRow row = new SnapshotForm.DebtRow();

        row.setLabel(null);

        assertThat(row.getLabel()).isNull();
    }

    @Test
    void moneyTextFieldsAreCappedAtMoneyMaxTextLengthPlusOne() {
        SnapshotForm form = new SnapshotForm();
        String huge = "9".repeat(1000);

        form.setMonthlyTakeHomeIncome(huge);
        form.setMonthlyEssentialExpenses(huge);
        form.setStarterEmergencyFund(huge);
        form.setFullEmergencyFund(huge);
        form.setRetirementContributionPercent(huge);
        form.setMortgageBalance(huge);
        form.setMortgageRate(huge);
        form.setMortgagePayment(huge);
        form.setMonthlyRent(huge);

        int expected = Money.MAX_TEXT_LENGTH + 1;
        assertThat(form.getMonthlyTakeHomeIncome()).hasSize(expected);
        assertThat(form.getMonthlyEssentialExpenses()).hasSize(expected);
        assertThat(form.getStarterEmergencyFund()).hasSize(expected);
        assertThat(form.getFullEmergencyFund()).hasSize(expected);
        assertThat(form.getRetirementContributionPercent()).hasSize(expected);
        assertThat(form.getMortgageBalance()).hasSize(expected);
        assertThat(form.getMortgageRate()).hasSize(expected);
        assertThat(form.getMortgagePayment()).hasSize(expected);
        assertThat(form.getMonthlyRent()).hasSize(expected);
    }

    @Test
    void debtRowMoneyTextFieldsAreCappedAtMoneyMaxTextLengthPlusOne() {
        SnapshotForm.DebtRow row = new SnapshotForm.DebtRow();
        String huge = "9".repeat(1000);

        row.setBalance(huge);
        row.setAnnualInterestRate(huge);
        row.setMinimumPayment(huge);

        int expected = Money.MAX_TEXT_LENGTH + 1;
        assertThat(row.getBalance()).hasSize(expected);
        assertThat(row.getAnnualInterestRate()).hasSize(expected);
        assertThat(row.getMinimumPayment()).hasSize(expected);
    }

    @Test
    void housingIsCappedAtSixteenCharacters() {
        SnapshotForm form = new SnapshotForm();

        form.setHousing("X".repeat(1000));

        assertThat(form.getHousing()).hasSize(16);
    }

    @Test
    void sampleIsCappedAtThirtyTwoCharacters() {
        SnapshotForm form = new SnapshotForm();

        form.setSample("X".repeat(1000));

        assertThat(form.getSample()).hasSize(32);
    }

    @Test
    void debtRowTypeIsCappedAtSixteenCharacters() {
        SnapshotForm.DebtRow row = new SnapshotForm.DebtRow();

        row.setType("X".repeat(1000));

        assertThat(row.getType()).hasSize(16);
    }

    @Test
    void shortValuesAreNotPadded() {
        SnapshotForm form = new SnapshotForm();

        form.setMonthlyTakeHomeIncome("6200");

        assertThat(form.getMonthlyTakeHomeIncome()).isEqualTo("6200");
    }
}
