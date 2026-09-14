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

    public String getSample() {
        return sample;
    }

    public void setSample(String sample) {
        this.sample = sample;
    }

    public String getHousing() {
        return housing;
    }

    public void setHousing(String housing) {
        this.housing = housing;
    }

    public String getMortgageBalance() {
        return mortgageBalance;
    }

    public void setMortgageBalance(String mortgageBalance) {
        this.mortgageBalance = mortgageBalance;
    }

    public String getMortgageRate() {
        return mortgageRate;
    }

    public void setMortgageRate(String mortgageRate) {
        this.mortgageRate = mortgageRate;
    }

    public String getMortgagePayment() {
        return mortgagePayment;
    }

    public void setMortgagePayment(String mortgagePayment) {
        this.mortgagePayment = mortgagePayment;
    }

    public String getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(String monthlyRent) {
        this.monthlyRent = monthlyRent;
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
