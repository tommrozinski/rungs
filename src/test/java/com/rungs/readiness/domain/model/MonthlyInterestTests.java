package com.rungs.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * The one interest formula shared by the simulator (story 008) and the negative-amortization warning (story 012):
 * balance times annual rate divided by 1200, rounded half-even to the cent.
 */
class MonthlyInterestTests {

    @Test
    void multipliesBalanceByRateDividedByTwelveHundred() {
        // 12000 cents * (22.9 / 1200) = 229.00 cents exactly.
        Money interest = MonthlyInterest.on(Money.ofCents(12_000), new BigDecimal("22.9"));

        assertThat(interest).isEqualTo(Money.ofCents(229));
    }

    @Test
    void exactlyHalfACentRoundsHalfEvenDownToAnEvenCent() {
        // 8 cents * (75 / 1200) = 0.5 cents; half-even rounds 0.5 to 0 (the nearest even cent).
        Money interest = MonthlyInterest.on(Money.ofCents(8), new BigDecimal("75"));

        assertThat(interest).isEqualTo(Money.ZERO);
    }

    @Test
    void exactlyHalfACentRoundsHalfEvenUpToAnEvenCent() {
        // 24 cents * (75 / 1200) = 1.5 cents; half-even rounds 1.5 up to 2 (the nearest even cent).
        Money interest = MonthlyInterest.on(Money.ofCents(24), new BigDecimal("75"));

        assertThat(interest).isEqualTo(Money.ofCents(2));
    }

    @Test
    void rejectsANullBalanceOrRate() {
        assertThatThrownBy(() -> MonthlyInterest.on(null, BigDecimal.TEN)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> MonthlyInterest.on(Money.ZERO, null)).isInstanceOf(NullPointerException.class);
    }
}
