package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.Money;
import java.util.ArrayList;
import java.util.List;

/**
 * Backing bean for the snapshot form. Holds raw strings exactly as submitted; no
 * parsing or validation happens here. Unchecked boxes bind to {@code false} because the
 * fields are primitive {@code boolean}.
 *
 * <p>Every setter caps the length of what it stores, so a hostile submission's oversized field can never be echoed
 * back at full size into the form or the result page's hidden fields, however many times the value is copied. A
 * money/percent field is capped one character past {@link Money#MAX_TEXT_LENGTH} rather than at it, so a value long
 * enough to already be invalid stays exactly as invalid after capping instead of being trimmed down to a shorter,
 * accidentally-valid number.
 */
class SnapshotForm {

    /** Caps every money, percent, and other numeric text field one past {@link Money#MAX_TEXT_LENGTH}. */
    private static final int NUMERIC_TEXT_MAX_LENGTH = Money.MAX_TEXT_LENGTH + 1;

    /** Caps a debt row's label; long enough for a real name, short enough to never balloon a response. */
    private static final int LABEL_MAX_LENGTH = 60;

    /** Caps a debt row's {@code type}, whose only real values ({@link com.rungs.readiness.domain.model.DebtType}
     * names) are a few characters long. */
    private static final int DEBT_TYPE_MAX_LENGTH = 16;

    /** Caps {@code housing}, whose only real values ({@code MORTGAGE}, {@code RENT}) are a few characters long. */
    private static final int HOUSING_MAX_LENGTH = 16;

    /** Caps {@code sample}, whose only real values are {@link SampleHousehold} ids, a few characters long. */
    private static final int SAMPLE_MAX_LENGTH = 32;

    private String monthlyTakeHomeIncome = "";
    private String monthlyEssentialExpenses = "";
    private String starterEmergencyFund = "";
    private String fullEmergencyFund = "";
    private String retirementContributionPercent = "";
    private String sample = "";
    private String housing = "MORTGAGE";
    private String mortgageBalance = "";
    private String mortgageRate = "";
    private String mortgagePayment = "";
    private String monthlyRent = "";
    private boolean intendsToFundCollege;
    private boolean retired;
    private List<DebtRow> debts = new ArrayList<>();

    public String getMonthlyTakeHomeIncome() {
        return monthlyTakeHomeIncome;
    }

    public void setMonthlyTakeHomeIncome(String monthlyTakeHomeIncome) {
        this.monthlyTakeHomeIncome = cap(monthlyTakeHomeIncome, NUMERIC_TEXT_MAX_LENGTH);
    }

    public String getMonthlyEssentialExpenses() {
        return monthlyEssentialExpenses;
    }

    public void setMonthlyEssentialExpenses(String monthlyEssentialExpenses) {
        this.monthlyEssentialExpenses = cap(monthlyEssentialExpenses, NUMERIC_TEXT_MAX_LENGTH);
    }

    public String getStarterEmergencyFund() {
        return starterEmergencyFund;
    }

    public void setStarterEmergencyFund(String starterEmergencyFund) {
        this.starterEmergencyFund = cap(starterEmergencyFund, NUMERIC_TEXT_MAX_LENGTH);
    }

    public String getFullEmergencyFund() {
        return fullEmergencyFund;
    }

    public void setFullEmergencyFund(String fullEmergencyFund) {
        this.fullEmergencyFund = cap(fullEmergencyFund, NUMERIC_TEXT_MAX_LENGTH);
    }

    public String getRetirementContributionPercent() {
        return retirementContributionPercent;
    }

    public void setRetirementContributionPercent(String retirementContributionPercent) {
        this.retirementContributionPercent = cap(retirementContributionPercent, NUMERIC_TEXT_MAX_LENGTH);
    }

    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = cap(sample, SAMPLE_MAX_LENGTH);
    }

    public String getHousing() {
        return housing;
    }

    public void setHousing(String housing) {
        this.housing = cap(housing, HOUSING_MAX_LENGTH);
    }

    public String getMortgageBalance() {
        return mortgageBalance;
    }

    public void setMortgageBalance(String mortgageBalance) {
        this.mortgageBalance = cap(mortgageBalance, NUMERIC_TEXT_MAX_LENGTH);
    }

    public String getMortgageRate() {
        return mortgageRate;
    }

    public void setMortgageRate(String mortgageRate) {
        this.mortgageRate = cap(mortgageRate, NUMERIC_TEXT_MAX_LENGTH);
    }

    public String getMortgagePayment() {
        return mortgagePayment;
    }

    public void setMortgagePayment(String mortgagePayment) {
        this.mortgagePayment = cap(mortgagePayment, NUMERIC_TEXT_MAX_LENGTH);
    }

    public String getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(String monthlyRent) {
        this.monthlyRent = cap(monthlyRent, NUMERIC_TEXT_MAX_LENGTH);
    }

    public boolean isIntendsToFundCollege() {
        return intendsToFundCollege;
    }

    public void setIntendsToFundCollege(boolean intendsToFundCollege) {
        this.intendsToFundCollege = intendsToFundCollege;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public List<DebtRow> getDebts() {
        return debts;
    }

    public void setDebts(List<DebtRow> debts) {
        this.debts = debts;
    }

    /**
     * Returns {@code value} truncated to at most {@code maxLength} characters, or {@code value} itself when it is
     * null or already no longer than that.
     */
    private static String cap(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /** One row of the debts table. */
    static class DebtRow {

        private String label = "";
        private String type = "CREDIT_CARD";
        private String balance = "";
        private String annualInterestRate = "";
        private String minimumPayment = "";

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = cap(label, LABEL_MAX_LENGTH);
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = cap(type, DEBT_TYPE_MAX_LENGTH);
        }

        public String getBalance() {
            return balance;
        }

        public void setBalance(String balance) {
            this.balance = cap(balance, NUMERIC_TEXT_MAX_LENGTH);
        }

        public String getAnnualInterestRate() {
            return annualInterestRate;
        }

        public void setAnnualInterestRate(String annualInterestRate) {
            this.annualInterestRate = cap(annualInterestRate, NUMERIC_TEXT_MAX_LENGTH);
        }

        public String getMinimumPayment() {
            return minimumPayment;
        }

        public void setMinimumPayment(String minimumPayment) {
            this.minimumPayment = cap(minimumPayment, NUMERIC_TEXT_MAX_LENGTH);
        }
    }
}
