package com.rungs.readiness.domain.stage;

import com.rungs.readiness.domain.model.Debt;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.MonthlyInterest;
import com.rungs.readiness.domain.model.Severity;
import com.rungs.readiness.domain.model.Warning;
import com.rungs.readiness.domain.model.WarningCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Detects the three non-fatal conditions of PRD FR-5 on a normalized {@link FinancialSnapshot} (012 REQ-003). Pure
 * domain logic, O(number of debts), no I/O.
 */
public final class WarningDetector {

    /** Months of expenses a full emergency fund target is capped at (matches {@link FullEmergencyFundEvaluator}). */
    private static final int MAX_EMERGENCY_FUND_MONTHS = 6;

    private WarningDetector() {
    }

    /** Returns {@code snapshot}'s warnings, sorted by {@link WarningCode} order; empty when none apply. */
    public static List<Warning> detect(FinancialSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<Warning> warnings = new ArrayList<>();
        expensesExceedIncome(snapshot).ifPresent(warnings::add);
        negativeAmortization(snapshot).ifPresent(warnings::add);
        emergencyFundTargetCapped(snapshot).ifPresent(warnings::add);
        return List.copyOf(warnings);
    }

    private static Optional<Warning> expensesExceedIncome(FinancialSnapshot snapshot) {
        if (!snapshot.monthlySurplus().isNegative()) {
            return Optional.empty();
        }
        String shortfall = snapshot.monthlySurplus().negate().toDisplayString();
        String message = "Your essential expenses and minimum payments are %s more than your take-home pay each "
                .formatted(shortfall) + "month.";
        return Optional.of(new Warning(WarningCode.EXPENSES_EXCEED_INCOME, Severity.CAUTION, message,
                List.of()));
    }

    private static Optional<Warning> negativeAmortization(FinancialSnapshot snapshot) {
        List<String> ids = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (Debt debt : snapshot.debts()) {
            if (debt.minimumPayment().compareTo(MonthlyInterest.on(debt.balance(), debt.annualInterestRate())) < 0) {
                ids.add(debt.id());
                labels.add(debt.label());
            }
        }
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        String message = ids.size() == 1
                ? "The minimum payment on %s doesn't cover its monthly interest, so the balance grows."
                        .formatted(labels.get(0))
                : "The minimum payments on %s don't cover their monthly interest, so those balances grow."
                        .formatted(joinLabels(labels));
        return Optional.of(
                new Warning(WarningCode.NEGATIVE_AMORTIZATION, Severity.CAUTION, message, ids));
    }

    private static Optional<Warning> emergencyFundTargetCapped(FinancialSnapshot snapshot) {
        int months = snapshot.emergencyFundMonths();
        if (months <= MAX_EMERGENCY_FUND_MONTHS) {
            return Optional.empty();
        }
        String message = ("Emergency fund targets are capped at %d months, so your target uses %d months instead "
                + "of %d.").formatted(MAX_EMERGENCY_FUND_MONTHS, MAX_EMERGENCY_FUND_MONTHS, months);
        return Optional.of(
                new Warning(WarningCode.EMERGENCY_FUND_TARGET_CAPPED, Severity.CAUTION, message, List.of()));
    }

    /** Joins {@code labels} for the several-debt message: two are "A and B"; three or more use an Oxford comma. */
    private static String joinLabels(List<String> labels) {
        if (labels.size() == 2) {
            return labels.get(0) + " and " + labels.get(1);
        }
        String allButLast = String.join(", ", labels.subList(0, labels.size() - 1));
        return allButLast + ", and " + labels.get(labels.size() - 1);
    }
}
