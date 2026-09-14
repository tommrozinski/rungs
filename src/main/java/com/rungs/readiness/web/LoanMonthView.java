package com.rungs.readiness.web;

/**
 * One loan's formatted figures for one month of the payoff schedule (design.md 040 section 2). A zero amount is
 * rendered as {@code "-"}, matching the workbook it follows.
 *
 * @param open the balance owed at the start of the month
 * @param interest this month's interest
 * @param payment the amount applied this month
 * @param end the balance owed after this month's payment
 */
record LoanMonthView(String open, String interest, String payment, String end) {
}
