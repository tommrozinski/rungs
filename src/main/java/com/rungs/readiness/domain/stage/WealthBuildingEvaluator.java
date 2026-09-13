package com.rungs.readiness.domain.stage;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import java.math.BigDecimal;
import java.util.List;

/**
 * Stage 7: building wealth, reached once stages 1 to 6 are each satisfied or skipped. Otherwise progress is the share
 * of those six stages that are, with no remaining dollar amount. Never skipped.
 */
public final class WealthBuildingEvaluator implements StageEvaluator {

    private static final int STAGE = 7;

    /** Stages 1 to 6, the ones this stage waits on. */
    private static final int EARLIER_STAGES = STAGE - 1;

    private static final String BLOCKER = "%s of the 6 earlier stages are complete.";

    @Override
    public int stage() {
        return STAGE;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException unless {@code lowerStages} holds exactly six evaluations
     */
    @Override
    public StageEvaluation evaluate(FinancialSnapshot snapshot, List<StageEvaluation> lowerStages) {
        if (lowerStages.size() != EARLIER_STAGES) {
            throw new IllegalArgumentException("lowerStages must hold exactly 6 evaluations");
        }
        long complete = lowerStages.stream()
                .filter(lower -> lower.status() == StageStatus.SATISFIED || lower.status() == StageStatus.SKIPPED)
                .count();
        if (complete == EARLIER_STAGES) {
            return StageEvaluation.satisfied(STAGE);
        }
        String blocker = BLOCKER.formatted(complete);
        if (complete == 0) {
            return StageEvaluation.notStarted(STAGE, null, blocker);
        }
        BigDecimal percent = Percentages.ofRatio(BigDecimal.valueOf(complete), BigDecimal.valueOf(EARLIER_STAGES));
        return StageEvaluation.inProgress(STAGE, percent, null, blocker);
    }
}
