package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Money;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * One month of a {@link PayoffSchedule}.
 *
 * @param month the calendar month, starting from the snapshot's {@code asOf}
 * @param extraCash the household's monthly surplus, floored at {@link Money#ZERO}; the same every month
 * @param cascade the sum of the minimum payments of loans already closed by an earlier month
 * @param totalAvailable {@code extraCash} plus {@code cascade}
 * @param loanMonths each loan's figures this month, aligned by position with the schedule's loans
 * @param monthInterest the sum of every loan's interest this month
 * @param totalBalance the sum of every loan's ending balance this month
 * @param cumulativeInterest the running total of {@code monthInterest} through this month
 */
public record ScheduleMonth(
        YearMonth month,
        Money extraCash,
        Money cascade,
        Money totalAvailable,
        List<LoanMonth> loanMonths,
        Money monthInterest,
        Money totalBalance,
        Money cumulativeInterest) {

    /** Validates every component and copies {@code loanMonths}; throws NullPointerException naming the component. */
    public ScheduleMonth {
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(extraCash, "extraCash");
        Objects.requireNonNull(cascade, "cascade");
        Objects.requireNonNull(totalAvailable, "totalAvailable");
        loanMonths = List.copyOf(Objects.requireNonNull(loanMonths, "loanMonths"));
        Objects.requireNonNull(monthInterest, "monthInterest");
        Objects.requireNonNull(totalBalance, "totalBalance");
        Objects.requireNonNull(cumulativeInterest, "cumulativeInterest");
    }
}
