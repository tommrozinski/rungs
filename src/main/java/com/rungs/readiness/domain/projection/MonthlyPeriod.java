package com.rungs.readiness.domain.projection;

import java.util.List;
import java.util.Objects;

/**
 * One simulated month's activity across every debt unretired at its start (008 REQ-006).
 *
 * @param month the 1-based month number
 * @param lines one entry per debt unretired at the start of this month, in supplied order; an unmodifiable copy
 * @param retiredDebtIds the ids of debts retired this month; an unmodifiable copy
 */
public record MonthlyPeriod(int month, List<DebtLine> lines, List<String> retiredDebtIds) {

    /** Validates every component and copies the lists; throws NullPointerException or IllegalArgumentException. */
    public MonthlyPeriod {
        if (month < 1) {
            throw new IllegalArgumentException("month must be at least 1");
        }
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        retiredDebtIds = List.copyOf(Objects.requireNonNull(retiredDebtIds, "retiredDebtIds"));
    }
}
