package com.rungs.readiness.domain.stage;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import java.util.List;

/**
 * Stage 2: every consumer debt paid off. The mortgage is never among the debts. Progress cannot be measured, so an
 * unmet stage is {@code IN_PROGRESS} at 0.0, as an explicit exception to the usual progress rule, with the total
 * balance remaining.
 */
public final class DebtFreeEvaluator implements StageEvaluator {

    private static final int STAGE = 2;

    /** Every placeholder is %s, so the count's digits do not depend on the default locale. */
    private static final String BLOCKER = "You have %s of debt across %s %s.";

    private static final String DEBT_SINGULAR = "debt";

    private static final String DEBT_PLURAL = "debts";

    @Override
    public int stage() {
        return STAGE;
    }

    @Override
    public StageEvaluation evaluate(FinancialSnapshot snapshot, List<StageEvaluation> lowerStages) {
        int count = snapshot.debts().size();
        if (count == 0) {
            return StageEvaluation.satisfied(STAGE);
        }
        Money total = snapshot.totalDebtBalance();
        String blocker = BLOCKER.formatted(total.toDisplayString(), count, count == 1 ? DEBT_SINGULAR : DEBT_PLURAL);
        return StageEvaluation.inProgress(STAGE, Percentages.ZERO_PERCENT, total, blocker);
    }
}
