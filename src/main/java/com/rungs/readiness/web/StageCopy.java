package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.projection.PayoffSchedule;
import com.rungs.readiness.domain.stage.StageEvaluation;
import com.rungs.readiness.domain.stage.StageStatus;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * The result page's stage copy: each stage's title, its small/blocker line, and the current stage's verdict
 * headline and sub line. Kept apart from {@link ResultViews} so the seven stages' wording lives in one place, one
 * method per stage, rather than three parallel switches.
 */
final class StageCopy {

    static final String[] TITLES = {
        "Save $1,000 to start",
        "Pay off everything but the house",
        "Save three months of expenses",
        "Put 15% toward retirement",
        "Save for college",
        "Pay off the house",
        "Build wealth and give",
    };

    private static final BigDecimal RETIREMENT_TARGET_PERCENT = BigDecimal.valueOf(15);

    private StageCopy() {
    }

    /** Returns the stage's line: the small text when not open, or the blocker paragraph's text when open. */
    static StageLine line(
            int stage,
            StageEvaluation evaluation,
            FinancialSnapshot snapshot,
            Optional<SnowballComparison> mortgageComparison) {
        return switch (stage) {
            case 1 -> StageLine.plain(Formats.money(snapshot.starterEmergencyFund()) + " of $1,000.");
            case 2 -> stage2Line(evaluation, snapshot);
            case 3 -> StageLine.plain(stage3Line(snapshot));
            case 4 -> StageLine.plain(stage4Line(evaluation, snapshot));
            case 5 -> StageLine.plain(stage5Line(evaluation, snapshot));
            case 6 -> StageLine.plain(stage6Line(evaluation, mortgageComparison));
            case 7 -> StageLine.plain(stage7Line(evaluation));
            default -> throw new IllegalArgumentException("stage must be between 1 and 7");
        };
    }

    private static StageLine stage2Line(StageEvaluation evaluation, FinancialSnapshot snapshot) {
        if (evaluation.status() == StageStatus.SATISFIED) {
            return StageLine.plain("No debts besides the house.");
        }
        int count = snapshot.debts().size();
        String noun = count == 1 ? "debt" : "debts";
        String verb = count == 1 ? "remains" : "remain";
        String text = Formats.capitalize(Formats.spellOrDigits(count)) + " " + noun + " " + verb + ", totalling ";
        return new StageLine(text, Formats.money(evaluation.remainingAmount()), ".");
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
        // appended here, computed once by ResultViews.build() and passed down, rather than being recomputed in the
        // domain.
        return evaluation.blocker() + mortgageComparison.map(StageCopy::snowballSuffix).orElse("");
    }

    /** Returns the stage 6 line's trailing sentence for {@code comparison}, with its leading space. */
    private static String snowballSuffix(SnowballComparison comparison) {
        return comparison.duration() != null
                ? " With the debt snowball it's paid off in " + Formats.monthYear(comparison.date()) + ", "
                        + comparison.duration() + " sooner."
                : " With the debt snowball it's paid off in " + Formats.monthYear(comparison.date()) + ".";
    }

    private static String stage7Line(StageEvaluation evaluation) {
        return evaluation.status() == StageStatus.SATISFIED
                ? "Every earlier stage is done or skipped."
                : "Opens once stages 1 through 6 are done or skipped.";
    }

    static String verdictLine(StageEvaluation current) {
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

    static String verdictSub(
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
