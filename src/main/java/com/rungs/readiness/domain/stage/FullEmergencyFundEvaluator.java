package com.rungs.readiness.domain.stage;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.util.List;

/**
 * Stage 3: a full emergency fund of the household's months of essential costs, including housing, capped at six
 * months. Satisfied at the target or above, including a $0 target; below it, progress is the fund as a share of the
 * target and the remaining amount is the shortfall.
 */
public final class FullEmergencyFundEvaluator implements StageEvaluator {

    private static final int STAGE = 3;

    /** Most months of expenses the target is measured against. */
    private static final int MAX_MONTHS = 6;

    private static final String BLOCKER = "Your emergency fund is %s short of a %s-month target of %s.";

    @Override
    public int stage() {
        return STAGE;
    }

    /**
     * Returns the full emergency fund target for {@code snapshot}: its monthly essential costs, including housing,
     * times its {@code emergencyFundMonths}, capped at {@link #MAX_MONTHS}. Public so callers that need the same
     * target this evaluator is measured against, such as the web layer's copy, compute it identically rather than
     * a diverging, uncapped, version of their own.
     */
    public static Money target(FinancialSnapshot snapshot) {
        int months = Math.min(snapshot.emergencyFundMonths(), MAX_MONTHS);
        // Exact: the factor is a whole number, so no rounding happens.
        return snapshot.monthlyEssentialCosts().times(BigDecimal.valueOf(months));
    }

    @Override
    public StageEvaluation evaluate(FinancialSnapshot snapshot, List<StageEvaluation> lowerStages) {
        int months = Math.min(snapshot.emergencyFundMonths(), MAX_MONTHS);
        Money target = target(snapshot);
        Money fund = snapshot.fullEmergencyFund();
        // Also covers a $0 target, so Percentages is never asked for a share of zero.
        if (fund.compareTo(target) >= 0) {
            return StageEvaluation.satisfied(STAGE);
        }
        Money shortfall = target.minus(fund);
        String blocker = BLOCKER.formatted(shortfall.toDisplayString(), months, target.toDisplayString());
        BigDecimal percent = Percentages.of(fund, target);
        // Status follows progress, so a fund that rounds to 0.0 is NOT_STARTED.
        if (percent.signum() == 0) {
            return StageEvaluation.notStarted(STAGE, shortfall, blocker);
        }
        return StageEvaluation.inProgress(STAGE, percent, shortfall, blocker);
    }
}
