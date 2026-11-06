package com.rungs.readiness.domain.stage;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.HouseholdFlags;
import java.util.List;

/**
 * Stage 5: saving for college. Skipped without college plans, or else when retired; otherwise satisfied by any monthly
 * college contribution above $0.
 */
public final class CollegeEvaluator implements StageEvaluator {

    private static final int STAGE = 5;

    private static final String NO_PLANS_REASON = "You don't plan to fund college, so there's nothing to save for.";

    private static final String RETIRED_REASON = "You're retired, so there's no college fund to build.";

    private static final String BLOCKER = "You plan to fund college but aren't contributing to it yet.";

    @Override
    public int stage() {
        return STAGE;
    }

    @Override
    public StageEvaluation evaluate(FinancialSnapshot snapshot, List<StageEvaluation> lowerStages) {
        HouseholdFlags flags = snapshot.householdFlags();
        if (!flags.intendsToFundCollege()) {
            return StageEvaluation.skipped(STAGE, NO_PLANS_REASON);
        }
        if (flags.retired()) {
            return StageEvaluation.skipped(STAGE, RETIRED_REASON);
        }
        if (snapshot.monthlyCollegeContribution().isPositive()) {
            return StageEvaluation.satisfied(STAGE);
        }
        return StageEvaluation.notStarted(STAGE, null, BLOCKER);
    }
}
