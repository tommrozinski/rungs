package com.rungs.readiness.application.validation;

import com.rungs.readiness.domain.projection.Strategy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Checks {@link StrategyName}. */
public final class StrategyNameValidator implements ConstraintValidator<StrategyName, String> {

    private static final Set<String> NAMES =
            Arrays.stream(Strategy.values()).map(Strategy::name).collect(Collectors.toUnmodifiableSet());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || NAMES.contains(value)) {
            return true;
        }
        return Violations.reject(context, ValidationCode.INVALID_STRATEGY);
    }
}
