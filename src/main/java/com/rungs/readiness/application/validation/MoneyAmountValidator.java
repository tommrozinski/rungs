package com.rungs.readiness.application.validation;

import com.rungs.readiness.application.InputText;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.MoneyFormatException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Checks {@link MoneyAmount}. */
public final class MoneyAmountValidator implements ConstraintValidator<MoneyAmount, String> {

    /** The largest accepted amount; 50 debts or 6 months of expenses at this size stay far inside a long of cents. */
    static final Money MAXIMUM = Money.ofDollars(1_000_000_000L);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (InputText.isBlank(value)) {
            return true;
        }
        Money amount;
        try {
            amount = Money.parse(value);
        } catch (MoneyFormatException unparseable) {
            return Violations.reject(context, ValidationCode.INVALID_AMOUNT);
        }
        if (amount.isNegative()) {
            return Violations.reject(context, ValidationCode.NEGATIVE);
        }
        if (amount.compareTo(MAXIMUM) > 0) {
            return Violations.reject(context, ValidationCode.AMOUNT_TOO_LARGE);
        }
        return true;
    }
}
