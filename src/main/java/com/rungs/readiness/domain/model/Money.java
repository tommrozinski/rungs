package com.rungs.readiness.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

/**
 * An immutable US dollar amount in whole cents; negative amounts are allowed. Arithmetic is
 * exact and throws {@link ArithmeticException} on overflow.
 *
 * @param cents the amount in cents
 */
public record Money(long cents) implements Comparable<Money> {

    /** Zero dollars. */
    public static final Money ZERO = new Money(0);

    private static final long CENTS_PER_DOLLAR = 100;

    /** Caps factor digits so an enormous factor cannot force a costly rescale; DECIMAL128 needs only 34. */
    private static final int MAX_FACTOR_PRECISION = 64;

    /** Products whose leading digit is below the tenths of a cent are under 0.1 cent and round to zero. */
    private static final long MIN_ROUNDED_EXPONENT = -1;

    /** Products whose leading digit is above 10^18 cents cannot fit a long, so they overflow without rescaling. */
    private static final long MAX_FITTING_EXPONENT = 18;

    /**
     * Returns the exact total of {@code amounts}, or {@link #ZERO} when empty; adds in iteration order and
     * throws ArithmeticException as soon as a running total overflows, and NullPointerException for a null
     * collection or element.
     */
    public static Money sum(Collection<Money> amounts) {
        Objects.requireNonNull(amounts, "amounts");
        long total = 0;
        for (Money amount : amounts) {
            Objects.requireNonNull(amount, "amount");
            total = Math.addExact(total, amount.cents);
        }
        return new Money(total);
    }

    /** Returns the amount of {@code cents} cents. */
    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    /** Returns the amount of {@code dollars} whole dollars; throws ArithmeticException on overflow. */
    public static Money ofDollars(long dollars) {
        return new Money(Math.multiplyExact(dollars, CENTS_PER_DOLLAR));
    }

    /** Returns this amount plus {@code other}; throws ArithmeticException on overflow. */
    public Money plus(Money other) {
        Objects.requireNonNull(other, "other");
        return new Money(Math.addExact(cents, other.cents));
    }

    /** Returns this amount minus {@code other}; throws ArithmeticException on overflow. */
    public Money minus(Money other) {
        Objects.requireNonNull(other, "other");
        return new Money(Math.subtractExact(cents, other.cents));
    }

    /** Returns this amount with its sign flipped; throws ArithmeticException for {@code Long.MIN_VALUE} cents. */
    public Money negate() {
        return new Money(Math.negateExact(cents));
    }

    /**
     * Returns this amount multiplied by {@code factor}, rounded half-even to the cent. Throws ArithmeticException
     * when the result does not fit a long or the factor has more than 64 significant digits, and
     * NullPointerException for a null factor. Products under 0.1 cent return {@link #ZERO} and products of 10^19
     * cents or more throw before rounding, so extreme exponents stay cheap.
     */
    public Money times(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor");
        if (factor.precision() > MAX_FACTOR_PRECISION) {
            throw new ArithmeticException("Money factor too precise");
        }
        BigDecimal product = BigDecimal.valueOf(cents).multiply(factor);
        if (product.signum() == 0) {
            return ZERO;
        }
        // Computed in long: precision - scale - 1 overflows int for scales near Integer.MIN_VALUE.
        long leadingDigitExponent = (long) product.precision() - product.scale() - 1;
        if (leadingDigitExponent < MIN_ROUNDED_EXPONENT) {
            return ZERO;
        }
        if (leadingDigitExponent > MAX_FITTING_EXPONENT) {
            throw new ArithmeticException("Money overflow");
        }
        return new Money(product.setScale(0, RoundingMode.HALF_EVEN).longValueExact());
    }

    /** Orders amounts by cents, consistent with {@link #equals(Object)}; throws NullPointerException if null. */
    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "other");
        return Long.compare(cents, other.cents);
    }

    /** Returns the smaller of this and {@code other}, or this on a tie; throws NullPointerException if null. */
    public Money min(Money other) {
        return compareTo(other) <= 0 ? this : other;
    }

    /** Returns the larger of this and {@code other}, or this on a tie; throws NullPointerException if null. */
    public Money max(Money other) {
        return compareTo(other) >= 0 ? this : other;
    }

    /** Returns whether this amount is exactly zero cents. */
    public boolean isZero() {
        return cents == 0;
    }

    /** Returns whether this amount is below zero cents. */
    public boolean isNegative() {
        return cents < 0;
    }

    /** Returns whether this amount is above zero cents. */
    public boolean isPositive() {
        return cents > 0;
    }
}
