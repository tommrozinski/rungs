package com.rungs.readiness.web;

import java.util.List;

/**
 * One row of the payoff schedule's month-by-month table (design.md 040 section 2), formatted for display.
 *
 * @param monthLabel the calendar month, e.g. "Sep 2026"
 * @param extraCash the household's surplus this month
 * @param cascade the minimums of loans already closed by an earlier month
 * @param totalAvailable {@code extraCash} plus {@code cascade}
 * @param loanMonths each loan's figures this month, aligned by position with the schedule's loans
 * @param monthInterest the sum of every loan's interest this month
 * @param totalBalance the sum of every loan's ending balance this month
 * @param cumulativeInterest the running total of interest through this month
 * @param closed whether a loan closes this month, so the row gets a green tint
 */
record ScheduleMonthView(
        String monthLabel,
        String extraCash,
        String cascade,
        String totalAvailable,
        List<LoanMonthView> loanMonths,
        String monthInterest,
        String totalBalance,
        String cumulativeInterest,
        boolean closed) {
}
