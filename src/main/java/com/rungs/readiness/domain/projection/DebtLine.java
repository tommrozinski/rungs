package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Money;
import java.util.Objects;

/**
 * One debt's activity within a single simulated month (008 REQ-002, REQ-006).
 *
 * @param debtId the debt's id
 * @param openingBalance the balance owed before this month's interest and payment; at least zero
 * @param interest interest accrued this month on the opening balance, added before the payment; at least zero
 * @param payment the amount applied to this debt this month; at least zero
 * @param closingBalance the balance owed after this month's interest and payment; at least zero
 */
public record DebtLine(String debtId, Money openingBalance, Money interest, Money payment, Money closingBalance) {

    /** Validates every component; throws NullPointerException or IllegalArgumentException. */
    public DebtLine {
        Objects.requireNonNull(debtId, "debtId");
        Invariants.nonNegative(openingBalance, "openingBalance");
        Invariants.nonNegative(interest, "interest");
        Invariants.nonNegative(payment, "payment");
        Invariants.nonNegative(closingBalance, "closingBalance");
    }
}
