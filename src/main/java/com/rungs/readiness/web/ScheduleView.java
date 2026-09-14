package com.rungs.readiness.web;

import java.util.List;

/**
 * The payoff schedule page's view model, built from a
 * {@link com.rungs.readiness.domain.projection.PayoffSchedule}.
 *
 * @param hasLoans whether the household has any debt or mortgage balance to pay down
 * @param loans the "Your loans" table's rows, in payoff order
 * @param loanNames each loan's name, in payoff order, for the schedule table's per-loan column headers
 * @param totalLoanBalance the sum of every loan's starting balance, formatted
 * @param totalMinimumPayment the sum of every loan's minimum payment, formatted
 * @param months every month of the schedule
 * @param totalExtraCash the sum of every month's extra cash, formatted
 * @param totalPaidPerLoan each loan's total paid, formatted, aligned by position with {@code loans}
 * @param totalInterest the sum of every month's interest, formatted
 * @param truncated whether the schedule stopped at 600 months before every loan closed
 */
record ScheduleView(
        boolean hasLoans,
        List<LoanSummaryView> loans,
        List<String> loanNames,
        String totalLoanBalance,
        String totalMinimumPayment,
        List<ScheduleMonthView> months,
        String totalExtraCash,
        List<String> totalPaidPerLoan,
        String totalInterest,
        boolean truncated) {
}
