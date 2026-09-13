package com.rungs.readiness.application;

import com.rungs.readiness.application.validation.AtMostFiftyDebts;
import com.rungs.readiness.application.validation.EmergencyFundMonths;
import com.rungs.readiness.application.validation.FieldsThenWholeForm;
import com.rungs.readiness.application.validation.MoneyAmount;
import com.rungs.readiness.application.validation.Percentage;
import com.rungs.readiness.application.validation.WholeForm;
import com.rungs.readiness.domain.model.Debt;
import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.HouseholdFlags;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A financial snapshot as submitted, before normalization. Money and percentages are raw decimal strings so a blank
 * value can become zero (PRD FR-4).
 *
 * <p>Validation (story 011): validate with {@link FieldsThenWholeForm}, so the field-only constraints on the
 * components, the debts, and the mortgage are checked first and the {@link WholeForm} rule {@link AtMostFiftyDebts}
 * runs only when they all pass (REQ-007). A missing ({@code null}) entry in {@code debts} is {@code INVALID_TYPE} at
 * its index.
 */
@AtMostFiftyDebts(groups = WholeForm.class)
public record SnapshotInput(
        @MoneyAmount String monthlyTakeHomeIncome,
        @MoneyAmount String monthlyEssentialExpenses,
        @MoneyAmount String starterEmergencyFund,
        @MoneyAmount String fullEmergencyFund,
        @MoneyAmount String monthlyCollegeContribution,
        @Percentage(scale = RETIREMENT_PERCENT_SCALE) String retirementContributionPercent,
        @EmergencyFundMonths Integer emergencyFundMonths,
        List<@NotNull(message = "INVALID_TYPE") @Valid DebtInput> debts,
        @Valid MortgageInput mortgage,
        HouseholdFlagsInput householdFlags) {

    /** Decimal places allowed in the retirement contribution percent. */
    static final int RETIREMENT_PERCENT_SCALE = 2;

    /** Decimal places allowed in an annual interest rate. */
    static final int INTEREST_RATE_SCALE = 3;

    /** Months of expenses the full emergency fund covers when none is supplied. */
    private static final int DEFAULT_EMERGENCY_FUND_MONTHS = 3;

    /** Prefix of a debt's positional id, {@code debt-N}. */
    private static final String ID_PREFIX = "debt-";

    /** Prefix of the positional label given to a debt with a blank label, {@code Debt N}. */
    private static final String DEFAULT_LABEL_PREFIX = "Debt ";

    /** The display default for a blank money or percentage value ({@link #forForm()}). */
    private static final String ZERO_TEXT = "0";

    /**
     * Applies the PRD FR-4 rules and builds the domain snapshot. In order: blank or null money and percentages become
     * zero and a null {@code emergencyFundMonths} becomes 3; null flags, a null {@code householdFlags}, and null
     * {@code debts} become false, all false, and empty; each debt, by its 1-based position as supplied, gets id
     * {@code debt-N} and, when its label is blank, the label {@code Debt N}; debts with a zero balance are then
     * removed, so numbering still counts them.
     *
     * <p>Call only on input that passed validation (story 011 REQ-007). Invalid input throws: unparseable money
     * {@link com.rungs.readiness.domain.model.MoneyFormatException}, an unparseable percentage
     * {@link NumberFormatException}, an unknown debt type or a value the domain rejects (negative amount, percentage
     * out of range, months below 3) {@link IllegalArgumentException}, and a missing debt entry
     * {@link NullPointerException}.
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

    /**
     * Returns a copy normalized for display after a server render (027 REQ-007): a blank or null money or percentage
     * becomes {@code "0"}, a null {@code emergencyFundMonths} becomes 3, a null {@code mortgage} becomes one with
     * every value {@code "0"}, null flags become {@code false}, and a blank debt label becomes {@code Debt N} by
     * position. A debt with a zero balance is kept, unlike {@link #toSnapshot()}.
     */
    public SnapshotInput forForm() {
        List<DebtInput> suppliedDebts = debts == null ? List.of() : debts;
        List<DebtInput> normalizedDebts = new ArrayList<>();
        for (int index = 0; index < suppliedDebts.size(); index++) {
            DebtInput debt = suppliedDebts.get(index);
            String label = InputText.isBlank(debt.label()) ? DEFAULT_LABEL_PREFIX + (index + 1) : debt.label();
            normalizedDebts.add(new DebtInput(label, debt.type(), formMoney(debt.balance()),
                    formPercent(debt.annualInterestRate()), formMoney(debt.minimumPayment())));
        }
        MortgageInput formMortgage = mortgage == null
                ? new MortgageInput(ZERO_TEXT, ZERO_TEXT, ZERO_TEXT, ZERO_TEXT)
                : new MortgageInput(formMoney(mortgage.balance()), formPercent(mortgage.annualInterestRate()),
                        formMoney(mortgage.monthlyPayment()), formMoney(mortgage.homeValue()));
        HouseholdFlags flags = flags();
        return new SnapshotInput(formMoney(monthlyTakeHomeIncome), formMoney(monthlyEssentialExpenses),
                formMoney(starterEmergencyFund), formMoney(fullEmergencyFund), formMoney(monthlyCollegeContribution),
                formPercent(retirementContributionPercent),
                emergencyFundMonths == null ? DEFAULT_EMERGENCY_FUND_MONTHS : emergencyFundMonths, normalizedDebts,
                formMortgage, new HouseholdFlagsInput(flags.rents(), flags.retired(), flags.intendsToFundCollege()));
    }

    private static String formMoney(String text) {
        return InputText.isBlank(text) ? ZERO_TEXT : text;
    }

    private static String formPercent(String text) {
        return InputText.isBlank(text) ? ZERO_TEXT : text;
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
