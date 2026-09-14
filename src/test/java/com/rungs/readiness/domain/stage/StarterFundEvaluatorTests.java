package com.rungs.readiness.domain.stage;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.FinancialSnapshotBuilder;
import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class StarterFundEvaluatorTests {

    private final StarterFundEvaluator evaluator = new StarterFundEvaluator();

    @Test
    void decidesStageOne() {
        assertThat(evaluator.stage()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(longs = {100_000, 100_001, 5_000_000})
    void isSatisfiedAtOneThousandDollarsAndAbove(long cents) {
        StageEvaluation result = evaluate(Money.ofCents(cents));

        assertThat(result).isEqualTo(StageEvaluation.satisfied(1));
    }

    @ParameterizedTest
    @CsvSource({
        "99999, 99.9, 1, 'Your starter emergency fund is $0.01 short of $1,000.00.'",
        "50000, 50.0, 50000, 'Your starter emergency fund is $500.00 short of $1,000.00.'",
        "100, 0.1, 99900, 'Your starter emergency fund is $999.00 short of $1,000.00.'"
    })
    void isInProgressWithTheShortfallBelowOneThousandDollars(
            long fundCents, String percent, long remainingCents, String blocker) {
        StageEvaluation result = evaluate(Money.ofCents(fundCents));

        assertThat(result).isEqualTo(StageEvaluation.inProgress(
                1, new BigDecimal(percent), Money.ofCents(remainingCents), blocker));
    }

    /** Status follows the computed progress, so a fund that rounds to 0.0 is not started. */
    @ParameterizedTest
    @CsvSource({
        "0, 100000, 'Your starter emergency fund is $1,000.00 short of $1,000.00.'",
        "1, 99999, 'Your starter emergency fund is $999.99 short of $1,000.00.'",
        "99, 99901, 'Your starter emergency fund is $999.01 short of $1,000.00.'"
    })
    void isNotStartedWithTheShortfallRemainingWhenProgressRoundsToZero(
            long fundCents, long remainingCents, String blocker) {
        StageEvaluation result = evaluate(Money.ofCents(fundCents));

        assertThat(result).isEqualTo(StageEvaluation.notStarted(1, Money.ofCents(remainingCents), blocker));
    }

    private StageEvaluation evaluate(Money fund) {
        FinancialSnapshot snapshot = FinancialSnapshotBuilder.aSnapshot().withStarterEmergencyFund(fund).build();
        return evaluator.evaluate(snapshot, List.of());
    }
}
