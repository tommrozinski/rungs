package com.rungs.readiness.application.validation;

import com.rungs.readiness.domain.projection.SimulationInput;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Checks {@link HorizonMonths}. */
public final class HorizonMonthsValidator implements ConstraintValidator<HorizonMonths, Integer> {

    @Override
    public boolean isValid(Integer months, ConstraintValidatorContext context) {
        if (months == null
                || (months >= SimulationInput.MIN_HORIZON_MONTHS && months <= SimulationInput.MAX_HORIZON_MONTHS)) {
            return true;
        }
        return Violations.reject(context, ValidationCode.HORIZON_RANGE);
    }
}
