package com.rungs.readiness.domain.model;

/**
 * A non-fatal condition detected on a {@link FinancialSnapshot} (PRD FR-5). Declared in the order
 * {@link com.rungs.readiness.domain.stage.WarningDetector} reports them.
 */
public enum WarningCode {

    /** The household's essential expenses and minimum payments exceed its take-home pay. */
    EXPENSES_EXCEED_INCOME,

    /** One or more debts have a minimum payment below one month's interest on their current balance. */
    NEGATIVE_AMORTIZATION,

    /** The full emergency fund target was capped at 6 months of expenses. */
    EMERGENCY_FUND_TARGET_CAPPED
}
