package com.rungs.readiness.domain.stage;

import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * One stage's result. Construction enforces the rules below, so an inconsistent result cannot exist:
 *
 * <ul>
 *   <li>{@code stage} is 1 to 7, {@code status} and {@code percentComplete} are non-null, and {@code percentComplete}
 *       has exactly scale 1 (it is not normalized; evaluators produce it with {@link Percentages}) and is 0.0 to
 *       100.0;
 *   <li>{@code SATISFIED}: percent is 100.0, and remaining amount, blocker, and skip reason are null;
 *   <li>{@code IN_PROGRESS}: percent is 0.0 to 99.9 inclusive (0.0 is allowed: some stages, such as stage 2, have no
 *       usable progress metric and report 0.0 rather than {@code NOT_STARTED}), blocker is non-blank, and skip
 *       reason is null;
 *   <li>{@code NOT_STARTED}: percent is 0.0, blocker is non-blank, and skip reason is null;
 *   <li>{@code SKIPPED}: percent is 0.0, skip reason is non-blank, and remaining amount and blocker are null.
 * </ul>
 *
 * <p>A null status or percent throws {@link NullPointerException} naming the component; every other rule throws
 * {@link IllegalArgumentException} naming the stage and the rule, never the copy or amounts.
 *
 * @param stage the stage number, 1 to 7
 * @param status where the household stands on the stage
 * @param percentComplete progress at exactly scale 1, from 0.0 to 100.0
 * @param remainingAmount the dollar shortfall, or null when the stage has none
 * @param blocker user-facing copy on what stands in the way, or null
 * @param skipReason user-facing copy on why the stage does not apply, or null
 */
public record StageEvaluation(
        int stage,
        StageStatus status,
        BigDecimal percentComplete,
        Money remainingAmount,
        String blocker,
        String skipReason) {

    /** Lowest stage number. */
    static final int FIRST_STAGE = 1;

    /** Highest stage number. */
    static final int LAST_STAGE = 7;

    /** The only scale a percentage may have. */
    private static final int PERCENT_SCALE = 1;

    private static final BigDecimal ZERO_PERCENT = Percentages.ZERO_PERCENT;

    private static final BigDecimal FULL_PERCENT = Percentages.FULL_PERCENT;

    /** Highest percentage a stage that is not satisfied may report. */
    private static final BigDecimal UNMET_CAP = Percentages.UNMET_CAP;

    /** Validates the components; see the class Javadoc for the rules. */
    public StageEvaluation {
        if (stage < FIRST_STAGE || stage > LAST_STAGE) {
            throw new IllegalArgumentException("stage must be between 1 and 7");
        }
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(percentComplete, "percentComplete");
        if (percentComplete.scale() != PERCENT_SCALE) {
            throw new IllegalArgumentException(prefix(stage) + "percentComplete must have exactly 1 decimal place");
        }
        if (percentComplete.compareTo(ZERO_PERCENT) < 0 || percentComplete.compareTo(FULL_PERCENT) > 0) {
            throw new IllegalArgumentException(prefix(stage) + "percentComplete must be between 0.0 and 100.0");
        }
        String violation = switch (status) {
            case SATISFIED -> satisfiedViolation(percentComplete, remainingAmount, blocker, skipReason);
            case IN_PROGRESS -> percentComplete.compareTo(UNMET_CAP) > 0
                    ? "requires percentComplete at most 99.9"
                    : unmetViolation(blocker, skipReason);
            case NOT_STARTED -> percentComplete.compareTo(ZERO_PERCENT) != 0
                    ? "requires percentComplete 0.0"
                    : unmetViolation(blocker, skipReason);
            case SKIPPED -> skippedViolation(percentComplete, remainingAmount, blocker, skipReason);
        };
        if (violation != null) {
            throw new IllegalArgumentException(prefix(stage) + status + " " + violation);
        }
    }

    /** Returns a {@code SATISFIED} result for {@code stage}: 100.0 percent and nothing remaining. */
    public static StageEvaluation satisfied(int stage) {
        return new StageEvaluation(stage, StageStatus.SATISFIED, FULL_PERCENT, null, null, null);
    }

    /**
     * Returns an {@code IN_PROGRESS} result for {@code stage}.
     *
     * @param percentComplete progress from {@link Percentages}, at most 99.9
     * @param remainingAmount the dollar shortfall, or null when the stage has none
     * @param blocker non-blank copy on what stands in the way
     */
    public static StageEvaluation inProgress(
            int stage, BigDecimal percentComplete, Money remainingAmount, String blocker) {
        return new StageEvaluation(stage, StageStatus.IN_PROGRESS, percentComplete, remainingAmount, blocker, null);
    }

    /**
     * Returns a {@code NOT_STARTED} result for {@code stage} at 0.0 percent.
     *
     * @param remainingAmount the dollar shortfall, or null when the stage has none
     * @param blocker non-blank copy on what stands in the way
     */
    public static StageEvaluation notStarted(int stage, Money remainingAmount, String blocker) {
        return new StageEvaluation(stage, StageStatus.NOT_STARTED, ZERO_PERCENT, remainingAmount, blocker, null);
    }

    /** Returns a {@code SKIPPED} result for {@code stage} at 0.0 percent with a non-blank {@code skipReason}. */
    public static StageEvaluation skipped(int stage, String skipReason) {
        return new StageEvaluation(stage, StageStatus.SKIPPED, ZERO_PERCENT, null, null, skipReason);
    }

    /** Returns the message prefix naming {@code stage}. */
    private static String prefix(int stage) {
        return "stage " + stage + " ";
    }

    /** Returns the first SATISFIED rule the components break, or null when they break none. */
    private static String satisfiedViolation(
            BigDecimal percentComplete, Money remainingAmount, String blocker, String skipReason) {
        if (percentComplete.compareTo(FULL_PERCENT) != 0) {
            return "requires percentComplete 100.0";
        }
        if (remainingAmount != null) {
            return "must not have a remainingAmount";
        }
        if (blocker != null) {
            return "must not have a blocker";
        }
        return skipReason == null ? null : "must not have a skipReason";
    }

    /** Returns the first blocker or skip-reason rule shared by IN_PROGRESS and NOT_STARTED broken, or null. */
    private static String unmetViolation(String blocker, String skipReason) {
        if (!isNotBlank(blocker)) {
            return "requires a non-blank blocker";
        }
        return skipReason == null ? null : "must not have a skipReason";
    }

    /** Returns the first SKIPPED rule the components break, or null when they break none. */
    private static String skippedViolation(
            BigDecimal percentComplete, Money remainingAmount, String blocker, String skipReason) {
        if (percentComplete.compareTo(ZERO_PERCENT) != 0) {
            return "requires percentComplete 0.0";
        }
        if (remainingAmount != null) {
            return "must not have a remainingAmount";
        }
        if (blocker != null) {
            return "must not have a blocker";
        }
        return isNotBlank(skipReason) ? null : "requires a non-blank skipReason";
    }

    /** Returns whether {@code text} is non-null and has a non-whitespace character. */
    private static boolean isNotBlank(String text) {
        return text != null && !text.isBlank();
    }
}
