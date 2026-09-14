package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import com.rungs.readiness.domain.model.MortgagePayoff;
import com.rungs.readiness.domain.model.MortgagePayoffCalculator;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * The five sample households the "Load sample data" control offers (design.md 039 section 4), each a fully
 * populated {@link SnapshotForm} keyed by its id. The debt-free and homestretch mortgage payments and rates are
 * fixed, but their maturity is computed relative to the {@code asOf} the caller supplies (the assessment month the
 * running {@link java.time.Clock} produces), using {@link MortgagePayoffCalculator} itself so the samples can never
 * drift out of sync with it: debt-free's maturity always lands exactly on its computed payoff month, and
 * homestretch's always lands {@value #HOMESTRETCH_AHEAD_MONTHS} months after its own (design.md 039 review; see
 * claude-progress.txt for the underlying loan numbers).
 */
final class SampleHouseholds {

    /** How far ahead of its own computed payoff month homestretch's maturity is set. */
    private static final int HOMESTRETCH_AHEAD_MONTHS = 13;

    private SampleHouseholds() {
    }

    /**
     * Returns the sample household form for {@code id} as of {@code asOf}, or null when {@code id} names none.
     * {@code asOf} only affects the debt-free and homestretch households' maturity; the others ignore it.
     */
    static SnapshotForm byId(String id, YearMonth asOf) {
        if (id == null) {
            return null;
        }
        return switch (id) {
            case "buried" -> buried();
            case "board" -> board();
            case "debt-free" -> debtFree(asOf);
            case "homestretch" -> homestretch(asOf);
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

    private static SnapshotForm debtFree(YearMonth asOf) {
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
        form.setAnnualPropertyTaxes("4800");
        setMaturity(form, matchingMaturity(form, asOf, 0));
        return form;
    }

    private static SnapshotForm homestretch(YearMonth asOf) {
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
        form.setAnnualPropertyTaxes("6000");
        setMaturity(form, matchingMaturity(form, asOf, HOMESTRETCH_AHEAD_MONTHS));
        return form;
    }

    /**
     * Returns the maturity that keeps {@code form}'s already-set mortgage fields' payoff, computed as of
     * {@code asOf}, exactly {@code aheadMonths} months ahead of its stated maturity (0 for an exact match).
     */
    private static YearMonth matchingMaturity(SnapshotForm form, YearMonth asOf, int aheadMonths) {
        Mortgage probe = new Mortgage(
                Money.parse(form.getMortgageBalance()),
                new BigDecimal(form.getMortgageRate()),
                Money.parse(form.getMortgagePayment()),
                null);
        MortgagePayoff payoff = MortgagePayoffCalculator.payoff(probe, asOf);
        return asOf.plusMonths(payoff.monthsToPayoff() + aheadMonths);
    }

    /** Sets {@code form}'s maturity month and year fields from {@code maturity}. */
    private static void setMaturity(SnapshotForm form, YearMonth maturity) {
        form.setMaturityMonth(Integer.toString(maturity.getMonthValue()));
        form.setMaturityYear(Integer.toString(maturity.getYear()));
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
        form.setAnnualPropertyTaxes("6300");
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
