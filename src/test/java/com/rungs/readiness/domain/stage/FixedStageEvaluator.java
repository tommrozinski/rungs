package com.rungs.readiness.domain.stage;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Test fixture that claims a configured stage number and returns a configured result, which may be null or name
 * another stage, so ladder tests do not depend on the real evaluators. It records every {@code lowerStages} list it
 * receives, in call order, exactly as passed.
 */
public final class FixedStageEvaluator implements StageEvaluator {

    private final int stage;
    private final StageEvaluation result;
    private final List<List<StageEvaluation>> receivedLowerStages = new ArrayList<>();

    private FixedStageEvaluator(int stage, StageEvaluation result) {
        this.stage = stage;
        this.result = result;
    }

    /** Returns an evaluator for {@code stage} that always returns {@code result}, which may be null. */
    public static FixedStageEvaluator returning(int stage, StageEvaluation result) {
        return new FixedStageEvaluator(stage, result);
    }

    /** Returns an evaluator for {@code stage} that reports it satisfied. */
    public static FixedStageEvaluator satisfied(int stage) {
        return returning(stage, StageEvaluation.satisfied(stage));
    }

    /** Returns an evaluator for {@code stage} that reports it half done. */
    public static FixedStageEvaluator inProgress(int stage) {
        return returning(stage, StageEvaluation.inProgress(
                stage, new BigDecimal("50.0"), Money.ofDollars(500), "Stage " + stage + " is in progress."));
    }

    /** Returns an evaluator for {@code stage} that reports it not started. */
    public static FixedStageEvaluator notStarted(int stage) {
        return returning(stage, StageEvaluation.notStarted(stage, null, "Stage " + stage + " is not started."));
    }

    /** Returns an evaluator for {@code stage} that reports it skipped. */
    public static FixedStageEvaluator skipped(int stage) {
        return returning(stage, StageEvaluation.skipped(stage, "Stage " + stage + " does not apply."));
    }

    @Override
    public int stage() {
        return stage;
    }

    @Override
    public StageEvaluation evaluate(FinancialSnapshot snapshot, List<StageEvaluation> lowerStages) {
        receivedLowerStages.add(lowerStages);
        return result;
    }

    /** Returns how many times {@link #evaluate} ran. */
    public int calls() {
        return receivedLowerStages.size();
    }

    /** Returns the {@code lowerStages} list passed to the most recent call; throws if never called. */
    public List<StageEvaluation> lastLowerStages() {
        return receivedLowerStages.getLast();
    }
}
