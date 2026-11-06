package com.rungs.readiness.domain.stage;

import java.util.List;
import java.util.Objects;

/**
 * The full ladder for one assessment. Construction copies {@code stages} and requires exactly seven evaluations for
 * stages 1 to 7 in order and a current stage of 1 to 7. Null arguments or entries throw
 * {@link NullPointerException}; every other rule throws {@link IllegalArgumentException}.
 *
 * @param currentStage the lowest stage neither satisfied nor skipped, or 7 when every stage is
 * @param stages all seven results, unmodifiable and ordered by stage
 */
public record LadderResult(int currentStage, List<StageEvaluation> stages) {

    /** Validates and copies the components. */
    public LadderResult {
        if (currentStage < StageEvaluation.FIRST_STAGE || currentStage > StageEvaluation.LAST_STAGE) {
            throw new IllegalArgumentException("currentStage must be between 1 and 7");
        }
        stages = List.copyOf(Objects.requireNonNull(stages, "stages"));
        if (stages.size() != StageEvaluation.LAST_STAGE) {
            throw new IllegalArgumentException("stages must hold exactly 7 evaluations");
        }
        for (int index = 0; index < stages.size(); index++) {
            int expectedStage = index + 1;
            if (stages.get(index).stage() != expectedStage) {
                throw new IllegalArgumentException(
                        "stages entry " + expectedStage + " must be the stage " + expectedStage + " evaluation");
            }
        }
    }

    /** Returns the result for {@code stage}; throws IllegalArgumentException unless it is 1 to 7. */
    public StageEvaluation evaluation(int stage) {
        if (stage < StageEvaluation.FIRST_STAGE || stage > StageEvaluation.LAST_STAGE) {
            throw new IllegalArgumentException("stage must be between 1 and 7");
        }
        return stages.get(stage - 1);
    }
}
