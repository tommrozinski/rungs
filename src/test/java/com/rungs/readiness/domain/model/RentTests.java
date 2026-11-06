package com.rungs.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RentTests {

    @Test
    void exposesTheMonthlyRentAsSupplied() {
        Rent rent = new Rent(Money.ofDollars(1_250));

        assertThat(rent.monthlyRent()).isEqualTo(Money.ofDollars(1_250));
    }

    @Test
    void acceptsZeroRent() {
        Rent rent = new Rent(Money.ZERO);

        assertThat(rent.monthlyRent()).isEqualTo(Money.ZERO);
    }

    @Test
    void rejectsNegativeRent() {
        assertThatThrownBy(() -> new Rent(Money.ofCents(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("monthlyRent must not be negative");
    }

    @Test
    void rejectsANullRent() {
        assertThatThrownBy(() -> new Rent(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("monthlyRent");
    }

    @Test
    void isAHousing() {
        Housing housing = new Rent(Money.ZERO);

        assertThat(housing).isInstanceOf(Rent.class);
    }
}
