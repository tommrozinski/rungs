package com.rungs.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MortgageTests {

    private static final Money BALANCE = Money.ofDollars(180_000);
    private static final BigDecimal RATE = new BigDecimal("6.125");
    private static final Money PAYMENT = Money.ofDollars(1_450);
    private static final Money TAXES = Money.ofDollars(4_800);
    private static final YearMonth MATURITY = YearMonth.of(2051, 3);

    @Test
    void exposesComponentsAsSupplied() {
        Mortgage mortgage = new Mortgage(BALANCE, RATE, PAYMENT, TAXES, MATURITY);

        assertThat(mortgage.balance()).isEqualTo(BALANCE);
        assertThat(mortgage.annualInterestRate()).isEqualTo(RATE);
        assertThat(mortgage.monthlyPayment()).isEqualTo(PAYMENT);
        assertThat(mortgage.annualPropertyTaxes()).isEqualTo(TAXES);
        assertThat(mortgage.maturity()).isEqualTo(MATURITY);
    }

    @Test
    void aNullMaturityIsAllowedAndMeansUnknown() {
        Mortgage mortgage = new Mortgage(BALANCE, RATE, PAYMENT, TAXES, null);

        assertThat(mortgage.maturity()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"7.25", "7.250", "7.2500"})
    void ratesDifferingOnlyInTrailingZerosConstructEqualMortgages(String rate) {
        Mortgage mortgage = new Mortgage(BALANCE, new BigDecimal(rate), PAYMENT, TAXES, MATURITY);

        assertThat(mortgage).isEqualTo(new Mortgage(BALANCE, new BigDecimal("7.250"), PAYMENT, TAXES, MATURITY));
        assertThat(mortgage.annualInterestRate().scale()).isEqualTo(3);
    }

    @Test
    void acceptsZeroForEveryAmountAndBothRateBoundaries() {
        assertThat(new Mortgage(Money.ZERO, BigDecimal.ZERO, Money.ZERO, Money.ZERO, null).annualInterestRate())
                .isEqualTo(new BigDecimal("0.000"));
        assertThat(new Mortgage(Money.ZERO, new BigDecimal("100"), Money.ZERO, Money.ZERO, null).annualInterestRate())
                .isEqualTo(new BigDecimal("100.000"));
    }

    @ParameterizedTest
    @CsvSource({"balance", "monthlyPayment", "annualPropertyTaxes"})
    void rejectsANegativeAmount(String component) {
        Money negative = Money.ofCents(-1);

        assertThatThrownBy(() -> mortgageWith(component, negative))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(component + " must not be negative");
    }

    @ParameterizedTest
    @CsvSource({"balance", "monthlyPayment", "annualPropertyTaxes"})
    void rejectsANullAmount(String component) {
        assertThatThrownBy(() -> mortgageWith(component, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(component);
    }

    @Test
    void rejectsANullRate() {
        assertThatThrownBy(() -> new Mortgage(BALANCE, null, PAYMENT, TAXES, MATURITY))
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

        assertThatThrownBy(() -> new Mortgage(BALANCE, annualInterestRate, PAYMENT, TAXES, MATURITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage)
                .hasMessageNotContaining(rate);
    }

    @Test
    void isAHousing() {
        Housing housing = new Mortgage(BALANCE, RATE, PAYMENT, TAXES, MATURITY);

        assertThat(housing).isInstanceOf(Mortgage.class);
    }

    // monthlyPropertyTaxes: annual taxes / 12, rounded half-even to the cent (design.md 039 section 2)

    @Test
    void monthlyPropertyTaxesDividesEvenlyWhenTheAnnualAmountIsAMultipleOfTwelve() {
        Mortgage mortgage = new Mortgage(BALANCE, RATE, PAYMENT, Money.ofDollars(6_300), MATURITY);

        assertThat(mortgage.monthlyPropertyTaxes()).isEqualTo(Money.ofDollars(525));
    }

    @Test
    void monthlyPropertyTaxesRoundsHalfEvenToTheCent() {
        Mortgage mortgage = new Mortgage(BALANCE, RATE, PAYMENT, Money.ofDollars(1_000), MATURITY);

        assertThat(mortgage.monthlyPropertyTaxes()).isEqualTo(Money.ofCents(8_333));
    }

    @Test
    void monthlyPropertyTaxesIsZeroWithNoAnnualTaxes() {
        Mortgage mortgage = new Mortgage(BALANCE, RATE, PAYMENT, Money.ZERO, MATURITY);

        assertThat(mortgage.monthlyPropertyTaxes()).isEqualTo(Money.ZERO);
    }

    private static Mortgage mortgageWith(String component, Money amount) {
        return switch (component) {
            case "balance" -> new Mortgage(amount, RATE, PAYMENT, TAXES, MATURITY);
            case "monthlyPayment" -> new Mortgage(BALANCE, RATE, amount, TAXES, MATURITY);
            case "annualPropertyTaxes" -> new Mortgage(BALANCE, RATE, PAYMENT, amount, MATURITY);
            default -> throw new IllegalArgumentException(component);
        };
    }
}
