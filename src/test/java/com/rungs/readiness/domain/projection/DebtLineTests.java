package com.rungs.readiness.domain.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rungs.readiness.domain.model.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DebtLineTests {

    @Test
    void exposesComponentsAsSupplied() {
        DebtLine line = new DebtLine(
                "debt-1", Money.ofDollars(500), Money.ofCents(250), Money.ofDollars(90), Money.ofDollars(413));

        assertThat(line.debtId()).isEqualTo("debt-1");
        assertThat(line.openingBalance()).isEqualTo(Money.ofDollars(500));
        assertThat(line.interest()).isEqualTo(Money.ofCents(250));
        assertThat(line.payment()).isEqualTo(Money.ofDollars(90));
        assertThat(line.closingBalance()).isEqualTo(Money.ofDollars(413));
    }

    @Test
    void allowsEveryMoneyComponentToBeZero() {
        DebtLine line = new DebtLine("debt-1", Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO);

        assertThat(line.openingBalance()).isEqualTo(Money.ZERO);
        assertThat(line.interest()).isEqualTo(Money.ZERO);
        assertThat(line.payment()).isEqualTo(Money.ZERO);
        assertThat(line.closingBalance()).isEqualTo(Money.ZERO);
    }

    @Test
    void rejectsANullDebtId() {
        assertThatThrownBy(() -> new DebtLine(null, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("debtId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"openingBalance", "interest", "payment", "closingBalance"})
    void rejectsANegativeMoneyComponent(String component) {
        assertThatThrownBy(() -> lineWithNegative(component))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(component + " must not be negative");
    }

    @ParameterizedTest
    @ValueSource(strings = {"openingBalance", "interest", "payment", "closingBalance"})
    void rejectsANullMoneyComponentNamingIt(String component) {
        assertThatThrownBy(() -> lineWithNull(component))
                .isInstanceOf(NullPointerException.class)
                .hasMessage(component);
    }

    private static DebtLine lineWithNegative(String component) {
        Money negative = Money.ofCents(-1);
        return switch (component) {
            case "openingBalance" -> new DebtLine("debt-1", negative, Money.ZERO, Money.ZERO, Money.ZERO);
            case "interest" -> new DebtLine("debt-1", Money.ZERO, negative, Money.ZERO, Money.ZERO);
            case "payment" -> new DebtLine("debt-1", Money.ZERO, Money.ZERO, negative, Money.ZERO);
            case "closingBalance" -> new DebtLine("debt-1", Money.ZERO, Money.ZERO, Money.ZERO, negative);
            default -> throw new IllegalArgumentException(component);
        };
    }

    private static DebtLine lineWithNull(String component) {
        return switch (component) {
            case "openingBalance" -> new DebtLine("debt-1", null, Money.ZERO, Money.ZERO, Money.ZERO);
            case "interest" -> new DebtLine("debt-1", Money.ZERO, null, Money.ZERO, Money.ZERO);
            case "payment" -> new DebtLine("debt-1", Money.ZERO, Money.ZERO, null, Money.ZERO);
            case "closingBalance" -> new DebtLine("debt-1", Money.ZERO, Money.ZERO, Money.ZERO, null);
            default -> throw new IllegalArgumentException(component);
        };
    }
}
