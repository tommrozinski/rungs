package com.rungs.readiness.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rungs.readiness.application.response.AssessmentResponse;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.stage.CollegeEvaluator;
import com.rungs.readiness.domain.stage.DebtFreeEvaluator;
import com.rungs.readiness.domain.stage.FullEmergencyFundEvaluator;
import com.rungs.readiness.domain.stage.MortgageEvaluator;
import com.rungs.readiness.domain.stage.LadderResult;
import com.rungs.readiness.domain.stage.RetirementEvaluator;
import com.rungs.readiness.domain.stage.StageEvaluation;
import com.rungs.readiness.domain.stage.StageLadder;
import com.rungs.readiness.domain.stage.StarterFundEvaluator;
import com.rungs.readiness.domain.stage.WealthBuildingEvaluator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AssessmentServiceTests {

    private final StageLadder ladder = new StageLadder(List.of(
            new StarterFundEvaluator(),
            new DebtFreeEvaluator(),
            new FullEmergencyFundEvaluator(),
            new RetirementEvaluator(),
            new CollegeEvaluator(),
            new MortgageEvaluator(),
            new WealthBuildingEvaluator()));

    private final AssessmentService service = new AssessmentService(ladder);

    @Test
    void assessesTheNormalizedSnapshotWithTheLadder() {
        SnapshotInput input = new SnapshotInput("4000", "3000", "500", null, null, null, null,
                List.of(new DebtInput("", "CREDIT_CARD", "12000", "22.9", "300"),
                        new DebtInput("Paid off", "AUTO", "0", "5", "999")),
                null, null);

        ReadinessAssessment assessment = service.assess(input);

        LadderResult expected = ladder.assess(input.toSnapshot());
        assertThat(assessment.currentStage()).isEqualTo(1).isEqualTo(expected.currentStage());
        assertThat(assessment.stages()).isEqualTo(expected.stages());
        assertThat(assessment.monthlySurplus()).isEqualTo(Money.ofDollars(700));
        assertThat(assessment.warnings()).isEmpty();
    }

    @Test
    void assessPageCarriesTheSameAssessmentPlusTotalDebtAndEmergencyFund() {
        SnapshotInput input = new SnapshotInput("4000", "3000", "500", "9000", null, null, null,
                List.of(new DebtInput("Card", "CREDIT_CARD", "1200", "10", "50")), null, null);

        AssessmentPage page = service.assessPage(input);

        assertThat(page.assessment()).isEqualTo(AssessmentResponse.from(service.assess(input)));
        assertThat(page.totalDebt().amount()).isEqualTo("1200.00");
        assertThat(page.emergencyFund().amount()).isEqualTo("9000.00");
    }

    @Test
    void assessPageRejectsNullInput() {
        assertThatThrownBy(() -> service.assessPage(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void monthlySurplusMayBeNegative() {
        SnapshotInput input = new SnapshotInput("1000", "1050", null, null, null, null, null, null, null, null);

        assertThat(service.assess(input).monthlySurplus()).isEqualTo(Money.ofDollars(-50));
    }

    @Test
    void requiresALadderAndAnInput() {
        assertThatThrownBy(() -> new AssessmentService(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> service.assess(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void assessmentCopiesItsListsAndRejectsNulls() {
        List<StageEvaluation> stages = new ArrayList<>(ladder.assess(
                new SnapshotInput(null, null, null, null, null, null, null, null, null, null).toSnapshot()).stages());
        List<Warning> warnings = new ArrayList<>();
        ReadinessAssessment assessment = new ReadinessAssessment(1, stages, Money.ZERO, warnings);
        stages.clear();
        warnings.add(new Warning("CODE", "Message."));

        assertThat(assessment.stages()).hasSize(7);
        assertThat(assessment.warnings()).isEmpty();
        assertThatThrownBy(() -> new ReadinessAssessment(1, null, Money.ZERO, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ReadinessAssessment(1, List.of(), null, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ReadinessAssessment(1, List.of(), Money.ZERO, null))
                .isInstanceOf(NullPointerException.class);
    }
}
