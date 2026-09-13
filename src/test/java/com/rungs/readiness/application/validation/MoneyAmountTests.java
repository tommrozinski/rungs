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

class MoneyAmountTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    record Amount(@MoneyAmount String value) {
    }

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", " ", "0", "-0", "-$0.00", "1,234.56", "$1,000,000,000.00", "1000000000",
        " 12.5 ", "999999999.99"})
    void blankAndAmountsFromZeroToOneBillionAreValid(String value) {
        assertThat(templates(value)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "abc                  | INVALID_AMOUNT",
        "12.345               | INVALID_AMOUNT",
        "1,00                 | INVALID_AMOUNT",
        "1e3                  | INVALID_AMOUNT",
        "123456789012345678901234567890123 | INVALID_AMOUNT",
        "99999999999999999999 | INVALID_AMOUNT",
        "-0.01                | NEGATIVE",
        "-5                   | NEGATIVE",
        "-2000000000          | NEGATIVE",
        "1000000000.01        | AMOUNT_TOO_LARGE",
        "$92,233,720,368,547,758.07 | AMOUNT_TOO_LARGE"
    })
    void reportsOnlyTheFirstFailingRule(String value, String code) {
        assertThat(templates(value)).containsExactly(code);
    }

    private static List<String> templates(String value) {
        return VALIDATOR.validate(new Amount(value)).stream().map(ConstraintViolation::getMessageTemplate).toList();
    }
}
