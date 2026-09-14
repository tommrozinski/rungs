package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import com.rungs.readiness.domain.projection.MortgagePayoff;
import com.rungs.readiness.domain.projection.MortgagePayoffCalculator;
import com.rungs.readiness.domain.projection.PayoffSchedule;
import com.rungs.readiness.domain.stage.LadderResult;
import com.rungs.readiness.domain.stage.StageEvaluation;
import com.rungs.readiness.domain.stage.StageStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds the result page's view model from a {@link LadderResult} and the {@link FinancialSnapshot} it was computed
 * from. The copy lives here rather than in the domain evaluators, so the seven-stage rules stay free of
 * presentation concerns; only the evaluators' status, percent, and remaining-amount data drive it, plus the
 * snapshot's own figures where the table needs a raw value (the actual fund balance, the emergency-fund target, the
 * household flags).
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

    private static final BigDecimal RETIREMENT_TARGET_PERCENT = BigDecimal.valueOf(15);

    private static final int LAST_STAGE = 7;

    private ResultViews() {
    }

    /** Builds the view model for {@code ladder}, the result of assessing {@code snapshot}, whose debt-snowball
     * payoff schedule is {@code schedule}. Computes stage 6's current-payment-versus-snowball comparison once, up
     * front, since both the stage row and the verdict need it. */
    static ResultView build(LadderResult ladder, FinancialSnapshot snapshot, PayoffSchedule schedule) {
        Optional<SnowballComparison> mortgageComparison = mortgageComparison(snapshot, schedule);

        List<StageView> stages = new ArrayList<>(LAST_STAGE);
        for (int stage = 1; stage <= LAST_STAGE; stage++) {
            stages.add(stageView(stage, ladder, snapshot, schedule, mortgageComparison));
        }

        boolean allDone = ladder.evaluation(LAST_STAGE).status() == StageStatus.SATISFIED;
        String eyebrow = allDone
                ? "Every stage that applies to you is done"
                : "You're on stage " + ladder.currentStage() + " of 7";

        StageEvaluation current = ladder.evaluation(ladder.currentStage());
        long stagesComplete = ladder.stages().stream()
                .filter(evaluation -> evaluation.status() == StageStatus.SATISFIED)
                .count();
        Money surplus = snapshot.monthlySurplus();

        return new ResultView(
                eyebrow,
                verdictLine(current),
                verdictSub(current, snapshot, schedule, mortgageComparison),
                Formats.money(surplus),
                surplus.isPositive(),
                Formats.money(snapshot.totalDebtBalance()),
                stagesComplete + " of 7",
                stages);
    }

    private static StageView stageView(
            int stage,
            LadderResult ladder,
            FinancialSnapshot snapshot,
            PayoffSchedule schedule,
            Optional<SnowballComparison> mortgageComparison) {
        StageEvaluation evaluation = ladder.evaluation(stage);
        boolean open = stage == ladder.currentStage();

        // Stage 7 has no "in progress" presentation of its own: anything short of SATISFIED reads as Not started,
        // even though WealthBuildingEvaluator reports IN_PROGRESS once some earlier stages are done.
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
                stageLine(stage, evaluation, snapshot, mortgageComparison),
                showBar,
                barWidthPercent);
    }

    /** Returns the stage's line: the small text when not open, or the blocker paragraph's text when open. */
    private static String stageLine(
            int stage,
            StageEvaluation evaluation,
            FinancialSnapshot snapshot,
            Optional<SnowballComparison> mortgageComparison) {
        return switch (stage) {
            case 1 -> Formats.money(snapshot.starterEmergencyFund()) + " of $1,000.";
            case 2 -> stage2Line(evaluation, snapshot);
            case 3 -> stage3Line(snapshot);
            case 4 -> stage4Line(evaluation, snapshot);
            case 5 -> stage5Line(evaluation, snapshot);
            case 6 -> stage6Line(evaluation, mortgageComparison);
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
        return Formats.capitalize(Formats.spellOrDigits(count)) + " " + noun + " " + verb + ", totalling <b>"
                + Formats.money(evaluation.remainingAmount()) + "</b>.";
    }

    private static String stage3Line(FinancialSnapshot snapshot) {
        Money target = snapshot.monthlyEssentialCosts().times(BigDecimal.valueOf(snapshot.emergencyFundMonths()));
        return Formats.money(snapshot.fullEmergencyFund()) + " of " + Formats.money(target) + ".";
    }

    private static String stage4Line(StageEvaluation evaluation, FinancialSnapshot snapshot) {
        if (evaluation.status() == StageStatus.SKIPPED) {
            return "You told us you're retired.";
        }
        String percentText = Formats.plainPercent(snapshot.retirementContributionPercent());
        if (evaluation.status() == StageStatus.SATISFIED) {
            return "You're at " + percentText + "%.";
        }
        BigDecimal remainingPoints = RETIREMENT_TARGET_PERCENT.subtract(snapshot.retirementContributionPercent());
        return "You're at " + percentText + "%. " + Formats.capitalize(Formats.spellOrDigits(remainingPoints))
                + " points to go.";
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

    private static String stage6Line(StageEvaluation evaluation, Optional<SnowballComparison> mortgageComparison) {
        if (evaluation.status() == StageStatus.SATISFIED) {
            return "Your house is paid off.";
        }
        if (evaluation.status() == StageStatus.SKIPPED) {
            return "You rent, so there's no mortgage to pay down.";
        }
        // MortgageEvaluator's blocker already carries the current-payment payoff copy: the payment's coverage of
        // interest, or the current-payment payoff month, or the 600-month cap. The debt-snowball comparison is
        // appended here, computed once by build() and passed down, rather than being recomputed in the domain.
        return evaluation.blocker() + mortgageComparison.map(ResultViews::snowballSuffix).orElse("");
    }

    /** Returns the stage 6 line's trailing sentence for {@code comparison}, with its leading space. */
    private static String snowballSuffix(SnowballComparison comparison) {
        return comparison.duration() != null
                ? " With the debt snowball it's paid off in " + Formats.monthYear(comparison.date()) + ", "
                        + comparison.duration() + " sooner."
                : " With the debt snowball it's paid off in " + Formats.monthYear(comparison.date()) + ".";
    }

    /**
     * Returns the stage 6 comparison for {@code snapshot}, empty for a renter: whether, and how, the debt
     * snowball's mortgage payoff month beats the current-payment payoff. Computed once per request by
     * {@link #build} and threaded through the stage row and the verdict, rather than recomputed by each.
     */
    private static Optional<SnowballComparison> mortgageComparison(FinancialSnapshot snapshot, PayoffSchedule schedule) {
        if (!(snapshot.housing() instanceof Mortgage mortgage)) {
            return Optional.empty();
        }
        MortgagePayoff currentPayoff = MortgagePayoffCalculator.payoff(mortgage, snapshot.asOf());
        return snowballComparison(currentPayoff, schedule.mortgagePayoffMonth());
    }

    /**
     * Whether, and how, the debt snowball's mortgage payoff month beats the current-payment payoff: empty when the
     * snowball never closes the mortgage within 600 months, or when the current payment itself finishes at or
     * before the snowball's date. {@code duration} is the amount of time the snowball saves over the current
     * payment, or null when the current payment has no payoff date to compare against (not covered, or capped at
     * 600 months).
     */
    private static Optional<SnowballComparison> snowballComparison(
            MortgagePayoff currentPayoff, Optional<YearMonth> mortgagePayoffMonth) {
        if (mortgagePayoffMonth.isEmpty()) {
            return Optional.empty();
        }
        YearMonth snowballDate = mortgagePayoffMonth.get();
        if (!(currentPayoff instanceof MortgagePayoff.PaysOff paysOff)) {
            return Optional.of(new SnowballComparison(snowballDate, null));
        }
        YearMonth currentDate = paysOff.month();
        if (!snowballDate.isBefore(currentDate)) {
            return Optional.empty();
        }
        return Optional.of(new SnowballComparison(snowballDate, Formats.duration(snowballDate, currentDate)));
    }

    /** The debt snowball's mortgage payoff date, and how much sooner it is than the current payment's own payoff
     * date, or null when there's no current-payment date to compare against. */
    private record SnowballComparison(YearMonth date, String duration) {
    }

    private static String stage7Line(StageEvaluation evaluation) {
        return evaluation.status() == StageStatus.SATISFIED
                ? "Every earlier stage is done or skipped."
                : "Opens once stages 1 through 6 are done or skipped.";
    }

    private static String verdictLine(StageEvaluation current) {
        return switch (current.stage()) {
            case 1 -> Formats.money(current.remainingAmount()) + " more and your starter fund is done.";
            case 2 -> Formats.money(current.remainingAmount()) + " stands between you and debt freedom.";
            case 3 -> Formats.money(current.remainingAmount()) + " more and three months of expenses are covered.";
            case 4 -> "Raise retirement saving to 15%.";
            case 5 -> "Start saving for college.";
            case 6 -> "Pay off the house.";
            case 7 -> "Build wealth and give.";
            default -> throw new IllegalArgumentException("stage must be between 1 and 7");
        };
    }

    private static String verdictSub(
            StageEvaluation current,
            FinancialSnapshot snapshot,
            PayoffSchedule schedule,
            Optional<SnowballComparison> mortgageComparison) {
        Money surplus = snapshot.monthlySurplus();
        return switch (current.stage()) {
            case 1 -> "Save $1,000 before anything else.";
            case 2 -> stage2VerdictSub(schedule);
            case 3 -> surplusSentence(current.remainingAmount(), surplus);
            case 4 -> "You're at " + Formats.plainPercent(snapshot.retirementContributionPercent()) + "%.";
            case 5 -> "Set aside something every month.";
            case 6 -> stage6VerdictSub(current, mortgageComparison);
            case 7 -> "Every earlier stage is done or skipped.";
            default -> throw new IllegalArgumentException("stage must be between 1 and 7");
        };
    }

    /**
     * Returns the stage 2 verdict's sub line: the debt-free month the debt snowball reaches, and about how many
     * months from now that is (counting {@code schedule}'s own {@code asOf} month as month one), or the 50-year
     * giveup sentence when the debts never clear within the schedule.
     */
    private static String stage2VerdictSub(PayoffSchedule schedule) {
        Optional<YearMonth> debtFreeMonth = schedule.debtFreeMonth();
        if (debtFreeMonth.isEmpty()) {
            return "At these payments your debts won't be paid off within 50 years.";
        }
        YearMonth date = debtFreeMonth.get();
        long monthsFromNow = ChronoUnit.MONTHS.between(schedule.asOf(), date) + 1;
        String word = monthsFromNow == 1 ? "month" : "months";
        return "With the debt snowball you're free of everything but the house by " + Formats.monthYear(date)
                + ", about " + monthsFromNow + " " + word + " from now.";
    }

    /**
     * Returns the stage 6 verdict's sub line: the debt snowball's payoff date, with how much sooner it is than the
     * current payment alone when that has a date to compare against; falls back to the stage 6 line itself when the
     * snowball doesn't close the mortgage sooner than the current payment.
     */
    private static String stage6VerdictSub(StageEvaluation current, Optional<SnowballComparison> mortgageComparison) {
        if (mortgageComparison.isEmpty()) {
            return stage6Line(current, mortgageComparison);
        }
        SnowballComparison found = mortgageComparison.get();
        return found.duration() != null
                ? "With the debt snowball it's paid off in " + Formats.monthYear(found.date()) + ", "
                        + found.duration() + " sooner than your payment alone."
                : "With the debt snowball it's paid off in " + Formats.monthYear(found.date()) + ".";
    }

    /** Returns the surplus sentence for stages 2 and 3, or the alternate sentence when the surplus is $0 or below. */
    private static String surplusSentence(Money shortfall, Money surplus) {
        if (!surplus.isPositive()) {
            return "Right now your expenses and minimums use up your take-home pay.";
        }
        long months = ceilDiv(shortfall.cents(), surplus.cents());
        String word = months == 1 ? "month" : "months";
        return "At your current surplus of " + Formats.money(surplus) + " a month, that's about " + months + " "
                + word + " of work.";
    }

    /** Returns {@code numerator / denominator}, rounded up; both are positive cents. */
    private static long ceilDiv(long numerator, long denominator) {
        return (numerator + denominator - 1) / denominator;
    }
}
