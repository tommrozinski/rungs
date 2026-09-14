package com.rungs.readiness.domain.model;

/**
 * A household that rents its home rather than owning it.
 *
 * @param monthlyRent the monthly rent; at least zero
 */
public record Rent(Money monthlyRent) implements Housing {

    /** Validates {@code monthlyRent}; throws NullPointerException or IllegalArgumentException. */
    public Rent {
        Invariants.nonNegative(monthlyRent, "monthlyRent");
    }
}
