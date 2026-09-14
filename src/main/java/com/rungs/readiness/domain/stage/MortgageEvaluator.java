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
 * started, with the balance remaining and a blocker describing whether the payment even covers the interest and,
 * when it does, when the loan pays off at this payment and how that compares to its stated maturity
 * (design.md 039 section 2). It reads only the housing and the assessment month, never the debts.
 */
public final class MortgageEvaluator implements StageEvaluator {

    private static final int STAGE = 6;

    private static final String RENTS_REASON = "You rent, so there's no mortgage to pay down.";

    private static final String NOT_COVERED =
            "After property taxes, your %s payment doesn't cover the %s monthly interest.";

    private static final String CAPPED = "This payment won't pay the loan off within 50 years.";

    private static final String PAYOFF_LINE = "Paid off in %s at this payment, %s from now.";

    private static final String MATCHES_LINE = "That matches your loan's %s maturity.";

    private static final String AHEAD_LINE = "That's %s ahead of your %s maturity.";

    private static final String BEHIND_LINE = "That's later than your %s maturity, so check the balance, rate, or payment.";

    private static final String[] MONTH_WORD = {"month", "months"};

    private static final String[] YEAR_WORD = {"year", "years"};

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
        String blocker = blocker(payoff, mortgage.maturity());
        return StageEvaluation.notStarted(STAGE, mortgage.balance(), blocker);
    }

    private static String blocker(MortgagePayoff payoff, YearMonth maturity) {
        if (!payoff.covered()) {
            return NOT_COVERED.formatted(
                    payoff.principalAndInterest().toDisplayString(), payoff.firstMonthInterest().toDisplayString());
        }
        if (payoff.monthsToPayoff() == null) {
            return CAPPED;
        }
        String payoffLine =
                PAYOFF_LINE.formatted(monthYear(payoff.payoffMonth()), duration(payoff.monthsToPayoff()));
        String follow = maturityFollowUp(payoff, maturity);
        return follow == null ? payoffLine : payoffLine + " " + follow;
    }

    private static String maturityFollowUp(MortgagePayoff payoff, YearMonth maturity) {
        return switch (payoff.maturityComparison()) {
            case UNKNOWN -> null;
            case MATCHES -> MATCHES_LINE.formatted(monthYear(maturity));
            case AHEAD -> AHEAD_LINE.formatted(duration(payoff.monthsDifference()), monthYear(maturity));
            case BEHIND -> BEHIND_LINE.formatted(monthYear(maturity));
        };
    }

    /** Returns {@code month} as e.g. "March 2041". */
    private static String monthYear(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.US) + " " + month.getYear();
    }

    /** Returns {@code totalMonths} as "Y years M months", omitting a zero part, singular for exactly one. */
    private static String duration(int totalMonths) {
        int years = totalMonths / 12;
        int months = totalMonths % 12;
        if (years == 0) {
            return months + " " + word(MONTH_WORD, months);
        }
        if (months == 0) {
            return years + " " + word(YEAR_WORD, years);
        }
        return years + " " + word(YEAR_WORD, years) + " " + months + " " + word(MONTH_WORD, months);
    }

    /** Returns {@code words[0]} for exactly one, else {@code words[1]}. */
    private static String word(String[] words, int count) {
        return count == 1 ? words[0] : words[1];
    }
}
