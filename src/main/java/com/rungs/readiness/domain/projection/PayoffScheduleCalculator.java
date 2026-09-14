package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Debt;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Computes the debt snowball payoff schedule for a household's debts and mortgage: every debt smallest starting
 * balance first (ties in supplied order), then the mortgage last when it has a balance. Each month the household's
 * surplus, plus the minimum payments of loans already closed, goes to the loan at the front of the line; the rest
 * of every open loan gets at least its own minimum. Pure and deterministic: every fact comes from the supplied
 * {@link FinancialSnapshot}; nothing here reads a clock.
 */
public final class PayoffScheduleCalculator {

    private static final String MORTGAGE_NAME = "Mortgage";

    private PayoffScheduleCalculator() {
    }

    /** Returns the payoff schedule for {@code snapshot}; throws NullPointerException if it is null. */
    public static PayoffSchedule schedule(FinancialSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");

        List<ScheduleLoan> loans = orderedLoans(snapshot);
        if (loans.isEmpty()) {
            return new PayoffSchedule(loans, List.of(), Money.ZERO, List.of(), Money.ZERO, false, snapshot.asOf());
        }

        Money extraCash = snapshot.monthlySurplus().max(Money.ZERO);
        return simulate(loans, extraCash, snapshot.asOf());
    }

    /**
     * Returns every debt ordered by starting balance ascending (ties in supplied order, a stable sort), followed by
     * the mortgage when the household has one with a balance above $0: the mortgage always comes last, regardless
     * of its own balance.
     */
    private static List<ScheduleLoan> orderedLoans(FinancialSnapshot snapshot) {
        List<ScheduleLoan> loans = new ArrayList<>();
        List<Debt> debts = new ArrayList<>(snapshot.debts());
        debts.sort(Comparator.comparing(Debt::balance));
        for (Debt debt : debts) {
            loans.add(new ScheduleLoan(
                    debt.label(), debt.balance(), debt.annualInterestRate(), debt.minimumPayment(), false));
        }
        if (snapshot.housing() instanceof Mortgage mortgage && mortgage.balance().isPositive()) {
            loans.add(new ScheduleLoan(
                    MORTGAGE_NAME, mortgage.balance(), mortgage.annualInterestRate(), mortgage.monthlyPayment(), true));
        }
        return loans;
    }

    /**
     * Runs the month-by-month simulation until every loan closes or the schedule is capped at
     * {@link Amortization#MAX_MONTHS}. Each month is computed by {@link #stepMonth} into a fresh, immutable list of
     * {@link LoanState} without touching {@code states} itself; the running totals below are only updated, and the
     * month only appended to {@code months}, once that computation succeeds in full. An arithmetic overflow
     * anywhere in a month's computation therefore leaves every running total exactly as it was after the last
     * fully-recorded month, with no partial month's figures counted anywhere, rather than propagating the
     * {@link ArithmeticException}: that month, and every later one, is simply never recorded, and the schedule
     * comes back {@link PayoffSchedule#truncated()}.
     */
    private static PayoffSchedule simulate(List<ScheduleLoan> loans, Money extraCash, YearMonth asOf) {
        List<LoanState> states = initialStates(loans);

        List<ScheduleMonth> months = new ArrayList<>();
        Money cascade = Money.ZERO;
        Money cumulativeInterest = Money.ZERO;
        Money totalExtraCash = Money.ZERO;
        boolean truncated = true;

        try {
            for (int monthIndex = 0; monthIndex < Amortization.MAX_MONTHS; monthIndex++) {
                Money totalAvailable = extraCash.plus(cascade);
                MonthOutcome outcome = stepMonth(loans, states, totalAvailable);

                Money newCumulativeInterest = cumulativeInterest.plus(outcome.monthInterest());
                Money newTotalExtraCash = totalExtraCash.plus(extraCash);
                ScheduleMonth scheduleMonth = new ScheduleMonth(
                        asOf.plusMonths(monthIndex),
                        extraCash,
                        cascade,
                        totalAvailable,
                        outcome.loanMonths(),
                        outcome.monthInterest(),
                        outcome.totalBalance(),
                        newCumulativeInterest);

                // Nothing above this line touches states, months, or the running totals, so if any of it threw
                // (an overflow while adding into a running total, say) none of this month would be committed.
                states = outcome.states();
                cumulativeInterest = newCumulativeInterest;
                totalExtraCash = newTotalExtraCash;
                months.add(scheduleMonth);
                cascade = cascade.plus(outcome.justClosedMinimums());

                if (outcome.totalBalance().isZero()) {
                    truncated = false;
                    break;
                }
            }
        } catch (ArithmeticException overflow) {
            truncated = true;
        }

        List<Money> paidPerLoan = states.stream().map(LoanState::paid).toList();
        return new PayoffSchedule(loans, months, totalExtraCash, paidPerLoan, cumulativeInterest, truncated, asOf);
    }

