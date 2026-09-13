package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Debt;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.MonthlyInterest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Runs one household's month-by-month payoff plan (008 REQ-001 to REQ-010). Stateless: every method call
 * depends only on its argument.
 */
public final class PayoffSimulator {

    /**
     * A closing balance at or above this many cents ($1,000,000,000,000.00, 1e14) is treated as runaway
     * negative amortization rather than simulated further: validation caps every debt at $1,000,000,000.00
     * (1e11 cents), so no valid input starts anywhere near this ceiling, yet even one more month of interest
     * at the maximum allowed rate (100% APR, a monthly factor of 1/12) leaves it at roughly 1.083e14 cents —
     * nowhere close to overflowing a {@code long} (max ~9.2e18 cents). This bounds the simulation for a debt
     * whose interest permanently outgrows every payment directed at it (008 REQ-007, REQ-010; 010 REQ-011).
     */
    private static final long RUNAWAY_BALANCE_CENTS = 100_000_000_000_000L;

    /** Returns the payoff schedule for {@code input}; throws NullPointerException if it is null. */
    public PayoffSimulation simulate(SimulationInput input) {
        Objects.requireNonNull(input, "input");
        List<Debt> supplied = input.debts();
        int debtCount = supplied.size();
        if (debtCount == 0) {
            return new PayoffSimulation(List.of(), 0, Money.ZERO, false, List.of());
        }
        return new Run(input, supplied, debtCount).run();
    }

    /** One simulation's mutable working state, indexed throughout by each debt's position as supplied. */
    private static final class Run {

        private final SimulationInput input;
        private final List<Debt> supplied;
        private final int debtCount;

        private final String[] ids;
        private final long[] balanceCents;
        private final long[] startingBalanceCents;
        private final long[] minimumCents;
        private final BigDecimal[] annualInterestRates;
        private final boolean[] retired;
        private final int[] retiredMonth;
        private final int[] payoffOrder;

        private final long surplusCents;
        private final long extraCents;

        Run(SimulationInput input, List<Debt> supplied, int debtCount) {
            this.input = input;
            this.supplied = supplied;
            this.debtCount = debtCount;
            this.ids = new String[debtCount];
            this.balanceCents = new long[debtCount];
            this.startingBalanceCents = new long[debtCount];
            this.minimumCents = new long[debtCount];
            this.annualInterestRates = new BigDecimal[debtCount];
            this.retired = new boolean[debtCount];
            this.retiredMonth = new int[debtCount];

            Map<String, Integer> suppliedIndexById = new HashMap<>();
            for (int i = 0; i < debtCount; i++) {
                Debt debt = supplied.get(i);
                ids[i] = debt.id();
                balanceCents[i] = debt.balance().cents();
                startingBalanceCents[i] = debt.balance().cents();
                minimumCents[i] = debt.minimumPayment().cents();
                annualInterestRates[i] = debt.annualInterestRate();
                suppliedIndexById.put(debt.id(), i);
            }

            this.payoffOrder = new int[debtCount];
            List<Debt> ordered = input.ordering().order(supplied);
            for (int i = 0; i < debtCount; i++) {
                payoffOrder[i] = suppliedIndexById.get(ordered.get(i).id());
            }

            this.surplusCents = Math.max(input.monthlySurplus().cents(), 0);
            this.extraCents = input.extraMonthlyPayment().cents();
        }

