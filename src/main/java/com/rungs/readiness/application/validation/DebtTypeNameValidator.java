package com.rungs.readiness.application.validation;

import com.rungs.readiness.domain.model.DebtType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Checks {@link DebtTypeName}. */
public final class DebtTypeNameValidator implements ConstraintValidator<DebtTypeName, String> {

    /** The one type name that gets its own code, since a mortgage belongs in the mortgage section. */
    private static final String MORTGAGE = "MORTGAGE";

    private static final Set<String> NAMES =
            Arrays.stream(DebtType.values()).map(DebtType::name).collect(Collectors.toUnmodifiableSet());

    @Override
    public boolean isValid(String type, ConstraintValidatorContext context) {
        if (MORTGAGE.equals(type)) {
            return Violations.reject(context, ValidationCode.MORTGAGE_AS_DEBT);
        }
        if (type == null || !NAMES.contains(type)) {
            return Violations.reject(context, ValidationCode.INVALID_DEBT_TYPE);
        }
        return true;
    }
}
