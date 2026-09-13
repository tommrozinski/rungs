package com.rungs.readiness.adapter.in.web.view;

/**
 * One C-06 stage row, with every string chosen server-side by {@link StageRowViews} so the template only prints.
 *
 * @param stage the stage number, 1 to 7
 * @param title the fixed stage title
 * @param status the status name: SATISFIED, IN_PROGRESS, NOT_STARTED or SKIPPED
 * @param chipLabel the status chip text
 * @param discGlyph the disc content: a check, a dash, or the stage number
 * @param stageLabel visually hidden disc text, such as "Stage 3"
 * @param percentComplete the percent string from the assessment result, such as "40.0"
 * @param progressWidth the progress fill width attribute, such as "40.0%"
 * @param progressComplete whether progress is at 100, which turns the bar green
 * @param reason the blocker, skip reason, or satisfied copy
 * @param current whether this is the current stage, which expands the row
 * @param ctaLabel the call-to-action label on the current row, or empty
 * @param ctaAction the call-to-action form action on the current row, or empty
 */
public record StageRowView(
        int stage,
        String title,
        String status,
        String chipLabel,
        String discGlyph,
        String stageLabel,
        String percentComplete,
        String progressWidth,
        boolean progressComplete,
        String reason,
        boolean current,
        String ctaLabel,
        String ctaAction) {
}
