package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Money;
import java.util.List;
import java.util.Objects;

/**
 * The month-by-month payoff schedule {@link PayoffScheduleCalculator#schedule} computes for a household: every
 * debt paid highest-rate first, then the mortgage, with each closed loan's minimum cascading to the next
 * (design.md 040 section 3).
 *
 * @param loans every loan in payoff order; empty when the household has neither debts nor a mortgage balance
 * @param months every month of the schedule, starting from the snapshot's {@code asOf}; empty with no loans
 * @param totalExtraCash the sum of {@code extraCash} across every month
 * @param totalPaidPerLoan each loan's total paid (principal plus interest), aligned by position with {@code loans}
 * @param totalInterest the sum of every month's interest across every loan
 * @param truncated whether the schedule stopped at 600 months before every loan closed, rather than running until
 *     the last loan paid off
 */
public record PayoffSchedule(
        List<ScheduleLoan> loans,
        List<ScheduleMonth> months,
        Money totalExtraCash,
        List<Money> totalPaidPerLoan,
        Money totalInterest,
        boolean truncated) {

    /** Validates every component and copies the lists; throws NullPointerException naming the component. */
    public PayoffSchedule {
        loans = List.copyOf(Objects.requireNonNull(loans, "loans"));
        months = List.copyOf(Objects.requireNonNull(months, "months"));
        Objects.requireNonNull(totalExtraCash, "totalExtraCash");
        totalPaidPerLoan = List.copyOf(Objects.requireNonNull(totalPaidPerLoan, "totalPaidPerLoan"));
        Objects.requireNonNull(totalInterest, "totalInterest");
    }
}
