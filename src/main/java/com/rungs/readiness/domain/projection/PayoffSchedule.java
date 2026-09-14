package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Money;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The month-by-month payoff schedule {@link PayoffScheduleCalculator#schedule} computes for a household: every
 * debt paid smallest starting balance first (ties in supplied order), then the mortgage, with each closed loan's
 * minimum cascading to the next (design.md 040 section 3 for the pool mechanics; design.md 041 section 1 for the
 * snowball order).
 *
 * @param loans every loan in payoff order; empty when the household has neither debts nor a mortgage balance
 * @param months every month of the schedule, starting from the snapshot's {@code asOf}; empty with no loans
 * @param totalExtraCash the sum of {@code extraCash} across every month
 * @param totalPaidPerLoan each loan's total paid (principal plus interest), aligned by position with {@code loans}
 * @param totalInterest the sum of every month's interest across every loan
 * @param truncated whether the schedule stopped at 600 months before every loan closed, rather than running until
 *     the last loan paid off
 * @param asOf the month the schedule starts from, the snapshot's own {@code asOf}, kept even when the schedule has
 *     no months (no loans) so {@link #debtFreeMonth()} still has a month to report
 */
public record PayoffSchedule(
        List<ScheduleLoan> loans,
        List<ScheduleMonth> months,
        Money totalExtraCash,
        List<Money> totalPaidPerLoan,
        Money totalInterest,
        boolean truncated,
        YearMonth asOf) {

    /** Validates every component and copies the lists; throws NullPointerException naming the component. */
    public PayoffSchedule {
        loans = List.copyOf(Objects.requireNonNull(loans, "loans"));
        months = List.copyOf(Objects.requireNonNull(months, "months"));
        Objects.requireNonNull(totalExtraCash, "totalExtraCash");
        totalPaidPerLoan = List.copyOf(Objects.requireNonNull(totalPaidPerLoan, "totalPaidPerLoan"));
        Objects.requireNonNull(totalInterest, "totalInterest");
        Objects.requireNonNull(asOf, "asOf");
    }

    /**
     * Returns the month the last non-mortgage loan closes (design.md 041 section 3): the latest of every debt's
     * closing month, or empty when any debt never closes within the schedule. With no debts at all, this is
     * trivially "already debt-free", so it returns {@code asOf} rather than empty.
     */
    public Optional<YearMonth> debtFreeMonth() {
        YearMonth latest = null;
        boolean anyDebt = false;
        for (int i = 0; i < loans.size(); i++) {
            if (loans.get(i).mortgage()) {
                continue;
            }
            anyDebt = true;
            Optional<YearMonth> closes = closingMonth(i);
            if (closes.isEmpty()) {
                return Optional.empty();
            }
            if (latest == null || closes.get().isAfter(latest)) {
                latest = closes.get();
            }
        }
        return anyDebt ? Optional.of(latest) : Optional.of(asOf);
    }

    /**
     * Returns the month the mortgage row closes, empty when there is no mortgage row or it never closes within the
     * schedule (design.md 041 section 3).
     */
    public Optional<YearMonth> mortgagePayoffMonth() {
        for (int i = 0; i < loans.size(); i++) {
            if (loans.get(i).mortgage()) {
                return closingMonth(i);
            }
        }
        return Optional.empty();
    }

    /** Returns the first month {@code loanIndex}'s ending balance reaches zero, or empty if it never does. */
    private Optional<YearMonth> closingMonth(int loanIndex) {
        for (ScheduleMonth month : months) {
            if (month.loanMonths().get(loanIndex).end().isZero()) {
                return Optional.of(month.month());
            }
        }
        return Optional.empty();
    }
}
