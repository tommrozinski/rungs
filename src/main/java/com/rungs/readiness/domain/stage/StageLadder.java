package com.rungs.readiness.domain.stage;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Runs all seven stage evaluators and picks the household's current stage. Every evaluator runs on every
 * assessment; none is skipped because a lower stage is incomplete. The ladder reads no clock, random source, or
 * environment.
 */
public final class StageLadder {

    /** The evaluators indexed by stage number minus one; never modified after construction. */
    private final StageEvaluator[] evaluators;

    /**
     * Creates a ladder from exactly one evaluator per stage 1 to 7, in any order.
     *
     * @throws NullPointerException if {@code evaluators} or any element is null
     * @throws IllegalArgumentException for an evaluator whose stage is outside 1 to 7, a duplicate stage, or a
     *     missing stage; the message names the stage number
     */
    public StageLadder(Collection<StageEvaluator> evaluators) {
        this.evaluators = new StageEvaluator[StageEvaluation.LAST_STAGE];
        for (StageEvaluator evaluator : Objects.requireNonNull(evaluators, "evaluators")) {
            int stage = Objects.requireNonNull(evaluator, "evaluator").stage();
            if (stage < StageEvaluation.FIRST_STAGE || stage > StageEvaluation.LAST_STAGE) {
                throw new IllegalArgumentException("evaluator stage " + stage + " is outside 1 to 7");
            }
            if (this.evaluators[stage - 1] != null) {
                throw new IllegalArgumentException("duplicate evaluator for stage " + stage);
            }
            this.evaluators[stage - 1] = evaluator;
        }
        for (int index = 0; index < this.evaluators.length; index++) {
            if (this.evaluators[index] == null) {
                throw new IllegalArgumentException("missing evaluator for stage " + (index + 1));
            }
        }
    }

    /**
     * Evaluates stages 1 to 7 in order, passing each evaluator the unmodifiable results of the stages below it, and
     * returns all seven results with the current stage: the lowest stage neither {@code SATISFIED} nor
     * {@code SKIPPED}, or 7 when every stage is.
     *
     * @throws NullPointerException if {@code snapshot} is null
     * @throws IllegalStateException if an evaluator returns null or an evaluation for another stage
     */
    public LadderResult assess(FinancialSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<StageEvaluation> results = new ArrayList<>(evaluators.length);
        for (int index = 0; index < evaluators.length; index++) {
            // The slot, fixed at construction, is the stage; stage() is not asked again.
            int stage = index + 1;
            StageEvaluation result = evaluators[index].evaluate(snapshot, List.copyOf(results));
            if (result == null) {
                throw new IllegalStateException("evaluator for stage " + stage + " returned no evaluation");
            }
            if (result.stage() != stage) {
                throw new IllegalStateException(
                        "evaluator for stage " + stage + " returned an evaluation for stage " + result.stage());
            }
            results.add(result);
        }
        return new LadderResult(currentStage(results), results);
    }

    /** Returns the lowest stage whose status is neither SATISFIED nor SKIPPED, or 7 when there is none. */
    private static int currentStage(List<StageEvaluation> results) {
        for (StageEvaluation result : results) {
            if (result.status() != StageStatus.SATISFIED && result.status() != StageStatus.SKIPPED) {
                return result.stage();
            }
        }
        return StageEvaluation.LAST_STAGE;
    }
}
