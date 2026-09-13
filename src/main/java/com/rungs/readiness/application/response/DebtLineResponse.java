package com.rungs.readiness.application.response;

import com.rungs.readiness.domain.projection.DebtLine;

/**
 * One debt's activity within a single simulated month (story 010 REQ-006). Component order is the JSON property
 * order.
 *
 * @param debtId the debt's id
 * @param openingBalance the balance owed before this month's interest and payment
 * @param interest interest accrued this month
 * @param payment the amount applied to this debt this month
 * @param closingBalance the balance owed after this month's interest and payment
 */
public record DebtLineResponse(
        String debtId,
        MoneyResponse openingBalance,
        MoneyResponse interest,
        MoneyResponse payment,
        MoneyResponse closingBalance) {

    /** Returns the wire form of {@code line}. */
    public static DebtLineResponse from(DebtLine line) {
        return new DebtLineResponse(
                line.debtId(),
                MoneyResponse.of(line.openingBalance()),
                MoneyResponse.of(line.interest()),
                MoneyResponse.of(line.payment()),
                MoneyResponse.of(line.closingBalance()));
    }
}
