package com.rungs.readiness.application.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidationCodeTests {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "NEGATIVE          | NEGATIVE         | Enter zero or more.",
        "INVALID_AMOUNT    | INVALID_AMOUNT   | Enter an amount like 1,234.56.",
        "AMOUNT_TOO_LARGE  | AMOUNT_TOO_LARGE | Enter an amount of $1,000,000,000.00 or less.",
        "INVALID_PERCENT   | INVALID_PERCENT  | Enter a percentage like 7.25.",
        "PERCENT_RANGE     | PERCENT_RANGE    | Enter a percentage from 0 to 100.",
        "MONTHS_MIN        | MONTHS_MIN       | Enter 3 months or more.",
        "HORIZON_RANGE     | HORIZON_RANGE    | Enter a horizon from 1 to 600 months.",
        "INVALID_STRATEGY  | INVALID_CHOICE   | Choose snowball or avalanche.",
        "MORTGAGE_AS_DEBT  | MORTGAGE_AS_DEBT | Enter your mortgage in the mortgage section, not as a debt.",
        "INVALID_DEBT_TYPE | INVALID_CHOICE   | Choose a debt type from the list.",
        "TOO_MANY_DEBTS    | TOO_MANY_DEBTS   | Enter 50 debts or fewer.",
        "UNKNOWN_FIELD     | UNKNOWN_FIELD    | Remove this field; it isn't part of the request.",
        "INVALID_TYPE      | INVALID_TYPE     | Send this value as the JSON type the field expects."
    })
    void eachCodeCarriesItsWireCodeAndMessage(ValidationCode code, String wireCode, String message) {
        assertThat(code.code()).isEqualTo(wireCode);
        assertThat(code.message()).isEqualTo(message);
    }

    @ParameterizedTest
    @CsvSource({"3, Use at most 3 decimal places.", "2, Use at most 2 decimal places."})
    void tooPreciseFillsInTheDecimalPlaces(int places, String message) {
        assertThat(ValidationCode.TOO_PRECISE.code()).isEqualTo("TOO_PRECISE");
        assertThat(ValidationCode.TOO_PRECISE.message(places)).isEqualTo(message);
    }

    @ParameterizedTest
    @EnumSource(value = ValidationCode.class, names = "TOO_PRECISE", mode = EnumSource.Mode.EXCLUDE)
    void codesWithoutAPlaceholderIgnoreTheDecimalPlaces(ValidationCode code) {
        assertThat(code.message(3)).isEqualTo(code.message());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void tooPreciseNeedsAPositiveNumberOfPlaces(int places) {
        assertThatThrownBy(() -> ValidationCode.TOO_PRECISE.message(places))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(ValidationCode.class)
    void noResolvedMessageKeepsAPlaceholder(ValidationCode code) {
        assertThat(code.message(2)).doesNotContain("{n}").endsWith(".");
    }
}
