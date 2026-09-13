package com.rungs.readiness.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class EmergencyFundMonthsTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    record Months(@EmergencyFundMonths Integer value) {
    }

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {3, 4, 6, 7, Integer.MAX_VALUE})
    void nullAndThreeOrMoreAreValid(Integer months) {
        assertThat(templates(months)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 0, -1, Integer.MIN_VALUE})
    void belowThreeIsMonthsMin(Integer months) {
        assertThat(templates(months)).containsExactly("MONTHS_MIN");
    }

    private static List<String> templates(Integer months) {
        return VALIDATOR.validate(new Months(months)).stream().map(ConstraintViolation::getMessageTemplate).toList();
    }
}
