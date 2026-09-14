package com.rungs.readiness.domain.stage;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Mortgage;
import com.rungs.readiness.domain.model.MortgagePayoff;
import com.rungs.readiness.domain.model.MortgagePayoffCalculator;
import com.rungs.readiness.domain.model.Rent;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Stage 6: the home mortgage paid off. Renters skip it. A $0 balance satisfies it. Otherwise the stage is always Not
 * started, with the balance remaining and a blocker describing whether the current payment even covers the interest
 * and, when it does, when the loan pays off at that payment (design.md 041 section 3). The debt-snowball comparison
 * needs the payoff schedule, so it is appended by {@code ResultViews}, not here. It reads only the housing and the
 * assessment month, never the debts.
 */
public final class MortgageEvaluator implements StageEvaluator {

    private static final int STAGE = 6;

    private static final String RENTS_REASON = "You rent, so there's no mortgage to pay down.";

    private static final String NOT_COVERED = "Your %s payment doesn't cover the %s monthly interest.";

    private static final String CAPPED = "At your current payment it won't be paid off within 50 years.";

    private static final String PAYOFF_LINE = "At your current payment it's paid off in %s.";

    @Override
    public int stage() {
        return STAGE;
    }

    @Override
    public StageEvaluation evaluate(FinancialSnapshot snapshot, List<StageEvaluation> lowerStages) {
        return switch (snapshot.housing()) {
            case Rent ignored -> StageEvaluation.skipped(STAGE, RENTS_REASON);
            case Mortgage mortgage -> evaluateMortgage(mortgage, snapshot.asOf());
        };
    }

    private static StageEvaluation evaluateMortgage(Mortgage mortgage, YearMonth asOf) {
        if (mortgage.balance().isZero()) {
            return StageEvaluation.satisfied(STAGE);
        }
        MortgagePayoff payoff = MortgagePayoffCalculator.payoff(mortgage, asOf);
        return StageEvaluation.notStarted(STAGE, mortgage.balance(), blocker(payoff));
    }

    private static String blocker(MortgagePayoff payoff) {
        if (!payoff.covered()) {
            return NOT_COVERED.formatted(
                    payoff.principalAndInterest().toDisplayString(), payoff.firstMonthInterest().toDisplayString());
        }
        if (payoff.monthsToPayoff() == null) {
            return CAPPED;
        }
        return PAYOFF_LINE.formatted(monthYear(payoff.payoffMonth()));
    }

    /** Returns {@code month} as e.g. "March 2041". */
    private static String monthYear(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.US) + " " + month.getYear();
    }
}
