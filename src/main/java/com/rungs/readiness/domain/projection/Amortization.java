package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;

/**
 * The single month-by-month amortization step shared by {@link PayoffScheduleCalculator} and
 * {@link MortgagePayoffCalculator}: interest accrues on the balance open at the start of the month, then the
 * payment (capped at what's owed) is applied. Both callers treat a loan's first payment as due in its {@code asOf}
 * month, so a loan that closes on its first step pays off in {@code asOf} itself.
 */
final class Amortization {

    private Amortization() {
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
