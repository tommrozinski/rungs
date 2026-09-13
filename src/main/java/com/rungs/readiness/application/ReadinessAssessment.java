package com.rungs.readiness.application;

import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Warning;
import com.rungs.readiness.domain.stage.StageEvaluation;
import java.util.List;
import java.util.Objects;

/**
 * The result of assessing one snapshot (PRD FR-1).
 *
 * @param currentStage the lowest stage neither satisfied nor skipped, or 7 when every stage is
 * @param stages all seven stage results in order; an unmodifiable copy
 * @param monthlySurplus income minus essential expenses minus debt minimum payments; may be negative
 * @param warnings non-fatal conditions detected on the snapshot (story 012); an unmodifiable copy
 */
public record ReadinessAssessment(
        int currentStage, List<StageEvaluation> stages, Money monthlySurplus, List<Warning> warnings) {

    /** Copies the lists; throws NullPointerException for a null list or surplus. */
    public ReadinessAssessment {
        stages = List.copyOf(Objects.requireNonNull(stages, "stages"));
        Objects.requireNonNull(monthlySurplus, "monthlySurplus");
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }
}
