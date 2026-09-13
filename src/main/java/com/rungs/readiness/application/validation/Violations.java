package com.rungs.readiness.application.validation;

import jakarta.validation.ConstraintValidatorContext;

/** Replaces a constraint's default violation with one whose message template is a {@link ValidationCode} name. */
final class Violations {

    private Violations() {
    }

    /** Reports {@code code} for the validated element and returns false, the invalid result. */
    static boolean reject(ConstraintValidatorContext context, ValidationCode code) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(code.name()).addConstraintViolation();
        return false;
    }
}
