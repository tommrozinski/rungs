package com.rungs.readiness.application.response;

import com.rungs.readiness.domain.model.Debt;

/**
 * One debt as it appears in a projection response (story 010 REQ-006), in supplied order, so a client can label
 * schedule lines without holding the request. Component order is the JSON property order.
 *
 * @param id the debt's id
 * @param label the debt's label
 * @param type the {@code DebtType} name
 * @param startingBalance the balance at the start of the projection
 */
public record ProjectionDebtResponse(String id, String label, String type, MoneyResponse startingBalance) {

    /** Returns the wire form of {@code debt}. */
    public static ProjectionDebtResponse from(Debt debt) {
        return new ProjectionDebtResponse(
                debt.id(), debt.label(), debt.type().name(), MoneyResponse.of(debt.balance()));
    }
}
