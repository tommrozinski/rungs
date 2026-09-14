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
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds the result page's view model from a {@link LadderResult} and the {@link FinancialSnapshot} it was computed
 * from. The copy lives in {@link StageCopy} and the status-to-style mapping in {@link StatusStyle}, so this class
 * stays about assembling the view: only the evaluators' status, percent, and remaining-amount data drive it, plus
 * the snapshot's own figures where the table needs a raw value (the actual fund balance, the emergency-fund target,
 * the household flags).
 */
final class ResultViews {

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
                StageCopy.verdictLine(current),
                StageCopy.verdictSub(current, snapshot, schedule, mortgageComparison),
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
        StatusStyle style = StatusStyle.of(displayStatus);

        boolean showBar = (stage == 1 || stage == 3 || stage == 4) && evaluation.status() == StageStatus.IN_PROGRESS;
        int barWidthPercent = showBar
                ? evaluation.percentComplete().setScale(0, RoundingMode.DOWN).intValueExact()
                : 0;

        StageLine line = StageCopy.line(stage, evaluation, snapshot, mortgageComparison);

        return new StageView(
                stage,
                StageCopy.TITLES[stage - 1],
                style.chipLabel(),
                style.chipClass(),
                style.discClass(open),
                style.discContent(stage),
                open,
                line.text(),
                line.emphasis(),
                line.suffix(),
                showBar,
                barWidthPercent);
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
}
