package com.rungs.readiness.adapter.in.web.view;

import com.rungs.readiness.application.DebtInput;
import com.rungs.readiness.application.InputText;
import com.rungs.readiness.application.MortgageInput;
import com.rungs.readiness.application.SnapshotInput;
import com.rungs.readiness.application.validation.Rejection;
import com.rungs.readiness.application.validation.SnapshotValidator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the single-field {@link FieldView} an on-blur post re-renders (027 REQ-005): only the posted field's own
 * rules run, through {@link SnapshotValidator#validateProperty}, on a minimal {@code SnapshotInput}, {@code
 * DebtInput}, or {@code MortgageInput} built just to carry that one value.
 */
final class FormFields {

    private static final Pattern DEBT_FIELD = Pattern.compile(
            "debts\\[(\\d+)]\\.(label|balance|annualInterestRate|minimumPayment)");

    private static final Pattern MORTGAGE_FIELD =
            Pattern.compile("mortgage\\.(balance|annualInterestRate|monthlyPayment|homeValue)");

    private static final String ZERO_DISPLAY = "0";

    private static final int DEFAULT_EMERGENCY_FUND_MONTHS = 3;

    private static final String DEBT_LABEL_PREFIX = "Debt ";

    private FormFields() {
    }

    /** Returns the field view for {@code name} and its posted {@code value}, validated through {@code validator}. */
    static FieldView forBlur(String name, String value, SnapshotValidator validator) {
        Matcher debt = DEBT_FIELD.matcher(name);
        if (debt.matches()) {
            return debtField(Integer.parseInt(debt.group(1)), debt.group(2), value, validator);
        }
        Matcher mortgage = MORTGAGE_FIELD.matcher(name);
        if (mortgage.matches()) {
            return mortgageField(mortgage.group(1), value, validator);
        }
        return topLevelField(name, value, validator);
    }

    private static FieldView topLevelField(String property, String value, SnapshotValidator validator) {
        SnapshotInput probe = switch (property) {
            case "monthlyTakeHomeIncome" -> new SnapshotInput(value, null, null, null, null, null, null, null, null, null);
            case "monthlyEssentialExpenses" -> new SnapshotInput(null, value, null, null, null, null, null, null, null, null);
            case "starterEmergencyFund" -> new SnapshotInput(null, null, value, null, null, null, null, null, null, null);
            case "fullEmergencyFund" -> new SnapshotInput(null, null, null, value, null, null, null, null, null, null);
            case "monthlyCollegeContribution" -> new SnapshotInput(null, null, null, null, value, null, null, null, null, null);
            case "retirementContributionPercent" -> new SnapshotInput(null, null, null, null, null, value, null, null, null, null);
            case "emergencyFundMonths" -> new SnapshotInput(null, null, null, null, null, null, months(value), null, null, null);
            default -> throw new IllegalArgumentException("unknown field " + property);
        };
        Optional<Rejection> rejection = validator.validateProperty(probe, property);
        String display = "emergencyFundMonths".equals(property)
                ? (InputText.isBlank(value) ? String.valueOf(DEFAULT_EMERGENCY_FUND_MONTHS) : value)
                : moneyOrPercentDisplay(value);
        return new FieldView(FieldIds.of(property), property, FieldLabels.topLevel(property), display, rejection);
    }

    private static FieldView debtField(int index, String property, String value, SnapshotValidator validator) {
        DebtInput probe = switch (property) {
            case "label" -> new DebtInput(value, null, null, null, null);
            case "balance" -> new DebtInput(null, null, value, null, null);
            case "annualInterestRate" -> new DebtInput(null, null, null, value, null);
            case "minimumPayment" -> new DebtInput(null, null, null, null, value);
            default -> throw new IllegalArgumentException("unknown debt field " + property);
        };
        Optional<Rejection> rejection = validator.validateProperty(probe, property);
        String display = "label".equals(property)
                ? (InputText.isBlank(value) ? DEBT_LABEL_PREFIX + (index + 1) : value)
                : moneyOrPercentDisplay(value);
        String name = "debts[" + index + "]." + property;
        return new FieldView(FieldIds.of(name), name, FieldLabels.debt(property), display, rejection);
    }

    private static FieldView mortgageField(String property, String value, SnapshotValidator validator) {
        MortgageInput probe = switch (property) {
            case "balance" -> new MortgageInput(value, null, null, null);
            case "annualInterestRate" -> new MortgageInput(null, value, null, null);
            case "monthlyPayment" -> new MortgageInput(null, null, value, null);
            case "homeValue" -> new MortgageInput(null, null, null, value);
            default -> throw new IllegalArgumentException("unknown mortgage field " + property);
        };
        Optional<Rejection> rejection = validator.validateProperty(probe, property);
        String name = "mortgage." + property;
        return new FieldView(FieldIds.of(name), name, FieldLabels.mortgage(property), moneyOrPercentDisplay(value),
                rejection);
    }

    private static String moneyOrPercentDisplay(String value) {
        return InputText.isBlank(value) ? ZERO_DISPLAY : value;
    }

    /** Parses {@code value} leniently: blank is null (the default 3 months); unparsable text is left for later. */
    private static Integer months(String value) {
        if (InputText.isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(InputText.strip(value));
        } catch (NumberFormatException notAnInteger) {
            return null;
        }
    }
}
