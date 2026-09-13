package com.rungs.readiness.domain.stage;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.FinancialSnapshotBuilder;
import com.rungs.readiness.domain.model.HouseholdFlags;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MortgageEvaluatorTests {

    private static final HouseholdFlags OWNS = new HouseholdFlags(false, false, false);

    private static final HouseholdFlags RENTS = new HouseholdFlags(true, false, false);

    private final MortgageEvaluator evaluator = new MortgageEvaluator();

    @Test
    void decidesStageSix() {
        assertThat(evaluator.stage()).isEqualTo(6);
    }

    @Test
    void isSkippedForARenterEvenWithAMortgage() {
        StageEvaluation result = evaluate(RENTS, mortgage(25_000_000, 40_000_000));

        assertThat(result).isEqualTo(StageEvaluation.skipped(6, "You rent, so there's no mortgage to pay down."));
    }

    @Test
    void isSatisfiedForAHomeownerWithNoMortgage() {
        StageEvaluation result = evaluate(OWNS, null);

        assertThat(result).isEqualTo(StageEvaluation.satisfied(6));
    }

    @Test
    void isSatisfiedWhenTheMortgageBalanceIsZero() {
        StageEvaluation result = evaluate(OWNS, mortgage(0, 40_000_000));

        assertThat(result).isEqualTo(StageEvaluation.satisfied(6));
    }

    @Test
    void isInProgressWithHomeEquityAsProgressAndTheBalanceRemaining() {
        StageEvaluation result = evaluate(OWNS, mortgage(25_000_000, 40_000_000));

        assertThat(result).isEqualTo(StageEvaluation.inProgress(
                6, new BigDecimal("37.5"), Money.ofDollars(250_000), "You have $250,000.00 left on your mortgage."));
    }

    /** Columns: balance in cents, home value in cents, and the displayed balance. */
    @ParameterizedTest
    @CsvSource({
        "10000000, 0, '$100,000.00'",
        "40000000, 40000000, '$400,000.00'",
        "45000000, 40000000, '$450,000.00'",
        "99999999, 100000000, '$999,999.99'"
    })
    void isNotStartedWithTheBalanceRemainingWhenEquityIsNoneOrRoundsToZero(
            long balanceCents, long homeValueCents, String balance) {
        StageEvaluation result = evaluate(OWNS, mortgage(balanceCents, homeValueCents));

        assertThat(result).isEqualTo(StageEvaluation.notStarted(
                6, Money.ofCents(balanceCents), "You have " + balance + " left on your mortgage."));
    }

    private static Mortgage mortgage(long balanceCents, long homeValueCents) {
        return new Mortgage(
                Money.ofCents(balanceCents),
                Money.ofDollars(1_800),
                Money.ofCents(homeValueCents),
                new BigDecimal("6.125"));
    }

    private StageEvaluation evaluate(HouseholdFlags flags, Mortgage mortgage) {
        FinancialSnapshot snapshot =
                FinancialSnapshotBuilder.aSnapshot().withHouseholdFlags(flags).withMortgage(mortgage).build();
        return evaluator.evaluate(snapshot, List.of());
    }
}
