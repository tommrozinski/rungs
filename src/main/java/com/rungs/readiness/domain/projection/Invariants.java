package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Money;
import java.util.Objects;

/**
 * Checks shared by the projection records' constructors. Nulls throw {@link NullPointerException} with the
 * component name, and every other rule throws {@link IllegalArgumentException}.
 */
final class Invariants {

    private Invariants() {
    }

    /** Returns {@code amount}; throws if it is null or below zero. */
    static Money nonNegative(Money amount, String name) {
        if (Objects.requireNonNull(amount, name).isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return amount;
    }
}
