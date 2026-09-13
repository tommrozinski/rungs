package com.rungs.readiness.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Test fixture that builds a {@link FinancialSnapshot}, starting from a valid default: every amount $0, retirement
 * contribution 0%, 3 emergency-fund months, no debts, no mortgage, and every household flag false. Tests set only
 * the components they care about.
 */
public final class FinancialSnapshotBuilder {

    private Money monthlyTakeHomeIncome = Money.ZERO;
    private Money monthlyEssentialExpenses = Money.ZERO;
    private Money starterEmergencyFund = Money.ZERO;
    private Money fullEmergencyFund = Money.ZERO;
    private Money monthlyCollegeContribution = Money.ZERO;
    private BigDecimal retirementContributionPercent = BigDecimal.ZERO;
    private int emergencyFundMonths = 3;
    private List<Debt> debts = new ArrayList<>();
    private Mortgage mortgage;
    private HouseholdFlags householdFlags = new HouseholdFlags(false, false, false);

    /** Returns a builder holding the valid default snapshot. */
    public static FinancialSnapshotBuilder aSnapshot() {
        return new FinancialSnapshotBuilder();
    }

    public FinancialSnapshotBuilder withMonthlyTakeHomeIncome(Money amount) {
        monthlyTakeHomeIncome = amount;
        return this;
    }

    public FinancialSnapshotBuilder withMonthlyEssentialExpenses(Money amount) {
        monthlyEssentialExpenses = amount;
        return this;
    }

    public FinancialSnapshotBuilder withStarterEmergencyFund(Money amount) {
        starterEmergencyFund = amount;
        return this;
    }

    public FinancialSnapshotBuilder withFullEmergencyFund(Money amount) {
        fullEmergencyFund = amount;
        return this;
    }

    public FinancialSnapshotBuilder withMonthlyCollegeContribution(Money amount) {
        monthlyCollegeContribution = amount;
        return this;
    }

    public FinancialSnapshotBuilder withRetirementContributionPercent(BigDecimal percent) {
        retirementContributionPercent = percent;
        return this;
    }

    /** Sets the retirement contribution from a decimal string such as {@code "12.5"}. */
    public FinancialSnapshotBuilder withRetirementContributionPercent(String percent) {
        return withRetirementContributionPercent(new BigDecimal(percent));
    }

    public FinancialSnapshotBuilder withEmergencyFundMonths(int months) {
        emergencyFundMonths = months;
        return this;
    }

    /** Replaces the debts with {@code debts} as given, ids included; a null list is passed through as null. */
    public FinancialSnapshotBuilder withDebts(List<Debt> debts) {
        this.debts = debts == null ? null : new ArrayList<>(debts);
        return this;
    }

    /**
     * Appends a debt whose id is its 1-based position among the debts so far ({@code debt-1}, {@code debt-2}, ...).
     * Positional ids assume every earlier debt was also positional; after {@link #withDebts} with other ids,
     * {@link #build()} may reject duplicates.
     *
     * @param rate the annual interest rate as a decimal string such as {@code "7.25"}
     */
    public FinancialSnapshotBuilder withDebt(String label, DebtType type, Money balance, String rate, Money minimum) {
        debts.add(new Debt("debt-" + (debts.size() + 1), label, type, balance, new BigDecimal(rate), minimum));
        return this;
    }

    public FinancialSnapshotBuilder withMortgage(Mortgage mortgage) {
        this.mortgage = mortgage;
        return this;
    }

    public FinancialSnapshotBuilder withHouseholdFlags(HouseholdFlags flags) {
        householdFlags = flags;
        return this;
    }

    /** Constructs the snapshot, so the record's invariants apply. */
    public FinancialSnapshot build() {
        return new FinancialSnapshot(
                monthlyTakeHomeIncome,
                monthlyEssentialExpenses,
                starterEmergencyFund,
                fullEmergencyFund,
                monthlyCollegeContribution,
                retirementContributionPercent,
                emergencyFundMonths,
                debts,
                mortgage,
                householdFlags);
    }
}
