package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * The single month-by-month amortization step shared by {@link PayoffScheduleCalculator} and
 * {@link MortgagePayoffCalculator}: interest accrues on the balance open at the start of the month, then the
 * payment (capped at what's owed) is applied. Both callers treat a loan's first payment as due in its {@code asOf}
 * month, so a loan that closes on its first step pays off in {@code asOf} itself. A mortgage's payment is its full
 * scheduled payment, taxes and insurance included (see {@link com.rungs.readiness.domain.model.Mortgage}), so both
 * callers amortize that whole payment rather than principal and interest alone; payoff dates are correspondingly
 * optimistic wherever taxes or insurance are bundled into the payment.
 */
final class Amortization {

    /** Longest amortization schedule considered before a loan is treated as never paying off (50 years). */
    static final int MAX_MONTHS = 600;

    /** Turns a percentage rate (6.125 means 6.125%) into a decimal monthly rate. */
    private static final BigDecimal PERCENT_MONTHS = BigDecimal.valueOf(1200);

    private Amortization() {
    }

    /** Returns {@code annualPercent} (6.125 means 6.125%) as a decimal monthly rate. */
    static BigDecimal monthlyRate(BigDecimal annualPercent) {
        return annualPercent.divide(PERCENT_MONTHS, MathContext.DECIMAL128);
    }

    /**
     * Returns one month's figures for a loan open at {@code balance} at the start of the month, accruing at
     * {@code monthlyRate}, with at most {@code paymentCap} applied.
     */
    static LoanMonth step(Money balance, BigDecimal monthlyRate, Money paymentCap) {
        Money interest = balance.times(monthlyRate);
        Money owed = balance.plus(interest);
        Money payment = owed.min(paymentCap);
        Money end = owed.minus(payment);
        return new LoanMonth(balance, interest, payment, end);
    }
}
