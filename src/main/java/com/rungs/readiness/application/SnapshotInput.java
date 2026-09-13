package com.rungs.readiness.application;

import com.rungs.readiness.domain.model.Debt;
import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.HouseholdFlags;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A financial snapshot as submitted, before normalization. Money and percentages are raw decimal strings so a blank
 * value can become zero (PRD FR-4).
 */
public record SnapshotInput(
        String monthlyTakeHomeIncome,
        String monthlyEssentialExpenses,
        String starterEmergencyFund,
        String fullEmergencyFund,
        String monthlyCollegeContribution,
        String retirementContributionPercent,
        Integer emergencyFundMonths,
        List<DebtInput> debts,
        MortgageInput mortgage,
        HouseholdFlagsInput householdFlags) {

    /** Months of expenses the full emergency fund covers when none is supplied. */
    private static final int DEFAULT_EMERGENCY_FUND_MONTHS = 3;

    /** Prefix of a debt's positional id, {@code debt-N}. */
    private static final String ID_PREFIX = "debt-";

    /** Prefix of the positional label given to a debt with a blank label, {@code Debt N}. */
    private static final String DEFAULT_LABEL_PREFIX = "Debt ";

    /**
     * Applies the PRD FR-4 rules and builds the domain snapshot. In order: blank or null money and percentages become
     * zero and a null {@code emergencyFundMonths} becomes 3; null flags, a null {@code householdFlags}, and null
     * {@code debts} become false, all false, and empty; each debt, by its 1-based position as supplied, gets id
     * {@code debt-N} and, when its label is blank, the label {@code Debt N}; debts with a zero balance are then
     * removed, so numbering still counts them.
     *
     * <p>Input is assumed valid until story 011 adds constraints. Until then, unparseable money throws
     * {@link com.rungs.readiness.domain.model.MoneyFormatException}, an unparseable percentage
     * {@link NumberFormatException}, an unknown debt type or a value the domain rejects (negative amount, percentage
     * out of range, months below 3) {@link IllegalArgumentException}; all are IllegalArgumentExceptions.
     */
    public FinancialSnapshot toSnapshot() {
        List<Debt> normalizedDebts = new ArrayList<>();
        List<DebtInput> suppliedDebts = debts == null ? List.of() : debts;
        for (int index = 0; index < suppliedDebts.size(); index++) {
            DebtInput debt = suppliedDebts.get(index);
            Money balance = money(debt.balance());
            if (balance.isZero()) {
                continue;
            }
            int position = index + 1;
            String label = InputText.isBlank(debt.label()) ? DEFAULT_LABEL_PREFIX + position : debt.label();
            normalizedDebts.add(new Debt(ID_PREFIX + position, label, DebtType.valueOf(debt.type()), balance,
                    percent(debt.annualInterestRate()), money(debt.minimumPayment())));
        }
        return new FinancialSnapshot(
                money(monthlyTakeHomeIncome),
                money(monthlyEssentialExpenses),
                money(starterEmergencyFund),
                money(fullEmergencyFund),
                money(monthlyCollegeContribution),
                percent(retirementContributionPercent),
                emergencyFundMonths == null ? DEFAULT_EMERGENCY_FUND_MONTHS : emergencyFundMonths,
                normalizedDebts,
                mortgage == null ? null : new Mortgage(money(mortgage.balance()), money(mortgage.monthlyPayment()),
                        money(mortgage.homeValue()), percent(mortgage.annualInterestRate())),
                flags());
    }

    private HouseholdFlags flags() {
        if (householdFlags == null) {
            return new HouseholdFlags(false, false, false);
        }
        return new HouseholdFlags(isTrue(householdFlags.rents()), isTrue(householdFlags.retired()),
                isTrue(householdFlags.intendsToFundCollege()));
    }

    private static boolean isTrue(Boolean flag) {
        return Boolean.TRUE.equals(flag);
    }

    private static Money money(String text) {
        return InputText.isBlank(text) ? Money.ZERO : Money.parse(text);
    }

    private static BigDecimal percent(String text) {
        return InputText.isBlank(text) ? BigDecimal.ZERO : new BigDecimal(InputText.strip(text));
    }
}
