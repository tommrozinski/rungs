package com.rungs.readiness.adapter.in.web.view;

import java.util.Map;

/** The one place the form's field labels are spelled out (027 REQ-002), shared by a full render and an on-blur one. */
final class FieldLabels {

    private static final Map<String, String> TOP_LEVEL = Map.of(
            "monthlyTakeHomeIncome", "Take-home income",
            "monthlyEssentialExpenses", "Essential expenses, including rent or mortgage payment",
            "starterEmergencyFund", "Starter emergency fund",
            "fullEmergencyFund", "Full emergency fund",
            "emergencyFundMonths", "Emergency fund months",
            "retirementContributionPercent", "Retirement contribution (%)",
            "monthlyCollegeContribution", "Monthly college contribution");

    private static final Map<String, String> DEBT = Map.of(
            "label", "Label",
            "balance", "Balance",
            "annualInterestRate", "Rate (%)",
            "minimumPayment", "Minimum payment");

    private static final Map<String, String> MORTGAGE = Map.of(
            "balance", "Mortgage balance",
            "annualInterestRate", "Mortgage rate (%)",
            "monthlyPayment", "Mortgage monthly payment",
            "homeValue", "Home value");

    private FieldLabels() {
    }

    /** Returns the label for a top-level {@code SnapshotInput} property name; throws for an unknown one. */
    static String topLevel(String property) {
        return required(TOP_LEVEL, property);
    }

    /** Returns the label for a {@code DebtInput} property name; throws for an unknown one. */
    static String debt(String property) {
        return required(DEBT, property);
    }

    /** Returns the label for a {@code MortgageInput} property name; throws for an unknown one. */
    static String mortgage(String property) {
        return required(MORTGAGE, property);
    }

    private static String required(Map<String, String> labels, String property) {
        String label = labels.get(property);
        if (label == null) {
            throw new IllegalArgumentException("no label for property " + property);
        }
        return label;
    }
}
