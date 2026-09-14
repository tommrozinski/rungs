package com.rungs.readiness.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * A home mortgage. A balance of $0 is allowed and means the home is paid off.
 *
 * @param balance the amount still owed; at least zero
 * @param annualInterestRate the annual rate as a percentage ({@code 6.125} means 6.125%), from 0 to 100 inclusive,
 *     normalized to three decimal places; more decimal places are rejected
 * @param monthlyPayment the full scheduled monthly payment, taxes and insurance included; at least zero
 * @param maturity the month the loan is scheduled to be paid off, or null when unknown
 */
public record Mortgage(Money balance, BigDecimal annualInterestRate, Money monthlyPayment, YearMonth maturity)
        implements Housing {

    /** Validates every component and normalizes the rate; throws NullPointerException or IllegalArgumentException. */
    public Mortgage {
        Invariants.nonNegative(balance, "balance");
        annualInterestRate =
                Invariants.percent(annualInterestRate, Invariants.INTEREST_RATE_SCALE, "annualInterestRate");
        Invariants.nonNegative(monthlyPayment, "monthlyPayment");
    }
}
