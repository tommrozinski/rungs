package com.rungs.readiness.web;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * A fluent builder for the snapshot form's POST parameters, so controller tests can describe a household in a few
 * chained calls instead of a long run of individual {@code .param(...)} calls on the request builder. Every setter
 * returns {@code this}; {@link #values()} hands the finished parameters to
 * {@link org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder#params}.
 */
final class FormParams {

    private final MultiValueMap<String, String> values = new LinkedMultiValueMap<>();

    private FormParams() {
    }

    /** Returns a new, empty set of form parameters. */
    static FormParams form() {
        return new FormParams();
    }

    FormParams monthlyTakeHomeIncome(String value) {
        return set("monthlyTakeHomeIncome", value);
    }

    FormParams monthlyEssentialExpenses(String value) {
        return set("monthlyEssentialExpenses", value);
    }

    FormParams starterEmergencyFund(String value) {
        return set("starterEmergencyFund", value);
    }

    FormParams fullEmergencyFund(String value) {
        return set("fullEmergencyFund", value);
    }

    FormParams retirementContributionPercent(String value) {
        return set("retirementContributionPercent", value);
    }

    FormParams sample(String value) {
        return set("sample", value);
    }

    /** Sets housing to RENT with {@code monthlyRent}. */
    FormParams rent(String monthlyRent) {
        set("housing", "RENT");
        return set("monthlyRent", monthlyRent);
    }

    /** Sets housing to MORTGAGE with {@code balance}, {@code rate}, and {@code payment}. */
    FormParams mortgage(String balance, String rate, String payment) {
        set("housing", "MORTGAGE");
        set("mortgageBalance", balance);
        set("mortgageRate", rate);
        return set("mortgagePayment", payment);
    }

    FormParams housing(String value) {
        return set("housing", value);
    }

    FormParams mortgageBalance(String value) {
        return set("mortgageBalance", value);
    }

    FormParams mortgageRate(String value) {
        return set("mortgageRate", value);
    }

    FormParams mortgagePayment(String value) {
        return set("mortgagePayment", value);
    }

    FormParams monthlyRent(String value) {
        return set("monthlyRent", value);
    }

    FormParams intendsToFundCollege() {
        return set("intendsToFundCollege", "true");
    }

    FormParams retired() {
        return set("retired", "true");
    }

    /** Adds the debt row at {@code index} with every field. */
    FormParams debt(int index, String label, String type, String balance, String rate, String minimumPayment) {
        debtLabel(index, label);
        set("debts[" + index + "].type", type);
        debtBalance(index, balance);
        set("debts[" + index + "].annualInterestRate", rate);
        set("debts[" + index + "].minimumPayment", minimumPayment);
        return this;
    }

    FormParams debtLabel(int index, String label) {
        return set("debts[" + index + "].label", label);
    }

    FormParams debtBalance(int index, String balance) {
        return set("debts[" + index + "].balance", balance);
    }

    /** Sets an arbitrary field, for the odd param a named setter doesn't cover. */
    FormParams param(String name, String value) {
        return set(name, value);
    }

    private FormParams set(String name, String value) {
        values.set(name, value);
        return this;
    }

    MultiValueMap<String, String> values() {
        return values;
    }
}
