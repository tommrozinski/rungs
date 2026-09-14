package com.rungs.readiness.web;

/**
 * Builds the plain-text echo shown at the bottom of the result page for this phase, proving
 * the submitted values survived the round trip from {@code POST /assess}. Phase 3 removes
 * this in favor of a real assessment.
 */
final class SnapshotEcho {

    private SnapshotEcho() {
    }

    static String render(SnapshotForm form) {
        StringBuilder text = new StringBuilder();
        text.append("monthlyTakeHomeIncome=").append(form.getMonthlyTakeHomeIncome()).append('\n');
        text.append("monthlyEssentialExpenses=").append(form.getMonthlyEssentialExpenses()).append('\n');
        text.append("starterEmergencyFund=").append(form.getStarterEmergencyFund()).append('\n');
        text.append("fullEmergencyFund=").append(form.getFullEmergencyFund()).append('\n');
        text.append("retirementContributionPercent=").append(form.getRetirementContributionPercent()).append('\n');
        text.append("rents=").append(form.isRents()).append('\n');
        text.append("intendsToFundCollege=").append(form.isIntendsToFundCollege()).append('\n');
        text.append("retired=").append(form.isRetired()).append('\n');
        for (int i = 0; i < form.getDebts().size(); i++) {
            SnapshotForm.DebtRow row = form.getDebts().get(i);
            text.append("debts[").append(i).append("].label=").append(row.getLabel()).append('\n');
            text.append("debts[").append(i).append("].type=").append(row.getType()).append('\n');
            text.append("debts[").append(i).append("].balance=").append(row.getBalance()).append('\n');
            text.append("debts[").append(i).append("].annualInterestRate=").append(row.getAnnualInterestRate()).append('\n');
            text.append("debts[").append(i).append("].minimumPayment=").append(row.getMinimumPayment()).append('\n');
        }
        return text.toString();
    }
}
