package com.rungs.readiness.adapter.in.web.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.application.validation.SnapshotValidator;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/** {@link FormFields#forBlur}: on-blur, single-field re-render (027 REQ-005). */
class FormFieldsTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();

    private static final SnapshotValidator VALIDATOR = new SnapshotValidator(FACTORY.getValidator());

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    @Test
    void aValidTopLevelFieldHasNoError() {
        FieldView field = FormFields.forBlur("monthlyTakeHomeIncome", "5000", VALIDATOR);

        assertThat(field.name()).isEqualTo("monthlyTakeHomeIncome");
        assertThat(field.id()).isEqualTo("monthlyTakeHomeIncome");
        assertThat(field.value()).isEqualTo("5000");
        assertThat(field.state()).isEmpty();
    }

    @Test
    void anInvalidTopLevelFieldCarriesTheApisMessage() {
        FieldView field = FormFields.forBlur("monthlyTakeHomeIncome", "-1", VALIDATOR);

        assertThat(field.state()).isEqualTo("error");
        assertThat(field.errorMessage()).isEqualTo("Enter zero or more.");
    }

    @Test
    void aBlankTopLevelMoneyFieldDisplaysAsZero() {
        assertThat(FormFields.forBlur("fullEmergencyFund", "", VALIDATOR).value()).isEqualTo("0");
    }

    @Test
    void aBlankEmergencyFundMonthsDisplaysAsThree() {
        assertThat(FormFields.forBlur("emergencyFundMonths", "", VALIDATOR).value()).isEqualTo("3");
    }

    @Test
    void aTooSmallEmergencyFundMonthsIsAnError() {
        FieldView field = FormFields.forBlur("emergencyFundMonths", "2", VALIDATOR);

        assertThat(field.state()).isEqualTo("error");
        assertThat(field.errorMessage()).isEqualTo("Enter 3 months or more.");
    }

    @Test
    void aDebtBalanceFieldValidatesAgainstTheDebtInputAtItsIndex() {
        FieldView field = FormFields.forBlur("debts[3].balance", "-5", VALIDATOR);

        assertThat(field.name()).isEqualTo("debts[3].balance");
        assertThat(field.id()).isEqualTo("debts-3-balance");
        assertThat(field.state()).isEqualTo("error");
        assertThat(field.errorMessage()).isEqualTo("Enter zero or more.");
    }

    @Test
    void aBlankDebtLabelDisplaysAsDebtByPosition() {
        assertThat(FormFields.forBlur("debts[2].label", "", VALIDATOR).value()).isEqualTo("Debt 3");
    }

    @Test
    void aSuppliedDebtLabelIsKeptAsIs() {
        assertThat(FormFields.forBlur("debts[0].label", "Car loan", VALIDATOR).value()).isEqualTo("Car loan");
    }

    @Test
    void aMortgageFieldValidatesAgainstTheMortgageInput() {
        FieldView field = FormFields.forBlur("mortgage.annualInterestRate", "101", VALIDATOR);

        assertThat(field.name()).isEqualTo("mortgage.annualInterestRate");
        assertThat(field.id()).isEqualTo("mortgage-annualInterestRate");
        assertThat(field.state()).isEqualTo("error");
        assertThat(field.errorMessage()).isEqualTo("Enter a percentage from 0 to 100.");
    }

    @Test
    void aBlankMortgageFieldDisplaysAsZero() {
        assertThat(FormFields.forBlur("mortgage.homeValue", "  ", VALIDATOR).value()).isEqualTo("0");
    }

    @Test
    void anUnknownFieldNameIsRejected() {
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> FormFields.forBlur("nope", "1", VALIDATOR)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
