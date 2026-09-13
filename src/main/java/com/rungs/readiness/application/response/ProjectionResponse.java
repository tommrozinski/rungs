package com.rungs.readiness.application.response;

import java.util.List;

/**
 * The wire form of a payoff projection (story 010 REQ-006), returned by {@code POST /v1/projections}. Component
 * order is the JSON property order, so identical projections serialize to identical bytes; every key is always
 * present, with {@code null} written explicitly where a value is absent.
 *
 * @param monthsToNextStage equals {@code monthsToDebtFreedom} when the current stage is 2; null otherwise (REQ-005)
 * @param monthsToDebtFreedom the month the last payable debt is retired; null if any payable debt is still
 *     unretired at the horizon; 0 with no debts
 * @param totalInterestPaid the sum of all interest across the schedule
 * @param truncated whether the horizon was reached with a debt still unretired
 * @param unpayableDebtIds ids of debts that cannot be paid off within the horizon; an unmodifiable copy
 * @param debts every simulated debt, in supplied order; an unmodifiable copy
 * @param schedule every simulated month, in order; an unmodifiable copy
 * @param warnings non-fatal conditions detected on the snapshot (story 012); an unmodifiable copy
 */
public record ProjectionResponse(
        Integer monthsToNextStage,
        Integer monthsToDebtFreedom,
        MoneyResponse totalInterestPaid,
        boolean truncated,
        List<String> unpayableDebtIds,
        List<ProjectionDebtResponse> debts,
        List<MonthlyPeriodResponse> schedule,
        List<WarningResponse> warnings) {

    /** Copies the lists; throws NullPointerException for a null list or element. */
    public ProjectionResponse {
        unpayableDebtIds = List.copyOf(unpayableDebtIds);
        debts = List.copyOf(debts);
        schedule = List.copyOf(schedule);
        warnings = List.copyOf(warnings);
    }
}
