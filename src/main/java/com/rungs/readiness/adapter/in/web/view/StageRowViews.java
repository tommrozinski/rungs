package com.rungs.readiness.adapter.in.web.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Builds stage row and verdict views from plain assessment values (026). All fixed copy lives here: titles
 * (REQ-007), chip labels (REQ-001), calls to action (REQ-003) and the finished-ladder verdict (REQ-006). Blockers,
 * skip reasons and percent strings pass through verbatim (REQ-008).
 *
 * <p>Inputs are the values the assessment response carries, so a page mapper can call this without the view
 * depending on domain types. Invalid input throws {@link IllegalArgumentException}.
 */
public final class StageRowViews {

    /** Form action for "See the payoff plan"; the projection screen (030) owns the route. */
    public static final String PAYOFF_PLAN_ACTION = "/projection";

    /** Form action for "Change my numbers"; the snapshot screen (027) owns the route. */
    public static final String CHANGE_NUMBERS_ACTION = "/";

    /** Verdict when the current stage is 7 and nothing earlier is left to do. */
    static final String FINISHED_VERDICT = "Every earlier stage is complete or doesn't apply to you.";

    /**
     * Reason on a satisfied row. The assessment has no blocker for a satisfied stage, but every row shows a reason
     * (design spec §3), so this plain fixed copy is used.
     */
    static final String SATISFIED_REASON = "This stage is complete.";

    private static final String SATISFIED = "SATISFIED";

    private static final String IN_PROGRESS = "IN_PROGRESS";

    private static final String NOT_STARTED = "NOT_STARTED";

    private static final String SKIPPED = "SKIPPED";

    private static final List<String> TITLES = List.of("Starter emergency fund", "Pay off debt",
            "Full emergency fund", "Save for retirement", "Save for college", "Pay off your home", "Build wealth");

    private static final Map<String, String> CHIP_LABELS = Map.of(SATISFIED, "Done", IN_PROGRESS, "In progress",
            NOT_STARTED, "Not started", SKIPPED, "Doesn't apply");

    private static final String CHECK = "✓";

    private static final String DASH = "–";

    private static final int FIRST_STAGE = 1;

    private static final int LAST_STAGE = 7;

    private static final int PAYOFF_STAGE = 2;

    private static final BigDecimal FULL_PERCENT = BigDecimal.valueOf(100);

    private StageRowViews() {
    }

    /**
     * Returns the row for {@code stage}.
     *
     * @param stage the stage number, 1 to 7
     * @param status the status name
     * @param percentComplete the percent string from the result, 0 to 100, such as "40.0"
     * @param blocker the blocker, required and non-blank for IN_PROGRESS and NOT_STARTED
     * @param skipReason the skip reason, required and non-blank for SKIPPED
     * @param currentStage the current stage number, 1 to 7
     */
    public static StageRowView row(
            int stage, String status, String percentComplete, String blocker, String skipReason, int currentStage) {
        checkStage(stage, "stage");
        checkStage(currentStage, "currentStage");
        String chipLabel = CHIP_LABELS.get(status);
        if (chipLabel == null) {
            throw new IllegalArgumentException("status must be SATISFIED, IN_PROGRESS, NOT_STARTED or SKIPPED");
        }
        BigDecimal percent = percent(percentComplete);
        String reason = switch (status) {
            case SATISFIED -> SATISFIED_REASON;
            case SKIPPED -> nonBlank(skipReason, "skipReason");
            default -> nonBlank(blocker, "blocker");
        };
        String disc = switch (status) {
            case SATISFIED -> CHECK;
            case SKIPPED -> DASH;
            default -> String.valueOf(stage);
        };
        boolean current = stage == currentStage;
        String ctaLabel = "";
        String ctaAction = "";
        if (current) {
            ctaLabel = stage == PAYOFF_STAGE ? "See the payoff plan" : "Change my numbers";
            ctaAction = stage == PAYOFF_STAGE ? PAYOFF_PLAN_ACTION : CHANGE_NUMBERS_ACTION;
        }
        return new StageRowView(stage, TITLES.get(stage - 1), status, chipLabel, disc, "Stage " + stage,
                percentComplete, percentComplete + "%", percent.compareTo(FULL_PERCENT) == 0, reason, current,
                ctaLabel, ctaAction);
    }

    /**
     * Returns the verdict: fixed copy when the current stage is 7 and satisfied or skipped, otherwise the current
     * stage's blocker, which must be non-blank.
     */
    public static VerdictView verdict(int currentStage, String currentStatus, String currentBlocker) {
        checkStage(currentStage, "currentStage");
        if (!CHIP_LABELS.containsKey(currentStatus)) {
            throw new IllegalArgumentException("currentStatus must be SATISFIED, IN_PROGRESS, NOT_STARTED or SKIPPED");
        }
        boolean finished = currentStage == LAST_STAGE
                && (SATISFIED.equals(currentStatus) || SKIPPED.equals(currentStatus));
        return new VerdictView(finished ? FINISHED_VERDICT : nonBlank(currentBlocker, "currentBlocker"));
    }

    private static void checkStage(int stage, String name) {
        if (stage < FIRST_STAGE || stage > LAST_STAGE) {
            throw new IllegalArgumentException(name + " must be between 1 and 7");
        }
    }

    private static BigDecimal percent(String percentComplete) {
        BigDecimal percent;
        try {
            percent = new BigDecimal(String.valueOf(percentComplete));
        } catch (NumberFormatException notANumber) {
            throw new IllegalArgumentException("percentComplete must be a decimal number", notANumber);
        }
        if (percent.signum() < 0 || percent.compareTo(FULL_PERCENT) > 0) {
            throw new IllegalArgumentException("percentComplete must be between 0 and 100");
        }
        return percent;
    }

    private static String nonBlank(String text, String name) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }
}
