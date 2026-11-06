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
 * accidentally-valid number. Leading/trailing whitespace is stripped before that cap is applied (not after), so
 * padding a value with whitespace can never buy it extra digits under the cap that would otherwise put it back in
 * range.
 */
class SnapshotForm {

    /** Caps every money, percent, and other numeric text field one past {@link Money#MAX_TEXT_LENGTH}. */
    private static final int NUMERIC_TEXT_MAX_LENGTH = Money.MAX_TEXT_LENGTH + 1;

    /** Caps a debt row's label, in Unicode code points so a surrogate pair (e.g. an emoji) is never split; long
     * enough for a real name, short enough to never balloon a response. Package-private so the form template can
     * drive its {@code maxlength} from this same constant. */
    static final int LABEL_MAX_LENGTH = 60;

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
        this.monthlyTakeHomeIncome = capNumeric(monthlyTakeHomeIncome, NUMERIC_TEXT_MAX_LENGTH);
    }

    public String getMonthlyEssentialExpenses() {
        return monthlyEssentialExpenses;
    }

    public void setMonthlyEssentialExpenses(String monthlyEssentialExpenses) {
        this.monthlyEssentialExpenses = capNumeric(monthlyEssentialExpenses, NUMERIC_TEXT_MAX_LENGTH);
    }

    public String getStarterEmergencyFund() {
        return starterEmergencyFund;
    }

    public void setStarterEmergencyFund(String starterEmergencyFund) {
        this.starterEmergencyFund = capNumeric(starterEmergencyFund, NUMERIC_TEXT_MAX_LENGTH);
    }

    public String getFullEmergencyFund() {
        return fullEmergencyFund;
    }

    public void setFullEmergencyFund(String fullEmergencyFund) {
        this.fullEmergencyFund = capNumeric(fullEmergencyFund, NUMERIC_TEXT_MAX_LENGTH);
    }

    public String getRetirementContributionPercent() {
        return retirementContributionPercent;
    }

    public void setRetirementContributionPercent(String retirementContributionPercent) {
        this.retirementContributionPercent = capNumeric(retirementContributionPercent, NUMERIC_TEXT_MAX_LENGTH);
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
        this.mortgageBalance = capNumeric(mortgageBalance, NUMERIC_TEXT_MAX_LENGTH);
    }

    public String getMortgageRate() {
        return mortgageRate;
    }

    public void setMortgageRate(String mortgageRate) {
        this.mortgageRate = capNumeric(mortgageRate, NUMERIC_TEXT_MAX_LENGTH);
    }

    public String getMortgagePayment() {
        return mortgagePayment;
    }

    public void setMortgagePayment(String mortgagePayment) {
        this.mortgagePayment = capNumeric(mortgagePayment, NUMERIC_TEXT_MAX_LENGTH);
    }

    public String getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(String monthlyRent) {
        this.monthlyRent = capNumeric(monthlyRent, NUMERIC_TEXT_MAX_LENGTH);
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

    /**
     * Returns {@code value} stripped of leading/trailing whitespace, then capped as {@link #cap} would. Stripping
     * before capping (rather than after, as the mapper that later parses this field does on its own) keeps
     * whitespace from ever occupying a character of the cap that a digit otherwise would: without this, an
     * over-long numeric value padded with whitespace could be capped down to exactly {@link Money#MAX_TEXT_LENGTH}
     * digits once the mapper strips it, becoming a valid — if capped — number instead of staying invalid.
     */
    private static String capNumeric(String value, int maxLength) {
        return cap(value == null ? null : value.strip(), maxLength);
    }

    /**
     * Returns {@code value} truncated to at most {@code maxCodePoints} Unicode code points, or {@code value} itself
     * when it is null or already no longer than that. Counting and cutting by code point, rather than by {@code
     * char} as {@link #cap} does, keeps a surrogate pair (e.g. an emoji outside the Basic Multilingual Plane) from
     * ever being split at the boundary into a lone, invalid surrogate.
     */
    private static String capLabel(String value, int maxCodePoints) {
        if (value == null || value.codePointCount(0, value.length()) <= maxCodePoints) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
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
            this.label = capLabel(label, LABEL_MAX_LENGTH);
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
            this.balance = capNumeric(balance, NUMERIC_TEXT_MAX_LENGTH);
        }

        public String getAnnualInterestRate() {
            return annualInterestRate;
        }

        public void setAnnualInterestRate(String annualInterestRate) {
            this.annualInterestRate = capNumeric(annualInterestRate, NUMERIC_TEXT_MAX_LENGTH);
        }

        public String getMinimumPayment() {
            return minimumPayment;
        }

        public void setMinimumPayment(String minimumPayment) {
            this.minimumPayment = capNumeric(minimumPayment, NUMERIC_TEXT_MAX_LENGTH);
        }
    }
}
