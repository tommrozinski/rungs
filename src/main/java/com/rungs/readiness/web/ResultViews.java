package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import com.rungs.readiness.domain.model.MortgagePayoff;
import com.rungs.readiness.domain.model.MortgagePayoffCalculator;
import com.rungs.readiness.domain.stage.LadderResult;
import com.rungs.readiness.domain.stage.StageEvaluation;
import com.rungs.readiness.domain.stage.StageStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the result page's view model (design.md 038 section 4) from a {@link LadderResult} and the
 * {@link FinancialSnapshot} it was computed from. The copy lives here rather than in the domain evaluators, so the
 * seven-stage rules stay free of presentation concerns; only the evaluators' status, percent, and remaining-amount
 * data drive it, plus the snapshot's own figures where the table needs a raw value (the actual fund balance, the
 * three-month target, the household flags).
 */
final class ResultViews {

    private static final String[] STAGE_TITLES = {
        "Save $1,000 to start",
        "Pay off everything but the house",
        "Save three months of expenses",
        "Put 15% toward retirement",
        "Save for college",
        "Pay off the house",
        "Build wealth and give",
    };

    /** Index 0 is unused; index n spells the number n for one to ten. */
    private static final String[] SPELLED_NUMBERS = {
        "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
    };

    private static final BigDecimal RETIREMENT_TARGET_PERCENT = BigDecimal.valueOf(15);

    private static final BigDecimal EMERGENCY_FUND_MONTHS = BigDecimal.valueOf(3);

    private static final int LAST_STAGE = 7;

    private ResultViews() {
    }

    /** Builds the view model for {@code ladder}, the result of assessing {@code snapshot}. */
    static ResultView build(LadderResult ladder, FinancialSnapshot snapshot) {
        List<StageView> stages = new ArrayList<>(LAST_STAGE);
        for (int stage = 1; stage <= LAST_STAGE; stage++) {
            stages.add(stageView(stage, ladder, snapshot));
        }

        boolean allDone = ladder.evaluation(LAST_STAGE).status() == StageStatus.SATISFIED;
        String eyebrow = allDone
                ? "All seven stages are done or skipped"
                : "You're on stage " + ladder.currentStage() + " of 7";

        StageEvaluation current = ladder.evaluation(ladder.currentStage());
        long stagesComplete = ladder.stages().stream()
                .filter(evaluation -> evaluation.status() == StageStatus.SATISFIED)
                .count();
        Money surplus = snapshot.monthlySurplus();

        return new ResultView(
                eyebrow,
                verdictLine(current),
                verdictSub(current, snapshot),
                formatMoney(surplus),
                surplus.isPositive(),
                formatMoney(snapshot.totalDebtBalance()),
                stagesComplete + " of 7",
                stages);
    }

    private static StageView stageView(int stage, LadderResult ladder, FinancialSnapshot snapshot) {
        StageEvaluation evaluation = ladder.evaluation(stage);
        boolean open = stage == ladder.currentStage();

        // Stage 7 has no "in progress" presentation of its own (design.md 038 section 4): anything short of
        // SATISFIED reads as Not started, even though WealthBuildingEvaluator reports IN_PROGRESS once some
        // earlier stages are done.
        StageStatus displayStatus = stage == LAST_STAGE && evaluation.status() != StageStatus.SATISFIED
                ? StageStatus.NOT_STARTED
                : evaluation.status();

        String discClass;
        String discContent;
        String chipClass;
        String chipLabel;
        switch (displayStatus) {
            case SATISFIED -> {
                discClass = "done";
                discContent = "✓";
                chipClass = "chip-done";
                chipLabel = "Done";
            }
            case SKIPPED -> {
                discClass = "skipped";
                discContent = "–";
                chipClass = "chip-skipped";
                chipLabel = "Skipped";
            }
            case IN_PROGRESS -> {
                discClass = "active";
                discContent = String.valueOf(stage);
                chipClass = "chip-active";
                chipLabel = "In progress";
            }
            default -> {
                discClass = open ? "active" : null;
                discContent = String.valueOf(stage);
                chipClass = "chip-waiting";
                chipLabel = "Not started";
            }
        }

        boolean showBar = (stage == 1 || stage == 3 || stage == 4) && evaluation.status() == StageStatus.IN_PROGRESS;
        int barWidthPercent = showBar
                ? evaluation.percentComplete().setScale(0, RoundingMode.DOWN).intValueExact()
                : 0;

        return new StageView(
                stage,
                STAGE_TITLES[stage - 1],
                chipLabel,
                chipClass,
                discClass,
                discContent,
                open,
                stageLine(stage, evaluation, snapshot),
                showBar,
                barWidthPercent);
    }

