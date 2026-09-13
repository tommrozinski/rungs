package com.rungs.readiness.application;

import com.rungs.readiness.application.response.AssessmentResponse;
import com.rungs.readiness.application.response.MoneyResponse;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import java.util.Objects;

/**
 * Everything the assessment page (029) needs beyond {@link AssessmentResponse}: figures the template must not
 * compute itself (AC-G, 029 REQ-002), plus the document title (REQ-007).
 *
 * @param assessment the same wire response {@code POST /v1/assessments} serializes
 * @param totalDebt the sum of every debt balance
 * @param emergencyFund the full emergency fund
 * @param surplusPositive whether the monthly surplus is above zero, so the surplus stat card uses its positive
 *     variant
 * @param documentTitle the document title, naming the current stage, e.g. {@code "Your stage: 2 of 7 · Rungs"}
 */
public record AssessmentPage(
        AssessmentResponse assessment,
        MoneyResponse totalDebt,
        MoneyResponse emergencyFund,
        boolean surplusPositive,
        String documentTitle) {

    /** The number of stages in the ladder, for the document title. */
    private static final int STAGE_COUNT = 7;

    /** Creates a page; throws NullPointerException for a null component other than the primitive. */
    public AssessmentPage {
        Objects.requireNonNull(assessment, "assessment");
        Objects.requireNonNull(totalDebt, "totalDebt");
        Objects.requireNonNull(emergencyFund, "emergencyFund");
        Objects.requireNonNull(documentTitle, "documentTitle");
    }

    /**
     * Returns the page built from {@code assessment} and the {@code snapshot} it was computed from. Throws
     * NullPointerException for a null argument.
     */
    public static AssessmentPage from(ReadinessAssessment assessment, FinancialSnapshot snapshot) {
        Objects.requireNonNull(assessment, "assessment");
        Objects.requireNonNull(snapshot, "snapshot");
        return new AssessmentPage(
                AssessmentResponse.from(assessment),
                MoneyResponse.of(snapshot.totalDebtBalance()),
                MoneyResponse.of(snapshot.fullEmergencyFund()),
                assessment.monthlySurplus().isPositive(),
                documentTitle(assessment.currentStage()));
    }

    private static String documentTitle(int currentStage) {
        return "Your stage: " + currentStage + " of " + STAGE_COUNT + " · Rungs";
    }
}
