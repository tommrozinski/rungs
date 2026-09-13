package com.rungs.readiness.application;

import static com.rungs.readiness.domain.model.FinancialSnapshotBuilder.aSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.Debt;
import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.HouseholdFlags;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SnapshotInputTests {

    @Test
    void fullySuppliedInputBuildsTheMatchingSnapshot() {
        SnapshotInput input = new SnapshotInput(
                "6,000.00", "3500", "$1,000", "12000.50", "250", "15.5", 6,
                List.of(new DebtInput("Car", "AUTO", "9000", "4.9", "310.25")),
                new MortgageInput("150000", "5.5", "1200", "300000"),
                new HouseholdFlagsInput(true, false, true));

        assertThat(input.toSnapshot()).isEqualTo(aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(6_000))
                .withMonthlyEssentialExpenses(Money.ofDollars(3_500))
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withFullEmergencyFund(Money.ofCents(1_200_050))
                .withMonthlyCollegeContribution(Money.ofDollars(250))
                .withRetirementContributionPercent("15.5")
                .withEmergencyFundMonths(6)
                .withDebt("Car", DebtType.AUTO, Money.ofDollars(9_000), "4.9", Money.ofCents(31_025))
                .withMortgage(new Mortgage(Money.ofDollars(150_000), Money.ofDollars(1_200), Money.ofDollars(300_000),
                        new BigDecimal("5.5")))
                .withHouseholdFlags(new HouseholdFlags(true, false, true))
                .build());
    }

    @Test
    void everyMissingValueTakesItsDefault() {
        SnapshotInput input = new SnapshotInput(null, null, null, null, null, null, null, null, null, null);

        assertThat(input.toSnapshot()).isEqualTo(aSnapshot().build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "\t\n", " ", "   "})
    void blankMoneyAndPercentageAreZero(String blank) {
        SnapshotInput input = new SnapshotInput(blank, blank, blank, blank, blank, blank, null, List.of(), null,
                new HouseholdFlagsInput(null, null, null));

        assertThat(input.toSnapshot()).isEqualTo(aSnapshot().build());
    }

    @Test
    void emptyDebtsAndAllFalseFlagsAreKept() {
        SnapshotInput input = new SnapshotInput("0", "0", "0", "0", "0", "0", 3, List.of(), null,
                new HouseholdFlagsInput(false, false, false));

        assertThat(input.toSnapshot()).isEqualTo(aSnapshot().build());
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
        "true, null, null, true, false, false",
        "null, true, null, false, true, false",
        "null, null, true, false, false, true"
    })
    void eachNullFlagIsFalse(Boolean rents, Boolean retired, Boolean college, boolean expectedRents,
            boolean expectedRetired, boolean expectedCollege) {
        SnapshotInput input = new SnapshotInput(null, null, null, null, null, null, null, null, null,
                new HouseholdFlagsInput(rents, retired, college));

        assertThat(input.toSnapshot().householdFlags())
                .isEqualTo(new HouseholdFlags(expectedRents, expectedRetired, expectedCollege));
    }

    @Test
    void blankMortgageValuesAreZeroAndTheMortgageIsKept() {
        SnapshotInput input = new SnapshotInput(null, null, null, null, null, null, null, null,
                new MortgageInput(null, "", " ", " "), null);

        assertThat(input.toSnapshot().mortgage())
                .isEqualTo(new Mortgage(Money.ZERO, Money.ZERO, Money.ZERO, BigDecimal.ZERO));
    }

    @Test
    void debtsGetPositionalIdsAndBlankLabelsAreNamedByPosition() {
        SnapshotInput input = withDebts(
                new DebtInput(null, "CREDIT_CARD", "100", "20", "10"),
                new DebtInput("", "AUTO", "200", "5", "20"),
                new DebtInput("  ", "MEDICAL", "300", "0", "30"),
                new DebtInput("Visa", "CREDIT_CARD", "400", "19.99", "40"));

        assertThat(input.toSnapshot().debts()).containsExactly(
                new Debt("debt-1", "Debt 1", DebtType.CREDIT_CARD, Money.ofDollars(100), new BigDecimal("20"),
                        Money.ofDollars(10)),
                new Debt("debt-2", "Debt 2", DebtType.AUTO, Money.ofDollars(200), new BigDecimal("5"),
                        Money.ofDollars(20)),
                new Debt("debt-3", "Debt 3", DebtType.MEDICAL, Money.ofDollars(300), BigDecimal.ZERO,
                        Money.ofDollars(30)),
                new Debt("debt-4", "Visa", DebtType.CREDIT_CARD, Money.ofDollars(400), new BigDecimal("19.99"),
                        Money.ofDollars(40)));
    }

    @Test
    void zeroBalanceDebtsAreRemovedAfterPositionsAreAssigned() {
        SnapshotInput input = withDebts(
                new DebtInput("Old card", "CREDIT_CARD", "0.00", "22", "35"),
                new DebtInput(" ", "STUDENT_LOAN", "5000", "4.5", "60"),
                new DebtInput(null, "OTHER", null, null, null),
                new DebtInput("Car", "AUTO", "8000", "6.5", "250"));

        assertThat(input.toSnapshot().debts()).containsExactly(
                new Debt("debt-2", "Debt 2", DebtType.STUDENT_LOAN, Money.ofDollars(5_000), new BigDecimal("4.5"),
                        Money.ofDollars(60)),
                new Debt("debt-4", "Car", DebtType.AUTO, Money.ofDollars(8_000), new BigDecimal("6.5"),
                        Money.ofDollars(250)));
    }

    @Test
    void blankDebtRateAndMinimumPaymentAreZero() {
        SnapshotInput input = withDebts(new DebtInput("Clinic", "MEDICAL", "800.50", "", " "));

        assertThat(input.toSnapshot().debts()).containsExactly(
                new Debt("debt-1", "Clinic", DebtType.MEDICAL, Money.ofCents(80_050), BigDecimal.ZERO, Money.ZERO));
    }

    @Test
    void percentagesArePaddedOrTrimmedOfSurroundingSpace() {
        SnapshotInput input = new SnapshotInput(null, null, null, null, null, " 12.5 ", null,
                List.of(new DebtInput("Card", "CREDIT_CARD", "10", "\t19.99 ", "1")),
                new MortgageInput("1", " 6.125 ", "1", "1"), null);

        assertThat(input.toSnapshot().retirementContributionPercent()).isEqualTo(new BigDecimal("12.50"));
        assertThat(input.toSnapshot().debts().getFirst().annualInterestRate()).isEqualTo(new BigDecimal("19.990"));
        assertThat(input.toSnapshot().mortgage().annualInterestRate()).isEqualTo(new BigDecimal("6.125"));
    }

    private static SnapshotInput withDebts(DebtInput... debts) {
        return new SnapshotInput(null, null, null, null, null, null, null, List.of(debts), null, null);
    }
}
