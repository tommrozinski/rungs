package com.rungs.readiness.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PercentageTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    record Rates(@Percentage(scale = 3) String rate, @Percentage(scale = 2) String retirement) {
    }

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  ", " ", "0", "-0", "-0.000", "100", "100.00", "7.25", " 7.25 ", "7.", ".5",
        "007", "12.340000", "99.99"})
    void blankAndPlainDecimalsFromZeroToOneHundredAreValidAtEveryScale(String value) {
        assertThat(templates(new Rates(value, value))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1E2", "1e-2", "+5", "--5", "5-", "1,5", "1.2.3", ".", "-", "-.", "7%", "١",
        "７", "0x10", "NaN", "Infinity", "1 5", "123456789012345678901234567890123"})
    void textThatIsNotAPlainDecimalIsAnInvalidPercent(String value) {
        assertThat(templates(new Rates(value, null))).containsExactly("INVALID_PERCENT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.001", "-5", "100.001", "101", "99999999999999999999999999999999"})
    void decimalsOutsideZeroToOneHundredAreOutOfRangeEvenWhenTooPrecise(String value) {
        assertThat(templates(new Rates(value, null))).containsExactly("PERCENT_RANGE");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "7.2500  | ''",
        "7.125   | ''",
        "7.1255  | TOO_PRECISE",
        "0.0001  | TOO_PRECISE"
    }, emptyValue = "")
    void ratesAllowThreeDecimalPlaces(String value, String code) {
        assertThat(templates(new Rates(value, null))).isEqualTo(code.isEmpty() ? List.of() : List.of(code));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "15.25  | ''",
        "15.250 | ''",
        "15.125 | TOO_PRECISE",
        "0.001  | TOO_PRECISE"
    }, emptyValue = "")
    void retirementPercentAllowsTwoDecimalPlaces(String value, String code) {
        assertThat(templates(new Rates(null, value))).isEqualTo(code.isEmpty() ? List.of() : List.of(code));
    }

    private static List<String> templates(Rates rates) {
        return VALIDATOR.validate(rates).stream().map(ConstraintViolation::getMessageTemplate).toList();
    }
}
