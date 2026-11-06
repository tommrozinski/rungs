package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.projection.MortgagePayoff;
import com.rungs.readiness.domain.stage.FullEmergencyFundEvaluator;
import com.rungs.readiness.domain.stage.RetirementEvaluator;
import com.rungs.readiness.domain.stage.StageEvaluation;
import com.rungs.readiness.domain.stage.StageStatus;
import com.rungs.readiness.domain.stage.StarterFundEvaluator;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * One of the result page's seven stages, in ladder order: its number, title, whether its row ever shows a progress
 * bar, how a raw {@link StageStatus} displays for it, and its own copy — the row's small/blocker line and, when it
 * is the current stage, the verdict's headline and sub line. Each constant owns its stage's rules in one place
 * rather than the ladder's seven stages being spread across parallel switches and lists.
 */
enum Stage {

    STARTER_FUND(1, "Save $1,000 to start", true) {
        @Override
        StageLine line(StageContext context) {
            return StageLine.plain(Formats.money(context.snapshot().starterEmergencyFund()) + " of "
                    + Formats.money(StarterFundEvaluator.TARGET) + ".");
        }

        @Override
        String verdictLine(StageContext context) {
            return Formats.money(context.evaluation().remainingAmount()) + " more and your starter fund is done.";
        }

        @Override
        String verdictSub(StageContext context) {
            return "Save " + Formats.money(StarterFundEvaluator.TARGET) + " before anything else.";
        }
    },

    DEBT_FREE(2, "Pay off everything but the house", false) {
        @Override
        StageLine line(StageContext context) {
            StageEvaluation evaluation = context.evaluation();
            if (evaluation.status() == StageStatus.SATISFIED) {
                return StageLine.plain("No debts besides the house.");
            }
            int count = context.snapshot().debts().size();
            String noun = count == 1 ? "debt" : "debts";
            String verb = count == 1 ? "remains" : "remain";
            String text =
                    Formats.capitalize(Formats.spellOrDigits(count)) + " " + noun + " " + verb + ", totalling ";
            return new StageLine(text, Formats.money(evaluation.remainingAmount()), ".");
        }

        @Override
        String verdictLine(StageContext context) {
            return Formats.money(context.evaluation().remainingAmount()) + " stands between you and debt freedom.";
        }

        @Override
        String verdictSub(StageContext context) {
            Optional<YearMonth> debtFreeMonth = context.schedule().debtFreeMonth();
            if (debtFreeMonth.isEmpty()) {
                return "At these payments your debts won't be paid off within 50 years.";
            }
            YearMonth date = debtFreeMonth.get();
            long monthsFromNow = ChronoUnit.MONTHS.between(context.schedule().asOf(), date) + 1;
            String word = monthsFromNow == 1 ? "month" : "months";
            return "With the debt snowball you're free of everything but the house by " + Formats.monthYear(date)
                    + ", about " + monthsFromNow + " " + word + " from now.";
        }
    },

    FULL_EMERGENCY_FUND(3, "Save three months of expenses", true) {
        @Override
        StageLine line(StageContext context) {
            FinancialSnapshot snapshot = context.snapshot();
            Money target = FullEmergencyFundEvaluator.target(snapshot);
            return StageLine.plain(
                    Formats.money(snapshot.fullEmergencyFund()) + " of " + Formats.money(target) + ".");
        }

        @Override
        String verdictLine(StageContext context) {
            return Formats.money(context.evaluation().remainingAmount())
                    + " more and three months of expenses are covered.";
        }

        @Override
        String verdictSub(StageContext context) {
            return surplusSentence(context.evaluation().remainingAmount(), context.snapshot().monthlySurplus());
        }
    },

    RETIREMENT(4, "Put 15% toward retirement", true) {
        @Override
        StageLine line(StageContext context) {
            StageEvaluation evaluation = context.evaluation();
            FinancialSnapshot snapshot = context.snapshot();
            if (evaluation.status() == StageStatus.SKIPPED) {
                return StageLine.plain("You told us you're retired.");
            }
            String percentText = Formats.plainPercent(snapshot.retirementContributionPercent());
            if (evaluation.status() == StageStatus.SATISFIED) {
                return StageLine.plain("You're at " + percentText + "%.");
            }
            BigDecimal remainingPoints =
                    RetirementEvaluator.TARGET_PERCENT.subtract(snapshot.retirementContributionPercent());
            String noun = remainingPoints.compareTo(BigDecimal.ONE) == 0 ? "point" : "points";
            return StageLine.plain("You're at " + percentText + "%. "
                    + Formats.capitalize(Formats.spellOrDigits(remainingPoints)) + " " + noun + " to go.");
        }

        @Override
        String verdictLine(StageContext context) {
            return "Raise retirement saving to " + Formats.plainPercent(RetirementEvaluator.TARGET_PERCENT) + "%.";
        }

        @Override
        String verdictSub(StageContext context) {
            return "You're at " + Formats.plainPercent(context.snapshot().retirementContributionPercent()) + "%.";
        }
    },

    COLLEGE(5, "Save for college", false) {
        @Override
        StageLine line(StageContext context) {
            StageEvaluation evaluation = context.evaluation();
            FinancialSnapshot snapshot = context.snapshot();
            if (evaluation.status() == StageStatus.SATISFIED) {
                return StageLine.plain("You're saving for college.");
            }
            if (evaluation.status() == StageStatus.SKIPPED) {
                return StageLine.plain(snapshot.householdFlags().intendsToFundCollege()
                        ? "You told us you're retired."
                        : "You told us you're not funding college.");
            }
            return StageLine.plain("You plan to fund college.");
        }

        @Override
        String verdictLine(StageContext context) {
            return "Start saving for college.";
        }

        @Override
        String verdictSub(StageContext context) {
            return "Set aside something every month.";
        }
    },

