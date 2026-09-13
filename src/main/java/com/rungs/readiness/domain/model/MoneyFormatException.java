package com.rungs.readiness.domain.model;

import java.util.Objects;

/**
 * Thrown when text cannot be read as a {@link Money} amount. The message is a fixed sentence
 * chosen by the {@link Reason}; it never contains the rejected text, so a logged failure cannot
 * leak a household figure.
 */
public final class MoneyFormatException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    /** Why the text was rejected; each reason carries one fixed message. */
    public enum Reason {
        /** The text is null, empty, or only whitespace. */
        BLANK("Amount is blank."),
        /** The text is longer than the accepted maximum before trimming. */
        TOO_LONG("Amount is too long."),
        /** The text contains a character that is not allowed at its position. */
        INVALID_CHARACTER("Amount contains an invalid character."),
        /** The text has no digits where digits are required. */
        MISSING_DIGITS("Amount is missing digits."),
        /** A thousands separator is not between groups of exactly three digits. */
        MISPLACED_SEPARATOR("Amount has a misplaced thousands separator."),
        /** The text has more than two digits after the decimal point. */
        TOO_MANY_DECIMALS("Amount has more than two decimal places."),
        /** The amount does not fit in the supported range of cents. */
        OUT_OF_RANGE("Amount is out of range.");

        private final String message;

        Reason(String message) {
            this.message = message;
        }
    }

    private final Reason reason;

    /**
     * Creates an exception whose message is the fixed sentence for {@code reason}.
     *
     * @param reason why the text was rejected
     * @throws NullPointerException if {@code reason} is null
     */
    public MoneyFormatException(Reason reason) {
        super(Objects.requireNonNull(reason, "reason").message);
        this.reason = reason;
    }

    /**
     * Returns why the text was rejected.
     *
     * @return the rejection reason
     */
    public Reason reason() {
        return reason;
    }
}
