package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Debt;
import com.rungs.readiness.domain.model.Money;
import java.util.List;
import java.util.Objects;

/**
 * Everything {@link PayoffSimulator} (story 008 task 2.1) needs to run one household's payoff schedule (008
 * REQ-001).
 *
 * @param debts the consumer debts to simulate, in supplied order; an unmodifiable copy
 * @param monthlySurplus income left after essential expenses and minimums; may be negative, in which case the
 *     simulator treats it as $0 (008 REQ-003)
 * @param extraMonthlyPayment additional amount directed at the target debt every month; at least zero
 * @param ordering the fixed sequence extra payments target, applied once from starting balances (009)
 * @param horizonMonths the most months to simulate before giving up; from 1 to 600 inclusive
 */
public record SimulationInput(
        List<Debt> debts, Money monthlySurplus, Money extraMonthlyPayment, PayoffOrdering ordering, int horizonMonths) {

    /** Fewest months a horizon may run. */
    public static final int MIN_HORIZON_MONTHS = 1;

    /** Most months a horizon may run. */
    public static final int MAX_HORIZON_MONTHS = 600;

    /** Validates every component and copies the debts; throws NullPointerException or IllegalArgumentException. */
    public SimulationInput {
        debts = List.copyOf(Objects.requireNonNull(debts, "debts"));
        Objects.requireNonNull(monthlySurplus, "monthlySurplus");
        Invariants.nonNegative(extraMonthlyPayment, "extraMonthlyPayment");
        Objects.requireNonNull(ordering, "ordering");
        if (horizonMonths < MIN_HORIZON_MONTHS || horizonMonths > MAX_HORIZON_MONTHS) {
            throw new IllegalArgumentException("horizonMonths must be between 1 and 600");
        }
    }
}
