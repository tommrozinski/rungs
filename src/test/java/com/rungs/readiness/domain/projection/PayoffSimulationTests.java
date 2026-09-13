package com.rungs.readiness.domain.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rungs.readiness.domain.model.Money;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PayoffSimulationTests {

    private static final DebtLine LINE_1 =
            new DebtLine("debt-1", Money.ofDollars(500), Money.ZERO, Money.ofDollars(500), Money.ZERO);

    private static final DebtLine LINE_2 =
            new DebtLine("debt-2", Money.ofDollars(300), Money.ZERO, Money.ofDollars(150), Money.ofDollars(150));

    private static final MonthlyPeriod MONTH_1 = new MonthlyPeriod(1, List.of(LINE_1, LINE_2), List.of("debt-1"));

    private static final MonthlyPeriod MONTH_2 =
            new MonthlyPeriod(2, List.of(new DebtLine("debt-2", Money.ofDollars(150), Money.ZERO, Money.ofDollars(150), Money.ZERO)), List.of("debt-2"));

    @Test
    void exposesComponentsAsSupplied() {
        PayoffSimulation simulation =
                new PayoffSimulation(List.of(MONTH_1, MONTH_2), 2, Money.ofDollars(0), false, List.of());

        assertThat(simulation.schedule()).containsExactly(MONTH_1, MONTH_2);
        assertThat(simulation.monthsToDebtFreedom()).isEqualTo(2);
        assertThat(simulation.totalInterestPaid()).isEqualTo(Money.ZERO);
        assertThat(simulation.truncated()).isFalse();
        assertThat(simulation.unpayableDebtIds()).isEmpty();
    }

    @Test
    void allowsANullMonthsToDebtFreedom() {
        PayoffSimulation simulation =
                new PayoffSimulation(List.of(MONTH_1), null, Money.ZERO, true, List.of("debt-2"));

        assertThat(simulation.monthsToDebtFreedom()).isNull();
        assertThat(simulation.truncated()).isTrue();
        assertThat(simulation.unpayableDebtIds()).containsExactly("debt-2");
    }

    @Test
    void allowsZeroMonthsToDebtFreedomWithNoDebts() {
        PayoffSimulation simulation = new PayoffSimulation(List.of(), 0, Money.ZERO, false, List.of());

        assertThat(simulation.monthsToDebtFreedom()).isEqualTo(0);
        assertThat(simulation.schedule()).isEmpty();
    }

    @Test
    void copiesTheListsSoLaterMutationOfTheSourceListsIsNotVisible() {
        var schedule = new java.util.ArrayList<>(List.of(MONTH_1));
        var unpayable = new java.util.ArrayList<>(List.of("debt-2"));

        PayoffSimulation simulation = new PayoffSimulation(schedule, null, Money.ZERO, true, unpayable);
        schedule.clear();
        unpayable.clear();

        assertThat(simulation.schedule()).containsExactly(MONTH_1);
        assertThat(simulation.unpayableDebtIds()).containsExactly("debt-2");
    }

    @Test
    void rejectsANegativeMonthsToDebtFreedom() {
        assertThatThrownBy(() -> new PayoffSimulation(List.of(), -1, Money.ZERO, false, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("monthsToDebtFreedom must not be negative");
    }

    @Test
    void rejectsANegativeTotalInterestPaid() {
        assertThatThrownBy(() -> new PayoffSimulation(List.of(), 0, Money.ofCents(-1), false, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("totalInterestPaid must not be negative");
    }

    @Test
    void rejectsANullSchedule() {
        assertThatThrownBy(() -> new PayoffSimulation(null, 0, Money.ZERO, false, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("schedule");
    }

    @Test
    void rejectsANullTotalInterestPaid() {
        assertThatThrownBy(() -> new PayoffSimulation(List.of(), 0, null, false, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("totalInterestPaid");
    }

    @Test
    void rejectsANullUnpayableDebtIds() {
        assertThatThrownBy(() -> new PayoffSimulation(List.of(), 0, Money.ZERO, false, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("unpayableDebtIds");
    }

    @Test
    void monthRetiredReturnsTheMonthADebtWasRetired() {
        PayoffSimulation simulation = new PayoffSimulation(List.of(MONTH_1, MONTH_2), 2, Money.ZERO, false, List.of());

        assertThat(simulation.monthRetired("debt-1")).contains(1);
        assertThat(simulation.monthRetired("debt-2")).contains(2);
    }

    @Test
    void monthRetiredReturnsEmptyForADebtNeverRetiredInTheSchedule() {
        PayoffSimulation simulation = new PayoffSimulation(List.of(MONTH_1), null, Money.ZERO, true, List.of("debt-2"));

        assertThat(simulation.monthRetired("debt-2")).isEqualTo(Optional.empty());
    }

    @Test
    void rejectsANullDebtIdToMonthRetired() {
        PayoffSimulation simulation = new PayoffSimulation(List.of(), 0, Money.ZERO, false, List.of());

        assertThatThrownBy(() -> simulation.monthRetired(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("debtId");
    }
}
