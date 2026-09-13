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

    /** Digits after the decimal point in both string formats. */
    private static final int FRACTION_DIGITS = 2;

    /** Cents are zero-padded to at least one whole-dollar digit plus the fraction digits. */
    private static final int MIN_DIGITS = FRACTION_DIGITS + 1;

    /** Base of the decimal digits {@link #parse} accumulates. */
    private static final int RADIX = 10;

    /** Longest text {@link #parse} inspects, counted before trimming, so oversized input is rejected cheaply. */
    private static final int MAX_TEXT_LENGTH = 32;

    /** Whole-dollar digits between comma separators in the display format. */
    private static final int GROUP_SIZE = 3;

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

    /**
     * Parses {@code text} as an amount, independent of the default locale.
     *
     * <p>Leading and trailing characters that are Java whitespace or Unicode space separators (including
     * non-breaking spaces) are ignored. What remains is an optional {@code -}, then an optional {@code $}, then
     * whole dollars written with ASCII digits 0-9, optionally grouped by commas into a first group of one to three
     * digits followed by groups of exactly three, then optionally a {@code .} and one or two digits. Leading zeros
     * are allowed, and a negative zero such as {@code -0} or {@code -$0.00} parses to {@link #ZERO}. Every amount
     * from {@code Long.MIN_VALUE} to {@code Long.MAX_VALUE} cents is accepted, so {@code parse(m.toBareString())}
     * and {@code parse(m.toDisplayString())} both return {@code m}.
     *
     * <p>When text has several problems, the reason is decided in this order: {@code BLANK} for null;
     * {@code TOO_LONG} for more than 32 characters, counted before trimming; {@code BLANK} for nothing left after
     * trimming; {@code INVALID_CHARACTER} for any other character, a sign or symbol out of place, or a second
     * {@code .}; then the whole dollars ({@code MISSING_DIGITS} if absent, {@code MISPLACED_SEPARATOR} for bad
     * commas); then the cents ({@code MISPLACED_SEPARATOR}, {@code MISSING_DIGITS}, {@code TOO_MANY_DECIMALS});
     * and finally {@code OUT_OF_RANGE}.
     *
     * @param text the text to parse; may be null
     * @return the amount the text denotes
     * @throws MoneyFormatException if the text is not an amount; its message never contains the text and it has
     *     no cause
     */
    public static Money parse(String text) {
        if (text == null) {
            throw new MoneyFormatException(MoneyFormatException.Reason.BLANK);
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new MoneyFormatException(MoneyFormatException.Reason.TOO_LONG);
        }
        String stripped = trim(text);
        if (stripped.isEmpty()) {
            throw new MoneyFormatException(MoneyFormatException.Reason.BLANK);
        }
        boolean negative = stripped.startsWith("-");
        int start = negative ? 1 : 0;
        if (stripped.startsWith("$", start)) {
            start++;
        }
        String number = stripped.substring(start);
        int point = -1;
        for (int index = 0; index < number.length(); index++) {
            char symbol = number.charAt(index);
            if (symbol == '.' && point < 0) {
                point = index;
            } else if (!isAsciiDigit(symbol) && symbol != ',') {
                throw new MoneyFormatException(MoneyFormatException.Reason.INVALID_CHARACTER);
            }
        }
        String whole = point < 0 ? number : number.substring(0, point);
        String fraction = point < 0 ? "" : number.substring(point + 1);
        checkWholeDollars(whole);
        if (point >= 0) {
            checkCents(fraction);
        }
        String digits = whole + fraction + "0".repeat(FRACTION_DIGITS - fraction.length());
        try {
            // Accumulate on the negative side, where a long reaches one further than on the positive side.
            long negativeCents = 0;
            for (int index = 0; index < digits.length(); index++) {
                char digit = digits.charAt(index);
                if (digit != ',') {
                    negativeCents = Math.subtractExact(Math.multiplyExact(negativeCents, RADIX), digit - '0');
                }
            }
            return new Money(negative ? negativeCents : Math.negateExact(negativeCents));
        } catch (ArithmeticException overflow) {
            // Not chained as a cause, so nothing derived from the text travels with the failure.
            throw new MoneyFormatException(MoneyFormatException.Reason.OUT_OF_RANGE);
        }
    }

    /** Removes leading and trailing characters that are Java whitespace or Unicode space separators. */
    private static String trim(String text) {
        int start = 0;
        int end = text.length();
        while (start < end && isSpace(text.charAt(start))) {
            start++;
        }
        while (end > start && isSpace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(start, end);
    }

    /**
     * Rejects whole dollars that are empty, or whose commas do not split them into a first group of one to three
     * digits followed by groups of exactly three.
     */
    private static void checkWholeDollars(String whole) {
        if (whole.isEmpty()) {
            throw new MoneyFormatException(MoneyFormatException.Reason.MISSING_DIGITS);
        }
        int firstComma = whole.indexOf(',');
        if (firstComma < 0) {
            return;
        }
        if (firstComma == 0 || firstComma > GROUP_SIZE) {
            throw new MoneyFormatException(MoneyFormatException.Reason.MISPLACED_SEPARATOR);
        }
        // From the first comma on, the text repeats ",ddd": a comma exactly every GROUP_SIZE + 1 characters.
        int period = GROUP_SIZE + 1;
        if ((whole.length() - firstComma) % period != 0) {
            throw new MoneyFormatException(MoneyFormatException.Reason.MISPLACED_SEPARATOR);
        }
        for (int index = firstComma; index < whole.length(); index++) {
            boolean commaExpected = (index - firstComma) % period == 0;
            if ((whole.charAt(index) == ',') != commaExpected) {
                throw new MoneyFormatException(MoneyFormatException.Reason.MISPLACED_SEPARATOR);
            }
        }
    }

    /** Rejects digits after a decimal point that contain a comma, are missing, or number more than two. */
    private static void checkCents(String fraction) {
        if (fraction.indexOf(',') >= 0) {
            throw new MoneyFormatException(MoneyFormatException.Reason.MISPLACED_SEPARATOR);
        }
        if (fraction.isEmpty()) {
            throw new MoneyFormatException(MoneyFormatException.Reason.MISSING_DIGITS);
        }
        if (fraction.length() > FRACTION_DIGITS) {
            throw new MoneyFormatException(MoneyFormatException.Reason.TOO_MANY_DECIMALS);
        }
    }

    /** Returns whether {@code symbol} is one of the ASCII digits 0-9; other Unicode digits are not accepted. */
    private static boolean isAsciiDigit(char symbol) {
        return symbol >= '0' && symbol <= '9';
    }

    /** Returns whether {@code symbol} is whitespace or a space separator, including non-breaking spaces. */
    private static boolean isSpace(char symbol) {
        return Character.isWhitespace(symbol) || Character.isSpaceChar(symbol);
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

    /**
     * Returns the amount with exactly two decimals and no symbol or separators, independent of the default
     * locale; for example {@code 1234.56}, {@code -1234.56}, or {@code 0.00}.
     */
    public String toBareString() {
        String digits = paddedDigits();
        int point = digits.length() - FRACTION_DIGITS;
        return signPrefix() + digits.substring(0, point) + "." + digits.substring(point);
    }

    /**
     * Returns the amount with a dollar sign, comma thousands separators, and exactly two decimals, independent of
     * the default locale; for example {@code $1,234.56}, {@code -$1,234.56}, or {@code $0.00}.
     */
    public String toDisplayString() {
        String digits = paddedDigits();
        int point = digits.length() - FRACTION_DIGITS;
        return signPrefix() + "$" + groupThousands(digits.substring(0, point)) + "." + digits.substring(point);
    }

    /** Returns "-" for negative amounts, else ""; zero is never negative. */
    private String signPrefix() {
        return cents < 0 ? "-" : "";
    }

    /** Returns the unsigned decimal digits of cents, left-padded with zeros to at least {@link #MIN_DIGITS}. */
    private String paddedDigits() {
        // Long.toString always emits ASCII digits, whatever the default locale.
        String text = Long.toString(cents);
        String digits = text.startsWith("-") ? text.substring(1) : text;
        return digits.length() >= MIN_DIGITS ? digits : "0".repeat(MIN_DIGITS - digits.length()) + digits;
    }

    /** Inserts a comma every three digits from the right of {@code whole}. */
    private static String groupThousands(String whole) {
        StringBuilder grouped = new StringBuilder(whole);
        for (int index = whole.length() - GROUP_SIZE; index > 0; index -= GROUP_SIZE) {
            grouped.insert(index, ',');
        }
        return grouped.toString();
    }
}
