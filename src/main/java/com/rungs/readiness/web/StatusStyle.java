package com.rungs.readiness.web;

import com.rungs.readiness.domain.stage.StageStatus;

/**
 * How a {@link StageStatus} renders on the result page: the disc's modifier class and content, and the chip's
 * modifier class and label. The disc class and content also depend on the stage number and on whether the stage is
 * open (the current stage), which {@link #discClass} and {@link #discContent} take as parameters rather than
 * encoding a second dimension into the enum.
 */
enum StatusStyle {

    SATISFIED("done", "✓", "chip-done", "Done"),
    SKIPPED("skipped", "–", "chip-skipped", "Skipped"),
    IN_PROGRESS("active", null, "chip-active", "In progress"),
    NOT_STARTED(null, null, "chip-waiting", "Not started");

    private final String discClass;
    private final String fixedDiscContent;
    private final String chipClass;
    private final String chipLabel;

    StatusStyle(String discClass, String fixedDiscContent, String chipClass, String chipLabel) {
        this.discClass = discClass;
        this.fixedDiscContent = fixedDiscContent;
        this.chipClass = chipClass;
        this.chipLabel = chipLabel;
    }

    /** Returns the style for {@code status}. */
    static StatusStyle of(StageStatus status) {
        return switch (status) {
            case SATISFIED -> SATISFIED;
            case SKIPPED -> SKIPPED;
            case IN_PROGRESS -> IN_PROGRESS;
            case NOT_STARTED -> NOT_STARTED;
        };
    }

    /** Returns the disc's modifier class, or null for a plain disc; {@code NOT_STARTED} renders "active" when
     * {@code open} is the current stage, otherwise null. */
    String discClass(boolean open) {
        return this == NOT_STARTED && open ? "active" : discClass;
    }

    /** Returns the disc's content: a fixed glyph for {@code SATISFIED} and {@code SKIPPED}, else {@code stage}. */
    String discContent(int stage) {
        return fixedDiscContent != null ? fixedDiscContent : String.valueOf(stage);
    }

    String chipClass() {
        return chipClass;
    }

    String chipLabel() {
        return chipLabel;
    }
}
