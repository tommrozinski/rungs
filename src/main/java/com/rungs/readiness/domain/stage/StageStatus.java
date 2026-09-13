package com.rungs.readiness.domain.stage;

/** Where a household stands on one stage of the ladder. */
public enum StageStatus {

    /** The stage's condition is met. */
    SATISFIED,

    /** Some progress is measured, or the stage cannot measure progress but is not met (stage 2). */
    IN_PROGRESS,

    /** No progress is measured. */
    NOT_STARTED,

    /** The stage does not apply to this household. */
    SKIPPED
}
