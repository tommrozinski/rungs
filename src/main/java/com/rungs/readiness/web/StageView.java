package com.rungs.readiness.web;

/**
 * One row of the seven-stage ladder as the result page renders it.
 *
 * @param stage the stage number, 1 to 7
 * @param title the stage's title, e.g. "Save $1,000 to start"
 * @param chipLabel the chip's text: "Done", "In progress", "Not started", or "Skipped"
 * @param chipClass the chip's modifier class: "chip-done", "chip-active", "chip-waiting", or "chip-skipped"
 * @param discClass the disc's modifier class: "done", "active", "skipped", or null for a plain, not-started disc
 * @param discContent the disc's content: a checkmark, an en dash, or the stage number as text
 * @param open whether this is the current stage, rendered inside {@code .stage-open} with the blocker paragraph
 *     rather than the small line
 * @param line the stage's copy, shown as the small text or, when {@code open}, as the blocker: {@code line} is its
 *     leading text (or the entire text when there is no emphasis)
 * @param lineEmphasis the line's bold portion, or null when it has none (every stage but stage 2 in progress)
 * @param lineSuffix the text following {@code lineEmphasis}; empty when {@code lineEmphasis} is null
 * @param showBar whether the progress bar is shown (stages 1, 3, and 4, only while in progress)
 * @param barWidthPercent the bar's fill percentage, meaningful only when {@code showBar} is true
 */
record StageView(
        int stage,
        String title,
        String chipLabel,
        String chipClass,
        String discClass,
        String discContent,
        boolean open,
        String line,
        String lineEmphasis,
        String lineSuffix,
        boolean showBar,
        int barWidthPercent) {
}
