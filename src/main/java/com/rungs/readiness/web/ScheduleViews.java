package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.projection.LoanMonth;
import com.rungs.readiness.domain.projection.PayoffSchedule;
import com.rungs.readiness.domain.projection.ScheduleLoan;
import com.rungs.readiness.domain.projection.ScheduleMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the payoff schedule page's view model from a {@link PayoffSchedule}, formatting every figure as money (a
 * zero amount shown as {@code "-"}) or a two-decimal percentage, and each month's calendar label as e.g. "Sep 2026".
 */
final class ScheduleViews {

    private ScheduleViews() {
    }

    /** Builds the view model for {@code schedule}. */
    static ScheduleView build(PayoffSchedule schedule) {
        List<ScheduleLoan> loans = schedule.loans();
        if (loans.isEmpty()) {
            return new ScheduleView(false, List.of(), List.of(), null, null, List.of(), null, List.of(), null, false);
        }

        List<LoanSummaryView> loanSummaries = new ArrayList<>(loans.size());
        List<String> loanNames = new ArrayList<>(loans.size());
        Money totalBalance = Money.ZERO;
        Money totalMinimum = Money.ZERO;
        for (ScheduleLoan loan : loans) {
            loanSummaries.add(new LoanSummaryView(
                    loan.name(),
                    Formats.moneyOrDash(loan.startingBalance()),
                    Formats.fixedPercent(loan.annualRate()),
                    Formats.moneyOrDash(loan.minimumPayment())));
            loanNames.add(loan.name());
            totalBalance = totalBalance.plus(loan.startingBalance());
            totalMinimum = totalMinimum.plus(loan.minimumPayment());
        }

        List<ScheduleMonthView> months = new ArrayList<>(schedule.months().size());
        for (ScheduleMonth month : schedule.months()) {
            months.add(monthView(month));
        }

        List<String> totalPaidPerLoan = schedule.totalPaidPerLoan().stream().map(Formats::moneyOrDash).toList();

        return new ScheduleView(
                true,
                loanSummaries,
                loanNames,
                Formats.moneyOrDash(totalBalance),
                Formats.moneyOrDash(totalMinimum),
                months,
                Formats.moneyOrDash(schedule.totalExtraCash()),
                totalPaidPerLoan,
                Formats.moneyOrDash(schedule.totalInterest()),
                schedule.truncated());
    }

    private static ScheduleMonthView monthView(ScheduleMonth month) {
        List<LoanMonthView> loanMonths = new ArrayList<>(month.loanMonths().size());
        boolean closed = false;
        for (LoanMonth loanMonth : month.loanMonths()) {
            loanMonths.add(new LoanMonthView(
                    Formats.moneyOrDash(loanMonth.open()),
                    Formats.moneyOrDash(loanMonth.interest()),
                    Formats.moneyOrDash(loanMonth.payment()),
                    Formats.moneyOrDash(loanMonth.end())));
            if (loanMonth.payment().isPositive() && loanMonth.end().isZero()) {
                closed = true;
            }
        }
        return new ScheduleMonthView(
                Formats.monthShort(month.month()),
                Formats.moneyOrDash(month.extraCash()),
                Formats.moneyOrDash(month.cascade()),
                Formats.moneyOrDash(month.totalAvailable()),
                loanMonths,
                Formats.moneyOrDash(month.monthInterest()),
                Formats.moneyOrDash(month.totalBalance()),
                Formats.moneyOrDash(month.cumulativeInterest()),
                closed);
    }
}
