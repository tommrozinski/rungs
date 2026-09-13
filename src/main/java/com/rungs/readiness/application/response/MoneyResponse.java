package com.rungs.readiness.application.response;

import com.rungs.readiness.domain.model.Money;

/**
 * A money amount in the fixed wire format (PRD §5), shared by the API and the view pages. Component order is the
 * JSON property order.
 *
 * @param amount the bare decimal string, for example {@code 1234.56} or {@code -50.00}
 * @param display the formatted string, for example {@code $1,234.56} or {@code -$50.00}
 */
public record MoneyResponse(String amount, String display) {

    /** Returns the wire form of {@code money}; throws NullPointerException if it is null. */
    public static MoneyResponse of(Money money) {
        return new MoneyResponse(money.toBareString(), money.toDisplayString());
    }
}
