package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Debt;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The two payoff orderings the PRD offers. Each sorts a copy of the supplied debts with a stable sort, so
 * debts that compare equal keep the order they were supplied in — the documented tie-break (009 REQ-003,
 * REQ-004, REQ-007).
 */
public enum Strategy {

    /** Ascending starting balance; ties keep supplied order (009 REQ-003). */
    SNOWBALL(Comparator.comparing(Debt::balance)),

    /**
     * Descending annual interest rate, compared numerically so {@code 7.25} equals {@code 7.250}; ties keep
     * supplied order. Balance is never a tie-breaker (009 REQ-004).
     */
    AVALANCHE(Comparator.comparing(Debt::annualInterestRate).reversed());

    private final Comparator<Debt> comparator;

    Strategy(Comparator<Debt> comparator) {
        this.comparator = comparator;
    }

    /** Returns this strategy's ordering. */
    public PayoffOrdering ordering() {
        return debts -> {
            List<Debt> ordered = new ArrayList<>(Objects.requireNonNull(debts, "debts"));
            ordered.sort(comparator);
            return List.copyOf(ordered);
        };
    }
}
