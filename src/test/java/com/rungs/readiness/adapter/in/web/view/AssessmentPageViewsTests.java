package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.application.AssessmentPage;
import com.rungs.readiness.application.ReadinessAssessment;
import com.rungs.readiness.application.SnapshotInput;
import com.rungs.readiness.application.response.MoneyResponse;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.HouseholdFlags;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Severity;
import com.rungs.readiness.domain.model.Warning;
import com.rungs.readiness.domain.model.WarningCode;
import com.rungs.readiness.domain.stage.StageEvaluation;
import com.rungs.readiness.domain.stage.StageStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 029: maps an {@link AssessmentPage} to what {@code assessment/page.html} prints, with every string built here. */
class AssessmentPageViewsTests {

    private static final FinancialSnapshot SNAPSHOT = new FinancialSnapshot(
            Money.ofDollars(4000), Money.ofDollars(3000), Money.ZERO, Money.ofDollars(9000), Money.ZERO,
            BigDecimal.ZERO, 3, List.of(), null, new HouseholdFlags(false, false, false));

    @Test
    void mapsVerdictStatsAndAllSevenLadderRows() {
        ReadinessAssessment assessment = new ReadinessAssessment(3, sevenStages(3), Money.ofDollars(500), List.of());
        AssessmentPage page = AssessmentPage.from(assessment, SNAPSHOT);

        AssessmentPageView view = AssessmentPageViews.of(page, blankSnapshot().forForm());

        assertThat(view.documentTitle()).isEqualTo("Your stage: 3 of 7 · Rungs");
        assertThat(view.ladder()).hasSize(7);
        assertThat(view.ladder()).extracting(StageRowView::stage).containsExactly(1, 2, 3, 4, 5, 6, 7);
        assertThat(view.verdict().text()).isEqualTo("Blocker 3");
        assertThat(view.surplus().label()).isEqualTo("Monthly surplus");
        assertThat(view.surplus().figure()).isEqualTo(MoneyResponse.of(Money.ofDollars(500)).display());
        assertThat(view.surplus().tone()).isEqualTo("positive");
        assertThat(view.totalDebt().label()).isEqualTo("Total debt");
        assertThat(view.totalDebt().figure()).isEqualTo(MoneyResponse.of(Money.ZERO).display());
        assertThat(view.emergencyFund().label()).isEqualTo("Emergency fund");
        assertThat(view.emergencyFund().figure()).isEqualTo(MoneyResponse.of(Money.ofDollars(9000)).display());
    }

    @Test
    void negativeSurplusUsesTheNeutralTone() {
        ReadinessAssessment assessment = new ReadinessAssessment(1, sevenStages(1), Money.ofDollars(-50), List.of());
        AssessmentPage page = AssessmentPage.from(assessment, SNAPSHOT);

        assertThat(AssessmentPageViews.of(page, blankSnapshot().forForm()).surplus().tone()).isEqualTo("neutral");
    }

    @Test
    void warningsBecomeCautionBannersWithTheirOwnMessage() {
        ReadinessAssessment assessment = new ReadinessAssessment(1, sevenStages(1), Money.ofDollars(500),
                List.of(new Warning(WarningCode.EXPENSES_EXCEED_INCOME, Severity.CAUTION,
                        "Your expenses are higher than your income.", List.of())));
        AssessmentPage page = AssessmentPage.from(assessment, SNAPSHOT);

        List<WarningBannerView> warnings = AssessmentPageViews.of(page, blankSnapshot().forForm()).warnings();

        assertThat(warnings).hasSize(1);
        assertThat(warnings.get(0).message()).isEqualTo("Your expenses are higher than your income.");
        assertThat(warnings.get(0).heading()).isNotBlank();
    }

    @Test
    void noWarningsMeansAnEmptyList() {
        ReadinessAssessment assessment = new ReadinessAssessment(1, sevenStages(1), Money.ofDollars(500), List.of());
        AssessmentPage page = AssessmentPage.from(assessment, SNAPSHOT);

        assertThat(AssessmentPageViews.of(page, blankSnapshot().forForm()).warnings()).isEmpty();
    }

    @Test
    void hiddenFieldsCarryTheNormalizedSnapshot() {
        ReadinessAssessment assessment = new ReadinessAssessment(1, sevenStages(1), Money.ofDollars(500), List.of());
        AssessmentPage page = AssessmentPage.from(assessment, SNAPSHOT);

        List<HiddenField> fields = AssessmentPageViews.of(page, blankSnapshot().forForm()).hiddenFields();

        assertThat(fields).contains(new HiddenField("monthlyTakeHomeIncome", "0"));
    }

    private static SnapshotInput blankSnapshot() {
        return new SnapshotInput(null, null, null, null, null, null, null, null, null, null);
    }

    private static List<StageEvaluation> sevenStages(int currentStage) {
        List<StageEvaluation> stages = new java.util.ArrayList<>();
        for (int stage = 1; stage <= 7; stage++) {
            if (stage < currentStage) {
                stages.add(new StageEvaluation(stage, StageStatus.SATISFIED, new BigDecimal("100.0"), null, null,
                        null));
            } else if (stage == currentStage) {
                stages.add(new StageEvaluation(stage, StageStatus.IN_PROGRESS, new BigDecimal("40.0"), null,
                        "Blocker " + stage, null));
            } else {
                stages.add(new StageEvaluation(stage, StageStatus.NOT_STARTED, new BigDecimal("0.0"), null,
                        "Blocker", null));
            }
        }
        return stages;
    }
}
