package com.rungs.readiness.web;

/**
 * One row of the payoff schedule's "Your loans" table (design.md 040 section 2): a loan's starting figures,
 * formatted for display.
 *
 * @param name the loan's label; "Mortgage" for the home loan
 * @param balance the starting balance, formatted as money
 * @param rate the annual rate, formatted as a percentage with two decimal places, e.g. "26.99%"
 * @param minimumPayment the minimum monthly payment, formatted as money
 */
record LoanSummaryView(String name, String balance, String rate, String minimumPayment) {
}
