package com.rungs.readiness.domain.stage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PercentagesTests {

    @Test
    void returnsZeroWhenTheWholeIsZero() {
        assertThat(Percentages.ofRatio(BigDecimal.TEN, BigDecimal.ZERO)).isEqualTo(new BigDecimal("0.0"));
    }

    @ParameterizedTest
    @CsvSource({"10000, 10000", "10001, 10000", "1, 0.5"})
    void returnsOneHundredWhenThePartReachesTheWhole(String part, String whole) {
        assertThat(Percentages.ofRatio(new BigDecimal(part), new BigDecimal(whole)))
                .isEqualTo(new BigDecimal("100.0"));
    }

    @ParameterizedTest
    @CsvSource({
        "9999, 10000, 99.9",
        "9995, 10000, 99.9",
        "99999, 100000, 99.9",
        "0.9999, 1, 99.9",
        "1, 3, 33.3",
        "2, 3, 66.6",
        "5, 6, 83.3",
        "0, 7, 0.0",
        "1, 1000000, 0.0"
    })
    void roundsAnUnmetRatioDownToOneDecimalPlace(String part, String whole, String expected) {
        BigDecimal percent = Percentages.ofRatio(new BigDecimal(part), new BigDecimal(whole));

        assertThat(percent).isEqualTo(new BigDecimal(expected));
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0.0",
        "999900, 1000000, 99.9",
        "999500, 1000000, 99.9",
        "1000000, 1000000, 100.0",
        "1000001, 1000000, 100.0",
        "1, 3, 33.3"
    })
    void appliesTheSameRulesToMoneyAmounts(long partCents, long wholeCents, String expected) {
        BigDecimal percent = Percentages.of(Money.ofCents(partCents), Money.ofCents(wholeCents));

        assertThat(percent).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void rejectsANullOrNegativeMoneyAmount() {
        assertThatThrownBy(() -> Percentages.of(null, Money.ZERO))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("part");
        assertThatThrownBy(() -> Percentages.of(Money.ZERO, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("whole");
        assertThatThrownBy(() -> Percentages.of(Money.ofCents(-1), Money.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("part must not be negative");
    }

    @Test
    void rejectsANullPartOrWhole() {
        assertThatThrownBy(() -> Percentages.ofRatio(null, BigDecimal.ONE))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("part");
        assertThatThrownBy(() -> Percentages.ofRatio(BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("whole");
    }

    @ParameterizedTest
    @CsvSource({"-0.01, 1, part must not be negative", "1, -0.01, whole must not be negative"})
    void rejectsANegativePartOrWhole(String part, String whole, String expectedMessage) {
        BigDecimal partValue = new BigDecimal(part);
        BigDecimal wholeValue = new BigDecimal(whole);

        assertThatThrownBy(() -> Percentages.ofRatio(partValue, wholeValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }
}
