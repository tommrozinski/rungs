package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Debt;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Computes the debt snowball payoff schedule for a household's debts and mortgage (design.md 040 section 3 for the
 * month-by-month pool mechanics; design.md 041 section 1 for the order): every debt smallest starting balance
 * first (ties in supplied order), then the mortgage last when it has a balance. Each month the household's
 * surplus, plus the minimum payments of loans already closed, goes to the loan at the front of the line; the rest
 * of every open loan gets at least its own minimum. Pure and deterministic: every fact comes from the supplied
 * {@link FinancialSnapshot}; nothing here reads a clock.
 */
public final class PayoffScheduleCalculator {

    /** Turns a percentage rate (6.125 means 6.125%) into a decimal monthly rate. */
    private static final BigDecimal PERCENT_MONTHS = BigDecimal.valueOf(1200);

    /** Longest schedule considered before it is flagged {@link PayoffSchedule#truncated()} (50 years). */
    private static final int MAX_MONTHS = 600;

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
     * the mortgage when the household has one with a balance above $0 (design.md 041 section 1: the mortgage
     * always comes last, regardless of its own balance).
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

    /** Runs the month-by-month simulation described in design.md 040 section 3 until every loan closes or the
     * schedule is capped at {@link #MAX_MONTHS}; any arithmetic overflow along the way is treated the same as
     * hitting the cap, rather than propagating. */
    private static PayoffSchedule simulate(List<ScheduleLoan> loans, Money extraCash, YearMonth asOf) {
        int loanCount = loans.size();
        Money[] balances = new Money[loanCount];
        boolean[] closed = new boolean[loanCount];
        Money[] paidPerLoan = new Money[loanCount];
        BigDecimal[] monthlyRates = new BigDecimal[loanCount];
        for (int i = 0; i < loanCount; i++) {
            balances[i] = loans.get(i).startingBalance();
            paidPerLoan[i] = Money.ZERO;
            monthlyRates[i] = loans.get(i).annualRate().divide(PERCENT_MONTHS, MathContext.DECIMAL128);
        }

        List<ScheduleMonth> months = new ArrayList<>();
        Money cascade = Money.ZERO;
        Money cumulativeInterest = Money.ZERO;
        Money totalExtraCash = Money.ZERO;
        boolean truncated = true;

        try {
            for (int monthIndex = 0; monthIndex < MAX_MONTHS; monthIndex++) {
                Money totalAvailable = extraCash.plus(cascade);
                Money pool = totalAvailable;
                Money monthInterest = Money.ZERO;
                Money totalBalance = Money.ZERO;
                Money justClosedMinimums = Money.ZERO;
                List<LoanMonth> loanMonths = new ArrayList<>(loanCount);

                for (int i = 0; i < loanCount; i++) {
                    ScheduleLoan loan = loans.get(i);
                    if (closed[i]) {
                        loanMonths.add(new LoanMonth(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO));
                        continue;
                    }
                    Money open = balances[i];
                    Money interest = open.times(monthlyRates[i]);
                    Money owed = open.plus(interest);
                    Money cap = loan.minimumPayment().plus(pool);
                    Money payment = owed.min(cap);
                    Money end = owed.minus(payment);

                    // pmt never exceeds minimum + pool, so this is never negative: an unused minimum (pmt below
                    // minimum, e.g. a small payoff) flows forward to the next loan this same month, and money
                    // taken above the minimum is subtracted from what is left.
                    pool = pool.plus(loan.minimumPayment()).minus(payment);

                    loanMonths.add(new LoanMonth(open, interest, payment, end));
                    monthInterest = monthInterest.plus(interest);
                    totalBalance = totalBalance.plus(end);
                    paidPerLoan[i] = paidPerLoan[i].plus(payment);
                    balances[i] = end;

                    if (end.isZero()) {
                        closed[i] = true;
                        justClosedMinimums = justClosedMinimums.plus(loan.minimumPayment());
                    }
                }

                cumulativeInterest = cumulativeInterest.plus(monthInterest);
                totalExtraCash = totalExtraCash.plus(extraCash);
                months.add(new ScheduleMonth(
                        asOf.plusMonths(monthIndex),
                        extraCash,
                        cascade,
                        totalAvailable,
                        loanMonths,
                        monthInterest,
                        totalBalance,
                        cumulativeInterest));

                cascade = cascade.plus(justClosedMinimums);

                if (totalBalance.isZero()) {
                    truncated = false;
                    break;
                }
            }
        } catch (ArithmeticException overflow) {
            truncated = true;
        }

        return new PayoffSchedule(
                loans, months, totalExtraCash, List.of(paidPerLoan), cumulativeInterest, truncated, asOf);
    }
}
