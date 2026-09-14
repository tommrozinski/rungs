package com.rungs.readiness.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Computes a mortgage's payoff schedule at this payment alone, with no extra payments (design.md 041 section 3).
 * Pure and deterministic: every fact comes from the {@link Mortgage} and the {@code asOf} month supplied by the
 * caller; nothing here reads a clock.
 */
public final class MortgagePayoffCalculator {

    /** Turns a percentage rate (6.125 means 6.125%) into a decimal monthly rate. */
    private static final BigDecimal PERCENT_MONTHS = BigDecimal.valueOf(1200);

    /** Longest amortization considered before the loan is treated as never paying off (50 years).*/
    private static final int MAX_MONTHS = 600;

    private MortgagePayoffCalculator() {
    }

    /**
     * Returns the payoff facts for {@code mortgage} as of {@code asOf}; throws NullPointerException if either is
     * null.
     */
    public static MortgagePayoff payoff(Mortgage mortgage, YearMonth asOf) {
        Objects.requireNonNull(mortgage, "mortgage");
        Objects.requireNonNull(asOf, "asOf");

        Money principalAndInterest = mortgage.monthlyPayment();
        BigDecimal monthlyRate = mortgage.annualInterestRate().divide(PERCENT_MONTHS, MathContext.DECIMAL128);
        Money firstMonthInterest = mortgage.balance().times(monthlyRate);

        if (mortgage.balance().isZero()) {
            return new MortgagePayoff(principalAndInterest, firstMonthInterest, true, 0, asOf);
        }

        boolean covered = principalAndInterest.compareTo(firstMonthInterest) > 0;
        if (!covered) {
            return new MortgagePayoff(principalAndInterest, firstMonthInterest, false, null, null);
        }

        Integer months = monthsToPayoff(mortgage.balance(), principalAndInterest, monthlyRate);
        if (months == null) {
            return new MortgagePayoff(principalAndInterest, firstMonthInterest, true, null, null);
        }
        return new MortgagePayoff(principalAndInterest, firstMonthInterest, true, months, asOf.plusMonths(months));
    }

    /**
     * Amortizes {@code balance} month by month in cents, interest first then payment, and returns the month it
     * reaches zero, or null when it takes more than {@link #MAX_MONTHS}. A balance so close to the range of a
     * {@code long} that a month's interest or payment overflows it is treated the same as running past
     * {@link #MAX_MONTHS} rather than propagating the {@link ArithmeticException}.
     */
    private static Integer monthsToPayoff(Money balance, Money payment, BigDecimal monthlyRate) {
        Money remaining = balance;
        try {
            for (int month = 1; month <= MAX_MONTHS; month++) {
                Money interest = remaining.times(monthlyRate);
                Money owed = remaining.plus(interest);
                Money paid = payment.min(owed);
                remaining = owed.minus(paid);
                if (!remaining.isPositive()) {
                    return month;
                }
            }
        } catch (ArithmeticException overflow) {
            return null;
        }
        return null;
    }
}
