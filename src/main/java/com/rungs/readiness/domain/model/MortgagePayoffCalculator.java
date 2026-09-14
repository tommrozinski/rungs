package com.rungs.readiness.domain.model;

import com.rungs.readiness.domain.model.MortgagePayoff.MaturityComparison;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Computes a mortgage's payoff schedule at this payment (design.md 039 section 2). Pure and deterministic: every
 * fact comes from the {@link Mortgage} and the {@code asOf} month supplied by the caller; nothing here reads a
 * clock.
 */
public final class MortgagePayoffCalculator {

    /** Turns a percentage rate (6.125 means 6.125%) into a decimal monthly rate. */
    private static final BigDecimal PERCENT_MONTHS = BigDecimal.valueOf(1200);

    /** Longest amortization considered before the loan is treated as never paying off (50 years).*/
    private static final int MAX_MONTHS = 600;

    /** At most this many months apart counts as matching the maturity. */
    private static final long MATCHES_TOLERANCE_MONTHS = 1;

    private MortgagePayoffCalculator() {
    }

    /**
     * Returns the payoff facts for {@code mortgage} as of {@code asOf}; throws NullPointerException if either is
     * null.
     */
    public static MortgagePayoff payoff(Mortgage mortgage, YearMonth asOf) {
        Objects.requireNonNull(mortgage, "mortgage");
        Objects.requireNonNull(asOf, "asOf");

        Money principalAndInterest = mortgage.monthlyPayment().minus(mortgage.monthlyPropertyTaxes());
        BigDecimal monthlyRate = mortgage.annualInterestRate().divide(PERCENT_MONTHS, MathContext.DECIMAL128);
        Money firstMonthInterest = mortgage.balance().times(monthlyRate);

        if (mortgage.balance().isZero()) {
            return withComparison(principalAndInterest, firstMonthInterest, true, 0, asOf, asOf, mortgage.maturity());
        }

        boolean covered = principalAndInterest.compareTo(firstMonthInterest) > 0;
        if (!covered) {
            return new MortgagePayoff(
                    principalAndInterest, firstMonthInterest, false, null, null, MaturityComparison.UNKNOWN, 0);
        }

        Integer months = monthsToPayoff(mortgage.balance(), principalAndInterest, monthlyRate);
        if (months == null) {
            return new MortgagePayoff(
                    principalAndInterest, firstMonthInterest, true, null, null, MaturityComparison.UNKNOWN, 0);
        }
        YearMonth payoffMonth = asOf.plusMonths(months);
        return withComparison(
                principalAndInterest, firstMonthInterest, true, months, payoffMonth, payoffMonth, mortgage.maturity());
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

    /** Builds the result, filling in the maturity comparison for {@code payoffMonth} against {@code maturity}. */
    private static MortgagePayoff withComparison(
            Money principalAndInterest,
            Money firstMonthInterest,
            boolean covered,
            Integer monthsToPayoff,
            YearMonth payoffMonth,
            YearMonth comparisonMonth,
            YearMonth maturity) {
        if (maturity == null) {
            return new MortgagePayoff(
                    principalAndInterest,
                    firstMonthInterest,
                    covered,
                    monthsToPayoff,
                    payoffMonth,
                    MaturityComparison.UNKNOWN,
                    0);
        }
        long diff = ChronoUnit.MONTHS.between(maturity, comparisonMonth);
        MaturityComparison comparison;
        if (Math.abs(diff) <= MATCHES_TOLERANCE_MONTHS) {
            comparison = MaturityComparison.MATCHES;
        } else if (diff < 0) {
            comparison = MaturityComparison.AHEAD;
        } else {
            comparison = MaturityComparison.BEHIND;
        }
        return new MortgagePayoff(
                principalAndInterest,
                firstMonthInterest,
                covered,
                monthsToPayoff,
                payoffMonth,
                comparison,
                (int) Math.abs(diff));
    }
}
