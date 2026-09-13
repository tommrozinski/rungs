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

class HorizonMonthsTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    record Horizon(@HorizonMonths Integer value) {
    }

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {1, 2, 300, 599, 600})
    void nullAndOneToSixHundredAreValid(Integer months) {
        assertThat(templates(months)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 601, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void outsideOneToSixHundredIsHorizonRange(Integer months) {
        assertThat(templates(months)).containsExactly("HORIZON_RANGE");
    }

    private static List<String> templates(Integer months) {
        return VALIDATOR.validate(new Horizon(months)).stream().map(ConstraintViolation::getMessageTemplate).toList();
    }
}
