package com.rungs.readiness.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A submitted money string. Blank (see {@code InputText}) is valid and means zero. Otherwise the first failing rule is
 * reported, in this order: {@link ValidationCode#INVALID_AMOUNT} when {@code Money.parse} rejects it,
 * {@link ValidationCode#NEGATIVE} below zero, {@link ValidationCode#AMOUNT_TOO_LARGE} above $1,000,000,000.00.
 */
@Documented
@Constraint(validatedBy = MoneyAmountValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MoneyAmount {

    /** The violation's template is replaced by the failing {@link ValidationCode} name. */
    String message() default "INVALID_AMOUNT";

    /** Validation groups. */
    Class<?>[] groups() default {};

    /** Payload, unused. */
    Class<? extends Payload>[] payload() default {};
}
