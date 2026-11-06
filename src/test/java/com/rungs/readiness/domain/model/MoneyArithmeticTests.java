package com.rungs.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyArithmeticTests {

    @Test
    void zeroHasNoCentsAndEqualsOfCentsZero() {
        assertThat(Money.ZERO.cents()).isZero();
        assertThat(Money.ZERO).isEqualTo(Money.ofCents(0));
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -1, 0, 1, 123_456, Long.MAX_VALUE})
    void ofCentsWrapsEveryLong(long cents) {
        assertThat(Money.ofCents(cents).cents()).isEqualTo(cents);
    }

    @ParameterizedTest
    @CsvSource({
        "12, 1200",
        "-3, -300",
        "0, 0",
        "92233720368547758, 9223372036854775800",
        "-92233720368547758, -9223372036854775800"
    })
    void ofDollarsConvertsWholeDollarsToCents(long dollars, long expectedCents) {
        assertThat(Money.ofDollars(dollars)).isEqualTo(Money.ofCents(expectedCents));
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MAX_VALUE / 100 + 1, Long.MIN_VALUE / 100 - 1})
    void ofDollarsThrowsWhenCentsOverflow(long dollars) {
        assertThatThrownBy(() -> Money.ofDollars(dollars)).isInstanceOf(ArithmeticException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0",
        "0, 1, 1",
        "0, -1, -1",
        "1, 1, 2",
        "-1, -1, -2",
        "1, -1, 0",
        "1200, -1250, -50",
        "-300, 450, 150",
        "9223372036854775806, 1, 9223372036854775807",
        "-9223372036854775807, -1, -9223372036854775808"
    })
    void plusAddsCentsExactly(long left, long right, long expected) {
        assertThat(Money.ofCents(left).plus(Money.ofCents(right))).isEqualTo(Money.ofCents(expected));
    }

    @Test
    void plusThrowsWhenSumExceedsLongRange() {
        Money max = Money.ofCents(Long.MAX_VALUE);
        Money oneCent = Money.ofCents(1);

        assertThatThrownBy(() -> max.plus(oneCent)).isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> Money.ofCents(Long.MIN_VALUE).plus(Money.ofCents(-1)))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void plusRejectsNullOperand() {
        assertThatThrownBy(() -> Money.ZERO.plus(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0",
        "0, 1, -1",
        "0, -1, 1",
        "1, 1, 0",
        "-1, -1, 0",
        "1, -1, 2",
        "-1, 1, -2",
        "1200, 1250, -50",
        "-300, -450, 150",
        "-9223372036854775807, 1, -9223372036854775808",
        "9223372036854775806, -1, 9223372036854775807",
        "-1, -9223372036854775808, 9223372036854775807"
    })
    void minusSubtractsCentsExactly(long left, long right, long expected) {
        assertThat(Money.ofCents(left).minus(Money.ofCents(right))).isEqualTo(Money.ofCents(expected));
    }

    @Test
    void minusThrowsWhenDifferenceExceedsLongRange() {
        Money min = Money.ofCents(Long.MIN_VALUE);
        Money oneCent = Money.ofCents(1);

        assertThatThrownBy(() -> min.minus(oneCent)).isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> Money.ofCents(Long.MAX_VALUE).minus(Money.ofCents(-1)))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void minusRejectsNullOperand() {
        assertThatThrownBy(() -> Money.ZERO.minus(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "1, -1",
        "-1, 1",
        "1250, -1250",
        "-1250, 1250",
        "9223372036854775807, -9223372036854775807"
    })
    void negateFlipsTheSign(long cents, long expected) {
        assertThat(Money.ofCents(cents).negate()).isEqualTo(Money.ofCents(expected));
    }

    @Test
    void negateOfZeroIsZero() {
        assertThat(Money.ZERO.negate()).isEqualTo(Money.ZERO);
    }

    @Test
    void negateThrowsForLongMinValue() {
        Money min = Money.ofCents(Long.MIN_VALUE);

        assertThatThrownBy(min::negate).isInstanceOf(ArithmeticException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0",
        "-1, 0, -1",
        "1, 0, 1",
        "-1, 1, -1",
        "1250, 1250, 0",
        "-9223372036854775808, 9223372036854775807, -1",
        "9223372036854775807, -9223372036854775808, 1",
        "-9223372036854775808, -9223372036854775808, 0"
    })
    void compareToOrdersByCentsWithoutOverflow(long left, long right, int expectedSign) {
        Money leftMoney = Money.ofCents(left);
        Money rightMoney = Money.ofCents(right);

        assertThat(Integer.signum(leftMoney.compareTo(rightMoney))).isEqualTo(expectedSign);
        assertThat(Integer.signum(rightMoney.compareTo(leftMoney))).isEqualTo(-expectedSign);
        assertThat(leftMoney.compareTo(rightMoney) == 0).isEqualTo(leftMoney.equals(rightMoney));
    }

    @ParameterizedTest
    @CsvSource({
        "100, 0.005, 0",
        "100, 0.015, 2",
        "100, 0.025, 2",
        "-100, 0.005, 0",
        "-100, 0.015, -2",
        "-100, 0.025, -2",
        "100, -0.005, 0",
        "100, -0.015, -2",
        "100, -0.025, -2"
    })
    void timesRoundsHalfEvenToTheCent(long cents, String factor, long expected) {
        assertThat(Money.ofCents(cents).times(new BigDecimal(factor))).isEqualTo(Money.ofCents(expected));
    }

    @Test
    void timesAcceptsAFactorWithSixtyFourSignificantDigits() {
        BigDecimal one = new BigDecimal("1." + "0".repeat(63));

        assertThat(one.precision()).isEqualTo(64);
        assertThat(Money.ofCents(1250).times(one)).isEqualTo(Money.ofCents(1250));
    }

    @Test
    void timesRejectsAFactorWithSixtyFiveSignificantDigits() {
        BigDecimal one = new BigDecimal("1." + "0".repeat(64));
        Money amount = Money.ofCents(1250);

        assertThat(one.precision()).isEqualTo(65);
        assertThatThrownBy(() -> amount.times(one)).isInstanceOf(ArithmeticException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "100, 0.0051, 1",
        "100, 0, 0",
        "-1250, 0, 0",
        "1250, -1, -1250",
        "-1250, -1, 1250",
        "1250, 1.000, 1250",
        "0, 123.456, 0",
        "-9223372036854775808, 1, -9223372036854775808"
    })
    void timesMultipliesExactlyBeforeRounding(long cents, String factor, long expected) {
        assertThat(Money.ofCents(cents).times(new BigDecimal(factor))).isEqualTo(Money.ofCents(expected));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 0.1, 0",
        "1, 0.6, 1",
        "-1, 0.6, -1",
        "1, 0.09, 0",
        "-1, 0.09, 0",
        "1, 1E18, 1000000000000000000",
        "-1, 1E18, -1000000000000000000"
    })
    void timesRoundsProductsAtTheExponentGuardEdges(long cents, String factor, long expected) {
        assertThat(Money.ofCents(cents).times(new BigDecimal(factor))).isEqualTo(Money.ofCents(expected));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1E19",
        "-1, 1E19",
        "10, 9.3E17",
        "9223372036854775807, 2",
        "-9223372036854775808, -1",
        // Long.MAX_VALUE + exactly 0.5 cent rounds half-even up to Long.MAX_VALUE + 1.
        "9223372036854775807, 1.0000000000000000000542101086242752218"
    })
    void timesThrowsWhenTheProductDoesNotFitALong(long cents, String factor) {
        Money amount = Money.ofCents(cents);
        BigDecimal multiplier = new BigDecimal(factor);

        assertThatThrownBy(() -> amount.times(multiplier)).isInstanceOf(ArithmeticException.class);
    }

    @ParameterizedTest
    @CsvSource({
        // Long.MAX_VALUE + just under 0.5 cent is past the limit before rounding but rounds back to it.
        "9223372036854775807, 1.0000000000000000000542101086242752217, 9223372036854775807",
        // Long.MIN_VALUE - about 0.46 cent rounds back to Long.MIN_VALUE.
        "-9223372036854775808, 1.00000000000000000005, -9223372036854775808"
    })
    void timesRoundsBackToTheLongLimitWhenTheProductIsJustPastIt(long cents, String factor, long expected) {
        assertThat(Money.ofCents(cents).times(new BigDecimal(factor))).isEqualTo(Money.ofCents(expected));
    }

    @Test
    void timesRejectsNullFactor() {
        assertThatThrownBy(() -> Money.ZERO.times(null)).isInstanceOf(NullPointerException.class);
    }

    private static final Duration FAST = Duration.ofSeconds(1);

    @Test
    void timesReturnsZeroQuicklyForAFactorWithAHugeNegativeExponent() {
        Money amount = Money.ofCents(Long.MAX_VALUE);

        assertTimeoutPreemptively(FAST, () -> {
            assertThat(amount.times(new BigDecimal("1E-999999999"))).isEqualTo(Money.ZERO);
            assertThat(amount.times(BigDecimal.valueOf(1, Integer.MAX_VALUE))).isEqualTo(Money.ZERO);
        });
    }

    @Test
    void timesThrowsQuicklyForAFactorWithAHugePositiveExponent() {
        Money amount = Money.ofCents(1);

        assertTimeoutPreemptively(FAST, () ->
                assertThatThrownBy(() -> amount.times(new BigDecimal("1E+999999999")))
                        .isInstanceOf(ArithmeticException.class));
    }

    @ParameterizedTest
    @CsvSource({
        "1, -2147483648",
        "100, -2147483647",
        "-9223372036854775808, -2147483648"
    })
    void timesThrowsQuicklyWhenTheLeadingDigitExponentExceedsIntRange(long cents, int factorScale) {
        Money amount = Money.ofCents(cents);
        BigDecimal factor = BigDecimal.valueOf(1, factorScale);

        assertTimeoutPreemptively(FAST, () ->
                assertThatThrownBy(() -> amount.times(factor)).isInstanceOf(ArithmeticException.class));
    }

    @Test
    void compareToRejectsNullOperand() {
        assertThatThrownBy(() -> Money.ZERO.compareTo(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "1, 2, 1, 2",
        "2, 1, 1, 2",
        "-5, 3, -5, 3",
        "3, -5, -5, 3",
        "-7, -2, -7, -2",
        "-2, -7, -7, -2",
        "-9223372036854775808, 9223372036854775807, -9223372036854775808, 9223372036854775807"
    })
    void minAndMaxPickSmallerAndLargerInEitherOrder(long left, long right, long expectedMin, long expectedMax) {
        Money leftMoney = Money.ofCents(left);
        Money rightMoney = Money.ofCents(right);

        assertThat(leftMoney.min(rightMoney)).isEqualTo(Money.ofCents(expectedMin));
        assertThat(leftMoney.max(rightMoney)).isEqualTo(Money.ofCents(expectedMax));
    }

    @ParameterizedTest
    @ValueSource(longs = {Long.MIN_VALUE, -1250, 0, 1250, Long.MAX_VALUE})
    void minAndMaxReturnTheReceiverOnATie(long cents) {
        Money receiver = Money.ofCents(cents);
        Money equalOther = Money.ofCents(cents);

        assertThat(receiver.min(equalOther)).isSameAs(receiver);
        assertThat(receiver.max(equalOther)).isSameAs(receiver);
    }

    @Test
    void minAndMaxRejectNullOperand() {
        assertThatThrownBy(() -> Money.ZERO.min(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.ZERO.max(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "-9223372036854775808, false, true, false",
        "-1, false, true, false",
        "0, true, false, false",
        "1, false, false, true",
        "9223372036854775807, false, false, true"
    })
    void signTestsReflectTheSignOfCents(long cents, boolean zero, boolean negative, boolean positive) {
        Money money = Money.ofCents(cents);

        assertThat(money.isZero()).isEqualTo(zero);
        assertThat(money.isNegative()).isEqualTo(negative);
        assertThat(money.isPositive()).isEqualTo(positive);
    }

    @Test
    void sumOfEmptyCollectionIsZero() {
        assertThat(Money.sum(List.of())).isEqualTo(Money.ZERO);
    }

    @Test
    void sumOfSingleElementIsThatElement() {
        assertThat(Money.sum(List.of(Money.ofCents(-1250)))).isEqualTo(Money.ofCents(-1250));
    }

    @Test
    void sumTotalsMixedSignsExactly() {
        List<Money> amounts = List.of(Money.ofCents(1200), Money.ofCents(-1250), Money.ofCents(305), Money.ofCents(-5));

        assertThat(Money.sum(amounts)).isEqualTo(Money.ofCents(250));
    }

    @Test
    void sumReachingExactlyLongMaxValueSucceeds() {
        List<Money> amounts = List.of(Money.ofCents(Long.MAX_VALUE - 10), Money.ofCents(4), Money.ofCents(6));

        assertThat(Money.sum(amounts)).isEqualTo(Money.ofCents(Long.MAX_VALUE));
    }

    @Test
    void sumThrowsWhenTotalExceedsLongRange() {
        List<Money> aboveMax = List.of(Money.ofCents(Long.MAX_VALUE), Money.ofCents(1));
        List<Money> belowMin = List.of(Money.ofCents(Long.MIN_VALUE), Money.ofCents(-1));

        assertThatThrownBy(() -> Money.sum(aboveMax)).isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> Money.sum(belowMin)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void sumThrowsOnIntermediateOverflowInIterationOrderEvenIfLaterElementsWouldCancelIt() {
        List<Money> amounts = List.of(Money.ofCents(Long.MAX_VALUE), Money.ofCents(1), Money.ofCents(-1));

        assertThatThrownBy(() -> Money.sum(amounts)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void sumRejectsNullCollection() {
        assertThatThrownBy(() -> Money.sum(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void sumRejectsNullElement() {
        List<Money> amounts = Arrays.asList(Money.ofCents(1), null);

        assertThatThrownBy(() -> Money.sum(amounts)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void operationsLeaveOperandsUnchanged() {
        Money left = Money.ofCents(500);
        Money right = Money.ofCents(-120);

        left.plus(right);
        left.minus(right);
        right.negate();
        left.times(new BigDecimal("0.015"));

        assertThat(left).isEqualTo(Money.ofCents(500));
        assertThat(right).isEqualTo(Money.ofCents(-120));
    }
}
