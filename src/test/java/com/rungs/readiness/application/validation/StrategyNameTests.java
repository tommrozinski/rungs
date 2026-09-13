package com.rungs.readiness.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.projection.Strategy;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class StrategyNameTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    record Name(@StrategyName String value) {
    }

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(Strategy.class)
    void nullAndEveryStrategyNameAreValid(Strategy strategy) {
        assertThat(templates(strategy == null ? null : strategy.name())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "snowball", "Snowball", " SNOWBALL", "SNOWBALL ", "FASTEST"})
    void blankUnknownOrDifferentlyWrittenNamesAreAnInvalidStrategy(String name) {
        assertThat(templates(name)).containsExactly("INVALID_STRATEGY");
    }

    private static List<String> templates(String name) {
        return VALIDATOR.validate(new Name(name)).stream().map(ConstraintViolation::getMessageTemplate).toList();
    }
}
