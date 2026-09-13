package com.rungs.readiness.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A submitted debt type, which is required. The text must equal a {@code DebtType} name exactly: matching is
 * case-sensitive and nothing is trimmed, the same as the normalization's {@code DebtType.valueOf}. Exactly
 * {@code MORTGAGE} is {@link ValidationCode#MORTGAGE_AS_DEBT}; null, blank, or any other text (including
 * {@code mortgage}) is {@link ValidationCode#INVALID_DEBT_TYPE}.
 */
@Documented
@Constraint(validatedBy = DebtTypeNameValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DebtTypeName {

    /** The violation's template is replaced by the failing {@link ValidationCode} name. */
    String message() default "INVALID_DEBT_TYPE";

    /** Validation groups. */
    Class<?>[] groups() default {};

    /** Payload, unused. */
    Class<? extends Payload>[] payload() default {};
}
