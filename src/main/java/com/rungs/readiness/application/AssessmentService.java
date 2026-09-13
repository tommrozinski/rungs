package com.rungs.readiness.application;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.stage.LadderResult;
import com.rungs.readiness.domain.stage.StageLadder;
import java.util.List;
import java.util.Objects;

/** Assesses a submitted snapshot: normalizes it, runs the stage ladder, and computes the monthly surplus (FR-1). */
public final class AssessmentService {

    private final StageLadder ladder;

    /** Creates a service over {@code ladder}; throws NullPointerException if it is null. */
    public AssessmentService(StageLadder ladder) {
        this.ladder = Objects.requireNonNull(ladder, "ladder");
    }

    /**
     * Returns the assessment of {@code input} after FR-4 normalization; warnings are empty until story 012. Throws
     * NullPointerException for null input, and IllegalArgumentException for input {@link SnapshotInput#toSnapshot}
     * cannot normalize.
     */
    public ReadinessAssessment assess(SnapshotInput input) {
        FinancialSnapshot snapshot = Objects.requireNonNull(input, "input").toSnapshot();
        LadderResult result = ladder.assess(snapshot);
        return new ReadinessAssessment(result.currentStage(), result.stages(), snapshot.monthlySurplus(), List.of());
    }
}
