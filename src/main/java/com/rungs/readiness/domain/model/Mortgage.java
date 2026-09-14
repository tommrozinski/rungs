package com.rungs.readiness.domain.model;

import java.math.BigDecimal;

/**
 * A home mortgage. A balance of $0 is allowed and means the home is paid off. Has no stated maturity
 * (design.md 041 section 1): the loan's payoff dates come from {@link MortgagePayoffCalculator} and, for the debt
 * snowball, from {@code PayoffScheduleCalculator}.
 *
 * @param balance the amount still owed; at least zero
 * @param annualInterestRate the annual rate as a percentage ({@code 6.125} means 6.125%), from 0 to 100 inclusive,
 *     normalized to three decimal places; more decimal places are rejected
 * @param monthlyPayment the full scheduled monthly payment, taxes and insurance included; at least zero
 */
public record Mortgage(Money balance, BigDecimal annualInterestRate, Money monthlyPayment) implements Housing {

    /** Validates every component and normalizes the rate; throws NullPointerException or IllegalArgumentException. */
    public Mortgage {
        Invariants.nonNegative(balance, "balance");
        annualInterestRate =
                Invariants.percent(annualInterestRate, Invariants.INTEREST_RATE_SCALE, "annualInterestRate");
        Invariants.nonNegative(monthlyPayment, "monthlyPayment");
    }
}
