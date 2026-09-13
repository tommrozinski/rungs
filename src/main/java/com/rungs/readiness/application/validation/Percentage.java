package com.rungs.readiness.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A submitted percentage string. Blank (see {@code InputText}) is valid and means zero. Otherwise the first failing
 * rule is reported, in this order:
 *
 * <ol>
 *   <li>{@link ValidationCode#INVALID_PERCENT} unless the text is at most 32 characters before trimming and, once
 *       trimmed, a plain decimal of ASCII digits: an optional leading {@code -}, digits with an optional point and
 *       fraction ({@code 7}, {@code 7.}, {@code 7.25}) or a point and digits ({@code .5}); no {@code +}, exponent,
 *       separators, or symbols;
 *   <li>{@link ValidationCode#PERCENT_RANGE} below 0 or above 100;
 *   <li>{@link ValidationCode#TOO_PRECISE} with more than {@link #scale()} decimal places, not counting trailing zeros
 *       (the same rule the domain applies).
 * </ol>
 */
@Documented
@Constraint(validatedBy = PercentageValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Percentage {

    /** The name of this attribute, read when resolving the {@link ValidationCode#TOO_PRECISE} message. */
    String SCALE_ATTRIBUTE = "scale";

    /** The most decimal places allowed: 3 for interest rates, 2 for the retirement contribution. */
    int scale();

    /** The violation's template is replaced by the failing {@link ValidationCode} name. */
    String message() default "INVALID_PERCENT";

    /** Validation groups. */
    Class<?>[] groups() default {};

    /** Payload, unused. */
    Class<? extends Payload>[] payload() default {};
}
