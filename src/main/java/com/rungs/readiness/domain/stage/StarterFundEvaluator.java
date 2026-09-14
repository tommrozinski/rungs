package com.rungs.readiness.domain.stage;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.util.List;

/**
 * Stage 1: a starter emergency fund of $1,000.00. Satisfied at the target or above; below it, progress is the fund as
 * a share of the target and the remaining amount is the shortfall.
 */
public final class StarterFundEvaluator implements StageEvaluator {

    private static final int STAGE = 1;

    /** The fixed starter fund target; not configurable. */
    private static final Money TARGET = Money.ofDollars(1_000);

    private static final String BLOCKER = "Your starter emergency fund is %s short of %s.";

    @Override
    public int stage() {
        return STAGE;
    }

    @Override
    public StageEvaluation evaluate(FinancialSnapshot snapshot, List<StageEvaluation> lowerStages) {
        Money fund = snapshot.starterEmergencyFund();
        if (fund.compareTo(TARGET) >= 0) {
            return StageEvaluation.satisfied(STAGE);
        }
        Money shortfall = TARGET.minus(fund);
        String blocker = BLOCKER.formatted(shortfall.toDisplayString(), TARGET.toDisplayString());
        BigDecimal percent = Percentages.of(fund, TARGET);
        // Status follows progress, so a fund that rounds to 0.0 is NOT_STARTED.
        if (percent.signum() == 0) {
            return StageEvaluation.notStarted(STAGE, shortfall, blocker);
        }
        return StageEvaluation.inProgress(STAGE, percent, shortfall, blocker);
    }
}
