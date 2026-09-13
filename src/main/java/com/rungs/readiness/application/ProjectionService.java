package com.rungs.readiness.application;

import com.rungs.readiness.application.response.MoneyResponse;
import com.rungs.readiness.application.response.MonthlyPeriodResponse;
import com.rungs.readiness.application.response.ProjectionDebtResponse;
import com.rungs.readiness.application.response.ProjectionResponse;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.projection.PayoffSimulation;
import com.rungs.readiness.domain.projection.PayoffSimulator;
import com.rungs.readiness.domain.projection.SimulationInput;
import com.rungs.readiness.domain.projection.Strategy;
import com.rungs.readiness.domain.stage.LadderResult;
import com.rungs.readiness.domain.stage.StageLadder;
import java.util.List;
import java.util.Objects;

/**
 * Runs a submitted snapshot's payoff plan (story 010): normalizes it through the story 007 input model, finds the
 * current stage with the shared {@link StageLadder}, and runs {@link PayoffSimulator} with the snapshot's debts,
 * {@link FinancialSnapshot#monthlySurplus()}, the extra payment, the chosen strategy's ordering, and the horizon
 * (REQ-004).
 */
public final class ProjectionService {

    /** The strategy used when none is submitted (REQ-003). */
    static final Strategy DEFAULT_STRATEGY = Strategy.SNOWBALL;

    /** The horizon used when none is submitted (REQ-003). */
    static final int DEFAULT_HORIZON_MONTHS = 360;

    /** The only current stage {@code monthsToNextStage} is populated for; any other stage gets null (REQ-005). */
    private static final int STAGE_WITH_NEXT_STAGE = 2;

    private final StageLadder ladder;

    private final PayoffSimulator simulator = new PayoffSimulator();

    /** Creates a service over {@code ladder}; throws NullPointerException if it is null. */
    public ProjectionService(StageLadder ladder) {
        this.ladder = Objects.requireNonNull(ladder, "ladder");
    }

    /**
     * Returns the projection response for {@code input} after FR-4 normalization and the REQ-003 defaults. Throws
     * NullPointerException for null input, and IllegalArgumentException for input {@link SnapshotInput#toSnapshot}
     * cannot normalize.
     */
    public ProjectionResponse project(ProjectionInput input) {
        Objects.requireNonNull(input, "input");
        FinancialSnapshot snapshot = input.snapshot().toSnapshot();
        LadderResult ladderResult = ladder.assess(snapshot);

        Strategy strategy = input.strategy() == null ? DEFAULT_STRATEGY : Strategy.valueOf(input.strategy());
        int horizon = input.horizonMonths() == null ? DEFAULT_HORIZON_MONTHS : input.horizonMonths();
        Money extra = money(input.extraMonthlyPayment());

        SimulationInput simulationInput = new SimulationInput(
                snapshot.debts(), snapshot.monthlySurplus(), extra, strategy.ordering(), horizon);
        PayoffSimulation simulation = simulator.simulate(simulationInput);

        Integer monthsToNextStage = ladderResult.currentStage() == STAGE_WITH_NEXT_STAGE
                ? simulation.monthsToDebtFreedom()
                : null;

        List<ProjectionDebtResponse> debts =
                snapshot.debts().stream().map(ProjectionDebtResponse::from).toList();
        List<MonthlyPeriodResponse> schedule =
                simulation.schedule().stream().map(MonthlyPeriodResponse::from).toList();

        return new ProjectionResponse(
                monthsToNextStage,
                simulation.monthsToDebtFreedom(),
                MoneyResponse.of(simulation.totalInterestPaid()),
                simulation.truncated(),
                simulation.unpayableDebtIds(),
                debts,
                schedule,
                List.of());
    }

    private static Money money(String text) {
        return InputText.isBlank(text) ? Money.ZERO : Money.parse(text);
    }
}
