package com.rungs.readiness.domain.stage;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import java.math.BigDecimal;
import java.util.List;

/**
 * Stage 4: contributing at least 15% of income to retirement. Retired households skip it; below the target, progress
 * is the contribution as a share of 15% and there is no remaining dollar amount.
 */
public final class RetirementEvaluator implements StageEvaluator {

    private static final int STAGE = 4;

    /** The contribution percentage that satisfies the stage. */
    public static final BigDecimal TARGET_PERCENT = BigDecimal.valueOf(15);

    private static final String RETIRED_REASON =
            "You're retired, so you're no longer saving for retirement from income.";

    private static final String BLOCKER = "You're contributing %s%% of income to retirement, short of 15%%.";

    @Override
    public int stage() {
        return STAGE;
    }

    @Override
    public StageEvaluation evaluate(FinancialSnapshot snapshot, List<StageEvaluation> lowerStages) {
        if (snapshot.householdFlags().retired()) {
            return StageEvaluation.skipped(STAGE, RETIRED_REASON);
        }
        BigDecimal percent = snapshot.retirementContributionPercent();
        if (percent.compareTo(TARGET_PERCENT) >= 0) {
            return StageEvaluation.satisfied(STAGE);
        }
        // toPlainString, so 10.00 reads "10" rather than the "1E+1" stripTrailingZeros alone would give.
        String blocker = BLOCKER.formatted(percent.stripTrailingZeros().toPlainString());
        BigDecimal progress = Percentages.ofRatio(percent, TARGET_PERCENT);
        // Status follows progress, so a contribution that rounds to 0.0 is NOT_STARTED.
        if (progress.signum() == 0) {
            return StageEvaluation.notStarted(STAGE, null, blocker);
        }
        return StageEvaluation.inProgress(STAGE, progress, null, blocker);
    }
}
