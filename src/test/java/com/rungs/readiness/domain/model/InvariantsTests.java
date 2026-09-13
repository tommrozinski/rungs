package com.rungs.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class InvariantsTests {

    @Test
    void nonNullReturnsTheValue() {
        String value = "present";

        assertThat(Invariants.nonNull(value, "label")).isSameAs(value);
    }

    @Test
    void nonNullRejectsNullNamingTheComponent() {
        assertThatThrownBy(() -> Invariants.nonNull(null, "label"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("label");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, Long.MAX_VALUE})
    void nonNegativeAcceptsZeroAndAbove(long cents) {
        Money amount = Money.ofCents(cents);

        assertThat(Invariants.nonNegative(amount, "balance")).isSameAs(amount);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, -123_456, Long.MIN_VALUE})
    void nonNegativeRejectsBelowZeroWithoutTheValue(long cents) {
        Money amount = Money.ofCents(cents);

        assertThatThrownBy(() -> Invariants.nonNegative(amount, "balance"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("balance must not be negative");
    }

    @Test
    void nonNegativeRejectsNullNamingTheComponent() {
        assertThatThrownBy(() -> Invariants.nonNegative(null, "balance"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("balance");
    }

    @ParameterizedTest
    @ValueSource(longs = {1, Long.MAX_VALUE})
    void positiveAcceptsAboveZero(long cents) {
        Money amount = Money.ofCents(cents);

        assertThat(Invariants.positive(amount, "balance")).isSameAs(amount);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    void positiveRejectsZeroAndBelow(long cents) {
        Money amount = Money.ofCents(cents);

        assertThatThrownBy(() -> Invariants.positive(amount, "balance"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("balance must be above zero");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Card 1", "x", " padded "})
    void notBlankAcceptsTextWithAVisibleCharacter(String text) {
        assertThat(Invariants.notBlank(text, "label")).isSameAs(text);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n", " "})
    void notBlankRejectsEmptyOrWhitespaceOnlyText(String text) {
        assertThatThrownBy(() -> Invariants.notBlank(text, "label"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("label must not be blank");
    }

    @Test
    void notBlankRejectsNullNamingTheComponent() {
        assertThatThrownBy(() -> Invariants.notBlank(null, "label"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("label");
    }

    @ParameterizedTest
    @CsvSource({
        "7.25, 3, 7.250",
        "7.250, 3, 7.250",
        "7.2500, 3, 7.250",
        "0, 3, 0.000",
        "0.000000, 3, 0.000",
        "0.001, 3, 0.001",
        "100, 3, 100.000",
        "100.000, 3, 100.000",
        "1E+2, 3, 100.000",
        "12.5, 2, 12.50",
        "12.50, 2, 12.50",
        "99.99, 2, 99.99"
    })
    void percentNormalizesToExactlyTheScale(String value, int scale, String expected) {
        BigDecimal normalized = Invariants.percent(new BigDecimal(value), scale, "annualInterestRate");

        assertThat(normalized).isEqualTo(new BigDecimal(expected));
        assertThat(normalized.scale()).isEqualTo(scale);
    }

    @ParameterizedTest
    @CsvSource({
        "-0.001, 3",
        "100.001, 3",
        "-1, 3",
        "-0.01, 2",
        "100.01, 2",
        "1E+999999999, 3",
        "-1E-999999999, 3"
    })
    void percentRejectsValuesOutsideZeroToOneHundred(String value, int scale) {
        BigDecimal percent = new BigDecimal(value);

        assertThatThrownBy(() -> Invariants.percent(percent, scale, "annualInterestRate"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("annualInterestRate must be between 0 and 100");
    }

    @ParameterizedTest
    @CsvSource({
        "7.2505, 3, annualInterestRate, annualInterestRate has more than 3 decimal places",
        "0.0001, 3, annualInterestRate, annualInterestRate has more than 3 decimal places",
        "12.505, 2, retirementContributionPercent, retirementContributionPercent has more than 2 decimal places",
        "1E-999999999, 3, annualInterestRate, annualInterestRate has more than 3 decimal places"
    })
    void percentRejectsMorePrecisionThanTheScaleInsteadOfRounding(
            String value, int scale, String name, String expectedMessage) {
        BigDecimal percent = new BigDecimal(value);

        assertThatThrownBy(() -> Invariants.percent(percent, scale, name))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    @Test
    void percentRejectsNullNamingTheComponent() {
        assertThatThrownBy(() -> Invariants.percent(null, 3, "annualInterestRate"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("annualInterestRate");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0E-999999999", "0E+999999999", "100.000", "7.2500", "0"})
    @Timeout(value = 1, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void percentAcceptsPathologicalScalesQuickly(String value) {
        BigDecimal percent = new BigDecimal(value);

        assertThat(Invariants.percent(percent, 3, "annualInterestRate").scale()).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1E+999999999", "1E-999999999"})
    @Timeout(value = 1, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void percentRejectsPathologicalScalesQuickly(String value) {
        BigDecimal percent = new BigDecimal(value);

        assertThatThrownBy(() -> Invariants.percent(percent, 3, "annualInterestRate"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void positiveRejectsNullNamingTheComponent() {
        assertThatThrownBy(() -> Invariants.positive(null, "balance"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("balance");
    }
}
