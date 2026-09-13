package com.rungs.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MortgageTests {

    private static final Money BALANCE = Money.ofDollars(180_000);
    private static final Money PAYMENT = Money.ofDollars(1_450);
    private static final Money HOME_VALUE = Money.ofDollars(320_000);
    private static final BigDecimal RATE = new BigDecimal("6.125");

    @Test
    void exposesComponentsAsSupplied() {
        Mortgage mortgage = new Mortgage(BALANCE, PAYMENT, HOME_VALUE, RATE);

        assertThat(mortgage.balance()).isEqualTo(BALANCE);
        assertThat(mortgage.monthlyPayment()).isEqualTo(PAYMENT);
        assertThat(mortgage.homeValue()).isEqualTo(HOME_VALUE);
        assertThat(mortgage.annualInterestRate()).isEqualTo(RATE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"7.25", "7.250", "7.2500"})
    void ratesDifferingOnlyInTrailingZerosConstructEqualMortgages(String rate) {
        Mortgage mortgage = new Mortgage(BALANCE, PAYMENT, HOME_VALUE, new BigDecimal(rate));

        assertThat(mortgage).isEqualTo(new Mortgage(BALANCE, PAYMENT, HOME_VALUE, new BigDecimal("7.250")));
        assertThat(mortgage.annualInterestRate().scale()).isEqualTo(3);
    }

    @Test
    void acceptsZeroForEveryAmountAndBothRateBoundaries() {
        assertThat(new Mortgage(Money.ZERO, Money.ZERO, Money.ZERO, BigDecimal.ZERO).annualInterestRate())
                .isEqualTo(new BigDecimal("0.000"));
        assertThat(new Mortgage(Money.ZERO, Money.ZERO, Money.ZERO, new BigDecimal("100")).annualInterestRate())
                .isEqualTo(new BigDecimal("100.000"));
    }

    @ParameterizedTest
    @CsvSource({"balance", "monthlyPayment", "homeValue"})
    void rejectsANegativeAmount(String component) {
        Money negative = Money.ofCents(-1);

        assertThatThrownBy(() -> mortgageWith(component, negative))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(component + " must not be negative");
    }

    @ParameterizedTest
    @CsvSource({"balance", "monthlyPayment", "homeValue"})
    void rejectsANullAmount(String component) {
        assertThatThrownBy(() -> mortgageWith(component, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(component);
    }

    @Test
    void rejectsANullRate() {
        assertThatThrownBy(() -> new Mortgage(BALANCE, PAYMENT, HOME_VALUE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("annualInterestRate");
    }

    @ParameterizedTest
    @CsvSource({
        "-0.001, annualInterestRate must be between 0 and 100",
        "100.001, annualInterestRate must be between 0 and 100",
        "7.2505, annualInterestRate has more than 3 decimal places"
    })
    void rejectsAnInvalidRateWithoutRounding(String rate, String expectedMessage) {
        BigDecimal annualInterestRate = new BigDecimal(rate);

        assertThatThrownBy(() -> new Mortgage(BALANCE, PAYMENT, HOME_VALUE, annualInterestRate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage)
                .hasMessageNotContaining(rate);
    }

    private static Mortgage mortgageWith(String component, Money amount) {
        return switch (component) {
            case "balance" -> new Mortgage(amount, PAYMENT, HOME_VALUE, RATE);
            case "monthlyPayment" -> new Mortgage(BALANCE, amount, HOME_VALUE, RATE);
            case "homeValue" -> new Mortgage(BALANCE, PAYMENT, amount, RATE);
            default -> throw new IllegalArgumentException(component);
        };
    }
}