        PayoffSimulation run() {
            List<MonthlyPeriod> schedule = new ArrayList<>();
            int remaining = debtCount;
            boolean truncated = false;
            long pendingFreedMinimumCents = 0;
            long totalInterestCents = 0;

            for (int month = 1; month <= input.horizonMonths() && remaining > 0; month++) {
                long[] openingCents = new long[debtCount];
                long[] interestCents = new long[debtCount];
                long[] paymentCents = new long[debtCount];
                long unusedMinimumCents = 0;

                // Step 1: accrue interest, and step 2: pay minimums, on every debt unretired at month start.
                for (int i = 0; i < debtCount; i++) {
                    if (retired[i]) {
                        continue;
                    }
                    openingCents[i] = balanceCents[i];
                    Money interest = MonthlyInterest.on(Money.ofCents(balanceCents[i]), annualInterestRates[i]);
                    interestCents[i] = interest.cents();
                    balanceCents[i] = Math.addExact(balanceCents[i], interestCents[i]);

                    long minimumPayment = Math.min(minimumCents[i], balanceCents[i]);
                    balanceCents[i] = Math.subtractExact(balanceCents[i], minimumPayment);
                    paymentCents[i] = minimumPayment;
                    unusedMinimumCents = Math.addExact(unusedMinimumCents, minimumCents[i] - minimumPayment);
                }

                // Step 3: the pool available this month.
                long poolCents = Math.addExact(
                        Math.addExact(surplusCents, extraCents),
                        Math.addExact(pendingFreedMinimumCents, unusedMinimumCents));

                // Step 4: cascade the pool through the fixed payoff order.
                for (int index : payoffOrder) {
                    if (retired[index] || poolCents <= 0) {
                        continue;
                    }
                    long applied = Math.min(poolCents, balanceCents[index]);
                    balanceCents[index] -= applied;
                    paymentCents[index] += applied;
                    poolCents -= applied;
                }

                // Step 5: retire any debt whose balance reached $0.
                List<DebtLine> lines = new ArrayList<>();
                List<String> retiredIds = new ArrayList<>();
                long newlyFreedMinimumCents = 0;
                for (int i = 0; i < debtCount; i++) {
                    if (retired[i]) {
                        continue;
                    }
                    lines.add(new DebtLine(
                            ids[i],
                            Money.ofCents(openingCents[i]),
                            Money.ofCents(interestCents[i]),
                            Money.ofCents(paymentCents[i]),
                            Money.ofCents(balanceCents[i])));
                    totalInterestCents = Math.addExact(totalInterestCents, interestCents[i]);
                    if (balanceCents[i] == 0) {
                        retired[i] = true;
                        retiredMonth[i] = month;
                        retiredIds.add(ids[i]);
                        newlyFreedMinimumCents = Math.addExact(newlyFreedMinimumCents, minimumCents[i]);
                        remaining--;
                    }
                }
                pendingFreedMinimumCents = Math.addExact(pendingFreedMinimumCents, newlyFreedMinimumCents);

                schedule.add(new MonthlyPeriod(month, lines, retiredIds));

                boolean runaway = false;
                for (int i = 0; i < debtCount; i++) {
                    if (!retired[i] && balanceCents[i] >= RUNAWAY_BALANCE_CENTS) {
                        runaway = true;
                        break;
                    }
                }
                if (runaway) {
                    truncated = true;
                    break;
                }
                if (month == input.horizonMonths() && remaining > 0) {
                    truncated = true;
                }
            }

            return buildResult(schedule, truncated, totalInterestCents);
        }

        private PayoffSimulation buildResult(List<MonthlyPeriod> schedule, boolean truncated, long totalInterestCents) {
            List<String> unpayableDebtIds = new ArrayList<>();
            for (int i = 0; i < debtCount; i++) {
                if (!retired[i] && balanceCents[i] >= startingBalanceCents[i]) {
                    unpayableDebtIds.add(ids[i]);
                }
            }

            Set<String> unpayable = new HashSet<>(unpayableDebtIds);
            boolean anyPayableDebtUnretired = false;
            int lastPayableRetiredMonth = 0;
            for (int i = 0; i < debtCount; i++) {
                if (unpayable.contains(ids[i])) {
                    continue;
                }
                if (!retired[i]) {
                    anyPayableDebtUnretired = true;
                } else {
                    lastPayableRetiredMonth = Math.max(lastPayableRetiredMonth, retiredMonth[i]);
                }
            }
            Integer monthsToDebtFreedom = anyPayableDebtUnretired ? null : lastPayableRetiredMonth;

            return new PayoffSimulation(
                    schedule, monthsToDebtFreedom, Money.ofCents(totalInterestCents), truncated, unpayableDebtIds);
        }
    }
}
