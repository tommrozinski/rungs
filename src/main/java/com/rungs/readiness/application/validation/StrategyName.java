package com.rungs.readiness.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A submitted projection {@code strategy} (story 010, 011). The text must equal a {@code Strategy} name exactly.
 * Null is valid and means {@code SNOWBALL}; any other text, including a differently-cased or unknown name, is
 * {@link ValidationCode#INVALID_STRATEGY}.
 */
@Documented
@Constraint(validatedBy = StrategyNameValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrategyName {

    /** The violation's template is replaced by the failing {@link ValidationCode} name. */
    String message() default "INVALID_STRATEGY";

    /** Validation groups. */
    Class<?>[] groups() default {};

    /** Payload, unused. */
    Class<? extends Payload>[] payload() default {};
}
