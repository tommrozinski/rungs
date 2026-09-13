package com.rungs.readiness.domain.stage;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.FinancialSnapshotBuilder;
import com.rungs.readiness.domain.model.HouseholdFlags;
import com.rungs.readiness.domain.model.Money;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CollegeEvaluatorTests {

    private static final String NO_PLANS_REASON = "You don't plan to fund college, so there's nothing to save for.";

    private static final String RETIRED_REASON = "You're retired, so there's no college fund to build.";

    private final CollegeEvaluator evaluator = new CollegeEvaluator();

    @Test
    void decidesStageFive() {
        assertThat(evaluator.stage()).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void isSkippedWithTheNoPlansReasonWithoutCollegePlansEvenWhenRetired(boolean retired) {
        StageEvaluation result = evaluate(new HouseholdFlags(false, retired, false), Money.ofDollars(200));

        assertThat(result).isEqualTo(StageEvaluation.skipped(5, NO_PLANS_REASON));
    }

    @Test
    void isSkippedWithTheRetiredReasonWhenRetiredWithCollegePlans() {
        StageEvaluation result = evaluate(new HouseholdFlags(false, true, true), Money.ofDollars(200));

        assertThat(result).isEqualTo(StageEvaluation.skipped(5, RETIRED_REASON));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 20_000})
    void isSatisfiedByAnyContributionAboveZeroWithCollegePlans(long contributionCents) {
        StageEvaluation result = evaluate(new HouseholdFlags(false, false, true), Money.ofCents(contributionCents));

        assertThat(result).isEqualTo(StageEvaluation.satisfied(5));
    }

    @Test
    void isNotStartedWithNoRemainingAmountWhenPlanningButNotContributing() {
        StageEvaluation result = evaluate(new HouseholdFlags(false, false, true), Money.ZERO);

        assertThat(result).isEqualTo(StageEvaluation.notStarted(
                5, null, "You plan to fund college but aren't contributing to it yet."));
    }

    private StageEvaluation evaluate(HouseholdFlags flags, Money contribution) {
        FinancialSnapshot snapshot = FinancialSnapshotBuilder.aSnapshot()
                .withHouseholdFlags(flags)
                .withMonthlyCollegeContribution(contribution)
                .build();
        return evaluator.evaluate(snapshot, List.of());
    }
}
