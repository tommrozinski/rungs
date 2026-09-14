package com.rungs.readiness.domain.projection;

import static com.rungs.readiness.domain.model.FinancialSnapshotBuilder.aSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import com.rungs.readiness.domain.model.Rent;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

/** {@link PayoffScheduleCalculator} (design.md 040 sections 3 and 5). */
class PayoffScheduleCalculatorTests {

    private static final YearMonth AS_OF = YearMonth.of(2026, 9);

    @Test
    void aHouseholdWithNoLoansHasAnEmptySchedule() {
        FinancialSnapshot snapshot = aSnapshot().build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.loans()).isEmpty();
        assertThat(schedule.months()).isEmpty();
        assertThat(schedule.totalExtraCash()).isEqualTo(Money.ZERO);
        assertThat(schedule.totalPaidPerLoan()).isEmpty();
        assertThat(schedule.totalInterest()).isEqualTo(Money.ZERO);
        assertThat(schedule.truncated()).isFalse();
        assertInvariants(schedule);
    }

    // Hand-verified two-debt case, exact to the cent, over the first three months.
    //
    // Debt A: $300.00 at 12% annual (1% monthly), $20 minimum. Debt B: $2,000.00 at 6% annual (0.5% monthly),
    // $100 minimum. Rate orders A before B. Income $320, no essential expenses, renting at $0: surplus (extraCash)
    // is 320 - 0 - 20 - 100 = $200.00 every month.
    //
    // Month 1 (cascade $0, totalAvail $200):
    //   A: open 300.00, int 3.00, owed 303.00, cap 20 + 200 = 220.00, payment 220.00, end 83.00.
    //      pool after A: 200 - (220 - 20) = 0.
    //   B: open 2000.00, int 10.00, owed 2010.00, cap 100 + 0 = 100.00, payment 100.00, end 1910.00.
    //   moInt 13.00, totalBal 1993.00, cumInt 13.00.
    //
    // Month 2 (cascade still $0: A not yet closed before this month; totalAvail $200):
    //   A: open 83.00, int 0.83, owed 83.83, cap 20 + 200 = 220.00, payment 83.83 (payoff), end 0.00 — closes.
    //      pool after A: 200 - (83.83 - 20) = 136.17.
    //   B: open 1910.00, int 9.55, owed 1919.55, cap 100 + 136.17 = 236.17, payment 236.17, end 1683.38.
    //   moInt 10.38, totalBal 1683.38, cumInt 23.38.
    //
    // Month 3 (cascade $20.00: A's freed minimum, counted once; totalAvail $220):
    //   A: closed — open/int/pmt/end all 0.00.
    //   B: open 1683.38, int 8.42, owed 1691.80, cap 100 + 220 = 320.00, payment 320.00, end 1371.80.
    //   moInt 8.42, totalBal 1371.80, cumInt 31.80.

    @Test
    void handVerifiedTwoDebtCaseMatchesTheFirstThreeMonthsExactly() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(320))
                .withHousing(new Rent(Money.ZERO))
                .withDebt("Debt A", DebtType.CREDIT_CARD, Money.ofDollars(300), "12", Money.ofDollars(20))
                .withDebt("Debt B", DebtType.CREDIT_CARD, Money.ofDollars(2_000), "6", Money.ofDollars(100))
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.loans()).extracting(ScheduleLoan::name).containsExactly("Debt A", "Debt B");
        assertThat(schedule.months()).hasSizeGreaterThanOrEqualTo(3);

        ScheduleMonth month1 = schedule.months().get(0);
        assertThat(month1.month()).isEqualTo(AS_OF);
        assertThat(month1.extraCash()).isEqualTo(dollars("200.00"));
        assertThat(month1.cascade()).isEqualTo(Money.ZERO);
        assertThat(month1.totalAvailable()).isEqualTo(dollars("200.00"));
        assertLoanMonth(month1.loanMonths().get(0), "300.00", "3.00", "220.00", "83.00");
        assertLoanMonth(month1.loanMonths().get(1), "2000.00", "10.00", "100.00", "1910.00");
        assertThat(month1.monthInterest()).isEqualTo(dollars("13.00"));
        assertThat(month1.totalBalance()).isEqualTo(dollars("1993.00"));
        assertThat(month1.cumulativeInterest()).isEqualTo(dollars("13.00"));

        ScheduleMonth month2 = schedule.months().get(1);
        assertThat(month2.month()).isEqualTo(AS_OF.plusMonths(1));
        assertThat(month2.extraCash()).isEqualTo(dollars("200.00"));
        assertThat(month2.cascade()).isEqualTo(Money.ZERO);
        assertThat(month2.totalAvailable()).isEqualTo(dollars("200.00"));
        assertLoanMonth(month2.loanMonths().get(0), "83.00", "0.83", "83.83", "0.00");
        assertLoanMonth(month2.loanMonths().get(1), "1910.00", "9.55", "236.17", "1683.38");
        assertThat(month2.monthInterest()).isEqualTo(dollars("10.38"));
        assertThat(month2.totalBalance()).isEqualTo(dollars("1683.38"));
        assertThat(month2.cumulativeInterest()).isEqualTo(dollars("23.38"));

        ScheduleMonth month3 = schedule.months().get(2);
        assertThat(month3.month()).isEqualTo(AS_OF.plusMonths(2));
        assertThat(month3.extraCash()).isEqualTo(dollars("200.00"));
        assertThat(month3.cascade()).isEqualTo(dollars("20.00"));
        assertThat(month3.totalAvailable()).isEqualTo(dollars("220.00"));
        LoanMonth aMonth3 = month3.loanMonths().get(0);
        assertThat(aMonth3).isEqualTo(new LoanMonth(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO));
        assertLoanMonth(month3.loanMonths().get(1), "1683.38", "8.42", "320.00", "1371.80");
        assertThat(month3.monthInterest()).isEqualTo(dollars("8.42"));
        assertThat(month3.totalBalance()).isEqualTo(dollars("1371.80"));
        assertThat(month3.cumulativeInterest()).isEqualTo(dollars("31.80"));

        assertInvariants(schedule);
    }

    /** Asserts a loan-month's four figures. */
    private static void assertLoanMonth(LoanMonth actual, String open, String interest, String payment, String end) {
        assertThat(actual.open()).isEqualTo(dollars(open));
        assertThat(actual.interest()).isEqualTo(dollars(interest));
        assertThat(actual.payment()).isEqualTo(dollars(payment));
        assertThat(actual.end()).isEqualTo(dollars(end));
    }

    // Ordering: rate descending, ties in supplied order.

    @Test
    void ordersLoansByRateDescendingWithTiesInSuppliedOrder() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(1_000))
                .withHousing(new Rent(Money.ZERO))
                .withDebt("Low", DebtType.CREDIT_CARD, Money.ofDollars(500), "5", Money.ofDollars(25))
                .withDebt("HighFirst", DebtType.CREDIT_CARD, Money.ofDollars(500), "18", Money.ofDollars(25))
                .withDebt("HighSecond", DebtType.CREDIT_CARD, Money.ofDollars(500), "18", Money.ofDollars(25))
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.loans())
                .extracting(ScheduleLoan::name)
                .containsExactly("HighFirst", "HighSecond", "Low");
        assertInvariants(schedule);
    }

    // The mortgage is always last, even when its rate is higher than a debt's.

    @Test
    void theMortgageIsAlwaysLastEvenWithAHigherRateThanADebt() {
        Mortgage mortgage = new Mortgage(Money.ofDollars(50_000), new BigDecimal("9"), Money.ofDollars(500), null);
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(2_000))
                .withHousing(mortgage)
                .withDebt("Card", DebtType.CREDIT_CARD, Money.ofDollars(500), "5", Money.ofDollars(25))
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.loans()).extracting(ScheduleLoan::name).containsExactly("Card", "Mortgage");
        assertInvariants(schedule);
    }

    // A renter has no mortgage row.

    @Test
    void aRenterHasNoMortgageRow() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(1_000))
                .withHousing(new Rent(Money.ofDollars(1_200)))
                .withDebt("Card", DebtType.CREDIT_CARD, Money.ofDollars(500), "5", Money.ofDollars(25))
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.loans()).extracting(ScheduleLoan::name).containsExactly("Card");
        assertInvariants(schedule);
    }

    // A mortgage at $0 balance never enters the schedule.

    @Test
    void aPaidOffMortgageHasNoRow() {
        Mortgage paidOff = new Mortgage(Money.ZERO, BigDecimal.ZERO, Money.ofDollars(650), null);
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(1_000))
                .withHousing(paidOff)
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.loans()).isEmpty();
        assertInvariants(schedule);
    }

    // A negative surplus becomes $0 extra cash, so only minimums (and any cascade) get applied.

    @Test
    void aNegativeSurplusBecomesZeroExtraCash() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(100))
                .withMonthlyEssentialExpenses(Money.ofDollars(500))
                .withHousing(new Rent(Money.ZERO))
                .withDebt("Card", DebtType.CREDIT_CARD, Money.ofDollars(500), "12", Money.ofDollars(25))
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.months().get(0).extraCash()).isEqualTo(Money.ZERO);
        assertInvariants(schedule);
    }

    // The 600-month cap, with a loan whose minimum never covers its own interest.

    @Test
    void aLoanWhoseMinimumNeverCoversInterestHitsTheSixHundredMonthCap() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(50))
                .withHousing(new Rent(Money.ZERO))
                .withDebt("Stuck", DebtType.CREDIT_CARD, Money.ofDollars(10_000), "24", Money.ofDollars(1))
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.truncated()).isTrue();
        assertThat(schedule.months()).hasSize(600);
        assertThat(schedule.months().get(599).totalBalance().isPositive()).isTrue();
        assertInvariants(schedule);
    }

    @Test
    void aLoanThatPaysOffWellBeforeTheCapIsNotTruncated() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(1_000))
                .withHousing(new Rent(Money.ZERO))
                .withDebt("Card", DebtType.CREDIT_CARD, Money.ofDollars(500), "12", Money.ofDollars(500))
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.truncated()).isFalse();
        assertThat(schedule.months()).hasSizeLessThan(600);
        assertThat(schedule.months().getLast().totalBalance()).isEqualTo(Money.ZERO);
        assertInvariants(schedule);
    }

    // Totals match the sums of the months.

    @Test
    void totalsMatchTheSumsOfTheMonths() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(320))
                .withHousing(new Rent(Money.ZERO))
                .withDebt("Debt A", DebtType.CREDIT_CARD, Money.ofDollars(300), "12", Money.ofDollars(20))
                .withDebt("Debt B", DebtType.CREDIT_CARD, Money.ofDollars(2_000), "6", Money.ofDollars(100))
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        Money expectedExtraCash = Money.ofCents(schedule.months().get(0).extraCash().cents() * schedule.months().size());
        assertThat(schedule.totalExtraCash()).isEqualTo(expectedExtraCash);

        Money expectedInterest = schedule.months().getLast().cumulativeInterest();
        assertThat(schedule.totalInterest()).isEqualTo(expectedInterest);

        for (int i = 0; i < schedule.loans().size(); i++) {
            Money summedPayments = Money.ZERO;
            for (ScheduleMonth month : schedule.months()) {
                summedPayments = summedPayments.plus(month.loanMonths().get(i).payment());
            }
            assertThat(schedule.totalPaidPerLoan().get(i)).isEqualTo(summedPayments);
        }

        assertInvariants(schedule);
    }

    // Determinism: the same snapshot always produces an equal schedule.

    @Test
    void isDeterministic() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(320))
                .withHousing(new Rent(Money.ZERO))
                .withDebt("Debt A", DebtType.CREDIT_CARD, Money.ofDollars(300), "12", Money.ofDollars(20))
                .withDebt("Debt B", DebtType.CREDIT_CARD, Money.ofDollars(2_000), "6", Money.ofDollars(100))
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule first = PayoffScheduleCalculator.schedule(snapshot);
        PayoffSchedule second = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(first).isEqualTo(second);
        assertInvariants(first);
    }

    private static Money dollars(String amount) {
        return Money.parse(amount);
    }

    /**
     * Asserts the invariants design.md 040 section 3 requires of every schedule: each loan's total paid equals its
     * starting balance minus its final balance plus its total interest, no balance ever goes negative, and no
     * month's total payment exceeds the minimums of that month's open loans plus that month's total available.
     */
    private static void assertInvariants(PayoffSchedule schedule) {
        int loanCount = schedule.loans().size();
        Money[] finalEnd = new Money[loanCount];
        Money[] totalInterestPerLoan = new Money[loanCount];
        for (int i = 0; i < loanCount; i++) {
            finalEnd[i] = schedule.loans().get(i).startingBalance();
            totalInterestPerLoan[i] = Money.ZERO;
        }

        for (ScheduleMonth month : schedule.months()) {
            Money minimumsOfOpenLoans = Money.ZERO;
            Money totalPayment = Money.ZERO;
            for (int i = 0; i < loanCount; i++) {
                LoanMonth loanMonth = month.loanMonths().get(i);
                assertThat(loanMonth.end().isNegative()).isFalse();
                if (loanMonth.open().isPositive()) {
                    minimumsOfOpenLoans = minimumsOfOpenLoans.plus(schedule.loans().get(i).minimumPayment());
                }
                totalPayment = totalPayment.plus(loanMonth.payment());
                totalInterestPerLoan[i] = totalInterestPerLoan[i].plus(loanMonth.interest());
                finalEnd[i] = loanMonth.end();
            }
            assertThat(totalPayment.compareTo(minimumsOfOpenLoans.plus(month.totalAvailable())))
                    .describedAs("month %s total payment should not exceed open minimums plus total available", month.month())
                    .isLessThanOrEqualTo(0);
        }

        for (int i = 0; i < loanCount; i++) {
            Money expectedPaid = schedule.loans().get(i).startingBalance().minus(finalEnd[i]).plus(totalInterestPerLoan[i]);
            assertThat(schedule.totalPaidPerLoan().get(i))
                    .describedAs("loan %s paid should equal start minus end plus interest", schedule.loans().get(i).name())
                    .isEqualTo(expectedPaid);
        }
    }
}
