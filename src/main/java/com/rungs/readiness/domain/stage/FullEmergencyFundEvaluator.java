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

    /** Most months of expenses the target is measured against (REQ-005). */
    private static final int MAX_MONTHS = 6;

    private static final String BLOCKER = "Your emergency fund is %s short of a %s-month target of %s.";

    @Override
    public int stage() {
        return STAGE;
    }

    @Override
    public StageEvaluation evaluate(FinancialSnapshot snapshot, List<StageEvaluation> lowerStages) {
        int months = Math.min(snapshot.emergencyFundMonths(), MAX_MONTHS);
        // Exact: the factor is a whole number, so no rounding happens.
        Money target = snapshot.monthlyEssentialCosts().times(BigDecimal.valueOf(months));
        Money fund = snapshot.fullEmergencyFund();
        // Also covers a $0 target, so Percentages is never asked for a share of zero.
        if (fund.compareTo(target) >= 0) {
            return StageEvaluation.satisfied(STAGE);
        }
        Money shortfall = target.minus(fund);
        String blocker = BLOCKER.formatted(shortfall.toDisplayString(), months, target.toDisplayString());
        BigDecimal percent = Percentages.of(fund, target);
        // Story 004 REQ-005: status follows progress, so a fund that rounds to 0.0 is NOT_STARTED.
        if (percent.signum() == 0) {
            return StageEvaluation.notStarted(STAGE, shortfall, blocker);
        }
        return StageEvaluation.inProgress(STAGE, percent, shortfall, blocker);
    }
}
