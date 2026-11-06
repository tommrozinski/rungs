package com.rungs.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MoneyFormattingTests {

    /** Default locales that change decimal and grouping separators (de-DE, fr-FR) or digit glyphs (ar-EG). */
    private static final List<String> NON_US_LOCALE_TAGS = List.of("de-DE", "fr-FR", "ar-EG-u-nu-arab");

    private Locale originalDefault;

    @BeforeEach
    void captureDefaultLocale() {
        originalDefault = Locale.getDefault();
    }

    @AfterEach
    void restoreDefaultLocale() {
        Locale.setDefault(originalDefault);
    }

    static Stream<Arguments> amounts() {
        return Stream.of(
                Arguments.of(0L, "0.00", "$0.00"),
                Arguments.of(1L, "0.01", "$0.01"),
                Arguments.of(10L, "0.10", "$0.10"),
                Arguments.of(99L, "0.99", "$0.99"),
                Arguments.of(100L, "1.00", "$1.00"),
                Arguments.of(123_456L, "1234.56", "$1,234.56"),
                Arguments.of(100_000_000L, "1000000.00", "$1,000,000.00"),
                Arguments.of(-1L, "-0.01", "-$0.01"),
                Arguments.of(-123_456L, "-1234.56", "-$1,234.56"),
                Arguments.of(Long.MAX_VALUE, "92233720368547758.07", "$92,233,720,368,547,758.07"),
                Arguments.of(Long.MIN_VALUE, "-92233720368547758.08", "-$92,233,720,368,547,758.08"),
                Arguments.of(99_999L, "999.99", "$999.99"),
                Arguments.of(100_000L, "1000.00", "$1,000.00"),
                Arguments.of(99_999_999L, "999999.99", "$999,999.99"),
                Arguments.of(100_000_000_000L, "1000000000.00", "$1,000,000,000.00"));
    }

    static Stream<Arguments> amountsUnderNonUsLocales() {
        return NON_US_LOCALE_TAGS.stream()
                .flatMap(tag -> amounts().map(amount -> Arguments.of(prepend(tag, amount.get()))));
    }

    private static Object[] prepend(String tag, Object[] amount) {
        return Stream.concat(Stream.of(tag), Stream.of(amount)).toArray();
    }

    @ParameterizedTest
    @MethodSource("amounts")
    void formatsUnderJvmDefaultLocale(long cents, String bare, String display) {
        assertFormats("JVM default " + originalDefault.toLanguageTag(), cents, bare, display);
    }

    @ParameterizedTest(name = "[{index}] {0}: {1} cents")
    @MethodSource("amountsUnderNonUsLocales")
    void formatsIdenticallyUnderNonUsDefaultLocale(String localeTag, long cents, String bare, String display) {
        Locale.setDefault(Locale.forLanguageTag(localeTag));

        assertFormats(localeTag, cents, bare, display);
    }

    @Test
    void zeroNeverRendersWithMinusSign() {
        Money negatedZero = Money.ZERO.negate();

        assertThat(negatedZero.toBareString()).isEqualTo("0.00");
        assertThat(negatedZero.toDisplayString()).isEqualTo("$0.00");
    }

    // String concatenation, not String.format, so the failure message itself stays locale-independent.
    private static void assertFormats(String localeTag, long cents, String bare, String display) {
        Money money = Money.ofCents(cents);
        String context = " for " + Long.toString(cents) + " cents under locale " + localeTag;

        assertThat(money.toBareString()).as("toBareString" + context).isEqualTo(bare);
        assertThat(money.toDisplayString()).as("toDisplayString" + context).isEqualTo(display);
    }
}
