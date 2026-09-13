package com.rungs.readiness.adapter.in.web.view;

import java.util.List;

/**
 * Sample content for the dev-only component gallery (025 REQ-005). Every string, including formatted figures and
 * element ids, is prepared here so the gallery template only prints.
 *
 * @param buttons one sample per button variant
 * @param buttonStates the forced states each button variant is shown in, besides disabled
 * @param field text field sample
 * @param toggle toggle group sample
 * @param checkboxLabel checkbox label
 * @param stats stat card samples
 * @param caution caution banner sample
 * @param problem problem banner sample
 * @param schedule schedule table sample
 * @param ladder seven stage rows covering all four statuses, with stage 2 current
 * @param verdict verdict showing the current stage's blocker
 * @param finishedVerdict verdict when stage 7 is current and satisfied
 */
public record GallerySamples(
        List<ButtonSample> buttons,
        List<String> buttonStates,
        FieldSample field,
        ToggleSample toggle,
        String checkboxLabel,
        List<StatSample> stats,
        BannerSample caution,
        BannerSample problem,
        ScheduleSample schedule,
        List<StageRowView> ladder,
        VerdictView verdict,
        VerdictView finishedVerdict) {

    private static final int CURRENT_STAGE = 2;

    private static final String BLOCKER = "$12,480 stands between you and being debt free.";

    /** Returns the fixed gallery samples. */
    public static GallerySamples create() {
        return new GallerySamples(
                List.of(new ButtonSample("Check my stage", "primary"),
                        new ButtonSample("See the payoff plan", "secondary"),
                        new ButtonSample("Change my numbers", "quiet")),
                List.of("", "hover", "focus"),
                new FieldSample("Monthly take-home pay", "$4,200.00", "-50", "Enter zero or more."),
                new ToggleSample("Payoff order", "SNOWBALL", "Smallest balance first", "AVALANCHE",
                        "Highest rate first"),
                "You rent your home",
                List.of(new StatSample("Total debt", "$12,480.00", "neutral"),
                        new StatSample("Interest saved", "$1,215.40", "positive")),
                new BannerSample("Check these numbers",
                        "Your monthly expenses are higher than your take-home pay."),
                new BannerSample("We couldn't use this file", "Line 4 has a balance that isn't a number."),
                new ScheduleSample("Month-by-month schedule",
                        List.of("Month", "Store card", "Car loan", "Total paid"),
                        List.of(new ScheduleRowView("Month 1", List.of("$310.00", "$9,120.00", "$590.00"), false),
                                new ScheduleRowView("Month 2", List.of("$0.00", "$8,940.00", "$590.00"), true),
                                new ScheduleRowView("Month 3", List.of("$0.00", "$8,450.00", "$590.00"), false))),
                List.of(StageRowViews.row(1, "SATISFIED", "100.0", null, null, CURRENT_STAGE),
                        StageRowViews.row(2, "IN_PROGRESS", "38.5", BLOCKER, null, CURRENT_STAGE),
                        StageRowViews.row(3, "NOT_STARTED", "0.0",
                                "$9,600 more gets you to three months of expenses.", null, CURRENT_STAGE),
                        StageRowViews.row(4, "NOT_STARTED", "0.0",
                                "You aren't putting any of your income toward retirement yet.", null, CURRENT_STAGE),
                        StageRowViews.row(5, "SKIPPED", "0.0", null,
                                "You have no children, so there's no college fund to build.", CURRENT_STAGE),
                        StageRowViews.row(6, "SKIPPED", "0.0", null,
                                "You rent, so there's no mortgage to pay down.", CURRENT_STAGE),
                        StageRowViews.row(7, "NOT_STARTED", "0.0",
                                "The earlier stages come first.", null, CURRENT_STAGE)),
                StageRowViews.verdict(CURRENT_STAGE, "IN_PROGRESS", BLOCKER),
                StageRowViews.verdict(7, "SATISFIED", null));
    }

    /** A button label and its variant. */
    public record ButtonSample(String label, String variant) {
    }

    /** Text field copy; the gallery shows it default, focused, in error, and read-only. */
    public record FieldSample(String label, String value, String invalidValue, String errorMessage) {
    }

    /** Toggle group copy and option values. */
    public record ToggleSample(String legend, String firstValue, String firstLabel, String secondValue,
            String secondLabel) {
    }

    /** A stat card sample; tone is neutral or positive. */
    public record StatSample(String label, String figure, String tone) {
    }

    /** A warning banner heading and message. */
    public record BannerSample(String heading, String message) {
    }

    /** A schedule table caption, column headers, and rows. */
    public record ScheduleSample(String caption, List<String> columns, List<ScheduleRowView> rows) {
    }
}
