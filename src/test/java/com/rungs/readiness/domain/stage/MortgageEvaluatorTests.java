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

/** {@link MortgageEvaluator}, stage 6. */
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
        Mortgage paidOff = new Mortgage(Money.ZERO, BigDecimal.ZERO, Money.ZERO);

        StageEvaluation result = evaluate(paidOff);

        assertThat(result).isEqualTo(StageEvaluation.satisfied(6));
    }

    @Test
    void isNotStartedWhenThePaymentDoesNotCoverTheFirstMonthInterest() {
        Mortgage mortgage = new Mortgage(Money.ofDollars(100_000), new BigDecimal("12"), Money.ofDollars(900));

        StageEvaluation result = evaluate(mortgage);

        assertThat(result).isEqualTo(StageEvaluation.notStarted(
                6,
                Money.ofDollars(100_000),
                "Your $900.00 payment doesn't cover the $1,000.00 monthly interest."));
    }

    @Test
    void isNotStartedWithThePayoffLineWhenThePaymentCoversInterestAndFinishesInTime() {
        // Hand-verified: balance $500 at 12% annual, $260 payment, no taxes, pays off after 2 payments, the first
        // one due in AS_OF itself (see MortgagePayoffCalculatorTests).
        Mortgage mortgage = new Mortgage(Money.ofDollars(500), new BigDecimal("12"), Money.ofDollars(260));

        StageEvaluation result = evaluate(mortgage);

        assertThat(result).isEqualTo(StageEvaluation.notStarted(
                6, Money.ofDollars(500), "At your current payment it's paid off in February 2024."));
    }

    @Test
    void isNotStartedWithTheCappedSentenceWhenTheLoanRunsPastSixHundredMonths() {
        // See MortgagePayoffCalculatorTests: covered, but never amortizes within 600 months.
        Mortgage mortgage = new Mortgage(Money.ofDollars(500_000), new BigDecimal("20"), Money.ofCents(833_340));

        StageEvaluation result = evaluate(mortgage);

        assertThat(result).isEqualTo(StageEvaluation.notStarted(
                6, Money.ofDollars(500_000), "At your current payment it won't be paid off within 50 years."));
    }

    private StageEvaluation evaluate(Housing housing) {
        FinancialSnapshot snapshot =
                FinancialSnapshotBuilder.aSnapshot().withHousing(housing).withAsOf(AS_OF).build();
        return evaluator.evaluate(snapshot, List.of());
    }
}
