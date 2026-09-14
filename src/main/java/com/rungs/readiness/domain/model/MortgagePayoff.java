package com.rungs.readiness.domain.model;

import java.time.YearMonth;

/**
 * The facts a mortgage's payoff schedule produces at a point in time, computed by
 * {@link MortgagePayoffCalculator}: the payoff at this payment alone, with no extra payments (design.md 041
 * section 3).
 *
 * @param principalAndInterest the full monthly payment
 * @param firstMonthInterest the interest the first month's payment accrues on the balance as supplied
 * @param covered whether {@code principalAndInterest} is above {@code firstMonthInterest}, so the loan amortizes
 *     at all
 * @param monthsToPayoff months until the balance reaches zero at this payment, or null when not {@code covered} or
 *     when it would take more than 600 months
 * @param payoffMonth the month the balance reaches zero, or null under the same conditions as {@code monthsToPayoff}
 */
public record MortgagePayoff(
        Money principalAndInterest, Money firstMonthInterest, boolean covered, Integer monthsToPayoff, YearMonth payoffMonth) {

    /** Validates the non-nullable components; throws NullPointerException naming the component. */
    public MortgagePayoff {
        Invariants.nonNull(principalAndInterest, "principalAndInterest");
        Invariants.nonNull(firstMonthInterest, "firstMonthInterest");
    }
}
