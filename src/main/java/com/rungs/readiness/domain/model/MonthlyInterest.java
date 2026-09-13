package com.rungs.readiness.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

/**
 * The one formula for a month's interest on a balance (story 008 REQ-002; story 012 REQ-003): the annual rate
 * divides by 1200 to become a monthly factor, then the balance times that factor rounds half-even to the cent.
 * Used by both {@link com.rungs.readiness.domain.projection.PayoffSimulator} and
 * {@link com.rungs.readiness.domain.stage.WarningDetector}, so the schedule and the warning always agree.
 */
public final class MonthlyInterest {

    /** The monthly rate divisor: an annual percentage rate divides by 1200 to become a monthly factor. */
    private static final BigDecimal MONTHS_TIMES_PERCENT = BigDecimal.valueOf(1200);

    private MonthlyInterest() {
    }

    /**
     * Returns one month's interest on {@code balance} at {@code annualRate} (a percentage, so {@code 22.9} means
     * 22.9%), rounded half-even to the cent. Throws NullPointerException if either argument is null.
     */
    public static Money on(Money balance, BigDecimal annualRate) {
        Objects.requireNonNull(balance, "balance");
        Objects.requireNonNull(annualRate, "annualRate");
        BigDecimal monthlyRateFactor = annualRate.divide(MONTHS_TIMES_PERCENT, MathContext.DECIMAL128);
        return balance.times(monthlyRateFactor);
    }
}
