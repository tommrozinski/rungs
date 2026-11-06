package com.rungs.readiness.domain.stage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.FinancialSnapshotBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class StageLadderTests {

    private static final FinancialSnapshot SNAPSHOT = FinancialSnapshotBuilder.aSnapshot().build();

    @Test
    void reportsStageSevenWhenEveryStageIsSatisfied() {
        StageLadder ladder = new StageLadder(allSatisfied());

        LadderResult result = ladder.assess(SNAPSHOT);

        assertThat(result.currentStage()).isEqualTo(7);
        assertThat(result.stages())
                .containsExactlyElementsOf(IntStream.rangeClosed(1, 7).mapToObj(StageEvaluation::satisfied).toList());
    }

    /** Each character is one stage, 1 to 7: S satisfied, K skipped, P in progress, N not started. */
    @ParameterizedTest
    @CsvSource({
        "SSSSSSS, 7",
        "SKSKSKS, 7",
        "KKKKKKK, 7",
        "SKPSSSS, 3",
        "KKKKKKN, 7",
        "SSSSSSP, 7",
        "NSSSSSS, 1",
        "PSSSSSS, 1",
        "SPNSSSS, 2",
        "SSKNPKS, 4"
    })
    void currentStageIsTheLowestStageNeitherSatisfiedNorSkipped(String statuses, int expectedCurrentStage) {
        List<FixedStageEvaluator> evaluators = evaluatorsFor(statuses);

        LadderResult result = new StageLadder(List.copyOf(evaluators)).assess(SNAPSHOT);

        assertThat(result.currentStage()).isEqualTo(expectedCurrentStage);
        assertThat(result.stages())
                .extracting(StageEvaluation::status)
                .containsExactlyElementsOf(statuses.chars().mapToObj(StageLadderTests::statusOf).toList());
        assertThat(evaluators).allSatisfy(evaluator -> assertThat(evaluator.calls()).isEqualTo(1));
    }

    @Test
    void rejectsANullCollectionOrEvaluator() {
        List<StageEvaluator> withNull = new ArrayList<>(allSatisfied());
        withNull.set(2, null);

        assertThatThrownBy(() -> new StageLadder(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("evaluators");
        assertThatThrownBy(() -> new StageLadder(withNull))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("evaluator");
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, 0, 8, Integer.MAX_VALUE})
    void rejectsAnEvaluatorForAStageOutsideOneToSeven(int stage) {
        List<StageEvaluator> evaluators = new ArrayList<>(allSatisfied());
        evaluators.add(FixedStageEvaluator.returning(stage, StageEvaluation.satisfied(1)));

        assertThatThrownBy(() -> new StageLadder(evaluators))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("evaluator stage " + stage + " is outside 1 to 7");
    }

    @Test
    void rejectsADuplicateEvaluator() {
        List<StageEvaluator> evaluators = new ArrayList<>(allSatisfied());
        evaluators.add(FixedStageEvaluator.inProgress(3));

        assertThatThrownBy(() -> new StageLadder(evaluators))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("duplicate evaluator for stage 3");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 7})
    void rejectsAMissingEvaluator(int missingStage) {
        List<StageEvaluator> evaluators = new ArrayList<>(allSatisfied());
        evaluators.remove(missingStage - 1);

        assertThatThrownBy(() -> new StageLadder(evaluators))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("missing evaluator for stage " + missingStage);
    }

    @Test
    void rejectsAnEmptyCollectionNamingTheFirstMissingStage() {
        List<StageEvaluator> none = List.of();

        assertThatThrownBy(() -> new StageLadder(none))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("missing evaluator for stage 1");
    }

    @Test
    void rejectsANullSnapshot() {
        StageLadder ladder = new StageLadder(allSatisfied());

        assertThatThrownBy(() -> ladder.assess(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("snapshot");
    }

    @Test
    void failsWhenAnEvaluatorReturnsNull() {
        List<StageEvaluator> evaluators = new ArrayList<>(allSatisfied());
        evaluators.set(3, FixedStageEvaluator.returning(4, null));
        StageLadder ladder = new StageLadder(evaluators);

        assertThatThrownBy(() -> ladder.assess(SNAPSHOT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("evaluator for stage 4 returned no evaluation");
    }

    @Test
    void failsWhenAnEvaluatorReturnsAnotherStagesNumber() {
        List<StageEvaluator> evaluators = new ArrayList<>(allSatisfied());
        evaluators.set(5, FixedStageEvaluator.returning(6, StageEvaluation.satisfied(2)));
        StageLadder ladder = new StageLadder(evaluators);

        assertThatThrownBy(() -> ladder.assess(SNAPSHOT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("evaluator for stage 6 returned an evaluation for stage 2");
    }

    @Test
    void passesEachEvaluatorTheResultsOfTheStagesBelowItInOrder() {
        List<FixedStageEvaluator> evaluators = evaluatorsFor("SKPNSKS");

        LadderResult result = new StageLadder(List.copyOf(evaluators)).assess(SNAPSHOT);

        assertThat(evaluators.get(0).lastLowerStages()).isEmpty();
        for (int stage = 2; stage <= 7; stage++) {
            assertThat(evaluators.get(stage - 1).lastLowerStages())
                    .containsExactlyElementsOf(result.stages().subList(0, stage - 1));
        }
    }

    @Test
    void runsEvaluatorsInStageOrderWhateverOrderTheyAreSupplied() {
        List<FixedStageEvaluator> evaluators = evaluatorsFor("SKPNSKS");
        List<StageEvaluator> reversed = new ArrayList<>(evaluators);
        Collections.reverse(reversed);

        LadderResult result = new StageLadder(reversed).assess(SNAPSHOT);

        assertThat(result.stages()).extracting(StageEvaluation::stage).containsExactly(1, 2, 3, 4, 5, 6, 7);
        assertThat(result.currentStage()).isEqualTo(3);
        assertThat(evaluators.get(0).lastLowerStages()).isEmpty();
        assertThat(evaluators.get(6).lastLowerStages())
                .containsExactlyElementsOf(result.stages().subList(0, 6));
    }

    @Test
    void lowerStagesPassedToAnEvaluatorAreUnmodifiable() {
        List<FixedStageEvaluator> evaluators = evaluatorsFor("SSSSSSS");

        new StageLadder(List.copyOf(evaluators)).assess(SNAPSHOT);

        List<StageEvaluation> lowerStages = evaluators.get(6).lastLowerStages();
        assertThatThrownBy(() -> lowerStages.add(StageEvaluation.satisfied(7)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void runsEveryEvaluatorOnEachAssessment() {
        List<FixedStageEvaluator> evaluators = evaluatorsFor("NNNNNNN");
        StageLadder ladder = new StageLadder(List.copyOf(evaluators));

        ladder.assess(SNAPSHOT);
        LadderResult second = ladder.assess(SNAPSHOT);

        assertThat(evaluators).allSatisfy(evaluator -> assertThat(evaluator.calls()).isEqualTo(2));
        assertThat(second.currentStage()).isEqualTo(1);
        assertThat(evaluators.get(6).lastLowerStages()).hasSize(6);
    }

    private static List<FixedStageEvaluator> evaluatorsFor(String statuses) {
        return IntStream.rangeClosed(1, 7)
                .mapToObj(stage -> switch (statusOf(statuses.charAt(stage - 1))) {
                    case SATISFIED -> FixedStageEvaluator.satisfied(stage);
                    case SKIPPED -> FixedStageEvaluator.skipped(stage);
                    case IN_PROGRESS -> FixedStageEvaluator.inProgress(stage);
                    case NOT_STARTED -> FixedStageEvaluator.notStarted(stage);
                })
                .toList();
    }

    private static StageStatus statusOf(int code) {
        return switch (code) {
            case 'S' -> StageStatus.SATISFIED;
            case 'K' -> StageStatus.SKIPPED;
            case 'P' -> StageStatus.IN_PROGRESS;
            case 'N' -> StageStatus.NOT_STARTED;
            default -> throw new IllegalArgumentException("unknown status code");
        };
    }

    private static List<StageEvaluator> allSatisfied() {
        return IntStream.rangeClosed(1, 7).<StageEvaluator>mapToObj(FixedStageEvaluator::satisfied).toList();
    }
}
