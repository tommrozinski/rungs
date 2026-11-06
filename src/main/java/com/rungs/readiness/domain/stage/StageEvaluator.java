package com.rungs.readiness.domain.stage;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import java.util.List;

/**
 * Decides how far along a household is on one stage. An internal domain interface, not a port: the application
 * constructs the {@link StageLadder} with all seven explicitly. Implementations must be deterministic and read no
 * clock, random source, or environment.
 */
public interface StageEvaluator {

    /** Returns the stage number, 1 to 7, this evaluator decides. */
    int stage();

    /**
     * Evaluates this stage for {@code snapshot}.
     *
     * @param snapshot the household's finances
     * @param lowerStages the unmodifiable results for stages 1 to {@code stage() - 1}, in order; empty for stage 1.
     *     Only stage 7 uses them.
     * @return this stage's result, whose {@code stage} equals {@link #stage()}
     */
    StageEvaluation evaluate(FinancialSnapshot snapshot, List<StageEvaluation> lowerStages);
}
