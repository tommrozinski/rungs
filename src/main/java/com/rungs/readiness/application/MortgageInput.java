package com.rungs.readiness.application;

/**
 * The home mortgage as submitted, before normalization; every value is a raw decimal string and blank means zero.
 *
 * @param balance the amount still owed
 * @param annualInterestRate the annual percentage ({@code "6.125"})
 * @param monthlyPayment the scheduled monthly payment; informational only
 * @param homeValue the estimated value of the home
 */
public record MortgageInput(String balance, String annualInterestRate, String monthlyPayment, String homeValue) {
}
