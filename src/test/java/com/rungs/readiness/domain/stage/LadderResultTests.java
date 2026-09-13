package com.rungs.readiness.domain.stage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LadderResultTests {

    @Test
    void exposesTheCurrentStageAndAllSevenEvaluationsInOrder() {
        List<StageEvaluation> stages = sevenSatisfied();

        LadderResult result = new LadderResult(7, stages);

        assertThat(result.currentStage()).isEqualTo(7);
        assertThat(result.stages()).containsExactlyElementsOf(stages);
    }

    @Test
    void copiesTheStagesSoLaterChangesToTheSourceDoNotLeakIn() {
        List<StageEvaluation> stages = new ArrayList<>(sevenSatisfied());
        LadderResult result = new LadderResult(7, stages);

        stages.set(0, StageEvaluation.skipped(1, "Replaced."));

        assertThat(result.evaluation(1)).isEqualTo(StageEvaluation.satisfied(1));
    }

    @Test
    void stagesAreUnmodifiable() {
        LadderResult result = new LadderResult(7, sevenSatisfied());

        assertThatThrownBy(() -> result.stages().remove(0)).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 7})
    void looksUpOneStageByNumber(int stage) {
        LadderResult result = new LadderResult(7, sevenSatisfied());

        assertThat(result.evaluation(stage)).isEqualTo(StageEvaluation.satisfied(stage));
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, 0, 8})
    void rejectsALookupOutsideOneToSeven(int stage) {
        LadderResult result = new LadderResult(7, sevenSatisfied());

        assertThatThrownBy(() -> result.evaluation(stage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("stage must be between 1 and 7");
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, 0, 8})
    void rejectsACurrentStageOutsideOneToSeven(int currentStage) {
        List<StageEvaluation> stages = sevenSatisfied();

        assertThatThrownBy(() -> new LadderResult(currentStage, stages))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("currentStage must be between 1 and 7");
    }

    @Test
    void rejectsNullStagesOrANullEntry() {
        List<StageEvaluation> withNull = new ArrayList<>(sevenSatisfied());
        withNull.set(3, null);

        assertThatThrownBy(() -> new LadderResult(1, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("stages");
        assertThatThrownBy(() -> new LadderResult(1, withNull)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, 8})
    void rejectsAnythingButSevenEvaluations(int count) {
        List<StageEvaluation> stages = IntStream.rangeClosed(1, count)
                .mapToObj(stage -> StageEvaluation.satisfied(Math.min(stage, 7)))
                .toList();

        assertThatThrownBy(() -> new LadderResult(1, stages))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("stages must hold exactly 7 evaluations");
    }

    @Test
    void rejectsEvaluationsOutOfStageOrder() {
        List<StageEvaluation> stages = new ArrayList<>(sevenSatisfied());
        stages.set(2, StageEvaluation.satisfied(4));
        stages.set(3, StageEvaluation.satisfied(3));

        assertThatThrownBy(() -> new LadderResult(1, stages))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("stages entry 3 must be the stage 3 evaluation");
    }

    private static List<StageEvaluation> sevenSatisfied() {
        return IntStream.rangeClosed(1, 7).mapToObj(StageEvaluation::satisfied).toList();
    }
}
