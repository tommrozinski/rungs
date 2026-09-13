package com.rungs.readiness.domain.stage;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import java.math.BigDecimal;
import java.util.List;

/**
 * Stage 6: the home mortgage paid off. Renters skip it, even when a mortgage is supplied; no mortgage or a $0 balance
 * satisfies it. Otherwise progress is home equity as a share of home value and the balance remains. It reads only the
 * mortgage, never the debts or the monthly payment.
 */
public final class MortgageEvaluator implements StageEvaluator {

    private static final int STAGE = 6;

    private static final String RENTS_REASON = "You rent, so there's no mortgage to pay down.";

    private static final String BLOCKER = "You have %s left on your mortgage.";

    @Override
    public int stage() {
        return STAGE;
    }

    @Override
    public StageEvaluation evaluate(FinancialSnapshot snapshot, List<StageEvaluation> lowerStages) {
        if (snapshot.householdFlags().rents()) {
            return StageEvaluation.skipped(STAGE, RENTS_REASON);
        }
        Mortgage mortgage = snapshot.mortgage();
        if (mortgage == null || mortgage.balance().isZero()) {
            return StageEvaluation.satisfied(STAGE);
        }
        Money balance = mortgage.balance();
        Money homeValue = mortgage.homeValue();
        String blocker = BLOCKER.formatted(balance.toDisplayString());
        // No equity: checked first, since Percentages rejects the negative part an underwater home would give.
        if (homeValue.compareTo(balance) <= 0) {
            return StageEvaluation.notStarted(STAGE, balance, blocker);
        }
        BigDecimal equity = Percentages.of(homeValue.minus(balance), homeValue);
        // Story 004 REQ-005: status follows progress, so equity that rounds to 0.0 is NOT_STARTED.
        if (equity.signum() == 0) {
            return StageEvaluation.notStarted(STAGE, balance, blocker);
        }
        return StageEvaluation.inProgress(STAGE, equity, balance, blocker);
    }
}
