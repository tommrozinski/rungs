package com.rungs.readiness.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The whole-form rule on a snapshot: at most 50 debts with a balance above zero, so zero-balance debts, which
 * normalization removes, are not counted. More is {@link ValidationCode#TOO_MANY_DEBTS} on the {@code debts} property.
 * Declare it in the {@link WholeForm} group.
 */
@Documented
@Constraint(validatedBy = AtMostFiftyDebtsValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AtMostFiftyDebts {

    /** The template of the only violation, {@link ValidationCode#TOO_MANY_DEBTS}. */
    String message() default "TOO_MANY_DEBTS";

    /** Validation groups; {@link WholeForm} where declared. */
    Class<?>[] groups() default {};

    /** Payload, unused. */
    Class<? extends Payload>[] payload() default {};
}
