package com.rungs.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyParsingTests {

    @Nested
    class FormatException {

        @ParameterizedTest
        @EnumSource(MoneyFormatException.Reason.class)
        void reasonRoundTrips(MoneyFormatException.Reason reason) {
            assertThat(new MoneyFormatException(reason).reason()).isEqualTo(reason);
        }

        @ParameterizedTest
        @EnumSource(MoneyFormatException.Reason.class)
        void isAnIllegalArgumentException(MoneyFormatException.Reason reason) {
            assertThat(new MoneyFormatException(reason)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @EnumSource(MoneyFormatException.Reason.class)
        void messageIsAFixedSentence(MoneyFormatException.Reason reason) {
            String message = new MoneyFormatException(reason).getMessage();

            assertThat(message).isNotBlank().endsWith(".");
            assertThat(new MoneyFormatException(reason).getMessage()).isEqualTo(message);
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', textBlock = """
                BLANK               | Amount is blank.
                TOO_LONG            | Amount is too long.
                INVALID_CHARACTER   | Amount contains an invalid character.
                MISSING_DIGITS      | Amount is missing digits.
                MISPLACED_SEPARATOR | Amount has a misplaced thousands separator.
                TOO_MANY_DECIMALS   | Amount has more than two decimal places.
                OUT_OF_RANGE        | Amount is out of range.
                """)
        void messageIsTheSentenceForItsReason(MoneyFormatException.Reason reason, String expected) {
            assertThat(new MoneyFormatException(reason).getMessage()).isEqualTo(expected);
        }

        @Test
        void everyReasonHasADistinctMessage() {
            List<String> messages = Arrays.stream(MoneyFormatException.Reason.values())
                    .map(reason -> new MoneyFormatException(reason).getMessage())
                    .toList();

            assertThat(messages).doesNotHaveDuplicates();
        }

        @Test
        void rejectsNullReason() {
            assertThatThrownBy(() -> new MoneyFormatException(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Parse {

        @ParameterizedTest
        @CsvSource(delimiter = '|', textBlock = """
                1234    | 123400
                1234.5  | 123450
                1234.56 | 123456
                0.07    | 7
                $1,234.56 | 123456
                -$50      | -5000
                -1234.56  | -123456
                $0.99     | 99
                1,000,000 | 100000000
                999,999   | 99999900
                '  $1,234.56  ' | 123456
                -0              | 0
                -$0.00          | 0
                007             | 700
                00.05           | 5
                92233720368547758.07  | 9223372036854775807
                -92233720368547758.08 | -9223372036854775808
                """)
        void acceptsValidAmounts(String text, long expectedCents) {
            assertThat(Money.parse(text)).isEqualTo(Money.ofCents(expectedCents));
        }

        @Test
        void toleratesUnicodeWhitespaceAtTheEnds() {
            assertThat(Money.parse("\u00A0\t12.50\n\u2003")).isEqualTo(Money.ofCents(1250));
        }

        @ParameterizedTest
        @ValueSource(strings = {"\u00A0", "\u2007", "\u202F"})
        void toleratesNonBreakingSpacesAtTheEnds(String space) {
            assertThat(Money.parse(space + "12.50" + space)).isEqualTo(Money.ofCents(1250));
        }

        @Test
        void rejectsNonBreakingSpaceInsideTheNumber() {
            assertRejected("1\u00A0234", MoneyFormatException.Reason.INVALID_CHARACTER);
        }

        @Test
        void rejectsOnlyNonBreakingSpacesAsBlank() {
            assertRejected("\u00A0\u2007\u202F", MoneyFormatException.Reason.BLANK);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   ", "\t\n"})
        void rejectsBlankInput(String text) {
            assertRejected(text, MoneyFormatException.Reason.BLANK);
        }

        @Test
        void rejectsThirtyThreeCharactersAsTooLong() {
            String text = " ".repeat(29) + "1.00";

            assertThat(text).hasSize(33);
            assertRejected(text, MoneyFormatException.Reason.TOO_LONG);
            assertRejected("0".repeat(30) + "1.5", MoneyFormatException.Reason.TOO_LONG);
        }

        @Test
        void checksLengthBeforeTrimming() {
            assertRejected(" ".repeat(40), MoneyFormatException.Reason.TOO_LONG);
        }

        @ParameterizedTest
        @ValueSource(strings = {"12a", "\u20AC5", "$-5", "--5", "$$5", "1.2.3", "\u0661\u0662", "5-", "1 234", "+5"})
        void rejectsInvalidCharacters(String text) {
            assertRejected(text, MoneyFormatException.Reason.INVALID_CHARACTER);
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', textBlock = """
                $        | MISSING_DIGITS
                -        | MISSING_DIGITS
                -$       | MISSING_DIGITS
                .50      | MISSING_DIGITS
                1234,567 | MISPLACED_SEPARATOR
                1,23     | MISPLACED_SEPARATOR
                ,123     | MISPLACED_SEPARATOR
                1,234,   | MISPLACED_SEPARATOR
                1,,234   | MISPLACED_SEPARATOR
                1,,23    | MISPLACED_SEPARATOR
                1,2,3    | MISPLACED_SEPARATOR
                1,23.456 | MISPLACED_SEPARATOR
                """)
        void rejectsMalformedWholeDollars(String text, MoneyFormatException.Reason reason) {
            assertRejected(text, reason);
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', textBlock = """
                1.2,3   | MISPLACED_SEPARATOR
                1.,     | MISPLACED_SEPARATOR
                5.      | MISSING_DIGITS
                -$5.    | MISSING_DIGITS
                12.345  | TOO_MANY_DECIMALS
                $0.001  | TOO_MANY_DECIMALS
                """)
        void rejectsMalformedCents(String text, MoneyFormatException.Reason reason) {
            assertRejected(text, reason);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "92233720368547758.08",
            "-92233720368547758.09",
            "$92,233,720,368,547,758.08",
            "99999999999999999999",
            "-999999999999999999999.99"
        })
        void rejectsAmountsBeyondLongCents(String text) {
            assertRejected(text, MoneyFormatException.Reason.OUT_OF_RANGE);
        }

        @Test
        void acceptsExactlyThirtyTwoCharacters() {
            String text = " ".repeat(28) + "1.00";

            assertThat(text).hasSize(32);
            assertThat(Money.parse(text)).isEqualTo(Money.ofCents(100));
            assertThat(Money.parse("0".repeat(29) + "1.5")).isEqualTo(Money.ofCents(150));
        }

        private static void assertRejected(String text, MoneyFormatException.Reason reason) {
            assertThatThrownBy(() -> Money.parse(text))
                    .isInstanceOfSatisfying(MoneyFormatException.class, rejected -> {
                        assertThat(rejected.reason()).isEqualTo(reason);
                        assertThat(rejected.getCause()).isNull();
                        if (text != null && !text.isBlank()) {
                            assertThat(rejected.getMessage()).doesNotContain(text);
                        }
                    });
        }
    }

    @Nested
    class RoundTrip {

        static LongStream edgeCents() {
            return LongStream.of(0, 1, -1, 50, -50, 123456, -123456, Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE + 1);
        }

        @ParameterizedTest
        @MethodSource("edgeCents")
        void parsesTheBareString(long cents) {
            Money amount = Money.ofCents(cents);

            assertThat(Money.parse(amount.toBareString())).isEqualTo(amount);
        }

        @ParameterizedTest
        @MethodSource("edgeCents")
        void parsesTheDisplayString(long cents) {
            Money amount = Money.ofCents(cents);

            assertThat(Money.parse(amount.toDisplayString())).isEqualTo(amount);
        }
    }
}
