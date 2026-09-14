package com.rungs.readiness.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Mortgage;
import com.rungs.readiness.domain.model.MortgagePayoff;
import com.rungs.readiness.domain.model.MortgagePayoff.MaturityComparison;
import com.rungs.readiness.domain.model.MortgagePayoffCalculator;
import com.rungs.readiness.domain.stage.CollegeEvaluator;
import com.rungs.readiness.domain.stage.DebtFreeEvaluator;
import com.rungs.readiness.domain.stage.FullEmergencyFundEvaluator;
import com.rungs.readiness.domain.stage.LadderResult;
import com.rungs.readiness.domain.stage.MortgageEvaluator;
import com.rungs.readiness.domain.stage.RetirementEvaluator;
import com.rungs.readiness.domain.stage.StageLadder;
import com.rungs.readiness.domain.stage.StarterFundEvaluator;
import com.rungs.readiness.domain.stage.WealthBuildingEvaluator;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The debt-free and homestretch sample households' maturity, which {@link SampleHouseholds} must keep relative to
 * whatever {@code asOf} it is built for so the sample never drifts against the running {@link java.time.Clock}
 * (design.md 039 review): debt-free's maturity always lands exactly on its computed payoff month, and
 * homestretch's always lands 13 months after it.
 */
class SampleHouseholdsTests {

    /** Two assessment months far apart, so a hard-coded maturity would fail at least one of them. */
    private static Stream<YearMonth> assessmentMonths() {
        return Stream.of(YearMonth.of(2026, 9), YearMonth.of(2031, 2));
    }

    @ParameterizedTest
    @MethodSource("assessmentMonths")
    void debtFreeMaturityAlwaysMatchesItsComputedPayoffMonth(YearMonth asOf) {
        SnapshotForm form = SampleHouseholds.byId("debt-free", asOf);

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, asOf);
        Mortgage mortgage = (Mortgage) snapshot.housing();
        MortgagePayoff payoff = MortgagePayoffCalculator.payoff(mortgage, asOf);

        assertThat(payoff.maturityComparison()).isEqualTo(MaturityComparison.MATCHES);

        LadderResult result = ladder().assess(snapshot);
        assertThat(result.currentStage()).isEqualTo(3);
        assertThat(result.evaluation(6).blocker()).contains("That matches your loan's");
    }

    @ParameterizedTest
    @MethodSource("assessmentMonths")
    void homestretchMaturityAlwaysStaysThirteenMonthsAheadOfItsComputedPayoffMonth(YearMonth asOf) {
        SnapshotForm form = SampleHouseholds.byId("homestretch", asOf);

        FinancialSnapshot snapshot = SnapshotFormMapper.toSnapshot(form, asOf);
        Mortgage mortgage = (Mortgage) snapshot.housing();
        MortgagePayoff payoff = MortgagePayoffCalculator.payoff(mortgage, asOf);

        assertThat(payoff.maturityComparison()).isEqualTo(MaturityComparison.AHEAD);
        assertThat(payoff.monthsDifference()).isEqualTo(13);

        LadderResult result = ladder().assess(snapshot);
        assertThat(result.currentStage()).isEqualTo(6);
        assertThat(result.evaluation(6).blocker()).contains("1 year 1 month ahead of your");
    }

    /** Builds the same seven-stage ladder {@link SnapshotController} does. */
    private static StageLadder ladder() {
        return new StageLadder(List.of(
                new StarterFundEvaluator(),
                new DebtFreeEvaluator(),
                new FullEmergencyFundEvaluator(),
                new RetirementEvaluator(),
                new CollegeEvaluator(),
                new MortgageEvaluator(),
                new WealthBuildingEvaluator()));
    }
}
