package com.rungs.readiness.adapter.in.web.view;

import com.rungs.readiness.application.DebtInput;
import com.rungs.readiness.application.MortgageInput;
import com.rungs.readiness.application.SnapshotInput;
import com.rungs.readiness.application.validation.FieldRejection;
import com.rungs.readiness.application.validation.Rejection;
import com.rungs.readiness.domain.model.DebtType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Builds a {@link SnapshotFormView} from a {@code forForm()}-normalized {@link SnapshotInput} and its violations. */
final class SnapshotFormViews {

    private static final List<String> DEBT_TYPES =
            Arrays.stream(DebtType.values()).map(Enum::name).toList();

    private static final String REMOVE_DEBT_ACTION_PREFIX = "/snapshot/debts/remove/";

    private SnapshotFormViews() {
    }

    /** Returns the view for {@code normalized}, marking each field named in {@code violations} as an error. */
    static SnapshotFormView of(SnapshotInput normalized, List<FieldRejection> violations) {
        Map<String, Rejection> errors = FormErrors.byName(violations);
        List<DebtRowView> debts = debtRows(normalized.debts(), errors);
        MortgageInput mortgage = normalized.mortgage();
        boolean rents = Boolean.TRUE.equals(normalized.householdFlags().rents());
        MortgageView mortgageView = new MortgageView(
                field("mortgage.balance", FieldLabels.mortgage("balance"), mortgage.balance(), errors),
                field("mortgage.annualInterestRate", FieldLabels.mortgage("annualInterestRate"),
                        mortgage.annualInterestRate(), errors),
                field("mortgage.monthlyPayment", FieldLabels.mortgage("monthlyPayment"), mortgage.monthlyPayment(),
                        errors),
                field("mortgage.homeValue", FieldLabels.mortgage("homeValue"), mortgage.homeValue(), errors), !rents);
        return new SnapshotFormView(
                field("monthlyTakeHomeIncome", FieldLabels.topLevel("monthlyTakeHomeIncome"),
                        normalized.monthlyTakeHomeIncome(), errors),
                field("monthlyEssentialExpenses", FieldLabels.topLevel("monthlyEssentialExpenses"),
                        normalized.monthlyEssentialExpenses(), errors),
                field("starterEmergencyFund", FieldLabels.topLevel("starterEmergencyFund"),
                        normalized.starterEmergencyFund(), errors),
                field("fullEmergencyFund", FieldLabels.topLevel("fullEmergencyFund"), normalized.fullEmergencyFund(),
                        errors),
                field("emergencyFundMonths", FieldLabels.topLevel("emergencyFundMonths"),
                        String.valueOf(normalized.emergencyFundMonths()), errors),
                field("retirementContributionPercent", FieldLabels.topLevel("retirementContributionPercent"),
                        normalized.retirementContributionPercent(), errors),
                field("monthlyCollegeContribution", FieldLabels.topLevel("monthlyCollegeContribution"),
                        normalized.monthlyCollegeContribution(), errors),
                rents,
                Boolean.TRUE.equals(normalized.householdFlags().retired()),
                Boolean.TRUE.equals(normalized.householdFlags().intendsToFundCollege()),
                debts, DEBT_TYPES, mortgageView, FormErrors.forTable(violations), FormErrors.summary(violations));
    }

    private static List<DebtRowView> debtRows(List<DebtInput> debts, Map<String, Rejection> errors) {
        List<DebtRowView> rows = new ArrayList<>();
        for (int index = 0; index < debts.size(); index++) {
            DebtInput debt = debts.get(index);
            String prefix = "debts[" + index + "]";
            String typeName = prefix + ".type";
            rows.add(new DebtRowView(
                    field(prefix + ".label", FieldLabels.debt("label"), debt.label(), errors),
                    FieldIds.of(typeName), typeName, debt.type() == null ? "" : debt.type(),
                    field(prefix + ".balance", FieldLabels.debt("balance"), debt.balance(), errors),
                    field(prefix + ".annualInterestRate", FieldLabels.debt("annualInterestRate"),
                            debt.annualInterestRate(), errors),
                    field(prefix + ".minimumPayment", FieldLabels.debt("minimumPayment"), debt.minimumPayment(),
                            errors),
                    REMOVE_DEBT_ACTION_PREFIX + index));
        }
        return rows;
    }

    private static FieldView field(String name, String label, String value, Map<String, Rejection> errors) {
        return new FieldView(FieldIds.of(name), name, label, value, Optional.ofNullable(errors.get(name)));
    }
}
