package com.rungs.readiness.web;

import java.util.Arrays;
import java.util.Optional;

/**
 * The five sample households the "Load sample data" control offers: each constant's {@code id} (the select
 * option's value), its {@code label} (the option's display text), and the fully populated {@link SnapshotForm} it
 * fills the form with. The single source for both the select's options and the id-to-form lookup, so the two never
 * drift apart. The mortgage payments, balances, and rates are fixed.
 */
enum SampleHousehold {

    BURIED("buried", "Buried in debt") {
        @Override
        SnapshotForm form() {
            SnapshotForm form = blank(id());
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
    },

    BOARD("board", "Paying down debt") {
        @Override
        SnapshotForm form() {
            SnapshotForm form = blank(id());
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
    },

    DEBT_FREE("debt-free", "Debt-free, building savings") {
        @Override
        SnapshotForm form() {
            SnapshotForm form = blank(id());
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
    },

    HOMESTRETCH("homestretch", "Paying off the house") {
        @Override
        SnapshotForm form() {
            SnapshotForm form = blank(id());
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
    },

    PAID_OFF("paid-off", "Paid-off home, no debt") {
        @Override
        SnapshotForm form() {
            SnapshotForm form = blank(id());
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
    };

    private final String id;
    private final String label;

    SampleHousehold(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    /** Returns this sample household's fully populated form. */
    abstract SnapshotForm form();

    /** Returns the sample household whose id is {@code id}, or empty when {@code id} names none. */
    static Optional<SampleHousehold> byId(String id) {
        return Arrays.stream(values()).filter(sample -> sample.id.equals(id)).findFirst();
    }

    /** Returns a new form with {@code sample} set to {@code id}, ready for the constant's own fields. */
    private static SnapshotForm blank(String id) {
        SnapshotForm form = new SnapshotForm();
        form.setSample(id);
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
