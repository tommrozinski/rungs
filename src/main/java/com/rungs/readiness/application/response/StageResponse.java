package com.rungs.readiness.application.response;

import com.rungs.readiness.domain.stage.StageEvaluation;

/**
 * One stage row of an assessment response. Component order is the JSON property order; null components are written
 * as JSON {@code null}.
 *
 * @param stage the stage number, 1 to 7
 * @param status the {@code StageStatus} name
 * @param percentComplete progress as a scale-1 decimal string, for example {@code 50.0}
 * @param remainingAmount the dollar shortfall, or null when the stage has none
 * @param blocker what stands in the way, or null
 * @param skipReason why the stage does not apply, or null
 */
public record StageResponse(
        int stage,
        String status,
        String percentComplete,
        MoneyResponse remainingAmount,
        String blocker,
        String skipReason) {

    /** Returns the wire form of {@code evaluation}. */
    public static StageResponse from(StageEvaluation evaluation) {
        return new StageResponse(
                evaluation.stage(),
                evaluation.status().name(),
                evaluation.percentComplete().toPlainString(),
                evaluation.remainingAmount() == null ? null : MoneyResponse.of(evaluation.remainingAmount()),
                evaluation.blocker(),
                evaluation.skipReason());
    }
}
