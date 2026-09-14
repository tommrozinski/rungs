package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.projection.LoanMonth;
import com.rungs.readiness.domain.projection.PayoffSchedule;
import com.rungs.readiness.domain.projection.ScheduleLoan;
import com.rungs.readiness.domain.projection.ScheduleMonth;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the payoff schedule page's view model (design.md 040 sections 2 and 3) from a {@link PayoffSchedule},
 * formatting every figure as money (a zero amount shown as {@code "-"}) or a two-decimal percentage, and each
 * month's calendar label as e.g. "Sep 2026".
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
                    loan.name(), money(loan.startingBalance()), percent(loan.annualRate()), money(loan.minimumPayment())));
            loanNames.add(loan.name());
            totalBalance = totalBalance.plus(loan.startingBalance());
            totalMinimum = totalMinimum.plus(loan.minimumPayment());
        }

        List<ScheduleMonthView> months = new ArrayList<>(schedule.months().size());
        for (ScheduleMonth month : schedule.months()) {
            months.add(monthView(month));
        }

        List<String> totalPaidPerLoan = schedule.totalPaidPerLoan().stream().map(ScheduleViews::money).toList();

        return new ScheduleView(
                true,
                loanSummaries,
                loanNames,
                money(totalBalance),
                money(totalMinimum),
                months,
                money(schedule.totalExtraCash()),
                totalPaidPerLoan,
                money(schedule.totalInterest()),
                schedule.truncated());
    }

    private static ScheduleMonthView monthView(ScheduleMonth month) {
        List<LoanMonthView> loanMonths = new ArrayList<>(month.loanMonths().size());
        boolean closed = false;
        for (LoanMonth loanMonth : month.loanMonths()) {
            loanMonths.add(new LoanMonthView(
                    money(loanMonth.open()), money(loanMonth.interest()), money(loanMonth.payment()), money(loanMonth.end())));
            if (loanMonth.payment().isPositive() && loanMonth.end().isZero()) {
                closed = true;
            }
        }
        return new ScheduleMonthView(
                monthLabel(month.month()),
                money(month.extraCash()),
                money(month.cascade()),
                money(month.totalAvailable()),
                loanMonths,
                money(month.monthInterest()),
                money(month.totalBalance()),
                money(month.cumulativeInterest()),
                closed);
    }

    /** Returns {@code month} as e.g. "Sep 2026". */
    private static String monthLabel(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.SHORT, Locale.US) + " " + month.getYear();
    }

    /** Returns {@code amount} formatted as money, or {@code "-"} when it is zero. */
    private static String money(Money amount) {
        return amount.isZero() ? "-" : amount.toDisplayString();
    }

    /** Returns {@code rate} formatted as a percentage with exactly two decimal places, e.g. "26.99%". */
    private static String percent(BigDecimal rate) {
        return rate.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
