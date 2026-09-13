package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** 026: the fixed copy and per-row choices are made in Java, so templates only print. */
class StageRowViewsTests {

    private static final String BLOCKER = "$12,480 stands between you and being debt free.";

    private static final String SKIP = "You rent, so there's no mortgage to pay down.";

    @ParameterizedTest
    @CsvSource({
        "1, Starter emergency fund", "2, Pay off debt", "3, Full emergency fund", "4, Save for retirement",
        "5, Save for college", "6, Pay off your home", "7, Build wealth"
    })
    void titlesAreFixedCopy(int stage, String title) {
        assertThat(StageRowViews.row(stage, "NOT_STARTED", "0.0", BLOCKER, null, 1).title()).isEqualTo(title);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
        "SATISFIED   | 100.0 | Done          | ✓ | This stage is complete.",
        "IN_PROGRESS | 40.0  | In progress   | 3 | " + BLOCKER,
        "NOT_STARTED | 0.0   | Not started   | 3 | " + BLOCKER,
        "SKIPPED     | 0.0   | Doesn't apply | – | " + SKIP
    })
    void chipDiscAndReasonFollowTheStatus(String status, String percent, String chip, String disc, String reason) {
        String blocker = status.equals("IN_PROGRESS") || status.equals("NOT_STARTED") ? BLOCKER : null;
        String skipReason = status.equals("SKIPPED") ? SKIP : null;

        StageRowView row = StageRowViews.row(3, status, percent, blocker, skipReason, 1);

        assertThat(row.stage()).isEqualTo(3);
        assertThat(row.status()).isEqualTo(status);
        assertThat(row.chipLabel()).isEqualTo(chip);
        assertThat(row.discGlyph()).isEqualTo(disc);
        assertThat(row.stageLabel()).isEqualTo("Stage 3");
        assertThat(row.reason()).isEqualTo(reason);
        assertThat(row.percentComplete()).isEqualTo(percent);
    }

    @Test
    void onlyTheCurrentRowCarriesACallToAction() {
        StageRowView other = StageRowViews.row(1, "SATISFIED", "100.0", null, null, 2);

        assertThat(other.current()).isFalse();
        assertThat(other.ctaLabel()).isEmpty();
        assertThat(other.ctaAction()).isEmpty();
    }

    @Test
    void currentStageTwoOffersThePayoffPlan() {
        StageRowView row = StageRowViews.row(2, "IN_PROGRESS", "12.5", BLOCKER, null, 2);

        assertThat(row.current()).isTrue();
        assertThat(row.ctaLabel()).isEqualTo("See the payoff plan");
        assertThat(row.ctaAction()).isEqualTo(StageRowViews.PAYOFF_PLAN_ACTION);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 4, 5, 6, 7})
    void everyOtherCurrentStageOffersToChangeTheNumbers(int stage) {
        StageRowView row = StageRowViews.row(stage, "NOT_STARTED", "0.0", BLOCKER, null, stage);

        assertThat(row.ctaLabel()).isEqualTo("Change my numbers");
        assertThat(row.ctaAction()).isEqualTo(StageRowViews.CHANGE_NUMBERS_ACTION);
    }

    @ParameterizedTest
    @CsvSource({"0.0, 0.0%, false", "40.0, 40.0%, false", "99.9, 99.9%, false", "100.0, 100.0%, true",
        "100, 100%, true", "100.00, 100.00%, true"})
    void progressWidthIsThePercentStringAndCompleteOnlyAtOneHundred(String percent, String width, boolean complete) {
        String status = complete ? "SATISFIED" : "IN_PROGRESS";

        StageRowView row = StageRowViews.row(7, status, percent, complete ? null : BLOCKER, null, 7);

        assertThat(row.progressWidth()).isEqualTo(width);
        assertThat(row.progressComplete()).isEqualTo(complete);
    }

    @Test
    void verdictIsTheCurrentBlocker() {
        assertThat(StageRowViews.verdict(2, "IN_PROGRESS", BLOCKER)).isEqualTo(new VerdictView(BLOCKER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SATISFIED", "SKIPPED"})
    void verdictForFinishedLadderIsFixedCopy(String status) {
        assertThat(StageRowViews.verdict(7, status, null).text())
                .isEqualTo("Every earlier stage is complete or doesn't apply to you.");
    }

    @Test
    void verdictForStageSevenInProgressIsStillTheBlocker() {
        assertThat(StageRowViews.verdict(7, "IN_PROGRESS", BLOCKER).text()).isEqualTo(BLOCKER);
    }

    @Test
    void rejectsValuesOutsideTheLadder() {
        assertThatIllegalArgumentException().isThrownBy(() -> StageRowViews.row(0, "SATISFIED", "100.0", null, null, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> StageRowViews.row(8, "SATISFIED", "100.0", null, null, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> StageRowViews.row(1, "SATISFIED", "100.0", null, null, 8));
        assertThatIllegalArgumentException().isThrownBy(() -> StageRowViews.row(1, "DONE", "100.0", null, null, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> StageRowViews.row(1, "SATISFIED", "lots", null, null, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> StageRowViews.row(1, "IN_PROGRESS", "101.0", "x", null, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> StageRowViews.row(1, "IN_PROGRESS", "5.0", " ", null, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> StageRowViews.row(1, "SKIPPED", "0.0", null, null, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> StageRowViews.verdict(2, "IN_PROGRESS", null));
        assertThatIllegalArgumentException().isThrownBy(() -> StageRowViews.verdict(9, "SATISFIED", null));
        assertThatIllegalArgumentException().isThrownBy(() -> StageRowViews.verdict(7, "satisfied", BLOCKER));
        assertThatIllegalArgumentException().isThrownBy(() -> StageRowViews.verdict(2, "DONE", BLOCKER));
    }
}
