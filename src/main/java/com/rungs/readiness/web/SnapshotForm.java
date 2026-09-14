package com.rungs.readiness.web;

import java.util.ArrayList;
import java.util.List;

/**
 * Backing bean for the snapshot form (S-01). Holds raw strings exactly as submitted; no
 * parsing or validation happens here. Unchecked boxes bind to {@code false} because the
 * fields are primitive {@code boolean}.
 */
public class SnapshotForm {

    private String monthlyTakeHomeIncome = "";
    private String monthlyEssentialExpenses = "";
    private String starterEmergencyFund = "";
    private String fullEmergencyFund = "";
    private String retirementContributionPercent = "";
    private boolean rents;
    private boolean intendsToFundCollege;
    private boolean retired;
    private List<DebtRow> debts = new ArrayList<>();

    public String getMonthlyTakeHomeIncome() {
        return monthlyTakeHomeIncome;
    }

    public void setMonthlyTakeHomeIncome(String monthlyTakeHomeIncome) {
        this.monthlyTakeHomeIncome = monthlyTakeHomeIncome;
    }

    public String getMonthlyEssentialExpenses() {
        return monthlyEssentialExpenses;
    }

    public void setMonthlyEssentialExpenses(String monthlyEssentialExpenses) {
        this.monthlyEssentialExpenses = monthlyEssentialExpenses;
    }

    public String getStarterEmergencyFund() {
        return starterEmergencyFund;
    }

    public void setStarterEmergencyFund(String starterEmergencyFund) {
        this.starterEmergencyFund = starterEmergencyFund;
    }

    public String getFullEmergencyFund() {
        return fullEmergencyFund;
    }

    public void setFullEmergencyFund(String fullEmergencyFund) {
        this.fullEmergencyFund = fullEmergencyFund;
    }

    public String getRetirementContributionPercent() {
        return retirementContributionPercent;
    }

    public void setRetirementContributionPercent(String retirementContributionPercent) {
        this.retirementContributionPercent = retirementContributionPercent;
    }

    public boolean isRents() {
        return rents;
    }

    public void setRents(boolean rents) {
        this.rents = rents;
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

    /** One row of the debts table (S-01). */
    public static class DebtRow {

        private String label = "";
        private String type = "CREDIT_CARD";
        private String balance = "";
        private String annualInterestRate = "";
        private String minimumPayment = "";

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getBalance() {
            return balance;
        }

        public void setBalance(String balance) {
            this.balance = balance;
        }

        public String getAnnualInterestRate() {
            return annualInterestRate;
        }

        public void setAnnualInterestRate(String annualInterestRate) {
            this.annualInterestRate = annualInterestRate;
        }

        public String getMinimumPayment() {
            return minimumPayment;
        }

        public void setMinimumPayment(String minimumPayment) {
            this.minimumPayment = minimumPayment;
        }
    }
}
