package com.rungs.readiness.domain.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rungs.readiness.domain.model.Debt;
import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SimulationInputTests {

    private static final Debt DEBT = new Debt(
            "debt-1", "Visa", DebtType.CREDIT_CARD, Money.ofDollars(500), new BigDecimal("10"), Money.ofDollars(25));

    private static final PayoffOrdering ORDERING = Strategy.SNOWBALL.ordering();

    @Test
    void exposesComponentsAsSupplied() {
        List<Debt> debts = List.of(DEBT);
        SimulationInput input = new SimulationInput(debts, Money.ofDollars(300), Money.ofDollars(50), ORDERING, 360);

        assertThat(input.debts()).containsExactly(DEBT);
        assertThat(input.monthlySurplus()).isEqualTo(Money.ofDollars(300));
        assertThat(input.extraMonthlyPayment()).isEqualTo(Money.ofDollars(50));
        assertThat(input.ordering()).isEqualTo(ORDERING);
        assertThat(input.horizonMonths()).isEqualTo(360);
    }

    @Test
    void copiesTheDebtsSoLaterMutationOfTheSourceListIsNotVisible() {
        List<Debt> source = new ArrayList<>(List.of(DEBT));

        SimulationInput input = new SimulationInput(source, Money.ZERO, Money.ZERO, ORDERING, 1);
        source.clear();

        assertThat(input.debts()).containsExactly(DEBT);
    }

    @Test
    void rejectsAnUnmodifiableAttemptOnTheReturnedDebts() {
        SimulationInput input = new SimulationInput(List.of(DEBT), Money.ZERO, Money.ZERO, ORDERING, 1);

        assertThatThrownBy(() -> input.debts().add(DEBT)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void allowsANegativeMonthlySurplus() {
        SimulationInput input = new SimulationInput(List.of(), Money.ofCents(-1), Money.ZERO, ORDERING, 1);

        assertThat(input.monthlySurplus()).isEqualTo(Money.ofCents(-1));
    }

    @Test
    void rejectsANegativeExtraMonthlyPayment() {
        assertThatThrownBy(() -> new SimulationInput(List.of(), Money.ZERO, Money.ofCents(-1), ORDERING, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("extraMonthlyPayment must not be negative");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 601, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void rejectsAHorizonOutsideOneTo600(int horizonMonths) {
        assertThatThrownBy(() -> new SimulationInput(List.of(), Money.ZERO, Money.ZERO, ORDERING, horizonMonths))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("horizonMonths must be between 1 and 600");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 360, 600})
    void acceptsAHorizonAtAndInsideTheBoundaries(int horizonMonths) {
        SimulationInput input = new SimulationInput(List.of(), Money.ZERO, Money.ZERO, ORDERING, horizonMonths);

        assertThat(input.horizonMonths()).isEqualTo(horizonMonths);
    }

    @Test
    void rejectsANullDebtsList() {
        assertThatThrownBy(() -> new SimulationInput(null, Money.ZERO, Money.ZERO, ORDERING, 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("debts");
    }

    @Test
    void rejectsANullMonthlySurplus() {
        assertThatThrownBy(() -> new SimulationInput(List.of(), null, Money.ZERO, ORDERING, 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("monthlySurplus");
    }

    @Test
    void rejectsANullExtraMonthlyPayment() {
        assertThatThrownBy(() -> new SimulationInput(List.of(), Money.ZERO, null, ORDERING, 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("extraMonthlyPayment");
    }

    @Test
    void rejectsANullOrdering() {
        assertThatThrownBy(() -> new SimulationInput(List.of(), Money.ZERO, Money.ZERO, null, 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ordering");
    }
}
