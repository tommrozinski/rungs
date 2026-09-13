package com.rungs.readiness.application.validation;

import jakarta.validation.ConstraintViolation;
import java.util.Objects;

/**
 * What a constraint violation means to a user: its {@link ValidationCode} and the resolved message. Built only from
 * the violation's template and constraint attributes, never from the rejected value (story 011 REQ-010), so the API
 * and the form (story 027) show identical text.
 *
 * @param code the reason for the rejection
 * @param message the user-facing message, with any placeholder filled in
 */
public record Rejection(ValidationCode code, String message) {

    /** Creates a rejection; throws NullPointerException if either component is null. */
    public Rejection {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }

    /**
     * Returns the rejection for {@code violation}, whose message template must be a {@link ValidationCode} name, as
     * every constraint in this package produces; {@link ValidationCode#TOO_PRECISE} takes its decimal places from the
     * {@link Percentage#scale()} that failed. Throws IllegalArgumentException for any other template.
     */
    public static Rejection of(ConstraintViolation<?> violation) {
        ValidationCode code = ValidationCode.valueOf(violation.getMessageTemplate());
        if (code == ValidationCode.TOO_PRECISE) {
            Object scale = violation.getConstraintDescriptor().getAttributes().get(Percentage.SCALE_ATTRIBUTE);
            return new Rejection(code, code.message((Integer) scale));
        }
        return new Rejection(code, code.message());
    }
}
