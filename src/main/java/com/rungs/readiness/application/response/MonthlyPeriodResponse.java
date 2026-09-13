package com.rungs.readiness.application.response;

import com.rungs.readiness.domain.projection.MonthlyPeriod;
import java.util.List;

/**
 * One simulated month's activity across every debt unretired at its start (story 010 REQ-006). Component order is
 * the JSON property order.
 *
 * @param month the 1-based month number
 * @param lines one entry per debt unretired at the start of this month, in supplied order; an unmodifiable copy
 * @param retiredDebtIds the ids of debts retired this month; an unmodifiable copy
 */
public record MonthlyPeriodResponse(int month, List<DebtLineResponse> lines, List<String> retiredDebtIds) {

    /** Copies the lists; throws NullPointerException for a null list or element. */
    public MonthlyPeriodResponse {
        lines = List.copyOf(lines);
        retiredDebtIds = List.copyOf(retiredDebtIds);
    }

    /** Returns the wire form of {@code period}. */
    public static MonthlyPeriodResponse from(MonthlyPeriod period) {
        return new MonthlyPeriodResponse(
                period.month(), period.lines().stream().map(DebtLineResponse::from).toList(),
                period.retiredDebtIds());
    }
}
