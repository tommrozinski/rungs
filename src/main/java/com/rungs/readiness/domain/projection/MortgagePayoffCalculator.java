package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Computes a mortgage's payoff at its current payment alone, with no extra payments. Pure and deterministic: every
 * fact comes from the {@link Mortgage} and the {@code asOf} month supplied by the caller; nothing here reads a
 * clock. Shares {@link Amortization#step} with {@link PayoffScheduleCalculator}, and the same convention that a
 * loan's first payment is due in its {@code asOf} month, so a lone mortgage with no household surplus to apply to
 * it amortizes identically whichever calculator runs it.
 */
public final class MortgagePayoffCalculator {

    private MortgagePayoffCalculator() {
    }

    /**
     * Returns the payoff facts for {@code mortgage} as of {@code asOf}; throws NullPointerException if either is
     * null.
     */
    public static MortgagePayoff payoff(Mortgage mortgage, YearMonth asOf) {
        Objects.requireNonNull(mortgage, "mortgage");
        Objects.requireNonNull(asOf, "asOf");

        Money payment = mortgage.monthlyPayment();
        BigDecimal monthlyRate = Amortization.monthlyRate(mortgage.annualInterestRate());
        Money firstMonthInterest = mortgage.balance().times(monthlyRate);

        if (mortgage.balance().isZero()) {
            return new MortgagePayoff.PaysOff(0, asOf);
        }

        boolean covered = payment.compareTo(firstMonthInterest) > 0;
        if (!covered) {
            return new MortgagePayoff.NotCovered(payment, firstMonthInterest);
        }

        OptionalInt months = monthsToPayoff(mortgage.balance(), payment, monthlyRate);
        if (months.isEmpty()) {
            return new MortgagePayoff.Capped();
        }
        return new MortgagePayoff.PaysOff(months.getAsInt(), asOf.plusMonths(months.getAsInt() - 1));
    }

    /**
     * Amortizes {@code balance} month by month in cents and returns the number of payments until it reaches zero,
     * or empty when it takes more than {@link Amortization#MAX_MONTHS}. A balance so close to the range of a
     * {@code long} that a month's interest or payment overflows it is treated the same as running past
     * {@link Amortization#MAX_MONTHS} rather than propagating the {@link ArithmeticException}.
     */
    private static OptionalInt monthsToPayoff(Money balance, Money payment, BigDecimal monthlyRate) {
        Money remaining = balance;
        try {
            for (int month = 1; month <= Amortization.MAX_MONTHS; month++) {
                LoanMonth step = Amortization.step(remaining, monthlyRate, payment);
                remaining = step.end();
                if (!remaining.isPositive()) {
                    return OptionalInt.of(month);
                }
            }
        } catch (ArithmeticException overflow) {
            return OptionalInt.empty();
        }
        return OptionalInt.empty();
    }
}
