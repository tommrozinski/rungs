package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Money;
import java.util.Objects;

/**
 * One loan's figures for one month of a {@link PayoffSchedule}, aligned by position with the schedule's loans. A
 * loan already closed by an earlier month shows every figure as {@link Money#ZERO}.
 *
 * @param open the balance owed at the start of the month, before this month's interest
 * @param interest this month's interest, open times the monthly rate, rounded half-even to the cent
 * @param payment the amount applied this month, at most {@code open} plus {@code interest}
 * @param end the balance owed after this month's payment, {@code open} plus {@code interest} minus {@code payment}
 */
public record LoanMonth(Money open, Money interest, Money payment, Money end) {

    /** Validates every component; throws NullPointerException naming the component. */
    public LoanMonth {
        Objects.requireNonNull(open, "open");
        Objects.requireNonNull(interest, "interest");
        Objects.requireNonNull(payment, "payment");
        Objects.requireNonNull(end, "end");
    }
}
