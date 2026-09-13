package com.rungs.readiness.application.validation;

import java.util.Objects;

/**
 * One field's rejection, at its pointer (story 011, story 027): the location an API error or a form field error is
 * reported at, together with the {@link Rejection} (code and message) for it.
 *
 * @param pointer the JSON-Pointer-shaped path to the rejected field
 * @param rejection the reason and message
 */
public record FieldRejection(String pointer, Rejection rejection) {

    /** Creates a field rejection; throws NullPointerException if either component is null. */
    public FieldRejection {
        Objects.requireNonNull(pointer, "pointer");
        Objects.requireNonNull(rejection, "rejection");
    }
}