    MORTGAGE(6, "Pay off the house", false) {
        @Override
        StageLine line(StageContext context) {
            return StageLine.plain(mortgageLine(context));
        }

        @Override
        String verdictLine(StageContext context) {
            return "Pay off the house.";
        }

        @Override
        String verdictSub(StageContext context) {
            Optional<SnowballComparison> mortgageComparison = context.mortgageComparison();
            if (mortgageComparison.isEmpty()) {
                return mortgageLine(context);
            }
            SnowballComparison found = mortgageComparison.get();
            return found.duration() != null
                    ? "With the debt snowball it's paid off in " + Formats.monthYear(found.date()) + ", "
                            + found.duration() + " sooner than your payment alone."
                    : "With the debt snowball it's paid off in " + Formats.monthYear(found.date()) + ".";
        }
    },

    WEALTH(7, "Build wealth and give", false) {
        @Override
        StageLine line(StageContext context) {
            return StageLine.plain(context.evaluation().status() == StageStatus.SATISFIED
                    ? "Every earlier stage is done or skipped."
                    : "Opens once stages 1 through 6 are done or skipped.");
        }

        @Override
        String verdictLine(StageContext context) {
            return "Build wealth and give.";
        }

        @Override
        String verdictSub(StageContext context) {
            return "Every earlier stage is done or skipped.";
        }

        @Override
        StageStatus displayStatus(StageStatus raw) {
            // Stage 7 has no "in progress" presentation of its own: anything short of SATISFIED reads as Not
            // started, even though WealthBuildingEvaluator reports IN_PROGRESS once some earlier stages are done.
            return raw == StageStatus.SATISFIED ? raw : StageStatus.NOT_STARTED;
        }
    };

    private final int number;
    private final String title;
    private final boolean hasBar;

    Stage(int number, String title, boolean hasBar) {
        this.number = number;
        this.title = title;
        this.hasBar = hasBar;
    }

    /** Returns the stage numbered {@code number}, 1 to 7; throws IllegalArgumentException otherwise. */
    static Stage of(int number) {
        for (Stage stage : values()) {
            if (stage.number == number) {
                return stage;
            }
        }
        throw new IllegalArgumentException("stage must be between 1 and 7");
    }

    int number() {
        return number;
    }

    String title() {
        return title;
    }

    /** Whether this stage's row ever shows a progress bar (only while {@code IN_PROGRESS}). */
    boolean hasBar() {
        return hasBar;
    }

    /** Returns how {@code raw} displays for this stage; every stage but {@link #WEALTH} shows it unchanged. */
    StageStatus displayStatus(StageStatus raw) {
        return raw;
    }

    /**
     * Returns this stage's row copy: the small text when it is not open, or the blocker paragraph's text when it
     * is the open, current stage.
     */
    abstract StageLine line(StageContext context);

    /** Returns this stage's verdict headline, shown only while it is the current stage. */
    abstract String verdictLine(StageContext context);

    /** Returns this stage's verdict sub line, shown only while it is the current stage. */
    abstract String verdictSub(StageContext context);

    /**
     * Returns stage 6's row/verdict text: "Your house is paid off." or the renter's skip sentence when satisfied or
     * skipped; otherwise the current-payment payoff sentence from {@code context}'s {@code currentPayoff}, with the
     * debt-snowball comparison appended when there is one.
     */
    private static String mortgageLine(StageContext context) {
        StageEvaluation evaluation = context.evaluation();
        if (evaluation.status() == StageStatus.SATISFIED) {
            return "Your house is paid off.";
        }
        if (evaluation.status() == StageStatus.SKIPPED) {
            return "You rent, so there's no mortgage to pay down.";
        }
        String currentPaymentSentence = currentPaymentLine(context.currentPayoff().orElseThrow());
        return currentPaymentSentence + context.mortgageComparison().map(Stage::snowballSuffix).orElse("");
    }

    /**
     * Returns the current-payment payoff sentence for {@code payoff}, formatted for display: whether the payment
     * covers the loan's interest, and when it does, when the loan pays off at that payment, or that it doesn't
     * within 50 years.
     */
    private static String currentPaymentLine(MortgagePayoff payoff) {
        return switch (payoff) {
            case MortgagePayoff.NotCovered notCovered -> "Your " + Formats.money(notCovered.payment())
                    + " payment doesn't cover the " + Formats.money(notCovered.firstMonthInterest())
                    + " monthly interest.";
            case MortgagePayoff.Capped ignored -> "At your current payment it won't be paid off within 50 years.";
            case MortgagePayoff.PaysOff paysOff ->
                    "At your current payment it's paid off in " + Formats.monthYear(paysOff.month()) + ".";
        };
    }

    /** Returns the stage 6 line's trailing sentence for {@code comparison}, with its leading space. */
    private static String snowballSuffix(SnowballComparison comparison) {
        return comparison.duration() != null
                ? " With the debt snowball it's paid off in " + Formats.monthYear(comparison.date()) + ", "
                        + comparison.duration() + " sooner."
                : " With the debt snowball it's paid off in " + Formats.monthYear(comparison.date()) + ".";
    }

    /**
     * Returns the surplus sentence for a remaining-amount stage, or the alternate sentence when the surplus is $0
     * or below.
     */
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
