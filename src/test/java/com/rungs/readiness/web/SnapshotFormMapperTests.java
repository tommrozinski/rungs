package com.rungs.readiness.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.rungs.readiness.domain.model.Debt;
import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Mortgage;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Rent;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** The lenient {@link SnapshotForm} to {@link FinancialSnapshot} conversion (design.md 038 section 3). */
class SnapshotFormMapperTests {

    @Test
    void blankMoneyFieldsBecomeZero() {
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome("");
        form.setMonthlyEssentialExpenses("");
        form.setStarterEmergencyFund("");
        form.setFullEmergencyFund("");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ZERO);
        assertThat(snapshot.monthlyEssentialExpenses()).isEqualTo(Money.ZERO);
        assertThat(snapshot.starterEmergencyFund()).isEqualTo(Money.ZERO);
        assertThat(snapshot.fullEmergencyFund()).isEqualTo(Money.ZERO);
    }

    @Test
    void unparseableMoneyFieldBecomesZero() {
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome("not a number");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ZERO);
    }

    @Test
    void negativeMoneyFieldBecomesZero() {
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome("-500");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ZERO);
    }

    @Test
    void validMoneyFieldParsesExactly() {
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome("6200.50");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ofCents(620_050));
    }

    @Test
    void dollarSignAndCommaGroupedMoneyFieldParsesExactly() {
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome("$1,234.56");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ofCents(123_456));
    }

    @Test
    void overPreciseMoneyFieldRoundsInsteadOfRejecting() {
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome("12.345");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ofCents(1_235));
    }

    @Test
    void blankPercentFieldBecomesZero() {
        SnapshotForm form = new SnapshotForm();
        form.setRetirementContributionPercent("");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.retirementContributionPercent()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void negativePercentFieldBecomesZero() {
        SnapshotForm form = new SnapshotForm();
        form.setRetirementContributionPercent("-5");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.retirementContributionPercent()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void percentAbove100IsClampedTo100() {
        SnapshotForm form = new SnapshotForm();
        form.setRetirementContributionPercent("250");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.retirementContributionPercent()).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void overPrecisePercentRoundsInsteadOfRejecting() {
        SnapshotForm form = new SnapshotForm();
        form.setRetirementContributionPercent("6.256");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.retirementContributionPercent()).isEqualTo(new BigDecimal("6.26"));
    }

    @Test
    void zeroBalanceDebtRowIsDropped() {
        SnapshotForm form = new SnapshotForm();
        SnapshotForm.DebtRow row = new SnapshotForm.DebtRow();
        row.setLabel("Paid off card");
        row.setBalance("0");
        form.getDebts().add(row);

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.debts()).isEmpty();
    }

    @Test
    void blankLabelBecomesDebtNByOneBasedRowPosition() {
        SnapshotForm form = new SnapshotForm();
        SnapshotForm.DebtRow first = new SnapshotForm.DebtRow();
        first.setLabel("Store card");
        first.setBalance("500");
        SnapshotForm.DebtRow second = new SnapshotForm.DebtRow();
        second.setLabel("");
        second.setBalance("1000");
        form.getDebts().add(first);
        form.getDebts().add(second);

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.debts()).extracting(Debt::label).containsExactly("Store card", "Debt 2");
    }

    @Test
    void unknownDebtTypeBecomesOther() {
        SnapshotForm form = new SnapshotForm();
        SnapshotForm.DebtRow row = new SnapshotForm.DebtRow();
        row.setLabel("Mystery debt");
        row.setBalance("500");
        row.setType("NOT_A_REAL_TYPE");
        form.getDebts().add(row);

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.debts()).extracting(Debt::type).containsExactly(DebtType.OTHER);
    }

    @Test
    void keptDebtIdsArePositionalAmongKeptRowsOnly() {
        SnapshotForm form = new SnapshotForm();
        SnapshotForm.DebtRow droppedRow = new SnapshotForm.DebtRow();
        droppedRow.setLabel("Paid off");
        droppedRow.setBalance("0");
        SnapshotForm.DebtRow keptRow = new SnapshotForm.DebtRow();
        keptRow.setLabel("Car loan");
        keptRow.setBalance("9140");
        form.getDebts().add(droppedRow);
        form.getDebts().add(keptRow);

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.debts()).extracting(Debt::id).containsExactly("debt-1");
    }

    @Test
    void fixedComponentsAreAlwaysSet() {
        SnapshotForm form = new SnapshotForm();

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.emergencyFundMonths()).isEqualTo(3);
        assertThat(snapshot.monthlyCollegeContribution()).isEqualTo(Money.ZERO);
    }

    @Test
    void unrentedFormMapsToAZeroMortgageWithHousingCostZero() {
        SnapshotForm form = new SnapshotForm();
        form.setRents(false);

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.housing()).isEqualTo(new Mortgage(Money.ZERO, BigDecimal.ZERO, Money.ZERO, Money.ZERO, null));
        assertThat(snapshot.monthlyHousingCost()).isEqualTo(Money.ZERO);
    }

    @Test
    void rentedFormMapsToRentAtZero() {
        SnapshotForm form = new SnapshotForm();
        form.setRents(true);

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.housing()).isEqualTo(new Rent(Money.ZERO));
        assertThat(snapshot.monthlyHousingCost()).isEqualTo(Money.ZERO);
    }

    @Test
    void hostilePercentTextNeverThrowsAndBecomesZero() {
        SnapshotForm form = new SnapshotForm();
        form.setRetirementContributionPercent("1e-999999999");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.retirementContributionPercent()).isEqualTo(new BigDecimal("0.00"));
    }

    @ParameterizedTest
    @MethodSource("hostileNumericStrings")
    void hostileNumericTextNeverThrowsForAnyNumericField(String hostile) {
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome(hostile);
        form.setMonthlyEssentialExpenses(hostile);
        form.setStarterEmergencyFund(hostile);
        form.setFullEmergencyFund(hostile);
        form.setRetirementContributionPercent(hostile);
        SnapshotForm.DebtRow row = new SnapshotForm.DebtRow();
        row.setBalance(hostile);
        row.setAnnualInterestRate(hostile);
        row.setMinimumPayment(hostile);
        form.getDebts().add(row);

        assertThatCode(() -> SnapshotFormMapper.toSnapshot(form)).doesNotThrowAnyException();
    }

    /** Last-resort guarantee (design.md 038 section 3): none of these ever reach the mapper as an exception. */
    private static Stream<String> hostileNumericStrings() {
        return Stream.of(
                "1e-999999999",
                "1e999999999",
                "NaN",
                "Infinity",
                "-0",
                "  12.5  ",
                "100.0001",
                "9".repeat(5000),
                "0.000000000000000000001",
                "1,234.56",
                "$1,234.56",
                "",
                null);
    }

    @Test
    void householdFlagsMapDirectlyFromTheForm() {
        SnapshotForm form = new SnapshotForm();
        form.setRetired(true);
        form.setIntendsToFundCollege(true);

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form);

        assertThat(snapshot.householdFlags().retired()).isTrue();
        assertThat(snapshot.householdFlags().intendsToFundCollege()).isTrue();
    }
}
