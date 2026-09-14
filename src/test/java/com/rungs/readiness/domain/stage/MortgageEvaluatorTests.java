package com.rungs.readiness.domain.stage;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.FinancialSnapshotBuilder;
import com.rungs.readiness.domain.model.Housing;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import com.rungs.readiness.domain.model.Rent;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@link MortgageEvaluator}, stage 6 (design.md 039 section 2). */
class MortgageEvaluatorTests {

    private static final YearMonth AS_OF = YearMonth.of(2024, 1);

    private final MortgageEvaluator evaluator = new MortgageEvaluator();

    @Test
    void decidesStageSix() {
        assertThat(evaluator.stage()).isEqualTo(6);
    }

    @Test
    void isSkippedForARenter() {
        StageEvaluation result = evaluate(new Rent(Money.ofDollars(1_500)));

        assertThat(result).isEqualTo(StageEvaluation.skipped(6, "You rent, so there's no mortgage to pay down."));
    }

    @Test
    void isSatisfiedWhenTheMortgageBalanceIsZero() {
        Mortgage paidOff = new Mortgage(Money.ZERO, BigDecimal.ZERO, Money.ZERO, Money.ofDollars(6_300), null);

        StageEvaluation result = evaluate(paidOff);

        assertThat(result).isEqualTo(StageEvaluation.satisfied(6));
    }

    @Test
    void isNotStartedWhenThePaymentDoesNotCoverTheFirstMonthInterest() {
        Mortgage mortgage = new Mortgage(
                Money.ofDollars(100_000), new BigDecimal("12"), Money.ofDollars(900), Money.ofDollars(4_800), null);

        StageEvaluation result = evaluate(mortgage);

        assertThat(result).isEqualTo(StageEvaluation.notStarted(
                6,
                Money.ofDollars(100_000),
                "After property taxes, your $500.00 payment doesn't cover the $1,000.00 monthly interest."));
    }

    @Test
    void isNotStartedWithThePayoffLineWhenTheMaturityIsUnknown() {
        // Hand-verified: balance $500 at 12% annual, $260 payment, no taxes, pays off in 2 months (see
        // MortgagePayoffCalculatorTests).
        Mortgage mortgage = new Mortgage(Money.ofDollars(500), new BigDecimal("12"), Money.ofDollars(260), Money.ZERO, null);

        StageEvaluation result = evaluate(mortgage);

        assertThat(result).isEqualTo(StageEvaluation.notStarted(
                6, Money.ofDollars(500), "Paid off in March 2024 at this payment, 2 months from now."));
    }

    @Test
    void isNotStartedWithTheMatchesFollowUpWhenThePayoffMonthIsTheMaturity() {
        Mortgage mortgage = new Mortgage(
                Money.ofDollars(500), new BigDecimal("12"), Money.ofDollars(260), Money.ZERO, YearMonth.of(2024, 3));

        StageEvaluation result = evaluate(mortgage);

        assertThat(result.blocker()).isEqualTo(
                "Paid off in March 2024 at this payment, 2 months from now. "
                        + "That matches your loan's March 2024 maturity.");
    }

    @Test
    void isNotStartedWithTheAheadFollowUpWhenThePayoffIsWellBeforeTheMaturity() {
        Mortgage mortgage = new Mortgage(
                Money.ofDollars(500), new BigDecimal("12"), Money.ofDollars(260), Money.ZERO, YearMonth.of(2025, 3));

        StageEvaluation result = evaluate(mortgage);

        assertThat(result.blocker()).isEqualTo(
                "Paid off in March 2024 at this payment, 2 months from now. "
                        + "That's 1 year ahead of your March 2025 maturity.");
    }

    @Test
    void isNotStartedWithTheBehindFollowUpWhenThePayoffIsWellAfterTheMaturity() {
        Mortgage mortgage = new Mortgage(
                Money.ofDollars(500), new BigDecimal("12"), Money.ofDollars(260), Money.ZERO, YearMonth.of(2024, 1));

        StageEvaluation result = evaluate(mortgage);

        assertThat(result.blocker()).isEqualTo(
                "Paid off in March 2024 at this payment, 2 months from now. "
                        + "That's later than your January 2024 maturity, so check the balance, rate, or payment.");
    }

    @Test
    void isNotStartedWithTheCappedSentenceWhenTheLoanRunsPastSixHundredMonths() {
        // See MortgagePayoffCalculatorTests: covered, but never amortizes within 600 months.
        Mortgage mortgage =
                new Mortgage(Money.ofDollars(500_000), new BigDecimal("20"), Money.ofCents(833_340), Money.ZERO, null);

        StageEvaluation result = evaluate(mortgage);

        assertThat(result).isEqualTo(StageEvaluation.notStarted(
                6, Money.ofDollars(500_000), "This payment won't pay the loan off within 50 years."));
    }

    @Test
    void singularDurationWordForExactlyOneYear() {
        // 0% rate, so a $12,000 balance at $1,000 a month pays off in exactly 12 months (1 year, 0 months).
        Mortgage mortgage = new Mortgage(Money.ofDollars(12_000), BigDecimal.ZERO, Money.ofDollars(1_000), Money.ZERO, null);

        StageEvaluation result = evaluate(mortgage);

        assertThat(result.blocker()).isEqualTo("Paid off in January 2025 at this payment, 1 year from now.");
    }

    @Test
    void singularDurationWordsForExactlyOneYearAndOneMonth() {
        // 0% rate, so a $13,000 balance at $1,000 a month pays off in exactly 13 months (1 year, 1 month).
        Mortgage mortgage = new Mortgage(Money.ofDollars(13_000), BigDecimal.ZERO, Money.ofDollars(1_000), Money.ZERO, null);

        StageEvaluation result = evaluate(mortgage);

        assertThat(result.blocker()).isEqualTo("Paid off in February 2025 at this payment, 1 year 1 month from now.");
    }

    private StageEvaluation evaluate(Housing housing) {
        FinancialSnapshot snapshot =
                FinancialSnapshotBuilder.aSnapshot().withHousing(housing).withAsOf(AS_OF).build();
        return evaluator.evaluate(snapshot, List.of());
    }
}