    /** Returns the stage's line: the small text when not open, or the blocker paragraph's text when open. */
    private static String stageLine(int stage, StageEvaluation evaluation, FinancialSnapshot snapshot) {
        return switch (stage) {
            case 1 -> formatMoney(snapshot.starterEmergencyFund()) + " of $1,000.";
            case 2 -> stage2Line(evaluation, snapshot);
            case 3 -> stage3Line(snapshot);
            case 4 -> stage4Line(evaluation, snapshot);
            case 5 -> stage5Line(evaluation, snapshot);
            case 6 -> stage6Line(evaluation);
            case 7 -> stage7Line(evaluation);
            default -> throw new IllegalArgumentException("stage must be between 1 and 7");
        };
    }

    private static String stage2Line(StageEvaluation evaluation, FinancialSnapshot snapshot) {
        if (evaluation.status() == StageStatus.SATISFIED) {
            return "No debts besides the house.";
        }
        int count = snapshot.debts().size();
        String noun = count == 1 ? "debt" : "debts";
        String verb = count == 1 ? "remains" : "remain";
        return capitalize(spellNumber(count)) + " " + noun + " " + verb + ", totalling <b>"
                + formatMoney(evaluation.remainingAmount()) + "</b>.";
    }

    private static String stage3Line(FinancialSnapshot snapshot) {
        Money target = snapshot.monthlyEssentialCosts().times(EMERGENCY_FUND_MONTHS);
        return formatMoney(snapshot.fullEmergencyFund()) + " of " + formatMoney(target) + ".";
    }

    private static String stage4Line(StageEvaluation evaluation, FinancialSnapshot snapshot) {
        if (evaluation.status() == StageStatus.SKIPPED) {
            return "You told us you're retired.";
        }
        String percentText = plainPercent(snapshot.retirementContributionPercent());
        if (evaluation.status() == StageStatus.SATISFIED) {
            return "You're at " + percentText + "%.";
        }
        BigDecimal remainingPoints = RETIREMENT_TARGET_PERCENT.subtract(snapshot.retirementContributionPercent());
        return "You're at " + percentText + "%. " + capitalize(spellOrDigits(remainingPoints)) + " points to go.";
    }

    private static String stage5Line(StageEvaluation evaluation, FinancialSnapshot snapshot) {
        if (evaluation.status() == StageStatus.SATISFIED) {
            return "You're saving for college.";
        }
        if (evaluation.status() == StageStatus.SKIPPED) {
            return snapshot.householdFlags().intendsToFundCollege()
                    ? "You told us you're retired."
                    : "You told us you're not funding college.";
        }
        return "You plan to fund college.";
    }

    private static String stage6Line(StageEvaluation evaluation) {
        if (evaluation.status() == StageStatus.SATISFIED) {
            return "Your house is paid off.";
        }
        if (evaluation.status() == StageStatus.SKIPPED) {
            return "You rent, so there's no mortgage to pay down.";
        }
        // MortgageEvaluator's blocker already carries the payoff copy (design.md 039 section 2): the payment's
        // coverage of interest, or the payoff month and how it compares to the loan's maturity.
        return evaluation.blocker();
    }

    private static String stage7Line(StageEvaluation evaluation) {
        return evaluation.status() == StageStatus.SATISFIED
                ? "Every earlier stage is done or skipped."
                : "Opens once stages 1 through 6 are done or skipped.";
    }

