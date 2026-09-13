package com.rungs.readiness.domain.model;

import java.math.BigDecimal;

/**
 * A home mortgage, kept apart from the consumer debts. A balance of $0 is allowed and means the home is paid off.
 *
 * @param balance the amount still owed; at least zero
 * @param monthlyPayment the scheduled monthly payment; at least zero
 * @param homeValue the estimated value of the home; at least zero
 * @param annualInterestRate the annual rate as a percentage ({@code 6.125} means 6.125%), from 0 to 100 inclusive,
 *     normalized to three decimal places; more decimal places are rejected
 */
public record Mortgage(Money balance, Money monthlyPayment, Money homeValue, BigDecimal annualInterestRate) {

    /** Validates every component and normalizes the rate; throws NullPointerException or IllegalArgumentException. */
    public Mortgage {
        Invariants.nonNegative(balance, "balance");
        Invariants.nonNegative(monthlyPayment, "monthlyPayment");
        Invariants.nonNegative(homeValue, "homeValue");
        annualInterestRate = Invariants.percent(annualInterestRate, Invariants.INTEREST_RATE_SCALE, "annualInterestRate");
    }
}
