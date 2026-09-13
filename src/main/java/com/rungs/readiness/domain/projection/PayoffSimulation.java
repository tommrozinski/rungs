package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Money;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The result of simulating one household's payoff plan (008 REQ-005).
 *
 * @param schedule every simulated month, in order; an unmodifiable copy
 * @param monthsToDebtFreedom the month the last payable debt was retired, ignoring unpayable debts; null if
 *     any payable debt is still unretired at the horizon; 0 when there are no debts (008 REQ-007)
 * @param totalInterestPaid the sum of all interest across the schedule; at least zero
 * @param truncated whether the horizon was reached with a debt still unretired
 * @param unpayableDebtIds ids of debts unretired at the horizon whose closing balance is at or above their
 *     starting balance (008 REQ-007); an unmodifiable copy
 */
public record PayoffSimulation(
        List<MonthlyPeriod> schedule,
        Integer monthsToDebtFreedom,
        Money totalInterestPaid,
        boolean truncated,
        List<String> unpayableDebtIds) {

    /** Validates every component and copies the lists; throws NullPointerException or IllegalArgumentException. */
    public PayoffSimulation {
        schedule = List.copyOf(Objects.requireNonNull(schedule, "schedule"));
        if (monthsToDebtFreedom != null && monthsToDebtFreedom < 0) {
            throw new IllegalArgumentException("monthsToDebtFreedom must not be negative");
        }
        Invariants.nonNegative(totalInterestPaid, "totalInterestPaid");
        unpayableDebtIds = List.copyOf(Objects.requireNonNull(unpayableDebtIds, "unpayableDebtIds"));
    }

    /** Returns the month {@code debtId} was retired in this schedule, or empty if it never was (008 REQ-013). */
    public Optional<Integer> monthRetired(String debtId) {
        Objects.requireNonNull(debtId, "debtId");
        for (MonthlyPeriod period : schedule) {
            if (period.retiredDebtIds().contains(debtId)) {
                return Optional.of(period.month());
            }
        }
        return Optional.empty();
    }
}
