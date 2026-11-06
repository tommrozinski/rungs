package com.rungs.readiness.domain.model;

import static com.rungs.readiness.domain.model.FinancialSnapshotBuilder.aSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class FinancialSnapshotTests {

    private static final Debt CARD = new Debt(
            "debt-1", "Card", DebtType.CREDIT_CARD, Money.ofDollars(500), new BigDecimal("24.99"), Money.ofDollars(25));
    private static final Debt LOAN = new Debt(
            "debt-2", "Loan", DebtType.STUDENT_LOAN, Money.ofDollars(12_000), new BigDecimal("5.5"), Money.ofDollars(140));
    private static final Debt CLINIC = new Debt(
            "debt-3", "Clinic", DebtType.MEDICAL, Money.ofCents(80_050), BigDecimal.ZERO, Money.ZERO);

    private static final Mortgage MORTGAGE = new Mortgage(Money.ofDollars(90_000), new BigDecimal("3.75"), Money.ofDollars(800));

    @Test
    void builderDefaultIsAValidEmptyHousehold() {
        FinancialSnapshot snapshot = aSnapshot().build();

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ZERO);
        assertThat(snapshot.monthlyEssentialExpenses()).isEqualTo(Money.ZERO);
        assertThat(snapshot.starterEmergencyFund()).isEqualTo(Money.ZERO);
        assertThat(snapshot.fullEmergencyFund()).isEqualTo(Money.ZERO);
        assertThat(snapshot.monthlyCollegeContribution()).isEqualTo(Money.ZERO);
        assertThat(snapshot.retirementContributionPercent()).isEqualTo(new BigDecimal("0.00"));
        assertThat(snapshot.emergencyFundMonths()).isEqualTo(3);
        assertThat(snapshot.debts()).isEmpty();
        assertThat(snapshot.housing()).isEqualTo(new Rent(Money.ZERO));
        assertThat(snapshot.householdFlags()).isEqualTo(new HouseholdFlags(false, false));
        assertThat(snapshot.asOf()).isEqualTo(FinancialSnapshotBuilder.DEFAULT_AS_OF);
    }

    @Test
    void exposesEveryComponentAsSupplied() {
        HouseholdFlags flags = new HouseholdFlags(true, true);
        YearMonth asOf = YearMonth.of(2026, 9);

        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(6_000))
                .withMonthlyEssentialExpenses(Money.ofDollars(3_500))
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withFullEmergencyFund(Money.ofDollars(12_000))
                .withMonthlyCollegeContribution(Money.ofDollars(250))
                .withRetirementContributionPercent("15")
                .withEmergencyFundMonths(6)
                .withDebt("Car", DebtType.AUTO, Money.ofDollars(9_000), "4.9", Money.ofDollars(310))
                .withHousing(MORTGAGE)
                .withHouseholdFlags(flags)
                .withAsOf(asOf)
                .build();

        assertThat(snapshot.monthlyTakeHomeIncome()).isEqualTo(Money.ofDollars(6_000));
        assertThat(snapshot.monthlyEssentialExpenses()).isEqualTo(Money.ofDollars(3_500));
        assertThat(snapshot.starterEmergencyFund()).isEqualTo(Money.ofDollars(1_000));
        assertThat(snapshot.fullEmergencyFund()).isEqualTo(Money.ofDollars(12_000));
        assertThat(snapshot.monthlyCollegeContribution()).isEqualTo(Money.ofDollars(250));
        assertThat(snapshot.retirementContributionPercent()).isEqualTo(new BigDecimal("15.00"));
        assertThat(snapshot.emergencyFundMonths()).isEqualTo(6);
        assertThat(snapshot.debts()).containsExactly(
                new Debt("debt-1", "Car", DebtType.AUTO, Money.ofDollars(9_000), new BigDecimal("4.9"),
                        Money.ofDollars(310)));
        assertThat(snapshot.housing()).isEqualTo(MORTGAGE);
        assertThat(snapshot.householdFlags()).isEqualTo(flags);
        assertThat(snapshot.asOf()).isEqualTo(asOf);
    }

    @Test
    void equalComponentsConstructEqualSnapshots() {
        FinancialSnapshot first = aSnapshot()
                .withDebt("Visa", DebtType.CREDIT_CARD, Money.ofCents(50_000), "19.99", Money.ofDollars(25))
                .build();
        FinancialSnapshot second = aSnapshot()
                .withDebt("Visa", DebtType.CREDIT_CARD, Money.ofCents(50_000), "19.990", Money.ofDollars(25))
                .build();

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    void copiesTheDebtListSoLaterChangesToTheSourceDoNotLeakIn() {
        List<Debt> source = new ArrayList<>(List.of(CARD, LOAN));
        // Constructed directly, not through the builder, so the record's own copy is what is tested.
        FinancialSnapshot snapshot = new FinancialSnapshot(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO,
                BigDecimal.ZERO, 3, source, new Rent(Money.ZERO), new HouseholdFlags(false, false),
                FinancialSnapshotBuilder.DEFAULT_AS_OF);

        source.add(CLINIC);
        source.set(0, LOAN);

        assertThat(snapshot.debts()).containsExactly(CARD, LOAN);
    }

    @Test
    void exposesAnUnmodifiableDebtList() {
        List<Debt> debts = aSnapshot().withDebts(List.of(CARD)).build().debts();

        assertThatThrownBy(() -> debts.add(LOAN)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(debts::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void preservesDebtOrderExactlyAsSupplied() {
        FinancialSnapshot snapshot = aSnapshot().withDebts(List.of(CLINIC, CARD, LOAN)).build();

        assertThat(snapshot.debts()).containsExactly(CLINIC, CARD, LOAN);
    }

    @Test
    void reorderingTheDebtsProducesAnUnequalSnapshot() {
        FinancialSnapshot inOrder = aSnapshot().withDebts(List.of(CARD, LOAN)).build();
        FinancialSnapshot reordered = aSnapshot().withDebts(List.of(LOAN, CARD)).build();

        assertThat(reordered).isNotEqualTo(inOrder);
    }

    @Test
    void builderWithDebtAssignsPositionalIds() {
        FinancialSnapshot snapshot = aSnapshot()
                .withDebt("Card", DebtType.CREDIT_CARD, Money.ofDollars(500), "24.99", Money.ofDollars(25))
                .withDebt("Loan", DebtType.STUDENT_LOAN, Money.ofDollars(12_000), "5.5", Money.ofDollars(140))
                .build();

        assertThat(snapshot.debts()).containsExactly(CARD, LOAN);
    }

    @Test
    void rejectsDebtsThatShareAnId() {
        Debt sameIdAsCard = new Debt(
                "debt-1", "Other", DebtType.OTHER, Money.ofDollars(75), BigDecimal.ONE, Money.ZERO);
        FinancialSnapshotBuilder builder = aSnapshot().withDebts(List.of(CARD, LOAN, sameIdAsCard));

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("debts must have unique ids");
    }

    @Test
    void rejectsANullDebtList() {
        FinancialSnapshotBuilder builder = aSnapshot().withDebts(null);

        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("debts");
    }

    @Test
    void rejectsANullDebt() {
        FinancialSnapshotBuilder builder = aSnapshot().withDebts(Arrays.asList(CARD, null));

        assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "monthlyTakeHomeIncome",
        "monthlyEssentialExpenses",
        "starterEmergencyFund",
        "fullEmergencyFund",
        "monthlyCollegeContribution"
    })
    void acceptsZeroForAMoneyComponent(String component) {
        assertThat(withMoney(component, Money.ZERO).build()).isEqualTo(aSnapshot().build());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "monthlyTakeHomeIncome",
        "monthlyEssentialExpenses",
        "starterEmergencyFund",
        "fullEmergencyFund",
        "monthlyCollegeContribution"
    })
    void rejectsANegativeMoneyComponent(String component) {
        FinancialSnapshotBuilder builder = withMoney(component, Money.ofCents(-1));

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(component + " must not be negative");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "monthlyTakeHomeIncome",
        "monthlyEssentialExpenses",
        "starterEmergencyFund",
        "fullEmergencyFund",
        "monthlyCollegeContribution"
    })
    void rejectsANullMoneyComponent(String component) {
        FinancialSnapshotBuilder builder = withMoney(component, null);

        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessage(component);
    }

    @Test
    void rejectsANullRetirementContributionPercent() {
        FinancialSnapshotBuilder builder = aSnapshot().withRetirementContributionPercent((BigDecimal) null);

        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("retirementContributionPercent");
    }

    @Test
    void rejectsNullHouseholdFlags() {
        FinancialSnapshotBuilder builder = aSnapshot().withHouseholdFlags(null);

        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("householdFlags");
    }

    @Test
    void rejectsNullHousing() {
        FinancialSnapshotBuilder builder = aSnapshot().withHousing(null);

        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("housing");
    }

    @Test
    void rejectsANullAsOf() {
        FinancialSnapshotBuilder builder = aSnapshot().withAsOf(null);

        assertThatThrownBy(builder::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("asOf");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.5", "12.50", "12.500"})
    void retirementPercentsDifferingOnlyInTrailingZerosConstructEqualSnapshots(String percent) {
        FinancialSnapshot snapshot = aSnapshot().withRetirementContributionPercent(percent).build();

        assertThat(snapshot).isEqualTo(aSnapshot().withRetirementContributionPercent("12.50").build());
        assertThat(snapshot.retirementContributionPercent().scale()).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({"0, 0.00", "100, 100.00", "0.01, 0.01", "99.99, 99.99"})
    void acceptsRetirementPercentsAtAndInsideTheBoundaries(String percent, String expected) {
        FinancialSnapshot snapshot = aSnapshot().withRetirementContributionPercent(percent).build();

        assertThat(snapshot.retirementContributionPercent()).isEqualTo(new BigDecimal(expected));
    }

    @ParameterizedTest
    @CsvSource({
        "-0.01, retirementContributionPercent must be between 0 and 100",
        "100.01, retirementContributionPercent must be between 0 and 100",
        "12.505, retirementContributionPercent has more than 2 decimal places"
    })
    void rejectsAnInvalidRetirementPercentWithoutRounding(String percent, String expectedMessage) {
        FinancialSnapshotBuilder builder = aSnapshot().withRetirementContributionPercent(percent);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage)
                .hasMessageNotContaining(percent);
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 6, 7, 12, Integer.MAX_VALUE})
    void storesEmergencyFundMonthsOfThreeOrMoreWithoutClamping(int months) {
        assertThat(aSnapshot().withEmergencyFundMonths(months).build().emergencyFundMonths()).isEqualTo(months);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 0, -1, Integer.MIN_VALUE})
    void rejectsFewerThanThreeEmergencyFundMonths(int months) {
        FinancialSnapshotBuilder builder = aSnapshot().withEmergencyFundMonths(months);

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("emergencyFundMonths must be at least 3");
    }

    @Test
    void totalDebtBalanceIsZeroWithoutDebts() {
        assertThat(aSnapshot().build().totalDebtBalance()).isEqualTo(Money.ZERO);
    }

    @Test
    void totalDebtBalanceOfOneDebtIsItsBalance() {
        assertThat(aSnapshot().withDebts(List.of(CLINIC)).build().totalDebtBalance()).isEqualTo(Money.ofCents(80_050));
    }

    @Test
    void totalDebtBalanceSumsEveryDebtAndIgnoresTheHousing() {
        FinancialSnapshot snapshot =
                aSnapshot().withDebts(List.of(CARD, LOAN, CLINIC)).withHousing(MORTGAGE).build();

        assertThat(snapshot.totalDebtBalance()).isEqualTo(Money.ofCents(50_000 + 1_200_000 + 80_050));
    }

    // monthlyHousingCost

    @Test
    void monthlyHousingCostIsTheRentWhenRenting() {
        FinancialSnapshot snapshot = aSnapshot().withHousing(new Rent(Money.ofDollars(1_250))).build();

        assertThat(snapshot.monthlyHousingCost()).isEqualTo(Money.ofDollars(1_250));
    }

    @Test
    void monthlyHousingCostIsThePaymentWhenTheMortgagePaymentIsAboveZero() {
        Mortgage mortgage = new Mortgage(Money.ofDollars(180_000), new BigDecimal("6.5"), Money.ofDollars(1_800));
        FinancialSnapshot snapshot = aSnapshot().withHousing(mortgage).build();

        assertThat(snapshot.monthlyHousingCost()).isEqualTo(Money.ofDollars(1_800));
    }

    @Test
    void monthlyHousingCostIsZeroWhenTheMortgagePaymentIsZero() {
        Mortgage paidOff = new Mortgage(Money.ZERO, BigDecimal.ZERO, Money.ZERO);
        FinancialSnapshot snapshot = aSnapshot().withHousing(paidOff).build();

        assertThat(snapshot.monthlyHousingCost()).isEqualTo(Money.ZERO);
    }

    // monthlyEssentialCosts

    @Test
    void monthlyEssentialCostsAddsHousingToEssentialExpenses() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyEssentialExpenses(Money.ofDollars(2_600))
                .withHousing(new Rent(Money.ofDollars(1_400)))
                .build();

        assertThat(snapshot.monthlyEssentialCosts()).isEqualTo(Money.ofDollars(4_000));
    }

    // monthlySurplus

    @Test
    void monthlySurplusSubtractsExpensesHousingAndEveryMinimumPayment() {
        Mortgage mortgage = new Mortgage(Money.ofDollars(90_000), new BigDecimal("3.75"), Money.ofDollars(800));
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(5_000))
                .withMonthlyEssentialExpenses(Money.ofDollars(3_000))
                .withDebts(List.of(CARD, LOAN, CLINIC))
                .withHousing(mortgage)
                .build();

        assertThat(snapshot.monthlySurplus()).isEqualTo(Money.ofDollars(5_000 - 3_000 - 800 - 25 - 140));
    }

    @Test
    void monthlySurplusIsIncomeMinusExpensesWithoutDebtsOrHousing() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofCents(400_050))
                .withMonthlyEssentialExpenses(Money.ofDollars(1_000))
                .build();

        assertThat(snapshot.monthlySurplus()).isEqualTo(Money.ofCents(300_050));
    }

    @Test
    void monthlySurplusMayBeNegative() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(2_000))
                .withMonthlyEssentialExpenses(Money.ofDollars(1_900))
                .withDebts(List.of(LOAN))
                .build();

        assertThat(snapshot.monthlySurplus()).isEqualTo(Money.ofDollars(-40));
    }

    private static FinancialSnapshotBuilder withMoney(String component, Money amount) {
        FinancialSnapshotBuilder builder = aSnapshot();
        return switch (component) {
            case "monthlyTakeHomeIncome" -> builder.withMonthlyTakeHomeIncome(amount);
            case "monthlyEssentialExpenses" -> builder.withMonthlyEssentialExpenses(amount);
            case "starterEmergencyFund" -> builder.withStarterEmergencyFund(amount);
            case "fullEmergencyFund" -> builder.withFullEmergencyFund(amount);
            case "monthlyCollegeContribution" -> builder.withMonthlyCollegeContribution(amount);
            default -> throw new IllegalArgumentException(component);
        };
    }
}
