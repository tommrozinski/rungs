package com.rungs.readiness.domain.stage;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.FinancialSnapshotBuilder;
import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class DebtFreeEvaluatorTests {

    private static final BigDecimal ZERO_PERCENT = new BigDecimal("0.0");

    private final DebtFreeEvaluator evaluator = new DebtFreeEvaluator();

    @Test
    void decidesStageTwo() {
        assertThat(evaluator.stage()).isEqualTo(2);
    }

    @Test
    void isSatisfiedWithNoDebts() {
        StageEvaluation result = evaluator.evaluate(FinancialSnapshotBuilder.aSnapshot().build(), List.of());

        assertThat(result).isEqualTo(StageEvaluation.satisfied(2));
    }

    @Test
    void isInProgressAtZeroPercentWithOneDebtNamedInTheSingular() {
        FinancialSnapshot snapshot = FinancialSnapshotBuilder.aSnapshot()
                .withDebt("Visa", DebtType.CREDIT_CARD, Money.ofDollars(12_000), "22.9", Money.ofDollars(300))
                .build();

        StageEvaluation result = evaluator.evaluate(snapshot, List.of());

        assertThat(result).isEqualTo(StageEvaluation.inProgress(
                2, ZERO_PERCENT, Money.ofDollars(12_000), "You have $12,000.00 of debt across 1 debt."));
    }

    @Test
    void isInProgressAtZeroPercentWithTheTotalOfSeveralDebts() {
        FinancialSnapshot snapshot = FinancialSnapshotBuilder.aSnapshot()
                .withDebt("Visa", DebtType.CREDIT_CARD, Money.ofCents(512_050), "22.9", Money.ofDollars(150))
                .withDebt("Car", DebtType.AUTO, Money.ofCents(620_000), "6.5", Money.ofDollars(250))
                .withDebt("Store card", DebtType.CREDIT_CARD, Money.ofCents(115_950), "24.99", Money.ofDollars(40))
                .build();

        StageEvaluation result = evaluator.evaluate(snapshot, List.of());

        assertThat(result).isEqualTo(StageEvaluation.inProgress(
                2, ZERO_PERCENT, Money.ofDollars(12_480), "You have $12,480.00 of debt across 3 debts."));
    }
}
