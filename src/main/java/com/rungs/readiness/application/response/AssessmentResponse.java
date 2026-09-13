package com.rungs.readiness.application.response;

import com.rungs.readiness.application.ReadinessAssessment;
import java.util.List;

/**
 * The wire form of a {@link ReadinessAssessment}, returned by {@code POST /v1/assessments} and shared with the view
 * pages so both show the same server-formatted values. Component order is the JSON property order, so identical
 * assessments serialize to identical bytes.
 *
 * @param currentStage the current stage, 1 to 7
 * @param stages all seven stage rows in order; an unmodifiable copy
 * @param monthlySurplus the monthly surplus; may be negative
 * @param warnings the warnings; an unmodifiable copy
 */
public record AssessmentResponse(
        int currentStage, List<StageResponse> stages, MoneyResponse monthlySurplus, List<WarningResponse> warnings) {

    /** Copies the lists; throws NullPointerException for a null list or element. */
    public AssessmentResponse {
        stages = List.copyOf(stages);
        warnings = List.copyOf(warnings);
    }

    /** Returns the wire form of {@code assessment}. */
    public static AssessmentResponse from(ReadinessAssessment assessment) {
        return new AssessmentResponse(
                assessment.currentStage(),
                assessment.stages().stream().map(StageResponse::from).toList(),
                MoneyResponse.of(assessment.monthlySurplus()),
                assessment.warnings().stream().map(WarningResponse::from).toList());
    }
}
