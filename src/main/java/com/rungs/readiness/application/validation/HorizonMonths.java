package com.rungs.readiness.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A submitted projection {@code horizonMonths} (story 010, 011). Null is valid and means 360; outside 1 to 600 is
 * {@link ValidationCode#HORIZON_RANGE}.
 */
@Documented
@Constraint(validatedBy = HorizonMonthsValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface HorizonMonths {

    /** The violation's template is replaced by the failing {@link ValidationCode} name. */
    String message() default "HORIZON_RANGE";

    /** Validation groups. */
    Class<?>[] groups() default {};

    /** Payload, unused. */
    Class<? extends Payload>[] payload() default {};
}
