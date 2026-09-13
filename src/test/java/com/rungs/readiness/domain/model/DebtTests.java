package com.rungs.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DebtTests {

    private static final String ID = "debt-1";
    private static final String LABEL = "Visa";
    private static final DebtType TYPE = DebtType.CREDIT_CARD;
    private static final Money BALANCE = Money.ofCents(425_050);
    private static final BigDecimal RATE = new BigDecimal("22.990");
    private static final Money MINIMUM = Money.ofDollars(95);

    @Test
    void exposesComponentsAsSupplied() {
        Debt debt = new Debt(ID, LABEL, TYPE, BALANCE, RATE, MINIMUM);

        assertThat(debt.id()).isEqualTo(ID);
        assertThat(debt.label()).isEqualTo(LABEL);
        assertThat(debt.type()).isEqualTo(TYPE);
        assertThat(debt.balance()).isEqualTo(BALANCE);
        assertThat(debt.annualInterestRate()).isEqualTo(RATE);
        assertThat(debt.minimumPayment()).isEqualTo(MINIMUM);
    }

    @ParameterizedTest
    @ValueSource(strings = {"7.25", "7.250", "7.2500"})
    void ratesDifferingOnlyInTrailingZerosConstructEqualDebts(String rate) {
        Debt debt = new Debt(ID, LABEL, TYPE, BALANCE, new BigDecimal(rate), MINIMUM);
        Debt expected = new Debt(ID, LABEL, TYPE, BALANCE, new BigDecimal("7.250"), MINIMUM);

        assertThat(debt).isEqualTo(expected).hasSameHashCodeAs(expected);
        assertThat(debt.annualInterestRate().scale()).isEqualTo(3);
    }

    @ParameterizedTest
    @CsvSource({"0, 0.000", "100, 100.000", "0.001, 0.001", "99.999, 99.999"})
    void acceptsRatesAtAndInsideTheBoundaries(String rate, String expected) {
        Debt debt = new Debt(ID, LABEL, TYPE, BALANCE, new BigDecimal(rate), MINIMUM);

        assertThat(debt.annualInterestRate()).isEqualTo(new BigDecimal(expected));
    }

    @ParameterizedTest
    @CsvSource({
        "-0.001, annualInterestRate must be between 0 and 100",
        "100.001, annualInterestRate must be between 0 and 100",
        "7.2505, annualInterestRate has more than 3 decimal places"
    })
    void rejectsAnInvalidRateWithoutRounding(String rate, String expectedMessage) {
        BigDecimal annualInterestRate = new BigDecimal(rate);

        assertThatThrownBy(() -> new Debt(ID, LABEL, TYPE, BALANCE, annualInterestRate, MINIMUM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage)
                .hasMessageNotContaining(rate);
    }

    @ParameterizedTest
    @ValueSource(strings = {"debt-1", "debt-9", "debt-10", "debt-50", "debt-1234567890"})
    void acceptsPositionalIds(String id) {
        assertThat(new Debt(id, LABEL, TYPE, BALANCE, RATE, MINIMUM).id()).isEqualTo(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "debt-0", "debt-01", "Debt-1", "DEBT-1", "debt-", "debt", "debt-1a", "debt--1", "debt-+1", " debt-1",
        "debt-1 ", "debt-1\n", "debt_1", "1", "", "debt-١"
    })
    void rejectsIdsThatAreNotPositional(String id) {
        assertThatThrownBy(() -> new Debt(id, LABEL, TYPE, BALANCE, RATE, MINIMUM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("id must be debt- followed by a whole number from 1 without leading zeros");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void rejectsABlankLabel(String label) {
        assertThatThrownBy(() -> new Debt(ID, label, TYPE, BALANCE, RATE, MINIMUM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("label must not be blank");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    void rejectsABalanceThatIsNotAboveZero(long cents) {
        Money balance = Money.ofCents(cents);

        assertThatThrownBy(() -> new Debt(ID, LABEL, TYPE, balance, RATE, MINIMUM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("balance must be above zero");
    }

    @Test
    void acceptsTheSmallestPositiveBalanceAndAZeroMinimumPayment() {
        Debt debt = new Debt(ID, LABEL, TYPE, Money.ofCents(1), RATE, Money.ZERO);

        assertThat(debt.balance()).isEqualTo(Money.ofCents(1));
        assertThat(debt.minimumPayment()).isEqualTo(Money.ZERO);
    }

    @Test
    void rejectsANegativeMinimumPayment() {
        Money minimumPayment = Money.ofCents(-1);

        assertThatThrownBy(() -> new Debt(ID, LABEL, TYPE, BALANCE, RATE, minimumPayment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("minimumPayment must not be negative");
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "label", "type", "balance", "annualInterestRate", "minimumPayment"})
    void rejectsANullComponentNamingIt(String component) {
        assertThatThrownBy(() -> debtWithNull(component))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(component);
    }

    private static Debt debtWithNull(String component) {
        return switch (component) {
            case "id" -> new Debt(null, LABEL, TYPE, BALANCE, RATE, MINIMUM);
            case "label" -> new Debt(ID, null, TYPE, BALANCE, RATE, MINIMUM);
            case "type" -> new Debt(ID, LABEL, null, BALANCE, RATE, MINIMUM);
            case "balance" -> new Debt(ID, LABEL, TYPE, null, RATE, MINIMUM);
            case "annualInterestRate" -> new Debt(ID, LABEL, TYPE, BALANCE, null, MINIMUM);
            case "minimumPayment" -> new Debt(ID, LABEL, TYPE, BALANCE, RATE, null);
            default -> throw new IllegalArgumentException(component);
        };
    }
}
