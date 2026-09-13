package com.rungs.readiness.application.validation;

import com.rungs.readiness.application.DebtInput;
import com.rungs.readiness.application.InputText;
import com.rungs.readiness.application.SnapshotInput;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.MoneyFormatException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Checks {@link AtMostFiftyDebts}. It normally runs only once the field rules pass, but it still tolerates a missing
 * debt entry or an unparseable balance, counting neither, so validating the group alone never throws.
 */
public final class AtMostFiftyDebtsValidator implements ConstraintValidator<AtMostFiftyDebts, SnapshotInput> {

    private static final int MAXIMUM_DEBTS = 50;

    private static final String DEBTS_PROPERTY = "debts";

    @Override
    public boolean isValid(SnapshotInput input, ConstraintValidatorContext context) {
        if (input == null || input.debts() == null) {
            return true;
        }
        long withBalance = input.debts().stream().filter(AtMostFiftyDebtsValidator::hasBalance).count();
        if (withBalance <= MAXIMUM_DEBTS) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(ValidationCode.TOO_MANY_DEBTS.name())
                .addPropertyNode(DEBTS_PROPERTY)
                .addConstraintViolation();
        return false;
    }

    private static boolean hasBalance(DebtInput debt) {
        if (debt == null || InputText.isBlank(debt.balance())) {
            return false;
        }
        try {
            return Money.parse(debt.balance()).isPositive();
        } catch (MoneyFormatException unparseable) {
            return false;
        }
    }
}
