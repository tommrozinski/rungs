package com.rungs.readiness.domain.stage;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.FinancialSnapshotBuilder;
import com.rungs.readiness.domain.model.HouseholdFlags;
import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** PRD acceptance criteria decided by the real seven-evaluator ladder, with no production wiring. */
class LadderAcceptanceTests {

    private final StageLadder ladder = new StageLadder(List.of(
            new StarterFundEvaluator(),
            new DebtFreeEvaluator(),
            new FullEmergencyFundEvaluator(),
            new RetirementEvaluator(),
            new CollegeEvaluator(),
            new MortgageEvaluator(),
            new WealthBuildingEvaluator()));

    @Test
    void ac11AHouseholdWithFiveHundredSavedAndCreditCardDebtIsAtStageOne() {
        FinancialSnapshot snapshot = FinancialSnapshotBuilder.aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(500))
                .withDebt("Credit card", DebtType.CREDIT_CARD, Money.ofDollars(12_000), "22.9", Money.ofDollars(300))
                .build();

        LadderResult result = ladder.assess(snapshot);

        assertThat(result.currentStage()).isEqualTo(1);
        assertThat(result.evaluation(1)).isEqualTo(StageEvaluation.inProgress(
                1,
                new BigDecimal("50.0"),
                Money.ofDollars(500),
                "Your starter emergency fund is $500.00 short of $1,000.00."));
        assertThat(result.evaluation(2)).isEqualTo(StageEvaluation.inProgress(
                2, new BigDecimal("0.0"), Money.ofDollars(12_000), "You have $12,000.00 of debt across 1 debt."));
    }

    @Test
    void ac12ARenterWithNoCollegePlansWhoMeetsStagesOneToFourIsAtStageSeven() {
        FinancialSnapshot snapshot = FinancialSnapshotBuilder.aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withMonthlyEssentialExpenses(Money.ofDollars(3_000))
                .withEmergencyFundMonths(3)
                .withFullEmergencyFund(Money.ofDollars(9_000))
                .withRetirementContributionPercent("15")
                .withHouseholdFlags(new HouseholdFlags(true, false, false))
                .build();

        LadderResult result = ladder.assess(snapshot);

        assertThat(result.currentStage()).isEqualTo(7);
        assertThat(result.stages().subList(0, 4))
                .extracting(StageEvaluation::status)
                .containsOnly(StageStatus.SATISFIED);
        assertThat(result.evaluation(5)).isEqualTo(StageEvaluation.skipped(
                5, "You don't plan to fund college, so there's nothing to save for."));
        assertThat(result.evaluation(6)).isEqualTo(StageEvaluation.skipped(
                6, "You rent, so there's no mortgage to pay down."));
        assertThat(result.evaluation(7)).isEqualTo(StageEvaluation.satisfied(7));
    }

    @Test
    void req008AHouseholdAtStageTwoAlreadyContributingFifteenPercentReportsStageFourSatisfied() {
        FinancialSnapshot snapshot = FinancialSnapshotBuilder.aSnapshot()
                .withStarterEmergencyFund(Money.ofDollars(1_000))
                .withDebt("Car", DebtType.AUTO, Money.ofDollars(8_000), "6.5", Money.ofDollars(250))
                .withMonthlyEssentialExpenses(Money.ofDollars(3_000))
                .withRetirementContributionPercent("15")
                .build();

        LadderResult result = ladder.assess(snapshot);

        assertThat(result.currentStage()).isEqualTo(2);
        assertThat(result.evaluation(4)).isEqualTo(StageEvaluation.satisfied(4));
    }
}
