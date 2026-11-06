package com.rungs.readiness.domain.stage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class StageEvaluationTests {

    private static final BigDecimal FULL = new BigDecimal("100.0");
    private static final Money REMAINING = Money.ofDollars(400);
    private static final String BLOCKER = "Save $400.00 more to finish your starter fund.";
    private static final String SKIP_REASON = "You rent your home.";

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
        "1, SATISFIED, 100.0, null, null, null",
        "2, IN_PROGRESS, 0.0, null, blocker, null",
        "3, IN_PROGRESS, 99.9, remaining, blocker, null",
        "4, IN_PROGRESS, 50.0, null, blocker, null",
        "5, NOT_STARTED, 0.0, remaining, blocker, null",
        "6, NOT_STARTED, 0.0, null, blocker, null",
        "7, SKIPPED, 0.0, null, null, skipReason"
    })
    void acceptsEveryAllowedCombinationAndExposesItsComponents(
            int stage, StageStatus status, String percent, String remaining, String blocker, String skipReason) {
        StageEvaluation evaluation = new StageEvaluation(
                stage, status, new BigDecimal(percent), remainingOf(remaining), blockerOf(blocker),
                skipReasonOf(skipReason));

        assertThat(evaluation.stage()).isEqualTo(stage);
        assertThat(evaluation.status()).isEqualTo(status);
        assertThat(evaluation.percentComplete()).isEqualTo(new BigDecimal(percent));
        assertThat(evaluation.remainingAmount()).isEqualTo(remainingOf(remaining));
        assertThat(evaluation.blocker()).isEqualTo(blockerOf(blocker));
        assertThat(evaluation.skipReason()).isEqualTo(skipReasonOf(skipReason));
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 8, Integer.MAX_VALUE})
    void rejectsAStageOutsideOneToSeven(int stage) {
        assertThatThrownBy(() -> new StageEvaluation(stage, StageStatus.SATISFIED, FULL, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("stage must be between 1 and 7");
    }

    @Test
    void rejectsANullStatusOrPercent() {
        assertThatThrownBy(() -> new StageEvaluation(1, null, FULL, null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("status");
        assertThatThrownBy(() -> new StageEvaluation(1, StageStatus.SATISFIED, null, null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("percentComplete");
    }

    @ParameterizedTest
    @CsvSource({
        "100, stage 3 percentComplete must have exactly 1 decimal place",
        "100.00, stage 3 percentComplete must have exactly 1 decimal place",
        "1E+2, stage 3 percentComplete must have exactly 1 decimal place",
        "-0.1, stage 3 percentComplete must be between 0.0 and 100.0",
        "100.1, stage 3 percentComplete must be between 0.0 and 100.0"
    })
    void rejectsAPercentNotAtScaleOneOrOutOfRange(String percent, String expectedMessage) {
        BigDecimal percentComplete = new BigDecimal(percent);

        assertThatThrownBy(() -> new StageEvaluation(
                        3, StageStatus.IN_PROGRESS, percentComplete, REMAINING, BLOCKER, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    /** Each row breaks exactly one construction rule; text markers are null, text, empty, or blank. */
    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
        "SATISFIED, 99.9, null, null, null, SATISFIED requires percentComplete 100.0",
        "SATISFIED, 0.0, null, null, null, SATISFIED requires percentComplete 100.0",
        "SATISFIED, 100.0, remaining, null, null, SATISFIED must not have a remainingAmount",
        "SATISFIED, 100.0, null, text, null, SATISFIED must not have a blocker",
        "SATISFIED, 100.0, null, empty, null, SATISFIED must not have a blocker",
        "SATISFIED, 100.0, null, null, text, SATISFIED must not have a skipReason",
        "SATISFIED, 100.0, null, null, empty, SATISFIED must not have a skipReason",
        "IN_PROGRESS, 100.0, null, text, null, IN_PROGRESS requires percentComplete at most 99.9",
        "IN_PROGRESS, 50.0, null, null, null, IN_PROGRESS requires a non-blank blocker",
        "IN_PROGRESS, 50.0, null, empty, null, IN_PROGRESS requires a non-blank blocker",
        "IN_PROGRESS, 50.0, null, blank, null, IN_PROGRESS requires a non-blank blocker",
        "IN_PROGRESS, 50.0, null, text, text, IN_PROGRESS must not have a skipReason",
        "IN_PROGRESS, 50.0, null, text, empty, IN_PROGRESS must not have a skipReason",
        "NOT_STARTED, 0.1, null, text, null, NOT_STARTED requires percentComplete 0.0",
        "NOT_STARTED, 100.0, null, text, null, NOT_STARTED requires percentComplete 0.0",
        "NOT_STARTED, 0.0, null, null, null, NOT_STARTED requires a non-blank blocker",
        "NOT_STARTED, 0.0, null, empty, null, NOT_STARTED requires a non-blank blocker",
        "NOT_STARTED, 0.0, null, blank, null, NOT_STARTED requires a non-blank blocker",
        "NOT_STARTED, 0.0, null, text, text, NOT_STARTED must not have a skipReason",
        "NOT_STARTED, 0.0, null, text, empty, NOT_STARTED must not have a skipReason",
        "SKIPPED, 0.1, null, null, text, SKIPPED requires percentComplete 0.0",
        "SKIPPED, 100.0, null, null, text, SKIPPED requires percentComplete 0.0",
        "SKIPPED, 0.0, remaining, null, text, SKIPPED must not have a remainingAmount",
        "SKIPPED, 0.0, null, text, text, SKIPPED must not have a blocker",
        "SKIPPED, 0.0, null, empty, text, SKIPPED must not have a blocker",
        "SKIPPED, 0.0, null, null, null, SKIPPED requires a non-blank skipReason",
        "SKIPPED, 0.0, null, null, empty, SKIPPED requires a non-blank skipReason",
        "SKIPPED, 0.0, null, null, blank, SKIPPED requires a non-blank skipReason"
    })
    void rejectsEveryForbiddenStatusCombination(
            StageStatus status, String percent, String remaining, String blocker, String skipReason, String rule) {
        BigDecimal percentComplete = new BigDecimal(percent);
        Money remainingAmount = remainingOf(remaining);
        String blockerText = textOf(blocker, BLOCKER);
        String skipReasonText = textOf(skipReason, SKIP_REASON);

        assertThatThrownBy(() -> new StageEvaluation(
                        5, status, percentComplete, remainingAmount, blockerText, skipReasonText))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("stage 5 " + rule)
                .hasMessageNotContaining(BLOCKER)
                .hasMessageNotContaining(SKIP_REASON);
    }

    @Test
    void factoriesBuildEachStatusWithItsFixedComponents() {
        assertThat(StageEvaluation.satisfied(1))
                .isEqualTo(new StageEvaluation(1, StageStatus.SATISFIED, FULL, null, null, null));
        assertThat(StageEvaluation.inProgress(2, new BigDecimal("0.0"), null, BLOCKER))
                .isEqualTo(new StageEvaluation(2, StageStatus.IN_PROGRESS, new BigDecimal("0.0"), null, BLOCKER, null));
        assertThat(StageEvaluation.notStarted(3, REMAINING, BLOCKER))
                .isEqualTo(new StageEvaluation(
                        3, StageStatus.NOT_STARTED, new BigDecimal("0.0"), REMAINING, BLOCKER, null));
        assertThat(StageEvaluation.skipped(4, SKIP_REASON))
                .isEqualTo(new StageEvaluation(4, StageStatus.SKIPPED, new BigDecimal("0.0"), null, null, SKIP_REASON));
    }

    @Test
    void factoriesStillEnforceTheRules() {
        BigDecimal full = FULL;

        assertThatThrownBy(() -> StageEvaluation.inProgress(2, full, null, BLOCKER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("stage 2 IN_PROGRESS requires percentComplete at most 99.9");
        assertThatThrownBy(() -> StageEvaluation.skipped(8, SKIP_REASON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("stage must be between 1 and 7");
    }

    private static Money remainingOf(String marker) {
        return marker == null ? null : REMAINING;
    }

    private static String blockerOf(String marker) {
        return textOf(marker, BLOCKER);
    }

    private static String skipReasonOf(String marker) {
        return textOf(marker, SKIP_REASON);
    }

    /** Maps a marker to null, an empty string, a whitespace-only string, or {@code text}. */
    private static String textOf(String marker, String text) {
        if (marker == null) {
            return null;
        }
        return switch (marker) {
            case "empty" -> "";
            case "blank" -> " \t ";
            default -> text;
        };
    }
}
