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
 * from. Each stage's copy and display rules live in {@link Stage} and the status-to-style mapping in
 * {@link StatusStyle}, so this class stays about assembling the view: only the evaluators' status, percent, and
 * remaining-amount data drive it, plus the snapshot's own figures where the table needs a raw value (the actual
 * fund balance, the emergency-fund target, the household flags).
 */
final class ResultViews {

    private static final int STAGE_COUNT = Stage.values().length;

    private ResultViews() {
    }

    /**
     * Builds the view model for {@code ladder}, the result of assessing {@code snapshot}, whose debt-snowball
     * payoff schedule is {@code schedule}. Computes stage 6's current-payment payoff and its debt-snowball
     * comparison once, up front, since both the stage row and the verdict need them.
     */
    static ResultView build(LadderResult ladder, FinancialSnapshot snapshot, PayoffSchedule schedule) {
        Optional<MortgagePayoff> currentPayoff = currentPayoff(snapshot);
        Optional<SnowballComparison> mortgageComparison = mortgageComparison(currentPayoff, schedule);

        List<StageView> stages = new ArrayList<>(STAGE_COUNT);
        for (Stage stage : Stage.values()) {
            StageEvaluation evaluation = ladder.evaluation(stage.number());
            StageContext context =
                    new StageContext(evaluation, snapshot, schedule, currentPayoff, mortgageComparison);
            stages.add(stageView(stage, context, ladder.currentStage()));
        }

        boolean allDone = ladder.evaluation(STAGE_COUNT).status() == StageStatus.SATISFIED;
        String eyebrow = allDone
                ? "Every stage that applies to you is done"
                : "You're on stage " + ladder.currentStage() + " of " + STAGE_COUNT;

        Stage currentStage = Stage.of(ladder.currentStage());
        StageEvaluation current = ladder.evaluation(ladder.currentStage());
        StageContext currentContext = new StageContext(current, snapshot, schedule, currentPayoff, mortgageComparison);
        long stagesComplete = ladder.stages().stream()
                .filter(evaluation -> evaluation.status() == StageStatus.SATISFIED)
                .count();
        Money surplus = snapshot.monthlySurplus();

        return new ResultView(
                eyebrow,
                currentStage.verdictLine(currentContext),
                currentStage.verdictSub(currentContext),
                Formats.money(surplus),
                surplus.isPositive(),
                Formats.money(snapshot.totalDebtBalance()),
                stagesComplete + " of " + STAGE_COUNT,
                stages);
    }

    private static StageView stageView(Stage stage, StageContext context, int currentStageNumber) {
        StageEvaluation evaluation = context.evaluation();
        boolean open = stage.number() == currentStageNumber;
        StatusStyle style = StatusStyle.of(stage.displayStatus(evaluation.status()));

        boolean showBar = stage.hasBar() && evaluation.status() == StageStatus.IN_PROGRESS;
        int barWidthPercent = showBar
                ? evaluation.percentComplete().setScale(0, RoundingMode.DOWN).intValueExact()
                : 0;

        StageLine line = stage.line(context);

        return new StageView(
                stage.number(),
                stage.title(),
                style.chipLabel(),
                style.chipClass(),
                style.discClass(open),
                style.discContent(stage.number()),
                open,
                line.text(),
                line.emphasis(),
                line.suffix(),
                showBar,
                barWidthPercent);
    }

    /**
     * Returns the household's mortgage payoff at its current payment alone, empty for a renter.
     */
    private static Optional<MortgagePayoff> currentPayoff(FinancialSnapshot snapshot) {
        if (!(snapshot.housing() instanceof Mortgage mortgage)) {
            return Optional.empty();
        }
        return Optional.of(MortgagePayoffCalculator.payoff(mortgage, snapshot.asOf()));
    }

    /**
     * Returns the stage 6 comparison for {@code currentPayoff}, empty for a renter: whether, and how, the debt
     * snowball's mortgage payoff month beats the current-payment payoff. Computed once per request by
     * {@link #build} and threaded through the stage row and the verdict, rather than recomputed by each.
     */
    private static Optional<SnowballComparison> mortgageComparison(
            Optional<MortgagePayoff> currentPayoff, PayoffSchedule schedule) {
        return currentPayoff.flatMap(payoff -> snowballComparison(payoff, schedule.mortgagePayoffMonth()));
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
