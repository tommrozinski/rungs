package com.rungs.readiness.domain.stage;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.FinancialSnapshotBuilder;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Rent;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FullEmergencyFundEvaluatorTests {

    private final FullEmergencyFundEvaluator evaluator = new FullEmergencyFundEvaluator();

    @Test
    void decidesStageThree() {
        assertThat(evaluator.stage()).isEqualTo(3);
    }

    /** Columns: essential expenses in cents, emergency-fund months, fund in cents. */
    @ParameterizedTest
    @CsvSource({
        "350000, 3, 1050000",
        "350000, 3, 1050001",
        "350000, 6, 2100000",
        "0, 3, 0",
        "0, 6, 500000"
    })
    void isSatisfiedAtOrAboveTheTargetIncludingAZeroTarget(long expensesCents, int months, long fundCents) {
        StageEvaluation result = evaluate(expensesCents, months, fundCents);

        assertThat(result).isEqualTo(StageEvaluation.satisfied(3));
    }

    @ParameterizedTest
    @CsvSource({
        "350000, 3, 1049999, 99.9, 1, 'Your emergency fund is $0.01 short of a 3-month target of $10,500.00.'",
        "350000, 3, 810000, 77.1, 240000, 'Your emergency fund is $2,400.00 short of a 3-month target of $10,500.00.'",
        "100000, 7, 300000, 50.0, 300000, 'Your emergency fund is $3,000.00 short of a 6-month target of $6,000.00.'"
    })
    void isInProgressWithTheShortfallBelowTheTarget(
            long expensesCents, int months, long fundCents, String percent, long remainingCents, String blocker) {
        StageEvaluation result = evaluate(expensesCents, months, fundCents);

        assertThat(result).isEqualTo(StageEvaluation.inProgress(
                3, new BigDecimal(percent), Money.ofCents(remainingCents), blocker));
    }

    @Test
    void capsTheTargetAtSixMonthsSoSixMonthsOfExpensesSatisfiesASevenMonthSetting() {
        StageEvaluation result = evaluate(100_000, 7, 600_000);

        assertThat(result).isEqualTo(StageEvaluation.satisfied(3));
    }

    /** Status follows the computed progress (story 004 REQ-005), so a fund that rounds to 0.0 is not started. */
    @ParameterizedTest
    @CsvSource({
        "0, 1050000, 'Your emergency fund is $10,500.00 short of a 3-month target of $10,500.00.'",
        "1, 1049999, 'Your emergency fund is $10,499.99 short of a 3-month target of $10,500.00.'"
    })
    void isNotStartedWithTheShortfallRemainingWhenProgressRoundsToZero(
            long fundCents, long remainingCents, String blocker) {
        StageEvaluation result = evaluate(350_000, 3, fundCents);

        assertThat(result).isEqualTo(StageEvaluation.notStarted(3, Money.ofCents(remainingCents), blocker));
    }

    // Housing (design.md 039 section 2): the target is months of monthlyEssentialCosts(), which includes housing.

    @Test
    void theTargetIncludesTheMonthlyHousingCost() {
        FinancialSnapshot snapshot = FinancialSnapshotBuilder.aSnapshot()
                .withMonthlyEssentialExpenses(Money.ofDollars(2_600))
                .withHousing(new Rent(Money.ofDollars(1_400)))
                .withFullEmergencyFund(Money.ofDollars(11_999))
                .build();

        StageEvaluation result = evaluator.evaluate(snapshot, List.of());

        // Target is 3 * (2,600 + 1,400) = 12,000; a $11,999 fund is $1 short of it.
        assertThat(result).isEqualTo(StageEvaluation.inProgress(
                3, new BigDecimal("99.9"), Money.ofCents(100),
                "Your emergency fund is $1.00 short of a 3-month target of $12,000.00."));
    }

    @Test
    void isSatisfiedWhenTheFundMeetsATargetThatIncludesHousing() {
        FinancialSnapshot snapshot = FinancialSnapshotBuilder.aSnapshot()
                .withMonthlyEssentialExpenses(Money.ofDollars(2_600))
                .withHousing(new Rent(Money.ofDollars(1_400)))
                .withFullEmergencyFund(Money.ofDollars(12_000))
                .build();

        StageEvaluation result = evaluator.evaluate(snapshot, List.of());

        assertThat(result).isEqualTo(StageEvaluation.satisfied(3));
    }

    private StageEvaluation evaluate(long expensesCents, int months, long fundCents) {
        FinancialSnapshot snapshot = FinancialSnapshotBuilder.aSnapshot()
                .withMonthlyEssentialExpenses(Money.ofCents(expensesCents))
                .withEmergencyFundMonths(months)
                .withFullEmergencyFund(Money.ofCents(fundCents))
                .build();
        return evaluator.evaluate(snapshot, List.of());
    }
}
