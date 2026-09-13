package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Debt;
import java.util.List;

/**
 * Decides the fixed sequence extra payments target, computed once from starting balances before month 1
 * (009 REQ-001, REQ-005). The simulator walks this sequence every month, skipping any debt already retired.
 */
public interface PayoffOrdering {

    /**
     * Returns every debt in {@code debts} exactly once, in payoff sequence; does not mutate {@code debts} or
     * change its order, which remains the tie-break reference (009 REQ-007).
     */
    List<Debt> order(List<Debt> debts);
}
