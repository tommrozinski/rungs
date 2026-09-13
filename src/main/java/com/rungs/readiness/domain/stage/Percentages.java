package com.rungs.readiness.domain.stage;

import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Turns "part of whole" into the scale-1 percentage every stage reports. Results round down, so an unmet stage never
 * displays {@code 100.0}. Null arguments throw {@link NullPointerException} with the argument name; negative ones
 * throw {@link IllegalArgumentException}.
 */
public final class Percentages {

    /** Decimal places of every percentage. */
    private static final int SCALE = 1;

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private static final BigDecimal ZERO_PERCENT = new BigDecimal("0.0");

    private static final BigDecimal FULL_PERCENT = new BigDecimal("100.0");

    /** Highest percentage a part below its whole can report. */
    private static final BigDecimal UNMET_CAP = new BigDecimal("99.9");

    private Percentages() {
    }

    /** Returns {@code part} as a percentage of {@code whole}, by the rules of {@link #ofRatio}. */
    public static BigDecimal of(Money part, Money whole) {
        Objects.requireNonNull(part, "part");
        Objects.requireNonNull(whole, "whole");
        return ofRatio(BigDecimal.valueOf(part.cents()), BigDecimal.valueOf(whole.cents()));
    }

    /**
     * Returns {@code part} as a percentage of {@code whole} at scale 1: {@code 0.0} when {@code whole} is zero,
     * {@code 100.0} when {@code part} is at least {@code whole}, and otherwise the ratio rounded down, at most
     * {@code 99.9}.
     */
    public static BigDecimal ofRatio(BigDecimal part, BigDecimal whole) {
        nonNegative(part, "part");
        nonNegative(whole, "whole");
        if (whole.signum() == 0) {
            return ZERO_PERCENT;
        }
        if (part.compareTo(whole) >= 0) {
            return FULL_PERCENT;
        }
        return part.multiply(ONE_HUNDRED).divide(whole, SCALE, RoundingMode.DOWN).min(UNMET_CAP);
    }

    /** Throws if {@code value} is null or below zero. */
    private static void nonNegative(BigDecimal value, String name) {
        if (Objects.requireNonNull(value, name).signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
