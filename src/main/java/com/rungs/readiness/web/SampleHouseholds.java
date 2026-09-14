package com.rungs.readiness.web;

/**
 * The five sample households the "Load sample data" control offers, each a fully populated {@link SnapshotForm}
 * keyed by its id. The mortgage payments, balances, and rates are fixed; {@link com.rungs.readiness.domain.model.Mortgage}
 * has no maturity, so none is set here.
 */
final class SampleHouseholds {

    private SampleHouseholds() {
    }

    /** Returns the sample household form for {@code id}, or null when {@code id} names none. */
    static SnapshotForm byId(String id) {
        if (id == null) {
            return null;
        }
        return switch (id) {
            case "buried" -> buried();
            case "board" -> board();
            case "debt-free" -> debtFree();
            case "homestretch" -> homestretch();
            case "paid-off" -> paidOff();
            default -> null;
        };
    }

    private static SnapshotForm buried() {
        SnapshotForm form = new SnapshotForm();
        form.setSample("buried");
        form.setMonthlyTakeHomeIncome("3800");
        form.setMonthlyEssentialExpenses("2100");
        form.setStarterEmergencyFund("150");
        form.setFullEmergencyFund("0");
        form.setRetirementContributionPercent("0");
        form.setHousing("RENT");
        form.setMonthlyRent("1250");
        form.getDebts().add(debtRow("Credit card", "CREDIT_CARD", "6400", "24.9", "190"));
        form.getDebts().add(debtRow("Car loan", "AUTO", "14200", "9.5", "385"));
        form.getDebts().add(debtRow("Medical bill", "MEDICAL", "2300", "0", "75"));
        form.getDebts().add(debtRow("Personal loan", "PERSONAL", "4800", "17", "160"));
        return form;
    }

    private static SnapshotForm board() {
        SnapshotForm form = new SnapshotForm();
        form.setSample("board");
        form.setMonthlyTakeHomeIncome("6200");
        form.setMonthlyEssentialExpenses("2100");
        form.setStarterEmergencyFund("1000");
        form.setFullEmergencyFund("0");
        form.setRetirementContributionPercent("6");
        form.setHousing("RENT");
        form.setMonthlyRent("1400");
        form.getDebts().add(debtRow("Store card", "CREDIT_CARD", "840", "26.99", "35"));
        form.getDebts().add(debtRow("Car loan", "AUTO", "9140", "6.40", "312"));
        form.getDebts().add(debtRow("Student loan", "STUDENT_LOAN", "2500", "4.50", "110"));
        return form;
    }

    private static SnapshotForm debtFree() {
        SnapshotForm form = new SnapshotForm();
        form.setSample("debt-free");
        form.setMonthlyTakeHomeIncome("7800");
        form.setMonthlyEssentialExpenses("2600");
        form.setStarterEmergencyFund("1000");
        form.setFullEmergencyFund("6000");
        form.setRetirementContributionPercent("4");
        form.setHousing("MORTGAGE");
        form.setMortgageBalance("285000");
        form.setMortgageRate("6.5");
        form.setMortgagePayment("2269");
        return form;
    }

    private static SnapshotForm homestretch() {
        SnapshotForm form = new SnapshotForm();
        form.setSample("homestretch");
        form.setMonthlyTakeHomeIncome("11500");
        form.setMonthlyEssentialExpenses("3400");
        form.setStarterEmergencyFund("1000");
        form.setFullEmergencyFund("17000");
        form.setRetirementContributionPercent("15");
        form.setHousing("MORTGAGE");
        form.setMortgageBalance("198000");
        form.setMortgageRate("3.25");
        form.setMortgagePayment("1900");
        return form;
    }

    private static SnapshotForm paidOff() {
        SnapshotForm form = new SnapshotForm();
        form.setSample("paid-off");
        form.setMonthlyTakeHomeIncome("9200");
        form.setMonthlyEssentialExpenses("3100");
        form.setStarterEmergencyFund("1000");
        form.setFullEmergencyFund("18000");
        form.setRetirementContributionPercent("15");
        form.setHousing("MORTGAGE");
        form.setMortgageBalance("0");
        form.setMortgageRate("0");
        form.setMortgagePayment("650");
        return form;
    }

    private static SnapshotForm.DebtRow debtRow(
            String label, String type, String balance, String annualInterestRate, String minimumPayment) {
        SnapshotForm.DebtRow row = new SnapshotForm.DebtRow();
        row.setLabel(label);
        row.setType(type);
        row.setBalance(balance);
        row.setAnnualInterestRate(annualInterestRate);
        row.setMinimumPayment(minimumPayment);
        return row;
    }
}
