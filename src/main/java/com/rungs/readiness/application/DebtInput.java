package com.rungs.readiness.application;

/**
 * One debt as submitted, before normalization. Money and rates are raw decimal strings so blank values can become
 * zero; the id is never submitted, it is assigned by position.
 *
 * @param label the name shown for the debt; blank means "Debt N" by position
 * @param type the {@code DebtType} name
 * @param balance the amount still owed as a decimal string; blank means zero
 * @param annualInterestRate the annual percentage as a decimal string ({@code "7.25"}); blank means zero
 * @param minimumPayment the required monthly payment as a decimal string; blank means zero
 */
public record DebtInput(String label, String type, String balance, String annualInterestRate, String minimumPayment) {
}
