package com.rungs.readiness.application.validation;

/**
 * Every reason a submitted value can be rejected (story 011 REQ-002 to REQ-004, REQ-009, REQ-013). Each constant
 * carries the stable wire {@link #code()} and the one user-facing message, so API responses, the web form, metrics,
 * and logs can refer to a rejection without free text or submitted values.
 *
 * <p>Constant names are unique, but two constants share the wire code {@code INVALID_CHOICE} because their messages
 * differ: {@link #INVALID_DEBT_TYPE} and {@link #INVALID_STRATEGY}. Constraint violations use the constant name as
 * their message template.
 */
public enum ValidationCode {

    /** A money value below zero. */
    NEGATIVE("NEGATIVE", "Enter zero or more."),

    /** A money value that is not an amount. */
    INVALID_AMOUNT("INVALID_AMOUNT", "Enter an amount like 1,234.56."),

    /** A money value above $1,000,000,000.00, which keeps every total inside a long of cents. */
    AMOUNT_TOO_LARGE("AMOUNT_TOO_LARGE", "Enter an amount of $1,000,000,000.00 or less."),

    /** A percentage that is not a plain decimal number. */
    INVALID_PERCENT("INVALID_PERCENT", "Enter a percentage like 7.25."),

    /** A percentage outside 0 to 100. */
    PERCENT_RANGE("PERCENT_RANGE", "Enter a percentage from 0 to 100."),

    /** A percentage with more decimal places than its field allows; see {@link #message(int)}. */
    TOO_PRECISE("TOO_PRECISE", "Use at most {n} decimal places."),

    /** An {@code emergencyFundMonths} below 3. */
    MONTHS_MIN("MONTHS_MIN", "Enter 3 months or more."),

    /** A projection {@code horizonMonths} outside 1 to 600 (stories 010, 013). */
    HORIZON_RANGE("HORIZON_RANGE", "Enter a horizon from 1 to 600 months."),

    /** An unknown projection {@code strategy} (stories 010, 013). */
    INVALID_STRATEGY("INVALID_CHOICE", "Choose snowball or avalanche."),

    /** A debt whose type is {@code MORTGAGE}. */
    MORTGAGE_AS_DEBT("MORTGAGE_AS_DEBT", "Enter your mortgage in the mortgage section, not as a debt."),

    /** A debt whose type is blank or not a known debt type. */
    INVALID_DEBT_TYPE("INVALID_CHOICE", "Choose a debt type from the list."),

    /** More than 50 debts with a balance above zero; a whole-form rule. */
    TOO_MANY_DEBTS("TOO_MANY_DEBTS", "Enter 50 debts or fewer."),

    /** A JSON property the request does not define; REST binding only. */
    UNKNOWN_FIELD("UNKNOWN_FIELD", "Remove this field; it isn't part of the request."),

    /** A JSON value of the wrong type, or a missing debt entry; REST binding only. */
    INVALID_TYPE("INVALID_TYPE", "Send this value as the JSON type the field expects.");

    private static final String PLACES_PLACEHOLDER = "{n}";

    private final String code;
    private final String message;

    ValidationCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /** Returns the stable code sent to clients. */
    public String code() {
        return code;
    }

    /** Returns the message; for {@link #TOO_PRECISE} use {@link #message(int)} instead, as this keeps {@code {n}}. */
    public String message() {
        return message;
    }

    /**
     * Returns the message with {@code {n}} replaced by {@code decimalPlaces}; messages without the placeholder are
     * returned unchanged. Throws IllegalArgumentException when {@code decimalPlaces} is not positive.
     */
    public String message(int decimalPlaces) {
        if (decimalPlaces < 1) {
            throw new IllegalArgumentException("decimalPlaces must be positive");
        }
        return message.replace(PLACES_PLACEHOLDER, Integer.toString(decimalPlaces));
    }
}
