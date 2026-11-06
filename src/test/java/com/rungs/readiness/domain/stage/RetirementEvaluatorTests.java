package com.rungs.readiness.domain.stage;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.FinancialSnapshotBuilder;
import com.rungs.readiness.domain.model.HouseholdFlags;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RetirementEvaluatorTests {

    private final RetirementEvaluator evaluator = new RetirementEvaluator();

    @Test
    void decidesStageFour() {
        assertThat(evaluator.stage()).isEqualTo(4);
    }

    @Test
    void isSkippedWhenRetiredWhateverTheContribution() {
        FinancialSnapshot snapshot = FinancialSnapshotBuilder.aSnapshot()
                .withRetirementContributionPercent("20")
                .withHouseholdFlags(new HouseholdFlags(true, false))
                .build();

        StageEvaluation result = evaluator.evaluate(snapshot, List.of());

        assertThat(result).isEqualTo(StageEvaluation.skipped(
                4, "You're retired, so you're no longer saving for retirement from income."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"15", "15.00", "15.01", "100"})
    void isSatisfiedAtFifteenPercentAndAbove(String percent) {
        StageEvaluation result = evaluate(percent);

        assertThat(result).isEqualTo(StageEvaluation.satisfied(4));
    }

    @ParameterizedTest
    @CsvSource({
        "14.99, 99.9, 'You''re contributing 14.99% of income to retirement, short of 15%.'",
        "10, 66.6, 'You''re contributing 10% of income to retirement, short of 15%.'",
        "10.00, 66.6, 'You''re contributing 10% of income to retirement, short of 15%.'",
        "6.5, 43.3, 'You''re contributing 6.5% of income to retirement, short of 15%.'",
        "6, 40.0, 'You''re contributing 6% of income to retirement, short of 15%.'",
        "0.02, 0.1, 'You''re contributing 0.02% of income to retirement, short of 15%.'"
    })
    void isInProgressWithNoRemainingAmountBelowFifteenPercent(String percent, String progress, String blocker) {
        StageEvaluation result = evaluate(percent);

        assertThat(result).isEqualTo(StageEvaluation.inProgress(4, new BigDecimal(progress), null, blocker));
    }

    /** Status follows the computed progress; the blocker still prints the real percent. */
    @ParameterizedTest
    @CsvSource({"0, 0", "0.00, 0", "0.01, 0.01"})
    void isNotStartedWithNoRemainingAmountWhenProgressRoundsToZero(String percent, String displayed) {
        StageEvaluation result = evaluate(percent);

        assertThat(result).isEqualTo(StageEvaluation.notStarted(
                4, null, "You're contributing " + displayed + "% of income to retirement, short of 15%."));
    }

    private StageEvaluation evaluate(String percent) {
        FinancialSnapshot snapshot =
                FinancialSnapshotBuilder.aSnapshot().withRetirementContributionPercent(percent).build();
        return evaluator.evaluate(snapshot, List.of());
    }
}
