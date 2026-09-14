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
import java.time.YearMonth;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** The lenient {@link SnapshotForm} to {@link FinancialSnapshot} conversion. */
class SnapshotFormMapperTests {

    private static final YearMonth ASOF = YearMonth.of(2026, 9);

    @Test
    void blankMoneyFieldsBecomeZero() {
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome("");
        form.setMonthlyEssentialExpenses("");
        form.setStarterEmergencyFund("");
        form.setFullEmergencyFund("");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ZERO);
        assertThat(snapshot.monthlyEssentialExpenses()).isEqualTo(Money.ZERO);
        assertThat(snapshot.starterEmergencyFund()).isEqualTo(Money.ZERO);
        assertThat(snapshot.fullEmergencyFund()).isEqualTo(Money.ZERO);
    }

    @Test
    void unparseableMoneyFieldBecomesZero() {
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome("not a number");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ZERO);
    }

    @Test
    void negativeMoneyFieldBecomesZero() {
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome("-500");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ZERO);
    }

    @Test
    void validMoneyFieldParsesExactly() {
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome("6200.50");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ofCents(620_050));
    }

    @Test
    void dollarSignAndCommaGroupedMoneyFieldParsesExactly() {
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome("$1,234.56");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ofCents(123_456));
    }

    @Test
    void overPreciseMoneyFieldRoundsInsteadOfRejecting() {
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome("12.345");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ofCents(1_235));
    }

    @Test
    void commaGroupedAndOverPreciseMoneyFieldBecomesZero() {
        // Money#parse rejects this for having more than two decimal places, and the over-precise fallback rejects
        // it right back for having a comma: the two leniencies don't compose, so the field becomes zero.
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome("1,000.555");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ZERO);
    }

    @Test
    void moneyFieldAboveTheBillionDollarCapIsCappedRatherThanOverflowing() {
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome("92233720368547748.07");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ofDollars(1_000_000_000));
    }

    @Test
    void twentyDigitMoneyFieldIsCappedRatherThanBecomingZero() {
        SnapshotForm form = new SnapshotForm();
        form.setMonthlyTakeHomeIncome("99999999999999999999");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ofDollars(1_000_000_000));
    }

    @Test
    void blankPercentFieldBecomesZero() {
        SnapshotForm form = new SnapshotForm();
        form.setRetirementContributionPercent("");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.retirementContributionPercent()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void negativePercentFieldBecomesZero() {
        SnapshotForm form = new SnapshotForm();
        form.setRetirementContributionPercent("-5");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.retirementContributionPercent()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void percentAbove100IsClampedTo100() {
        SnapshotForm form = new SnapshotForm();
        form.setRetirementContributionPercent("250");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.retirementContributionPercent()).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void overPrecisePercentRoundsInsteadOfRejecting() {
        SnapshotForm form = new SnapshotForm();
        form.setRetirementContributionPercent("6.256");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.retirementContributionPercent()).isEqualTo(new BigDecimal("6.26"));
    }

    @Test
    void zeroBalanceDebtRowIsDropped() {
        SnapshotForm form = new SnapshotForm();
        SnapshotForm.DebtRow row = new SnapshotForm.DebtRow();
        row.setLabel("Paid off card");
        row.setBalance("0");
        form.getDebts().add(row);

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

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

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

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

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

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

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.debts()).extracting(Debt::id).containsExactly("debt-1");
    }

    @Test
    void onlyTheFirstTwentyNonZeroDebtRowsAreKept() {
        SnapshotForm form = new SnapshotForm();
        for (int i = 0; i < 25; i++) {
            SnapshotForm.DebtRow row = new SnapshotForm.DebtRow();
            row.setLabel("Debt row " + i);
            row.setBalance("100");
            form.getDebts().add(row);
        }

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.debts()).hasSize(20);
        assertThat(snapshot.debts()).extracting(Debt::label).containsExactly(
                "Debt row 0", "Debt row 1", "Debt row 2", "Debt row 3", "Debt row 4",
                "Debt row 5", "Debt row 6", "Debt row 7", "Debt row 8", "Debt row 9",
                "Debt row 10", "Debt row 11", "Debt row 12", "Debt row 13", "Debt row 14",
                "Debt row 15", "Debt row 16", "Debt row 17", "Debt row 18", "Debt row 19");
    }

    @Test
    void fixedComponentsAreAlwaysSet() {
        SnapshotForm form = new SnapshotForm();

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.emergencyFundMonths()).isEqualTo(3);
        assertThat(snapshot.monthlyCollegeContribution()).isEqualTo(Money.ZERO);
    }

    @Test
    void blankHousingDefaultsToAnEmptyMortgage() {
        SnapshotForm form = new SnapshotForm();
        form.setHousing("");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.housing()).isEqualTo(new Mortgage(Money.ZERO, BigDecimal.ZERO, Money.ZERO));
    }

    @Test
    void unrecognizedHousingDefaultsToMortgage() {
        SnapshotForm form = new SnapshotForm();
        form.setHousing("SOMETHING_ELSE");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.housing()).isInstanceOf(Mortgage.class);
    }

    @Test
    void rentModeMapsToRentAndIgnoresMortgageFields() {
        SnapshotForm form = new SnapshotForm();
        form.setHousing("RENT");
        form.setMonthlyRent("1400");
        form.setMortgageBalance("285000");
        form.setMortgageRate("6.5");
        form.setMortgagePayment("2200");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.housing()).isEqualTo(new Rent(Money.ofDollars(1400)));
    }

    @Test
    void mortgageModeMapsToMortgageAndIgnoresTheRentField() {
        SnapshotForm form = new SnapshotForm();
        form.setHousing("MORTGAGE");
        form.setMortgageBalance("285000");
        form.setMortgageRate("6.5");
        form.setMortgagePayment("2200");
        form.setMonthlyRent("1400");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.housing())
                .isEqualTo(new Mortgage(Money.ofDollars(285_000), new BigDecimal("6.500"), Money.ofDollars(2_200)));
    }

    @Test
    void blankMortgageFieldsBecomeAZeroBalanceMortgage() {
        SnapshotForm form = new SnapshotForm();

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.housing()).isEqualTo(new Mortgage(Money.ZERO, BigDecimal.ZERO, Money.ZERO));
        assertThat(snapshot.monthlyHousingCost()).isEqualTo(Money.ZERO);
    }

    @Test
    void hostilePercentTextNeverThrowsAndBecomesZero() {
        SnapshotForm form = new SnapshotForm();
        form.setRetirementContributionPercent("1e-999999999");

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

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
        form.setMortgageBalance(hostile);
        form.setMortgageRate(hostile);
        form.setMortgagePayment(hostile);
        form.setMonthlyRent(hostile);
        SnapshotForm.DebtRow row = new SnapshotForm.DebtRow();
        row.setBalance(hostile);
        row.setAnnualInterestRate(hostile);
        row.setMinimumPayment(hostile);
        form.getDebts().add(row);

        form.setHousing("MORTGAGE");
        assertThatCode(() -> SnapshotFormMapper.toSnapshot(form, ASOF)).doesNotThrowAnyException();

        form.setHousing("RENT");
        assertThatCode(() -> SnapshotFormMapper.toSnapshot(form, ASOF)).doesNotThrowAnyException();
    }

    /** Last-resort guarantee: none of these hostile strings ever reach the mapper as an exception. */
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

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, ASOF);

        assertThat(snapshot.householdFlags().retired()).isTrue();
        assertThat(snapshot.householdFlags().intendsToFundCollege()).isTrue();
    }
}
