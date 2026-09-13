package com.rungs.readiness.application;

import com.rungs.readiness.application.validation.MoneyAmount;
import com.rungs.readiness.application.validation.Percentage;

/**
 * The home mortgage as submitted, before normalization; every value is a raw decimal string and blank means zero.
 *
 * @param balance the amount still owed
 * @param annualInterestRate the annual percentage ({@code "6.125"})
 * @param monthlyPayment the scheduled monthly payment; informational only
 * @param homeValue the estimated value of the home
 */
public record MortgageInput(
        @MoneyAmount String balance,
        @Percentage(scale = SnapshotInput.INTEREST_RATE_SCALE) String annualInterestRate,
        @MoneyAmount String monthlyPayment,
        @MoneyAmount String homeValue) {
}
