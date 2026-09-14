package com.rungs.readiness.domain.model;

import java.time.YearMonth;

/**
 * The facts a mortgage's payoff schedule produces at a point in time, computed by
 * {@link MortgagePayoffCalculator}.
 *
 * @param principalAndInterest the full monthly payment
 * @param firstMonthInterest the interest the first month's payment accrues on the balance as supplied
 * @param covered whether {@code principalAndInterest} is above {@code firstMonthInterest}, so the loan amortizes
 *     at all
 * @param monthsToPayoff months until the balance reaches zero at this payment, or null when not {@code covered} or
 *     when it would take more than 600 months
 * @param payoffMonth the month the balance reaches zero, or null under the same conditions as {@code monthsToPayoff}
 * @param maturityComparison how {@code payoffMonth} compares to the mortgage's maturity, or {@code UNKNOWN} when
 *     the maturity or the payoff month is unavailable
 * @param monthsDifference the absolute number of months between {@code payoffMonth} and the maturity, or 0 when
 *     {@code maturityComparison} is {@code UNKNOWN}
 */
public record MortgagePayoff(
        Money principalAndInterest,
        Money firstMonthInterest,
        boolean covered,
        Integer monthsToPayoff,
        YearMonth payoffMonth,
        MaturityComparison maturityComparison,
        int monthsDifference) {

    /** Validates the non-nullable components; throws NullPointerException naming the component. */
    public MortgagePayoff {
        Invariants.nonNull(principalAndInterest, "principalAndInterest");
        Invariants.nonNull(firstMonthInterest, "firstMonthInterest");
        Invariants.nonNull(maturityComparison, "maturityComparison");
    }

    /** How a mortgage's projected payoff month compares to its stated maturity. */
    public enum MaturityComparison {
        /** The maturity, the payoff month, or both are unavailable. */
        UNKNOWN,
        /** The payoff month is within one month of the maturity. */
        MATCHES,
        /** The payoff month is at least two months before the maturity. */
        AHEAD,
        /** The payoff month is at least two months after the maturity. */
        BEHIND
    }
}