    private static String verdictLine(StageEvaluation current) {
        return switch (current.stage()) {
            case 1 -> formatMoney(current.remainingAmount()) + " more and your starter fund is done.";
            case 2 -> formatMoney(current.remainingAmount()) + " stands between you and debt freedom.";
            case 3 -> formatMoney(current.remainingAmount()) + " more and three months of expenses are covered.";
            case 4 -> "Raise retirement saving to 15%.";
            case 5 -> "Start saving for college.";
            case 6 -> "Pay off the house.";
            case 7 -> "Build wealth and give.";
            default -> throw new IllegalArgumentException("stage must be between 1 and 7");
        };
    }

    private static String verdictSub(StageEvaluation current, FinancialSnapshot snapshot) {
        Money surplus = snapshot.monthlySurplus();
        return switch (current.stage()) {
            case 1 -> "Save $1,000 before anything else.";
            case 2, 3 -> surplusSentence(current.remainingAmount(), surplus);
            case 4 -> "You're at " + plainPercent(snapshot.retirementContributionPercent()) + "%.";
            case 5 -> "Set aside something every month.";
            case 6 -> stage6VerdictSub(current, snapshot);
            case 7 -> "Every earlier stage is done or skipped.";
            default -> throw new IllegalArgumentException("stage must be between 1 and 7");
        };
    }

    /**
     * Returns the stage 6 verdict's sub line (design.md 039 section 2): the payoff month at this payment, or, when
     * the payment does not cover the interest or the loan runs past 600 months, the evaluator's own sentence for
     * that case (identical to {@code current}'s blocker, since neither branch appends a maturity follow-up).
     */
    private static String stage6VerdictSub(StageEvaluation current, FinancialSnapshot snapshot) {
        Mortgage mortgage = (Mortgage) snapshot.housing();
        MortgagePayoff payoff = MortgagePayoffCalculator.payoff(mortgage, snapshot.asOf());
        if (!payoff.covered() || payoff.monthsToPayoff() == null) {
            return current.blocker();
        }
        return "At this payment it's paid off in " + monthYear(payoff.payoffMonth())
                + ". Every extra dollar gets you there sooner.";
    }

    /** Returns {@code month} as e.g. "March 2041". */
    private static String monthYear(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.US) + " " + month.getYear();
    }

    /** Returns the surplus sentence for stages 2 and 3, or the alternate sentence when the surplus is $0 or below. */
    private static String surplusSentence(Money shortfall, Money surplus) {
        if (!surplus.isPositive()) {
            return "Right now your expenses and minimums use up your take-home pay.";
        }
        long months = ceilDiv(shortfall.cents(), surplus.cents());
        String word = months == 1 ? "month" : "months";
        return "At your current surplus of " + formatMoney(surplus) + " a month, that's about " + months + " "
                + word + " of work.";
    }

    /** Returns {@code numerator / denominator}, rounded up; both are positive cents. */
    private static long ceilDiv(long numerator, long denominator) {
        return (numerator + denominator - 1) / denominator;
    }

    /** Returns {@code amount} with a dollar sign and, unless it is a whole number of dollars, exactly two cents. */
    private static String formatMoney(Money amount) {
        String display = amount.toDisplayString();
        return display.endsWith(".00") ? display.substring(0, display.length() - 3) : display;
    }

    /** Returns {@code percent} with no trailing zeros and never in scientific notation, e.g. {@code "6"} or {@code "6.5"}. */
    private static String plainPercent(BigDecimal percent) {
        return percent.stripTrailingZeros().toPlainString();
    }

    /** Returns the word for {@code count} from one to ten, else its digits. */
    private static String spellNumber(int count) {
        return count >= 1 && count <= SPELLED_NUMBERS.length - 1 ? SPELLED_NUMBERS[count] : Integer.toString(count);
    }

    /** Returns the word for a whole {@code value} from one to ten, else its digits (e.g. {@code "8.5"} or {@code "15"}). */
    private static String spellOrDigits(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() <= 0) {
            int whole = stripped.intValueExact();
            if (whole >= 1 && whole <= SPELLED_NUMBERS.length - 1) {
                return SPELLED_NUMBERS[whole];
            }
            return Integer.toString(whole);
        }
        return stripped.toPlainString();
    }

    /** Returns {@code word} with its first character upper-cased. */
    private static String capitalize(String word) {
        return word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }
}
