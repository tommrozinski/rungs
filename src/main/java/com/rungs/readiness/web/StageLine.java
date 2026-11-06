package com.rungs.readiness.web;

/**
 * A piece of stage copy — a stage's small line, or the blocker shown when it's open — as {@code text} followed,
 * optionally, by a bold {@code emphasis} and a trailing {@code suffix}. Only stage 2's in-progress line uses
 * {@code emphasis} (the debt total, e.g. "$12,480"); every other line is plain text, with a null {@code emphasis}
 * and an empty {@code suffix}.
 *
 * @param text the line's leading text, or its entire text when there is no emphasis
 * @param emphasis the bold portion, or null when the line has none
 * @param suffix the text following the emphasis; empty when {@code emphasis} is null
 */
record StageLine(String text, String emphasis, String suffix) {

    /** Returns a line with no emphasis: {@code text} in full. */
    static StageLine plain(String text) {
        return new StageLine(text, null, "");
    }
}
