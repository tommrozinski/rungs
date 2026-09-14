package com.rungs.readiness.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A household's finances at a point in time. Always valid once constructed, and compared by value across the whole
 * graph, including the debt list in order and the housing.
 *
 * @param monthlyTakeHomeIncome income after taxes per month; at least zero
 * @param monthlyEssentialExpenses essential spending per month, not counting housing; at least zero
 * @param starterEmergencyFund cash set aside as the starter emergency fund; at least zero
 * @param fullEmergencyFund cash set aside as the full emergency fund; at least zero
 * @param monthlyCollegeContribution amount saved for college per month; at least zero. The stage rules use this,
 *     but callers that have no way to collect it from a household may default it to $0
 * @param retirementContributionPercent share of income contributed to retirement as a percentage, from 0 to 100
 *     inclusive, normalized to two decimal places; more decimal places are rejected
 * @param emergencyFundMonths months of essential expenses the full emergency fund should cover; at least 3, stored
 *     as supplied with no upper clamp. The stage rules support any value at or above the minimum, but callers that
 *     have no way to collect it from a household may default it to exactly 3 months
 * @param debts the consumer debts in the order supplied, with unique ids; an unmodifiable copy
 * @param housing how the household pays for its home; never null
 * @param householdFlags yes-or-no facts about the household
 * @param asOf the month the assessment is made as of; never null
 */
public record FinancialSnapshot(
        Money monthlyTakeHomeIncome,
        Money monthlyEssentialExpenses,
        Money starterEmergencyFund,
        Money fullEmergencyFund,
        Money monthlyCollegeContribution,
        BigDecimal retirementContributionPercent,
        int emergencyFundMonths,
        List<Debt> debts,
        Housing housing,
        HouseholdFlags householdFlags,
        YearMonth asOf) {

    /** Decimal places the retirement contribution percentage is normalized to. */
    public static final int RETIREMENT_PERCENT_SCALE = 2;

    /** Fewest months of essential expenses a full emergency fund may be sized for. */
    private static final int MIN_EMERGENCY_FUND_MONTHS = 3;

    /**
     * Validates every component, normalizes the percentage, and copies the debts; throws NullPointerException for a
     * null component or a null debt, and IllegalArgumentException for any other broken rule.
     */
    public FinancialSnapshot {
        Invariants.nonNegative(monthlyTakeHomeIncome, "monthlyTakeHomeIncome");
        Invariants.nonNegative(monthlyEssentialExpenses, "monthlyEssentialExpenses");
        Invariants.nonNegative(starterEmergencyFund, "starterEmergencyFund");
        Invariants.nonNegative(fullEmergencyFund, "fullEmergencyFund");
        Invariants.nonNegative(monthlyCollegeContribution, "monthlyCollegeContribution");
        retirementContributionPercent = Invariants.percent(
                retirementContributionPercent, RETIREMENT_PERCENT_SCALE, "retirementContributionPercent");
        if (emergencyFundMonths < MIN_EMERGENCY_FUND_MONTHS) {
            throw new IllegalArgumentException("emergencyFundMonths must be at least " + MIN_EMERGENCY_FUND_MONTHS);
        }
        debts = List.copyOf(Invariants.nonNull(debts, "debts"));
        Set<String> ids = HashSet.newHashSet(debts.size());
        for (Debt debt : debts) {
            if (!ids.add(debt.id())) {
                throw new IllegalArgumentException("debts must have unique ids");
            }
        }
        Invariants.nonNull(housing, "housing");
        Invariants.nonNull(householdFlags, "householdFlags");
        Invariants.nonNull(asOf, "asOf");
    }

    /**
     * Returns the sum of every debt's balance, or {@link Money#ZERO} with no debts; the housing is not included.
     * Throws ArithmeticException if the total does not fit in {@link Money}.
     */
    public Money totalDebtBalance() {
        return Money.sum(debts.stream().map(Debt::balance).toList());
    }

    /** Returns the household's monthly housing cost: the rent when renting, or the mortgage payment. */
    public Money monthlyHousingCost() {
        return switch (housing) {
            case Rent rent -> rent.monthlyRent();
            case Mortgage mortgage -> mortgage.monthlyPayment();
        };
    }

    /** Returns {@link #monthlyEssentialExpenses()} plus {@link #monthlyHousingCost()}. */
    public Money monthlyEssentialCosts() {
        return monthlyEssentialExpenses.plus(monthlyHousingCost());
    }

    /**
     * Returns income minus essential costs (including housing) minus the sum of every debt's minimum payment; the
     * result may be negative. Throws ArithmeticException if the result does not fit in {@link Money}.
     */
    public Money monthlySurplus() {
        Money minimumPayments = Money.sum(debts.stream().map(Debt::minimumPayment).toList());
        return monthlyTakeHomeIncome.minus(monthlyEssentialCosts()).minus(minimumPayments);
    }
}
