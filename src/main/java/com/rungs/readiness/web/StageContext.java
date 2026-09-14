package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.projection.MortgagePayoff;
import com.rungs.readiness.domain.projection.PayoffSchedule;
import com.rungs.readiness.domain.stage.StageEvaluation;
import java.util.Optional;

/**
 * Everything a {@link Stage}'s copy methods need for one request: the stage's own evaluation, the household
 * snapshot, its debt-snowball payoff schedule, and stage 6's current-payment payoff and its debt-snowball
 * comparison against it (both empty for every other stage, and for a renter).
 *
 * @param evaluation this stage's own result from the ladder
 * @param snapshot the household snapshot the ladder was assessed from
 * @param schedule the household's debt-snowball payoff schedule
 * @param currentPayoff stage 6's mortgage payoff at the current payment alone, or empty for a renter
 * @param mortgageComparison whether, and how, the debt snowball beats {@code currentPayoff}; empty when there is no
 *     mortgage, or when the snowball does not finish it sooner
 */
record StageContext(
        StageEvaluation evaluation,
        FinancialSnapshot snapshot,
        PayoffSchedule schedule,
        Optional<MortgagePayoff> currentPayoff,
        Optional<SnowballComparison> mortgageComparison) {
}
