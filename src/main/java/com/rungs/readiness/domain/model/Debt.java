package com.rungs.readiness.domain.model;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * One consumer debt with a balance still owed. Debts with a zero balance never reach the domain.
 *
 * @param id the positional id {@code debt-<n>}, where {@code n} is the debt's 1-based position as supplied, written
 *     without leading zeros; assigned outside the domain
 * @param label the name shown for the debt; not blank
 * @param type the kind of debt
 * @param balance the amount still owed; above zero
 * @param annualInterestRate the annual rate as a percentage ({@code 7.25} means 7.25%), from 0 to 100 inclusive,
 *     normalized to three decimal places; more decimal places are rejected
 * @param minimumPayment the required monthly payment; at least zero
 */
public record Debt(
        String id,
        String label,
        DebtType type,
        Money balance,
        BigDecimal annualInterestRate,
        Money minimumPayment) {

    /** {@code debt-} followed by a whole number from 1, in ASCII digits without leading zeros. */
    private static final Pattern ID_PATTERN = Pattern.compile("debt-[1-9][0-9]*");

    /** Validates every component and normalizes the rate; throws NullPointerException or IllegalArgumentException. */
    public Debt {
        if (!ID_PATTERN.matcher(Invariants.nonNull(id, "id")).matches()) {
            throw new IllegalArgumentException("id must be debt- followed by a whole number from 1 without leading zeros");
        }
        Invariants.notBlank(label, "label");
        Invariants.nonNull(type, "type");
        Invariants.positive(balance, "balance");
        annualInterestRate = Invariants.percent(annualInterestRate, Invariants.INTEREST_RATE_SCALE, "annualInterestRate");
        Invariants.nonNegative(minimumPayment, "minimumPayment");
    }
}
