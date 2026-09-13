package com.rungs.readiness.adapter.in.web.view;

import com.rungs.readiness.application.AssessmentPage;
import com.rungs.readiness.application.SnapshotInput;
import com.rungs.readiness.application.response.AssessmentResponse;
import com.rungs.readiness.application.response.StageResponse;
import com.rungs.readiness.application.response.WarningResponse;
import java.util.List;

/** Builds an {@link AssessmentPageView} from an {@link AssessmentPage} (029), so {@code assessment/page.html} only prints. */
final class AssessmentPageViews {

    private static final String SURPLUS_LABEL = "Monthly surplus";

    private static final String TOTAL_DEBT_LABEL = "Total debt";

    private static final String EMERGENCY_FUND_LABEL = "Emergency fund";

    private static final String POSITIVE_TONE = "positive";

    private static final String NEUTRAL_TONE = "neutral";

    /** The fixed heading every caution banner shows above its own message (story 012 warnings have none of their own). */
    private static final String CAUTION_HEADING = "Check these numbers";

    private AssessmentPageViews() {
    }

    /**
     * Returns the view for {@code page}. {@code normalizedForHiddenFields} is the {@code forForm()}-normalized
     * snapshot submitted, flattened into the hidden fields that feed the ladder's current-row call to action.
     */
    static AssessmentPageView of(AssessmentPage page, SnapshotInput normalizedForHiddenFields) {
        AssessmentResponse assessment = page.assessment();
        return new AssessmentPageView(
                page.documentTitle(),
                verdict(assessment),
                new StatCardView(SURPLUS_LABEL, assessment.monthlySurplus().display(),
                        page.surplusPositive() ? POSITIVE_TONE : NEUTRAL_TONE),
                new StatCardView(TOTAL_DEBT_LABEL, page.totalDebt().display(), NEUTRAL_TONE),
                new StatCardView(EMERGENCY_FUND_LABEL, page.emergencyFund().display(), NEUTRAL_TONE),
                warnings(assessment.warnings()),
                ladder(assessment),
                HiddenSnapshotFields.of(normalizedForHiddenFields));
    }

    private static VerdictView verdict(AssessmentResponse assessment) {
        StageResponse current = assessment.stages().get(assessment.currentStage() - 1);
        return StageRowViews.verdict(assessment.currentStage(), current.status(), current.blocker());
    }

    private static List<StageRowView> ladder(AssessmentResponse assessment) {
        return assessment.stages().stream()
                .map(stage -> StageRowViews.row(stage.stage(), stage.status(), stage.percentComplete(),
                        stage.blocker(), stage.skipReason(), assessment.currentStage()))
                .toList();
    }

    private static List<WarningBannerView> warnings(List<WarningResponse> warnings) {
        return warnings.stream().map(warning -> new WarningBannerView(CAUTION_HEADING, warning.message())).toList();
    }
}
