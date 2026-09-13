package com.rungs.readiness.application;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.stage.LadderResult;
import com.rungs.readiness.domain.stage.StageLadder;
import com.rungs.readiness.domain.stage.WarningDetector;
import java.util.Objects;

/** Assesses a submitted snapshot: normalizes it, runs the stage ladder, and computes the monthly surplus (FR-1). */
public final class AssessmentService {

    private final StageLadder ladder;

    /** Creates a service over {@code ladder}; throws NullPointerException if it is null. */
    public AssessmentService(StageLadder ladder) {
        this.ladder = Objects.requireNonNull(ladder, "ladder");
    }

    /**
     * Returns the assessment of {@code input} after FR-4 normalization, with warnings from {@link WarningDetector}
     * (story 012). Throws NullPointerException for null input, and IllegalArgumentException for input
     * {@link SnapshotInput#toSnapshot} cannot normalize.
     */
    public ReadinessAssessment assess(SnapshotInput input) {
        return assessWithSnapshot(input).assessment();
    }

    /**
     * Returns the assessment page (029) built from {@code input}: the same {@link ReadinessAssessment} as
     * {@link #assess(SnapshotInput)}, plus the figures {@link AssessmentPage} adds. Throws NullPointerException for
     * null input.
     */
    public AssessmentPage assessPage(SnapshotInput input) {
        AssessedSnapshot assessed = assessWithSnapshot(input);
        return AssessmentPage.from(assessed.assessment(), assessed.snapshot());
    }

    private AssessedSnapshot assessWithSnapshot(SnapshotInput input) {
        FinancialSnapshot snapshot = Objects.requireNonNull(input, "input").toSnapshot();
        LadderResult result = ladder.assess(snapshot);
        ReadinessAssessment assessment = new ReadinessAssessment(
                result.currentStage(), result.stages(), snapshot.monthlySurplus(), WarningDetector.detect(snapshot));
        return new AssessedSnapshot(assessment, snapshot);
    }

    private record AssessedSnapshot(ReadinessAssessment assessment, FinancialSnapshot snapshot) {
    }
}
