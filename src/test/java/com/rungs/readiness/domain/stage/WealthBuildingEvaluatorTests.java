package com.rungs.readiness.domain.stage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.FinancialSnapshotBuilder;
import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class WealthBuildingEvaluatorTests {

    private static final FinancialSnapshot SNAPSHOT = FinancialSnapshotBuilder.aSnapshot().build();

    private final WealthBuildingEvaluator evaluator = new WealthBuildingEvaluator();

    @Test
    void decidesStageSeven() {
        assertThat(evaluator.stage()).isEqualTo(7);
    }

    /** Each character is one lower stage, 1 to 6: S satisfied, K skipped, P in progress, N not started. */
    @ParameterizedTest
    @ValueSource(strings = {"SSSSSS", "SKSKSK", "KKKKKK", "SSSSKK"})
    void isSatisfiedWhenEveryLowerStageIsSatisfiedOrSkipped(String statuses) {
        StageEvaluation result = evaluator.evaluate(SNAPSHOT, lowerStages(statuses));

        assertThat(result).isEqualTo(StageEvaluation.satisfied(7));
    }

    @ParameterizedTest
    @CsvSource({
        "SSSSSP, 83.3, 5 of the 6 earlier stages are complete.",
        "PKSKSS, 83.3, 5 of the 6 earlier stages are complete.",
        "SPNSKS, 66.6, 4 of the 6 earlier stages are complete.",
        "SPNPKN, 33.3, 2 of the 6 earlier stages are complete.",
        "KPNPNP, 16.6, 1 of the 6 earlier stages are complete."
    })
    void isInProgressWithTheShareOfLowerStagesSatisfiedOrSkipped(String statuses, String percent, String blocker) {
        StageEvaluation result = evaluator.evaluate(SNAPSHOT, lowerStages(statuses));

        assertThat(result).isEqualTo(StageEvaluation.inProgress(7, new BigDecimal(percent), null, blocker));
    }

    @Test
    void isNotStartedWhenNoLowerStageIsSatisfiedOrSkipped() {
        StageEvaluation result = evaluator.evaluate(SNAPSHOT, lowerStages("PNPNPN"));

        assertThat(result).isEqualTo(StageEvaluation.notStarted(7, null, "0 of the 6 earlier stages are complete."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "SSSSS", "SSSSSSS"})
    void rejectsLowerStagesThatAreNotExactlySix(String statuses) {
        List<StageEvaluation> lowerStages = lowerStages(statuses);

        assertThatThrownBy(() -> evaluator.evaluate(SNAPSHOT, lowerStages))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("lowerStages must hold exactly 6 evaluations");
    }

    private static List<StageEvaluation> lowerStages(String statuses) {
        return IntStream.rangeClosed(1, statuses.length())
                .mapToObj(stage -> switch (statuses.charAt(stage - 1)) {
                    case 'S' -> StageEvaluation.satisfied(stage);
                    case 'K' -> StageEvaluation.skipped(stage, "Stage " + stage + " does not apply.");
                    case 'P' -> StageEvaluation.inProgress(
                            stage, new BigDecimal("50.0"), Money.ofDollars(500), "Stage " + stage + " is in progress.");
                    case 'N' -> StageEvaluation.notStarted(stage, null, "Stage " + stage + " is not started.");
                    default -> throw new IllegalArgumentException("unknown status code");
                })
                .toList();
    }
}
