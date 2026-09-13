package com.rungs.readiness.domain.projection;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.Debt;
import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class StrategyTests {

    private static Debt debt(int position, Money balance, String rate) {
        return new Debt(
                "debt-" + position, "Debt " + position, DebtType.OTHER, balance, new BigDecimal(rate), Money.ZERO);
    }

    @Test
    void snowballOrdersAscendingByBalance() {
        Debt highest = debt(1, Money.ofDollars(900), "10");
        Debt lowest = debt(2, Money.ofDollars(100), "5");
        Debt middle = debt(3, Money.ofDollars(500), "20");

        List<Debt> ordered = Strategy.SNOWBALL.ordering().order(List.of(highest, lowest, middle));

        assertThat(ordered).containsExactly(lowest, middle, highest);
    }

    @Test
    void avalancheOrdersDescendingByRate() {
        Debt lowRate = debt(1, Money.ofDollars(100), "5");
        Debt highRate = debt(2, Money.ofDollars(900), "20");
        Debt midRate = debt(3, Money.ofDollars(500), "10");

        List<Debt> ordered = Strategy.AVALANCHE.ordering().order(List.of(lowRate, highRate, midRate));

        assertThat(ordered).containsExactly(highRate, midRate, lowRate);
    }

    @Test
    void snowballBreaksBalanceTiesBySuppliedOrder() {
        Debt first = debt(1, Money.ofDollars(500), "5");
        Debt second = debt(2, Money.ofDollars(500), "20");

        List<Debt> ordered = Strategy.SNOWBALL.ordering().order(List.of(first, second));

        assertThat(ordered).containsExactly(first, second);
    }

    @Test
    void avalancheBreaksRateTiesBySuppliedOrder() {
        Debt first = debt(1, Money.ofDollars(100), "7.25");
        Debt second = debt(2, Money.ofDollars(900), "7.25");

        List<Debt> ordered = Strategy.AVALANCHE.ordering().order(List.of(first, second));

        assertThat(ordered).containsExactly(first, second);
    }

    @Test
    void snowballBreaksAThreeWayBalanceTieBySuppliedOrder() {
        Debt first = debt(1, Money.ofDollars(500), "5");
        Debt second = debt(2, Money.ofDollars(500), "10");
        Debt third = debt(3, Money.ofDollars(500), "20");

        List<Debt> ordered = Strategy.SNOWBALL.ordering().order(List.of(first, second, third));

        assertThat(ordered).containsExactly(first, second, third);
    }

    @Test
    void avalancheBreaksAThreeWayRateTieBySuppliedOrder() {
        Debt first = debt(1, Money.ofDollars(100), "7.25");
        Debt second = debt(2, Money.ofDollars(500), "7.25");
        Debt third = debt(3, Money.ofDollars(900), "7.25");

        List<Debt> ordered = Strategy.AVALANCHE.ordering().order(List.of(first, second, third));

        assertThat(ordered).containsExactly(first, second, third);
    }

    @Test
    void avalancheTreatsRatesDifferingOnlyInScaleAsEqual() {
        Debt scaledLow = debt(1, Money.ofDollars(100), "7.25");
        Debt scaledHigh = debt(2, Money.ofDollars(900), "7.250");

        List<Debt> ordered = Strategy.AVALANCHE.ordering().order(List.of(scaledLow, scaledHigh));

        assertThat(ordered).containsExactly(scaledLow, scaledHigh);
    }

    @Test
    void avalancheDoesNotUseBalanceAsATieBreaker() {
        Debt largerBalance = debt(1, Money.ofDollars(900), "7.25");
        Debt smallerBalance = debt(2, Money.ofDollars(100), "7.25");

        List<Debt> ordered = Strategy.AVALANCHE.ordering().order(List.of(largerBalance, smallerBalance));

        assertThat(ordered).containsExactly(largerBalance, smallerBalance);
    }

    @Test
    void orderingDoesNotMutateOrReorderTheInputList() {
        Debt highest = debt(1, Money.ofDollars(900), "10");
        Debt lowest = debt(2, Money.ofDollars(100), "5");
        List<Debt> input = List.of(highest, lowest);

        Strategy.SNOWBALL.ordering().order(input);

        assertThat(input).containsExactly(highest, lowest);
    }

    @Test
    void emptyListOrdersToAnEmptyList() {
        assertThat(Strategy.SNOWBALL.ordering().order(List.of())).isEmpty();
        assertThat(Strategy.AVALANCHE.ordering().order(List.of())).isEmpty();
    }

    @Test
    void singleDebtListOrdersIdenticallyUnderBothStrategies() {
        Debt only = debt(1, Money.ofDollars(500), "10");

        assertThat(Strategy.SNOWBALL.ordering().order(List.of(only))).containsExactly(only);
        assertThat(Strategy.AVALANCHE.ordering().order(List.of(only))).containsExactly(only);
    }
}
