package com.rungs.readiness.application;

import com.rungs.readiness.application.validation.HorizonMonths;
import com.rungs.readiness.application.validation.MoneyAmount;
import com.rungs.readiness.application.validation.StrategyName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * A payoff projection request as submitted (story 010), before normalization: a snapshot plus the payoff options.
 * Money is a raw decimal string so a blank value can become zero (PRD FR-4); a missing {@code strategy} means
 * {@code SNOWBALL} and a missing {@code horizonMonths} means 360 (REQ-003), applied by {@code ProjectionService}
 * rather than here.
 *
 * @param snapshot the household snapshot to project; required, the same shape {@code /v1/assessments} accepts
 * @param extraMonthlyPayment additional amount directed at the target debt every month; blank means zero
 * @param strategy the payoff strategy name, {@code SNOWBALL} or {@code AVALANCHE}; null means {@code SNOWBALL}
 * @param horizonMonths the most months to simulate, 1 to 600; null means 360
 */
public record ProjectionInput(
        @Valid @NotNull(message = "INVALID_TYPE") SnapshotInput snapshot,
        @MoneyAmount String extraMonthlyPayment,
        @StrategyName String strategy,
        @HorizonMonths Integer horizonMonths) {
}
