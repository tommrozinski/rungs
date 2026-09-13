package com.rungs.readiness.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The number of months of expenses the full emergency fund covers. Null is valid and means 3; below 3 is
 * {@link ValidationCode#MONTHS_MIN}. Above 6 is accepted (it is capped with a warning, story 012).
 */
@Documented
@Constraint(validatedBy = EmergencyFundMonthsValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EmergencyFundMonths {

    /** The template of the only violation, {@link ValidationCode#MONTHS_MIN}. */
    String message() default "MONTHS_MIN";

    /** Validation groups. */
    Class<?>[] groups() default {};

    /** Payload, unused. */
    Class<? extends Payload>[] payload() default {};
}
