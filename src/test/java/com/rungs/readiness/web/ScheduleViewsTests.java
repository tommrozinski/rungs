package com.rungs.readiness.web;

import static com.rungs.readiness.domain.model.FinancialSnapshotBuilder.aSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.Debt;
import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import com.rungs.readiness.domain.model.Rent;
import com.rungs.readiness.domain.projection.PayoffSchedule;
import com.rungs.readiness.domain.projection.PayoffScheduleCalculator;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@link ScheduleViews}' formatting rules (design.md 040 section 2): money, percentages, month labels, the
 * "no loans" case, and the green-tint "closed" flag. */
class ScheduleViewsTests {

    @Test
    void aHouseholdWithNoLoansHasNoLoansInTheView() {
        FinancialSnapshot snapshot = aSnapshot().withHousing(new Rent(Money.ZERO)).build();
        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        ScheduleView view = ScheduleViews.build(schedule);

        assertThat(view.hasLoans()).isFalse();
        assertThat(view.loans()).isEmpty();
        assertThat(view.months()).isEmpty();
    }

    @Test
    void formatsMoneyAndPercentAndShowsZeroAsADash() {
        Debt debt = new Debt(
                "debt-1", "Store card", DebtType.CREDIT_CARD, Money.ofDollars(840), new BigDecimal("26.990"),
                Money.ofDollars(35));
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(6200))
                .withMonthlyEssentialExpenses(Money.ofDollars(2100))
                .withHousing(new Rent(Money.ofDollars(1400)))
                .withDebts(List.of(debt))
                .withAsOf(YearMonth.of(2026, 9))
                .build();
        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        ScheduleView view = ScheduleViews.build(schedule);

        assertThat(view.hasLoans()).isTrue();
        assertThat(view.loans()).hasSize(1);
        LoanSummaryView loan = view.loans().get(0);
        assertThat(loan.name()).isEqualTo("Store card");
        assertThat(loan.balance()).isEqualTo("$840.00");
        assertThat(loan.rate()).isEqualTo("26.99%");
        assertThat(loan.minimumPayment()).isEqualTo("$35.00");

        ScheduleMonthView firstMonth = view.months().get(0);
        assertThat(firstMonth.monthLabel()).isEqualTo("Sep 2026");
        assertThat(firstMonth.cascade()).isEqualTo("-");
        assertThat(firstMonth.closed()).isTrue();
    }

    @Test
    void theMortgageIsTheLastLoanAndAppearsInLoanNames() {
        Mortgage mortgage = new Mortgage(
                Money.ofDollars(185_000), new BigDecimal("5.250"), Money.ofDollars(1_650));
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(9500))
                .withMonthlyEssentialExpenses(Money.ofDollars(2100))
                .withHousing(mortgage)
                .build();
        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        ScheduleView view = ScheduleViews.build(schedule);

        assertThat(view.loanNames()).containsExactly("Mortgage");
        assertThat(view.totalPaidPerLoan()).hasSize(1);
    }
}
