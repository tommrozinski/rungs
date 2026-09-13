package com.rungs.readiness.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Checks shared by the snapshot records' constructors. Every failure names the component and the rule, never the
 * offending value: nulls throw {@link NullPointerException} with the component name, and every other rule throws
 * {@link IllegalArgumentException}.
 */
final class Invariants {

    /** Decimal places every annual interest rate is normalized to. */
    static final int INTEREST_RATE_SCALE = 3;

    /** Upper bound, inclusive, of every percentage; the lower bound is zero. */
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private Invariants() {
    }

    /** Returns {@code value}; throws NullPointerException with {@code name} as the message if it is null. */
    static <T> T nonNull(T value, String name) {
        return Objects.requireNonNull(value, name);
    }

    /** Returns {@code amount}; throws if it is null or below zero. */
    static Money nonNegative(Money amount, String name) {
        if (nonNull(amount, name).isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return amount;
    }

    /** Returns {@code text}; throws if it is null, empty, or only whitespace. */
    static String notBlank(String text, String name) {
        if (nonNull(text, name).isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }

    /**
     * Returns {@code value} at exactly {@code scale} decimal places; throws if it is null, outside 0 to 100
     * inclusive, or has more than {@code scale} significant decimal places. Over-precise values are rejected, never
     * rounded.
     *
     * <p>The steps run in an order that keeps extreme exponents cheap: the range check by {@code compareTo} rejects
     * huge positive exponents before any rescale, {@code stripTrailingZeros} handles huge negative exponents without
     * expanding them, and the final {@code setScale} starts from a stripped scale between -2 and {@code scale}, so it
     * is exact and small. Cost still grows with the number of digits supplied; callers bound input length (story 011
     * validation, CSV and JSON parsing).
     */
    static BigDecimal percent(BigDecimal value, int scale, String name) {
        nonNull(value, name);
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException(name + " must be between 0 and 100");
        }
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() > scale) {
            throw new IllegalArgumentException(name + " has more than " + scale + " decimal places");
        }
        return stripped.setScale(scale);
    }

    /** Returns {@code amount}; throws if it is null, zero, or below zero. */
    static Money positive(Money amount, String name) {
        if (!nonNull(amount, name).isPositive()) {
            throw new IllegalArgumentException(name + " must be above zero");
        }
        return amount;
    }
}
