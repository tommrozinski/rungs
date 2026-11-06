package com.rungs.readiness.web;

/**
 * One row of the payoff schedule's "Your loans" table: a loan's starting figures,
 * formatted for display. {@code balance} and {@code minimumPayment} render a zero amount as {@code "-"}.
 *
 * @param name the loan's label; "Mortgage" for the home loan
 * @param balance the starting balance, formatted as money, or {@code "-"} when it is zero
 * @param rate the annual rate, formatted as a percentage with two decimal places, e.g. "26.99%"
 * @param minimumPayment the minimum monthly payment, formatted as money, or {@code "-"} when it is zero
 */
record LoanSummaryView(String name, String balance, String rate, String minimumPayment) {
}
