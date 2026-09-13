package com.rungs.readiness.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Checks {@link EmergencyFundMonths}. */
public final class EmergencyFundMonthsValidator implements ConstraintValidator<EmergencyFundMonths, Integer> {

    private static final int MINIMUM_MONTHS = 3;

    @Override
    public boolean isValid(Integer months, ConstraintValidatorContext context) {
        if (months == null || months >= MINIMUM_MONTHS) {
            return true;
        }
        return Violations.reject(context, ValidationCode.MONTHS_MIN);
    }
}
