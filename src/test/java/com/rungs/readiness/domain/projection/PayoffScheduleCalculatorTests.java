package com.rungs.readiness.domain.projection;

import static com.rungs.readiness.domain.model.FinancialSnapshotBuilder.aSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import com.rungs.readiness.domain.model.Rent;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** {@link PayoffScheduleCalculator} (design.md 040 sections 3 and 5 for the pool mechanics; design.md 041
 * sections 1, 3, and 5 for the snowball order and the payoff-date helpers). */
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

    // Hand-verified two-debt case, exact to the cent, over the first three months, under the debt snowball
    // (design.md 041 section 1): smallest starting balance first, regardless of rate.
    //
    // Debt A: $300.00 balance, 6% annual (0.5% monthly), $20 minimum. Debt B: $2,000.00 balance, 12% annual
    // (1% monthly), $100 minimum. Debt A's smaller balance orders it before Debt B even though Debt B's rate is
    // higher — a rate-first ordering would have put B first, so this is a genuine test of the snowball order, not
    // just a coincidence of the numbers. Income $320, no essential expenses, renting at $0: surplus (extraCash) is
    // 320 - 0 - 20 - 100 = $200.00 every month.
    //
    // Month 1 (cascade $0, totalAvail $200):
    //   A: open 300.00, int 300.00*0.005=1.50, owed 301.50, cap 20 + 200 = 220.00, payment 220.00, end 81.50.
    //      pool after A: 200 - (220 - 20) = 0.
    //   B: open 2000.00, int 2000.00*0.01=20.00, owed 2020.00, cap 100 + 0 = 100.00, payment 100.00, end 1920.00.
    //   moInt 21.50, totalBal 2001.50, cumInt 21.50.
    //
    // Month 2 (cascade still $0: A not yet closed before this month; totalAvail $200):
    //   A: open 81.50, int 81.50*0.005=0.41 (0.4075 rounds up), owed 81.91, cap 20 + 200 = 220.00,
    //      payment 81.91 (payoff), end 0.00 — closes. pool after A: 200 - (81.91 - 20) = 138.09.
    //   B: open 1920.00, int 1920.00*0.01=19.20, owed 1939.20, cap 100 + 138.09 = 238.09, payment 238.09,
    //      end 1701.11.
    //   moInt 19.61, totalBal 1701.11, cumInt 41.11.
    //
    // Month 3 (cascade $20.00: A's freed minimum, counted once; totalAvail $220):
    //   A: closed — open/int/pmt/end all 0.00.
    //   B: open 1701.11, int 1701.11*0.01=17.0111 rounds to 17.01, owed 1718.12, cap 100 + 220 = 320.00,
    //      payment 320.00, end 1398.12.
    //   moInt 17.01, totalBal 1398.12, cumInt 58.12.

    @Test
    void handVerifiedTwoDebtCaseMatchesTheFirstThreeMonthsExactly() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(320))
                .withHousing(new Rent(Money.ZERO))
                .withDebt("Debt A", DebtType.CREDIT_CARD, Money.ofDollars(300), "6", Money.ofDollars(20))
                .withDebt("Debt B", DebtType.CREDIT_CARD, Money.ofDollars(2_000), "12", Money.ofDollars(100))
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
        assertLoanMonth(month1.loanMonths().get(0), "300.00", "1.50", "220.00", "81.50");
        assertLoanMonth(month1.loanMonths().get(1), "2000.00", "20.00", "100.00", "1920.00");
        assertThat(month1.monthInterest()).isEqualTo(dollars("21.50"));
        assertThat(month1.totalBalance()).isEqualTo(dollars("2001.50"));
        assertThat(month1.cumulativeInterest()).isEqualTo(dollars("21.50"));

        ScheduleMonth month2 = schedule.months().get(1);
        assertThat(month2.month()).isEqualTo(AS_OF.plusMonths(1));
        assertThat(month2.extraCash()).isEqualTo(dollars("200.00"));
        assertThat(month2.cascade()).isEqualTo(Money.ZERO);
        assertThat(month2.totalAvailable()).isEqualTo(dollars("200.00"));
        assertLoanMonth(month2.loanMonths().get(0), "81.50", "0.41", "81.91", "0.00");
        assertLoanMonth(month2.loanMonths().get(1), "1920.00", "19.20", "238.09", "1701.11");
        assertThat(month2.monthInterest()).isEqualTo(dollars("19.61"));
        assertThat(month2.totalBalance()).isEqualTo(dollars("1701.11"));
        assertThat(month2.cumulativeInterest()).isEqualTo(dollars("41.11"));

        ScheduleMonth month3 = schedule.months().get(2);
        assertThat(month3.month()).isEqualTo(AS_OF.plusMonths(2));
        assertThat(month3.extraCash()).isEqualTo(dollars("200.00"));
        assertThat(month3.cascade()).isEqualTo(dollars("20.00"));
        assertThat(month3.totalAvailable()).isEqualTo(dollars("220.00"));
        LoanMonth aMonth3 = month3.loanMonths().get(0);
        assertThat(aMonth3).isEqualTo(new LoanMonth(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO));
        assertLoanMonth(month3.loanMonths().get(1), "1701.11", "17.01", "320.00", "1398.12");
        assertThat(month3.monthInterest()).isEqualTo(dollars("17.01"));
        assertThat(month3.totalBalance()).isEqualTo(dollars("1398.12"));
        assertThat(month3.cumulativeInterest()).isEqualTo(dollars("58.12"));

        assertInvariants(schedule);
    }

    /** Asserts a loan-month's four figures. */
    private static void assertLoanMonth(LoanMonth actual, String open, String interest, String payment, String end) {
        assertThat(actual.open()).isEqualTo(dollars(open));
        assertThat(actual.interest()).isEqualTo(dollars(interest));
        assertThat(actual.payment()).isEqualTo(dollars(payment));
        assertThat(actual.end()).isEqualTo(dollars(end));
    }

    // A loan's final payment can be below its own minimum (its payoff amount is smaller than the minimum), which
    // must free the unused part of that minimum to the next loan in the same month rather than losing it.
    //
    // Loan A: $10.00 balance, 0% rate, $50 minimum — payoff amount is $10.00, $40.00 below its minimum.
    // Loan B: $200.00 balance, 0% rate, $30 minimum. No extra cash: totalAvail is $0, so pool starts at $0.
    //
    // Month 1: A: open 10.00, int 0.00, owed 10.00, cap 50 + 0 = 50.00, payment 10.00 (payoff), end 0.00 — closes.
    //   pool after A: 0 + 50 - 10 = 40.00 (A's unused minimum flows forward instead of being lost).
    //   B: open 200.00, int 0.00, owed 200.00, cap 30 + 40 = 70.00, payment 70.00, end 130.00.

    @Test
    void aLoanClosingBelowItsMinimumFreesTheLeftoverToTheNextLoanInTheSameMonth() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(0))
                .withHousing(new Rent(Money.ZERO))
                .withDebt("Loan A", DebtType.CREDIT_CARD, Money.ofDollars(10), "0", Money.ofDollars(50))
                .withDebt("Loan B", DebtType.CREDIT_CARD, Money.ofDollars(200), "0", Money.ofDollars(30))
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        ScheduleMonth month1 = schedule.months().get(0);
        assertLoanMonth(month1.loanMonths().get(0), "10.00", "0.00", "10.00", "0.00");
        assertLoanMonth(month1.loanMonths().get(1), "200.00", "0.00", "70.00", "130.00");
        assertInvariants(schedule);
    }

    // Ordering: starting balance ascending, ties in supplied order (design.md 041 section 1).

    @Test
    void ordersLoansByStartingBalanceAscendingWithTiesInSuppliedOrder() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(1_000))
                .withHousing(new Rent(Money.ZERO))
                .withDebt("Big", DebtType.CREDIT_CARD, Money.ofDollars(900), "18", Money.ofDollars(25))
                .withDebt("TieFirst", DebtType.CREDIT_CARD, Money.ofDollars(200), "5", Money.ofDollars(25))
                .withDebt("TieSecond", DebtType.CREDIT_CARD, Money.ofDollars(200), "5", Money.ofDollars(25))
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.loans())
                .extracting(ScheduleLoan::name)
                .containsExactly("TieFirst", "TieSecond", "Big");
        assertInvariants(schedule);
    }

    // The mortgage is always last, even when its balance is smaller than a debt's.

    @Test
    void theMortgageIsAlwaysLastEvenWithASmallerBalanceThanADebt() {
        Mortgage mortgage = new Mortgage(Money.ofDollars(200), new BigDecimal("9"), Money.ofDollars(500));
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
        Mortgage paidOff = new Mortgage(Money.ZERO, BigDecimal.ZERO, Money.ofDollars(650));
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

    // Arithmetic overflow (a balance near Long.MAX_VALUE cents accruing interest) is treated the same as hitting
    // the cap, rather than propagating.

    @Test
    void aBalanceNearLongMaxValueOverflowsIntoATruncatedScheduleInsteadOfThrowing() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(1_000))
                .withHousing(new Rent(Money.ZERO))
                .withDebt("Huge", DebtType.CREDIT_CARD, Money.ofCents(Long.MAX_VALUE - 100), "24", Money.ofDollars(10))
                .withAsOf(AS_OF)
                .build();

        assertThatCode(() -> PayoffScheduleCalculator.schedule(snapshot)).doesNotThrowAnyException();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);
        assertThat(schedule.truncated()).isTrue();
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

    // debtFreeMonth and mortgagePayoffMonth (design.md 041 section 3).

    @Test
    void debtFreeMonthIsAsOfWhenThereAreNoDebts() {
        FinancialSnapshot snapshot = aSnapshot().withHousing(new Rent(Money.ZERO)).withAsOf(AS_OF).build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.debtFreeMonth()).isEqualTo(Optional.of(AS_OF));
    }

    @Test
    void debtFreeMonthIsAsOfWhenThereAreNoDebtsEvenWithAMortgage() {
        Mortgage mortgage = new Mortgage(Money.ofDollars(200_000), new BigDecimal("6"), Money.ofDollars(1_500));
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(5_000))
                .withHousing(mortgage)
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.debtFreeMonth()).isEqualTo(Optional.of(AS_OF));
    }

    // Hand-verified: two debts and a mortgage, all 0% so the numbers are exact and easy to follow.
    //
    // Debt A: $30.00 balance, min $10. Debt B: $100.00 balance, min $10. Mortgage: $5.00 balance, min $5. Income
    // $45, no essential expenses: surplus (extraCash) is 45 - 0 - 5(housing) - 10 - 10 = $20.00 every month.
    // Payoff order (smallest balance first, mortgage last): A, B, Mortgage.
    //
    // Month 1 (cascade $0, totalAvail $20):
    //   A: open 30, cap 10 + 20 = 30, payment 30 (payoff), end 0 — closes. pool after A: 20 + 10 - 30 = 0.
    //   B: open 100, cap 10 + 0 = 10, payment 10, end 90. pool after B: 0 + 10 - 10 = 0.
    //   Mortgage: open 5, cap 5 + 0 = 5, payment 5 (payoff), end 0 — closes.
    //   Freed minimums this month: A's $10 + Mortgage's $5 = $15 (counted once).
    //
    // Month 2 (cascade $15, totalAvail $35):
    //   B: open 90, cap 10 + 35 = 45, payment 45, end 45. No further closures this month.
    //
    // Month 3 (cascade $15, totalAvail $35):
    //   B: open 45, cap 10 + 35 = 45, payment 45 (payoff), end 0 — closes. Every loan is now closed.
    //
    // debtFreeMonth is the later of A's (month 1) and B's (month 3) closing: month 3. mortgagePayoffMonth is the
    // mortgage's own closing: month 1. The two differ, and debtFreeMonth is unaffected by the mortgage closing
    // earlier than the last debt.

    @Test
    void debtFreeMonthIsTheLaterOfTwoDebtsClosingMonthsAndIgnoresTheMortgage() {
        Mortgage mortgage = new Mortgage(Money.ofDollars(5), BigDecimal.ZERO, Money.ofDollars(5));
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(45))
                .withHousing(mortgage)
                .withDebt("Debt A", DebtType.CREDIT_CARD, Money.ofDollars(30), "0", Money.ofDollars(10))
                .withDebt("Debt B", DebtType.CREDIT_CARD, Money.ofDollars(100), "0", Money.ofDollars(10))
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.months()).hasSize(3);
        assertThat(schedule.debtFreeMonth()).isEqualTo(Optional.of(AS_OF.plusMonths(2)));
        assertThat(schedule.mortgagePayoffMonth()).isEqualTo(Optional.of(AS_OF));
        assertThat(schedule.debtFreeMonth()).isNotEqualTo(schedule.mortgagePayoffMonth());
        assertInvariants(schedule);
    }

    @Test
    void mortgagePayoffMonthIsEmptyWithNoMortgageRow() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(1_000))
                .withHousing(new Rent(Money.ofDollars(1_200)))
                .withDebt("Card", DebtType.CREDIT_CARD, Money.ofDollars(500), "5", Money.ofDollars(25))
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.mortgagePayoffMonth()).isEmpty();
    }

    // The 600-month cap: a debt whose minimum never covers its own interest never closes, so debtFreeMonth is
    // empty (see aLoanWhoseMinimumNeverCoversInterestHitsTheSixHundredMonthCap for the same shape).

    @Test
    void debtFreeMonthIsEmptyWhenADebtNeverClosesWithinTheCap() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(50))
                .withHousing(new Rent(Money.ZERO))
                .withDebt("Stuck", DebtType.CREDIT_CARD, Money.ofDollars(10_000), "24", Money.ofDollars(1))
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.truncated()).isTrue();
        assertThat(schedule.debtFreeMonth()).isEmpty();
    }

    // The 600-month cap on the mortgage itself (see MortgagePayoffCalculatorTests.isNullWhenTheLoanRunsPastSix-
    // HundredMonths for the same balance, rate, and payment): a payment that never covers its own interest never
    // closes it, so mortgagePayoffMonth is empty even though there are no debts to hold debtFreeMonth back. Income
    // exactly matches the mortgage payment, so there is no surplus to speed the payoff up.

    @Test
    void mortgagePayoffMonthIsEmptyWhenTheMortgageNeverClosesWithinTheCap() {
        Mortgage stuck = new Mortgage(Money.ofDollars(500_000), new BigDecimal("20"), Money.ofCents(833_340));
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofCents(833_340))
                .withHousing(stuck)
                .withAsOf(AS_OF)
                .build();

        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(schedule.truncated()).isTrue();
        assertThat(schedule.mortgagePayoffMonth()).isEmpty();
        assertThat(schedule.debtFreeMonth()).isEqualTo(Optional.of(AS_OF));
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
