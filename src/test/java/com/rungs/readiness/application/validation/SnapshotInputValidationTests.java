package com.rungs.readiness.application.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.rungs.readiness.application.DebtInput;
import com.rungs.readiness.application.HouseholdFlagsInput;
import com.rungs.readiness.application.MortgageInput;
import com.rungs.readiness.application.SnapshotInput;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The constraints as declared on the application input records, through the real Jakarta Validator. */
class SnapshotInputValidationTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    private static final DebtInput CARD = new DebtInput("Card", "CREDIT_CARD", "2500", "21.5", "75");

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    @Test
    void aFullyValidSnapshotHasNoViolations() {
        SnapshotInput input = new SnapshotInput("5000", "3000", "1000", "9000", "100", "15.25", 6, List.of(CARD),
                new MortgageInput("150000", "6.125", "1200", "300000"), new HouseholdFlagsInput(true, false, true));

        assertThat(VALIDATOR.validate(input, FieldsThenWholeForm.class)).isEmpty();
    }

    @Test
    void anEmptySnapshotHasNoViolations() {
        assertThat(VALIDATOR.validate(new SnapshotInput(null, null, null, null, null, null, null, null, null, null)))
                .isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "monthlyTakeHomeIncome         | x      | INVALID_AMOUNT",
        "monthlyEssentialExpenses      | -1     | NEGATIVE",
        "starterEmergencyFund          | 2000000000 | AMOUNT_TOO_LARGE",
        "fullEmergencyFund             | x      | INVALID_AMOUNT",
        "monthlyCollegeContribution    | -1     | NEGATIVE",
        "retirementContributionPercent | 1E2    | INVALID_PERCENT",
        "retirementContributionPercent | 15.125 | TOO_PRECISE"
    })
    void eachTopLevelTextFieldIsConstrained(String field, String value, String code) {
        String[] values = new String[6];
        List<String> fields = List.of("monthlyTakeHomeIncome", "monthlyEssentialExpenses", "starterEmergencyFund",
                "fullEmergencyFund", "monthlyCollegeContribution", "retirementContributionPercent");
        values[fields.indexOf(field)] = value;
        SnapshotInput input = new SnapshotInput(values[0], values[1], values[2], values[3], values[4], values[5], null,
                null, null, null);

        assertThat(violations(input)).containsExactly(tuple(field, code));
    }

    @Test
    void emergencyFundMonthsIsConstrained() {
        SnapshotInput input = new SnapshotInput(null, null, null, null, null, null, 2, null, null, null);

        assertThat(violations(input)).containsExactly(tuple("emergencyFundMonths", "MONTHS_MIN"));
    }

    @Test
    void everyDebtFieldIsConstrainedAtItsIndex() {
        SnapshotInput input = withDebts(CARD, new DebtInput("Loan", "MORTGAGE", "x", "7.1255", "-1"));

        assertThat(violations(input)).containsExactlyInAnyOrder(
                tuple("debts[1].type", "MORTGAGE_AS_DEBT"),
                tuple("debts[1].balance", "INVALID_AMOUNT"),
                tuple("debts[1].annualInterestRate", "TOO_PRECISE"),
                tuple("debts[1].minimumPayment", "NEGATIVE"));
    }

    @Test
    void everyMortgageFieldIsConstrained() {
        SnapshotInput input = new SnapshotInput(null, null, null, null, null, null, null, null,
                new MortgageInput("-1", "101", "x", "1000000000.01"), null);

        assertThat(violations(input)).containsExactlyInAnyOrder(
                tuple("mortgage.balance", "NEGATIVE"),
                tuple("mortgage.annualInterestRate", "PERCENT_RANGE"),
                tuple("mortgage.monthlyPayment", "INVALID_AMOUNT"),
                tuple("mortgage.homeValue", "AMOUNT_TOO_LARGE"));
    }

    @Test
    void aMissingDebtEntryIsAnInvalidTypeAtItsIndex() {
        SnapshotInput input = withDebts(CARD, null);

        assertThat(violations(input)).containsExactly(tuple("debts[1].<list element>", "INVALID_TYPE"));
    }

    @Test
    void aNegativeBalanceIsRejectedEvenThoughItWouldNotBeRemovedAsZero() {
        SnapshotInput input = withDebts(new DebtInput("Refund", "OTHER", "-5", null, null));

        assertThat(violations(input)).containsExactly(tuple("debts[0].balance", "NEGATIVE"));
    }

    @Test
    void aBadTypeOnAZeroBalanceDebtIsStillRejected() {
        SnapshotInput input = withDebts(new DebtInput("Paid off", "BOAT", "0", null, null));

        assertThat(violations(input)).containsExactly(tuple("debts[0].type", "INVALID_DEBT_TYPE"));
    }

    @Test
    void fiftyOneDebtsWithABalanceAreTooMany() {
        assertThat(violations(withDebts(debts(51, 0)))).containsExactly(tuple("debts", "TOO_MANY_DEBTS"));
    }

    @Test
    void fiftyDebtsWithABalanceAreAllowed() {
        assertThat(violations(withDebts(debts(50, 0)))).isEmpty();
    }

    @Test
    void zeroAndBlankBalanceDebtsAreNotCounted() {
        List<DebtInput> debts = new ArrayList<>(Arrays.asList(debts(50, 5)));
        debts.addAll(Collections.nCopies(5, new DebtInput(null, "AUTO", " ", null, null)));

        assertThat(violations(withDebts(debts.toArray(DebtInput[]::new)))).isEmpty();
    }

    @Test
    void theWholeFormRuleIsSkippedWhileATopLevelFieldIsInvalid() {
        SnapshotInput tooMany = withDebts(debts(51, 0));
        SnapshotInput input = new SnapshotInput("x", null, null, null, null, null, null, tooMany.debts(), null, null);

        assertThat(violations(input)).containsExactly(tuple("monthlyTakeHomeIncome", "INVALID_AMOUNT"));
    }

    @Test
    void theWholeFormRuleIsSkippedWhileADebtFieldIsInvalid() {
        DebtInput[] debts = debts(51, 0);
        debts[50] = new DebtInput("Bad", "AUTO", "10", "x", null);

        assertThat(violations(withDebts(debts))).containsExactly(tuple("debts[50].annualInterestRate", "INVALID_PERCENT"));
    }

    @Test
    void theWholeFormRuleAloneToleratesInputTheFieldRulesWouldReject() {
        List<DebtInput> debts = new ArrayList<>(Arrays.asList(debts(50, 0)));
        debts.add(null);
        debts.add(new DebtInput(null, "AUTO", "not money", null, null));
        SnapshotInput input = new SnapshotInput(null, null, null, null, null, null, null, debts, null, null);

        assertThat(VALIDATOR.validate(input, WholeForm.class)).isEmpty();
        assertThat(VALIDATOR.validate(new SnapshotInput(null, null, null, null, null, null, null, null, null, null),
                WholeForm.class)).isEmpty();
    }

    @Test
    void rejectionsResolveTheCodeAndMessage() {
        SnapshotInput input = new SnapshotInput("-1", null, null, null, null, "15.125", null,
                List.of(new DebtInput("Loan", "MORTGAGE", "10", "7.1255", null)), null, null);

        assertThat(VALIDATOR.validate(input, FieldsThenWholeForm.class).stream().map(Rejection::of)).containsExactlyInAnyOrder(
                new Rejection(ValidationCode.NEGATIVE, "Enter zero or more."),
                new Rejection(ValidationCode.TOO_PRECISE, "Use at most 2 decimal places."),
                new Rejection(ValidationCode.MORTGAGE_AS_DEBT,
                        "Enter your mortgage in the mortgage section, not as a debt."),
                new Rejection(ValidationCode.TOO_PRECISE, "Use at most 3 decimal places."));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "monthlyTakeHomeIncome         | -0.01",
        "retirementContributionPercent | 15.125"
    })
    void oneFieldValidatedAloneGivesTheSameRejectionAsFullValidation(String field, String value) {
        SnapshotInput input = "monthlyTakeHomeIncome".equals(field)
                ? new SnapshotInput(value, "x", null, null, null, null, 1, null, null, null)
                : new SnapshotInput("x", null, null, null, null, value, 1, null, null, null);

        List<Rejection> alone = VALIDATOR.validateProperty(input, field).stream().map(Rejection::of).toList();
        List<Rejection> full = VALIDATOR.validate(input, FieldsThenWholeForm.class).stream()
                .filter(violation -> violation.getPropertyPath().toString().equals(field))
                .map(Rejection::of).toList();

        assertThat(alone).hasSize(1).isEqualTo(full);
    }

    private static List<org.assertj.core.groups.Tuple> violations(SnapshotInput input) {
        Set<ConstraintViolation<SnapshotInput>> violations = VALIDATOR.validate(input, FieldsThenWholeForm.class);
        return violations.stream()
                .map(violation -> tuple(violation.getPropertyPath().toString(), violation.getMessageTemplate()))
                .toList();
    }

    /** {@code count} debts, of which the last {@code zeroBalances} have a zero balance. */
    private static DebtInput[] debts(int count, int zeroBalances) {
        DebtInput[] debts = new DebtInput[count];
        for (int index = 0; index < count; index++) {
            String balance = index >= count - zeroBalances ? "0.00" : "100";
            debts[index] = new DebtInput(null, "PERSONAL", balance, "5", "10");
        }
        return debts;
    }

    private static SnapshotInput withDebts(DebtInput... debts) {
        return new SnapshotInput(null, null, null, null, null, null, null, Arrays.asList(debts), null, null);
    }
}
