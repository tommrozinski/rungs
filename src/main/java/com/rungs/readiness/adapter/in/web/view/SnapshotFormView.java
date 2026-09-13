package com.rungs.readiness.adapter.in.web.view;

import com.rungs.readiness.application.validation.Rejection;
import java.util.List;
import java.util.Optional;

/**
 * Everything {@code snapshot/form.html} needs to render S-01 (027 REQ-002), built by {@link SnapshotFormViews} from a
 * {@code forForm()}-normalized snapshot and its violations, if any.
 *
 * @param income take-home income
 * @param expenses essential expenses, "including rent or mortgage payment"
 * @param starterFund the starter emergency fund
 * @param fullFund the full emergency fund
 * @param emergencyFundMonths months of expenses the full fund covers
 * @param retirementPercent the retirement contribution percentage
 * @param collegeContribution the monthly college contribution
 * @param rents whether the household rents (hides the mortgage section)
 * @param retired whether the household is retired
 * @param intendsToFundCollege whether the household intends to fund college
 * @param debts one row per debt
 * @param debtTypes every {@code DebtType} name offered in each row's type select
 * @param mortgage the mortgage section
 * @param tableError the whole-form 50-debt violation, for the debts table
 * @param summary the C-09 "N field(s) need attention." summary, when there is at least one violation
 */
public record SnapshotFormView(
        FieldView income,
        FieldView expenses,
        FieldView starterFund,
        FieldView fullFund,
        FieldView emergencyFundMonths,
        FieldView retirementPercent,
        FieldView collegeContribution,
        boolean rents,
        boolean retired,
        boolean intendsToFundCollege,
        List<DebtRowView> debts,
        List<String> debtTypes,
        MortgageView mortgage,
        Optional<Rejection> tableError,
        Optional<String> summary) {
}
