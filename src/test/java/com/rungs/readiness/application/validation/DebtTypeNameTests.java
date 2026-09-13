package com.rungs.readiness.application.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.DebtType;
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

class DebtTypeNameTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    record Type(@DebtTypeName String value) {
    }

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    @ParameterizedTest
    @EnumSource(DebtType.class)
    void everyDebtTypeNameIsValid(DebtType type) {
        assertThat(templates(type.name())).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  ", " ", "auto", "Auto", " AUTO", "AUTO ", "CAR", "mortgage", "Mortgage",
        " MORTGAGE", "MORTGAGE "})
    void blankUnknownOrDifferentlyWrittenTypesAreAnInvalidChoice(String type) {
        assertThat(templates(type)).containsExactly("INVALID_DEBT_TYPE");
    }

    @ParameterizedTest
    @ValueSource(strings = "MORTGAGE")
    void exactlyMortgageIsAMortgageAsDebt(String type) {
        assertThat(templates(type)).containsExactly("MORTGAGE_AS_DEBT");
    }

    private static List<String> templates(String type) {
        return VALIDATOR.validate(new Type(type)).stream().map(ConstraintViolation::getMessageTemplate).toList();
    }
}
