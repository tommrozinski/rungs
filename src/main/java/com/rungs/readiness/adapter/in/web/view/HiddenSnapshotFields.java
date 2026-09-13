package com.rungs.readiness.adapter.in.web.view;

import com.rungs.readiness.application.DebtInput;
import com.rungs.readiness.application.MortgageInput;
import com.rungs.readiness.application.SnapshotInput;
import java.util.ArrayList;
import java.util.List;

/**
 * Flattens a {@code forForm()}-normalized {@link SnapshotInput} into the hidden fields {@code
 * fragments/hidden-snapshot} renders (029 REQ-004): the same form field names {@code @ModelAttribute} binding uses,
 * so posting them to {@code /} or a stage's call to action reproduces the snapshot exactly.
 */
final class HiddenSnapshotFields {

    private HiddenSnapshotFields() {
    }

    /** Returns every hidden field for {@code normalized}, in a fixed order. */
    static List<HiddenField> of(SnapshotInput normalized) {
        List<HiddenField> fields = new ArrayList<>();
        fields.add(new HiddenField("monthlyTakeHomeIncome", normalized.monthlyTakeHomeIncome()));
        fields.add(new HiddenField("monthlyEssentialExpenses", normalized.monthlyEssentialExpenses()));
        fields.add(new HiddenField("starterEmergencyFund", normalized.starterEmergencyFund()));
        fields.add(new HiddenField("fullEmergencyFund", normalized.fullEmergencyFund()));
        fields.add(new HiddenField("monthlyCollegeContribution", normalized.monthlyCollegeContribution()));
        fields.add(new HiddenField("retirementContributionPercent", normalized.retirementContributionPercent()));
        fields.add(new HiddenField("emergencyFundMonths", String.valueOf(normalized.emergencyFundMonths())));
        fields.add(new HiddenField("householdFlags.rents", flag(normalized.householdFlags().rents())));
        fields.add(new HiddenField("householdFlags.retired", flag(normalized.householdFlags().retired())));
        fields.add(new HiddenField(
                "householdFlags.intendsToFundCollege", flag(normalized.householdFlags().intendsToFundCollege())));
        List<DebtInput> debts = normalized.debts() == null ? List.of() : normalized.debts();
        for (int index = 0; index < debts.size(); index++) {
            addDebt(fields, index, debts.get(index));
        }
        addMortgage(fields, normalized.mortgage());
        return List.copyOf(fields);
    }

    private static void addDebt(List<HiddenField> fields, int index, DebtInput debt) {
        String prefix = "debts[" + index + "]";
        fields.add(new HiddenField(prefix + ".label", debt.label()));
        fields.add(new HiddenField(prefix + ".type", debt.type()));
        fields.add(new HiddenField(prefix + ".balance", debt.balance()));
        fields.add(new HiddenField(prefix + ".annualInterestRate", debt.annualInterestRate()));
        fields.add(new HiddenField(prefix + ".minimumPayment", debt.minimumPayment()));
    }

    private static void addMortgage(List<HiddenField> fields, MortgageInput mortgage) {
        fields.add(new HiddenField("mortgage.balance", mortgage.balance()));
        fields.add(new HiddenField("mortgage.annualInterestRate", mortgage.annualInterestRate()));
        fields.add(new HiddenField("mortgage.monthlyPayment", mortgage.monthlyPayment()));
        fields.add(new HiddenField("mortgage.homeValue", mortgage.homeValue()));
    }

    private static String flag(Boolean value) {
        return String.valueOf(Boolean.TRUE.equals(value));
    }
}