    /** Returns each loan's starting state: its own opening balance, its monthly rate, nothing paid yet, and open. */
    private static List<LoanState> initialStates(List<ScheduleLoan> loans) {
        List<LoanState> states = new ArrayList<>(loans.size());
        for (ScheduleLoan loan : loans) {
            states.add(new LoanState(
                    loan.startingBalance(), Amortization.monthlyRate(loan.annualRate()), Money.ZERO, false));
        }
        return states;
    }

    /**
     * Returns one month's outcome for every loan, aligned by position with {@code loans} and {@code states}:
     * {@code totalAvailable} goes to the loan at the front of the line, with what each open loan doesn't use of it
     * flowing to the next open loan the same month, so the pool for the last open loan may include everything
     * unused by every loan ahead of it. A closed loan reports every figure as {@link Money#ZERO} and carries its
     * state over unchanged. Neither {@code loans} nor {@code states} is modified; the caller decides whether to
     * keep the returned states.
     */
    private static MonthOutcome stepMonth(List<ScheduleLoan> loans, List<LoanState> states, Money totalAvailable) {
        Money pool = totalAvailable;
        Money monthInterest = Money.ZERO;
        Money totalBalance = Money.ZERO;
        Money justClosedMinimums = Money.ZERO;
        List<LoanState> nextStates = new ArrayList<>(states.size());
        List<LoanMonth> loanMonths = new ArrayList<>(states.size());

        for (int i = 0; i < loans.size(); i++) {
            ScheduleLoan loan = loans.get(i);
            LoanState state = states.get(i);
            if (state.closed()) {
                nextStates.add(state);
                loanMonths.add(new LoanMonth(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO));
                continue;
            }

            Money cap = loan.minimumPayment().plus(pool);
            LoanMonth step = Amortization.step(state.balance(), state.monthlyRate(), cap);
            Money payment = step.payment();
            Money end = step.end();

            // payment never exceeds minimum + pool, so this is never negative: an unused minimum (payment below
            // minimum, e.g. a small payoff) flows forward to the next loan this same month, and money taken above
            // the minimum is subtracted from what is left.
            pool = pool.plus(loan.minimumPayment()).minus(payment);

            boolean closes = end.isZero();
            nextStates.add(new LoanState(end, state.monthlyRate(), state.paid().plus(payment), closes));
            loanMonths.add(step);
            monthInterest = monthInterest.plus(step.interest());
            totalBalance = totalBalance.plus(end);
            if (closes) {
                justClosedMinimums = justClosedMinimums.plus(loan.minimumPayment());
            }
        }

        return new MonthOutcome(nextStates, loanMonths, monthInterest, totalBalance, justClosedMinimums);
    }

    /** One loan's running state between months, aligned by position with {@link PayoffScheduleCalculator}'s loans. */
    private record LoanState(Money balance, BigDecimal monthlyRate, Money paid, boolean closed) {
    }

    /** One month's outcome across every loan, computed by {@link #stepMonth} without mutating its inputs. */
    private record MonthOutcome(
            List<LoanState> states,
            List<LoanMonth> loanMonths,
            Money monthInterest,
            Money totalBalance,
            Money justClosedMinimums) {
    }
}
