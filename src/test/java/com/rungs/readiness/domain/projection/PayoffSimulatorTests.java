package com.rungs.readiness.domain.projection;

import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.Debt;
import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PayoffSimulatorTests {

    private final PayoffSimulator simulator = new PayoffSimulator();

    private static Debt debt(int position, Money balance, String rate, Money minimum) {
        return new Debt(
                "debt-" + position, "Debt " + position, DebtType.OTHER, balance, new BigDecimal(rate), minimum);
    }

    /**
     * Asserts REQ-009 on every debt in {@code input} against {@code simulation}: total payments equal total
     * interest plus (starting balance minus final balance), and no balance in the schedule is negative.
     */
    private static void assertAccountingInvariant(SimulationInput input, PayoffSimulation simulation) {
        Map<String, Money> totalPaymentById = new HashMap<>();
        Map<String, Money> totalInterestById = new HashMap<>();
        Map<String, Money> finalBalanceById = new HashMap<>();
        for (MonthlyPeriod period : simulation.schedule()) {
            for (DebtLine line : period.lines()) {
                assertThat(line.openingBalance().isNegative()).as("openingBalance never negative").isFalse();
                assertThat(line.closingBalance().isNegative()).as("closingBalance never negative").isFalse();
                totalPaymentById.merge(line.debtId(), line.payment(), Money::plus);
                totalInterestById.merge(line.debtId(), line.interest(), Money::plus);
                finalBalanceById.put(line.debtId(), line.closingBalance());
            }
        }
        for (Debt debt : input.debts()) {
            Money totalPayment = totalPaymentById.getOrDefault(debt.id(), Money.ZERO);
            Money totalInterest = totalInterestById.getOrDefault(debt.id(), Money.ZERO);
            Money finalBalance = finalBalanceById.getOrDefault(debt.id(), debt.balance());
            Money expectedPayment = totalInterest.plus(debt.balance()).minus(finalBalance);
            assertThat(totalPayment).as("REQ-009 accounting invariant for %s", debt.id()).isEqualTo(expectedPayment);
        }
    }

    @Test
    void interestOfExactlyHalfACentRoundsHalfEvenDown() {
        // 8 cents * (75 / 1200) = 0.5 cents; half-even rounds 0.5 to 0 (the nearest even cent).
        Debt debt = debt(1, Money.ofCents(8), "75", Money.ZERO);
        SimulationInput input = new SimulationInput(
                List.of(debt), Money.ZERO, Money.ZERO, Strategy.SNOWBALL.ordering(), 1);

        PayoffSimulation simulation = simulator.simulate(input);

        DebtLine line = simulation.schedule().get(0).lines().get(0);
        assertThat(line.openingBalance()).isEqualTo(Money.ofCents(8));
        assertThat(line.interest()).isEqualTo(Money.ZERO);
        assertThat(line.closingBalance()).isEqualTo(Money.ofCents(8));
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void interestOfExactlyOneAndAHalfCentsRoundsHalfEvenUp() {
        // 24 cents * (75 / 1200) = 1.5 cents; half-even rounds 1.5 to 2 (the nearest even cent).
        Debt debt = debt(1, Money.ofCents(24), "75", Money.ZERO);
        SimulationInput input = new SimulationInput(
                List.of(debt), Money.ZERO, Money.ZERO, Strategy.SNOWBALL.ordering(), 1);

        PayoffSimulation simulation = simulator.simulate(input);

        DebtLine line = simulation.schedule().get(0).lines().get(0);
        assertThat(line.openingBalance()).isEqualTo(Money.ofCents(24));
        assertThat(line.interest()).isEqualTo(Money.ofCents(2));
        assertThat(line.closingBalance()).isEqualTo(Money.ofCents(26));
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void aMinimumAboveTheRemainingBalancePaysOnlyTheBalanceAndRetiresTheDebt() {
        Debt debt = debt(1, Money.ofDollars(50), "0", Money.ofDollars(90));
        SimulationInput input = new SimulationInput(
                List.of(debt), Money.ZERO, Money.ZERO, Strategy.SNOWBALL.ordering(), 2);

        PayoffSimulation simulation = simulator.simulate(input);

        assertThat(simulation.schedule()).hasSize(1);
        DebtLine line = simulation.schedule().get(0).lines().get(0);
        assertThat(line.openingBalance()).isEqualTo(Money.ofDollars(50));
        assertThat(line.payment()).isEqualTo(Money.ofDollars(50));
        assertThat(line.closingBalance()).isEqualTo(Money.ZERO);
        assertThat(simulation.schedule().get(0).retiredDebtIds()).containsExactly("debt-1");
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void anOverpaymentCascadesToTheNextDebtInTheSameMonth() {
        Debt first = debt(1, Money.ofDollars(50), "0", Money.ofDollars(90));
        Debt second = debt(2, Money.ofDollars(1000), "0", Money.ofDollars(10));
        SimulationInput input = new SimulationInput(
                List.of(first, second),
                Money.ZERO,
                Money.ZERO,
                new FixedOrder(List.of(first, second)),
                1);

        PayoffSimulation simulation = simulator.simulate(input);

        MonthlyPeriod month1 = simulation.schedule().get(0);
        DebtLine firstLine = month1.lines().get(0);
        DebtLine secondLine = month1.lines().get(1);
        // First debt's minimum (90) exceeds its balance (50), leaving 40 unused this month, which cascades
        // to the second debt on top of its own 10 minimum: 40 + 10 = 50.
        assertThat(firstLine.payment()).isEqualTo(Money.ofDollars(50));
        assertThat(firstLine.closingBalance()).isEqualTo(Money.ZERO);
        assertThat(secondLine.payment()).isEqualTo(Money.ofDollars(50));
        assertThat(secondLine.closingBalance()).isEqualTo(Money.ofDollars(950));
        assertThat(month1.retiredDebtIds()).containsExactly("debt-1");
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void aRetiredDebtsMinimumJoinsThePoolTheFollowingMonthNotTheSameMonth() {
        Debt first = debt(1, Money.ofDollars(90), "0", Money.ofDollars(90));
        Debt second = debt(2, Money.ofDollars(1000), "0", Money.ofDollars(10));
        SimulationInput input = new SimulationInput(
                List.of(first, second),
                Money.ZERO,
                Money.ZERO,
                new FixedOrder(List.of(first, second)),
                2);

        PayoffSimulation simulation = simulator.simulate(input);

        MonthlyPeriod month1 = simulation.schedule().get(0);
        DebtLine secondLineMonth1 = month1.lines().get(1);
        // Month 1: debt-1's minimum exactly covers its balance, leaving nothing unused; debt-2 gets only its
        // own minimum.
        assertThat(secondLineMonth1.payment()).isEqualTo(Money.ofDollars(10));
        assertThat(month1.retiredDebtIds()).containsExactly("debt-1");

        MonthlyPeriod month2 = simulation.schedule().get(1);
        DebtLine secondLineMonth2 = month2.lines().get(0);
        // Month 2: debt-1's freed 90 minimum now joins the pool, on top of debt-2's own 10 minimum: 100.
        assertThat(secondLineMonth2.payment()).isEqualTo(Money.ofDollars(100));
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void aNegativeMonthlySurplusIsTreatedAsZero() {
        Debt debt = debt(1, Money.ofDollars(1000), "0", Money.ofDollars(50));
        SimulationInput withNegativeSurplus = new SimulationInput(
                List.of(debt), Money.ofCents(-50_000), Money.ZERO, Strategy.SNOWBALL.ordering(), 1);
        SimulationInput withZeroSurplus = new SimulationInput(
                List.of(debt), Money.ZERO, Money.ZERO, Strategy.SNOWBALL.ordering(), 1);

        PayoffSimulation negative = simulator.simulate(withNegativeSurplus);
        PayoffSimulation zero = simulator.simulate(withZeroSurplus);

        assertThat(negative).isEqualTo(zero);
        assertAccountingInvariant(withNegativeSurplus, negative);
        assertAccountingInvariant(withZeroSurplus, zero);
    }

    @Test
    void ac23AnUnpayableDebtTerminatesAtTheHorizonAndIsExcludedFromMonthsToDebtFreedom() {
        // debt-2 retires in month 1 from its own minimum and frees $50/month from month 2 on. Even with that
        // freed $50 added on top of debt-1's own $10 minimum (a fixed $60/month), it never catches up to
        // debt-1's interest (30%/12 = 2.5%/month = $75 on its $3,000 opening balance, only growing from
        // there), so debt-1's balance grows every month instead of amortizing.
        Debt unpayable = debt(1, Money.ofDollars(3000), "30", Money.ofDollars(10));
        Debt payable = debt(2, Money.ofDollars(50), "0", Money.ofDollars(50));
        SimulationInput input = new SimulationInput(
                List.of(unpayable, payable),
                Money.ZERO,
                Money.ZERO,
                new FixedOrder(List.of(payable, unpayable)),
                600);

        PayoffSimulation simulation = simulator.simulate(input);

        assertThat(simulation.schedule()).hasSize(600);
        assertThat(simulation.truncated()).isTrue();
        assertThat(simulation.unpayableDebtIds()).containsExactly("debt-1");
        assertThat(simulation.monthsToDebtFreedom()).isEqualTo(1);
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void ac24ZeroSurplusAndZeroExtraStillProduceAMinimumsOnlySchedule() {
        Debt first = debt(1, Money.ofDollars(100), "0", Money.ofDollars(50));
        Debt second = debt(2, Money.ofDollars(200), "0", Money.ofDollars(50));
        SimulationInput input = new SimulationInput(
                List.of(first, second),
                Money.ZERO,
                Money.ZERO,
                new FixedOrder(List.of(first, second)),
                10);

        PayoffSimulation simulation = simulator.simulate(input);

        assertThat(simulation.truncated()).isFalse();
        assertThat(simulation.monthsToDebtFreedom()).isEqualTo(3);
        // Before any minimum is freed by a retirement, every payment is exactly that debt's own minimum.
        MonthlyPeriod month1 = simulation.schedule().get(0);
        for (DebtLine line : month1.lines()) {
            assertThat(line.payment()).isEqualTo(line.openingBalance().min(Money.ofDollars(50)));
        }
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void ac25TheScheduleNeverExceedsTheHorizonAndMonthsToDebtFreedomIsNullWhenTruncated() {
        Debt debt = debt(1, Money.ofDollars(10_000), "0", Money.ofDollars(10));
        SimulationInput input = new SimulationInput(
                List.of(debt), Money.ZERO, Money.ZERO, Strategy.SNOWBALL.ordering(), 3);

        PayoffSimulation simulation = simulator.simulate(input);

        assertThat(simulation.schedule()).hasSize(3);
        assertThat(simulation.truncated()).isTrue();
        assertThat(simulation.monthsToDebtFreedom()).isNull();
        assertThat(simulation.unpayableDebtIds()).isEmpty();
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void identicalInputProducesAnIdenticalSimulation() {
        Debt first = debt(1, Money.ofDollars(500), "12", Money.ofDollars(50));
        Debt second = debt(2, Money.ofDollars(1500), "20", Money.ofDollars(75));
        SimulationInput input = new SimulationInput(
                List.of(first, second), Money.ofDollars(100), Money.ofDollars(25), Strategy.AVALANCHE.ordering(), 60);

        PayoffSimulation firstRun = simulator.simulate(input);
        PayoffSimulation secondRun = simulator.simulate(input);

        assertThat(firstRun).isEqualTo(secondRun);
        assertAccountingInvariant(input, firstRun);
    }

    @Test
    void ac22AvalancheTotalInterestPaidNeverExceedsSnowballOnTheThreeDebtScenario() {
        // The design's §3 fixed scenario: credit card, medical bill (mapped to DebtType.MEDICAL), car loan.
        Debt creditCard = debt(1, Money.ofDollars(3500), "23.99", Money.ofDollars(90));
        Debt medicalBill = new Debt(
                "debt-2", "Medical bill", DebtType.MEDICAL, Money.ofDollars(900), new BigDecimal("0"), Money.ofDollars(50));
        Debt carLoan = debt(3, Money.ofDollars(7200), "6.9", Money.ofDollars(210));
        List<Debt> debts = List.of(creditCard, medicalBill, carLoan);

        SimulationInput snowballInput = new SimulationInput(
                debts, Money.ofDollars(300), Money.ZERO, Strategy.SNOWBALL.ordering(), 360);
        SimulationInput avalancheInput = new SimulationInput(
                debts, Money.ofDollars(300), Money.ZERO, Strategy.AVALANCHE.ordering(), 360);

        PayoffSimulation snowball = simulator.simulate(snowballInput);
        PayoffSimulation avalanche = simulator.simulate(avalancheInput);

        assertThat(avalanche.totalInterestPaid()).isLessThanOrEqualTo(snowball.totalInterestPaid());
        assertAccountingInvariant(snowballInput, snowball);
        assertAccountingInvariant(avalancheInput, avalanche);
    }

    @Test
    void singleDebtSchedulesAreIdenticalUnderBothStrategies() {
        Debt only = debt(1, Money.ofDollars(1000), "15", Money.ofDollars(50));
        SimulationInput snowballInput = new SimulationInput(
                List.of(only), Money.ofDollars(100), Money.ZERO, Strategy.SNOWBALL.ordering(), 60);
        SimulationInput avalancheInput = new SimulationInput(
                List.of(only), Money.ofDollars(100), Money.ZERO, Strategy.AVALANCHE.ordering(), 60);

        PayoffSimulation snowball = simulator.simulate(snowballInput);
        PayoffSimulation avalanche = simulator.simulate(avalancheInput);

        assertThat(snowball).isEqualTo(avalanche);
        assertAccountingInvariant(snowballInput, snowball);
    }

    @Test
    void theFixedOrderIsNotReSelectedEachMonthEvenAsBalancesCross() {
        // debt-2 starts smaller (100 < 101), so the fixed snowball order targets it first with every
        // month's extra payment. debt-1 gets only its own minimum, and its zero-interest balance falls
        // below debt-2's balance well before debt-2 is retired. A monthly re-selection snowball would
        // switch the extra to debt-1 at that point; the fixed order does not.
        Debt debtOne = debt(1, Money.ofDollars(101), "0", Money.ofDollars(50));
        Debt debtTwo = debt(2, Money.ofDollars(100), "24", Money.ofDollars(5));
        SimulationInput input = new SimulationInput(
                List.of(debtOne, debtTwo), Money.ZERO, Money.ofDollars(20), Strategy.SNOWBALL.ordering(), 12);

        PayoffSimulation simulation = simulator.simulate(input);

        MonthlyPeriod month2 = simulation.schedule().get(1);
        DebtLine debtOneMonth2 = month2.lines().stream().filter(line -> line.debtId().equals("debt-1")).findFirst().orElseThrow();
        DebtLine debtTwoMonth2 = month2.lines().stream().filter(line -> line.debtId().equals("debt-2")).findFirst().orElseThrow();
        // By month 2, debt-1's balance (paid down by its own minimum only) is below debt-2's, yet debt-2
        // still receives the extra (its payment exceeds its own minimum of $5), while debt-1 receives only
        // its own minimum ($50, or less if it is smaller than the balance).
        assertThat(debtOneMonth2.closingBalance()).isLessThan(debtTwoMonth2.closingBalance());
        assertThat(debtTwoMonth2.payment()).isGreaterThan(Money.ofDollars(5));
        assertThat(debtOneMonth2.payment()).isEqualTo(debtOneMonth2.openingBalance().min(Money.ofDollars(50)));
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void monthRetiredMatchesTheSimulatedSchedule() {
        Debt first = debt(1, Money.ofDollars(50), "0", Money.ofDollars(50));
        Debt second = debt(2, Money.ofDollars(200), "0", Money.ofDollars(50));
        SimulationInput input = new SimulationInput(
                List.of(first, second), Money.ZERO, Money.ZERO, new FixedOrder(List.of(first, second)), 10);

        PayoffSimulation simulation = simulator.simulate(input);

        assertThat(simulation.monthRetired("debt-1")).contains(1);
        assertThat(simulation.monthRetired("debt-2")).contains(3);
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void fiftyDebtsOverSixHundredMonthsCompletesQuickly() {
        List<Debt> debts = new java.util.ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            debts.add(debt(i, Money.ofDollars(1000 + i * 100), "18", Money.ofDollars(30)));
        }
        SimulationInput input = new SimulationInput(
                debts, Money.ofDollars(500), Money.ofDollars(100), Strategy.AVALANCHE.ordering(), 600);

        long start = System.nanoTime();
        PayoffSimulation simulation = simulator.simulate(input);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMillis).isLessThan(5000);
        assertThat(simulation.schedule()).isNotEmpty();
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void ac21ThreeDebtSnowballScheduleMatchesTheIndependentlyComputedGoldenCsv() throws java.io.IOException {
        // design.md §3: the same fixed three-debt scenario the Python golden script computes independently.
        Debt creditCard = debt(1, Money.ofDollars(3500), "23.99", Money.ofDollars(90));
        Debt medicalBill = new Debt(
                "debt-2", "Medical bill", DebtType.MEDICAL, Money.ofDollars(900), new BigDecimal("0"), Money.ofDollars(50));
        Debt carLoan = debt(3, Money.ofDollars(7200), "6.9", Money.ofDollars(210));
        SimulationInput input = new SimulationInput(
                List.of(creditCard, medicalBill, carLoan),
                Money.ofDollars(300),
                Money.ZERO,
                Strategy.SNOWBALL.ordering(),
                360);

        PayoffSimulation simulation = simulator.simulate(input);
        assertAccountingInvariant(input, simulation);

        List<String[]> expectedRows = readGoldenCsv("/golden/payoff-three-debts-snowball.csv");
        List<String[]> actualRows = new java.util.ArrayList<>();
        for (MonthlyPeriod period : simulation.schedule()) {
            for (DebtLine line : period.lines()) {
                boolean retiredThisMonth = period.retiredDebtIds().contains(line.debtId());
                actualRows.add(new String[] {
                        String.valueOf(period.month()),
                        line.debtId(),
                        line.openingBalance().toBareString(),
                        line.interest().toBareString(),
                        line.payment().toBareString(),
                        line.closingBalance().toBareString(),
                        String.valueOf(retiredThisMonth)
                });
            }
        }

        assertThat(actualRows).hasSize(expectedRows.size());
        for (int i = 0; i < expectedRows.size(); i++) {
            assertThat(actualRows.get(i)).as("row %d", i).isEqualTo(expectedRows.get(i));
        }
    }

    /** Reads a golden CSV fixture (header, then {@code month,debtId,openingBalance,interest,payment,closingBalance,retired}). */
    private static List<String[]> readGoldenCsv(String classpathResource) throws java.io.IOException {
        List<String[]> rows = new java.util.ArrayList<>();
        try (var input = PayoffSimulatorTests.class.getResourceAsStream(classpathResource);
                var reader = new java.io.BufferedReader(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] fields = line.split(",", -1);
                rows.add(fields);
            }
        }
        return rows;
    }

    @Test
    void aDebtRetiredExactlyOnTheFinalHorizonMonthIsNotTruncated() {
        Debt debt = debt(1, Money.ofDollars(150), "0", Money.ofDollars(50));
        SimulationInput input = new SimulationInput(
                List.of(debt), Money.ZERO, Money.ZERO, Strategy.SNOWBALL.ordering(), 3);

        PayoffSimulation simulation = simulator.simulate(input);

        assertThat(simulation.schedule()).hasSize(3);
        assertThat(simulation.truncated()).isFalse();
        assertThat(simulation.monthsToDebtFreedom()).isEqualTo(3);
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void aRunawayBalanceAboveTheCeilingTruncatesInsteadOfOverflowing() {
        // The largest valid single debt at 100% APR with nothing paid toward it: interest compounds fast
        // enough that, left unbounded, the balance would overflow a long well before month 230 (008 code
        // review finding). The runaway ceiling must stop the simulation first.
        Debt debt = debt(1, Money.ofDollars(1_000_000_000L), "100", Money.ZERO);
        SimulationInput input = new SimulationInput(
                List.of(debt), Money.ZERO, Money.ZERO, Strategy.SNOWBALL.ordering(), 230);

        PayoffSimulation simulation = simulator.simulate(input);

        assertThat(simulation.truncated()).isTrue();
        assertThat(simulation.unpayableDebtIds()).containsExactly("debt-1");
        // The only debt present is unpayable, so there is no payable debt to leave unretired; per REQ-007
        // this is the same vacuous case as no debts at all, not the "still unretired" null case.
        assertThat(simulation.monthsToDebtFreedom()).isEqualTo(0);
        assertThat(simulation.schedule().size()).isLessThanOrEqualTo(230);
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void aRunawayBalanceAboveTheCeilingTruncatesAtTheMaximumHorizonToo() {
        Debt debt = debt(1, Money.ofDollars(1_000_000_000L), "100", Money.ZERO);
        SimulationInput input = new SimulationInput(
                List.of(debt), Money.ZERO, Money.ZERO, Strategy.SNOWBALL.ordering(), 600);

        PayoffSimulation simulation = simulator.simulate(input);

        assertThat(simulation.truncated()).isTrue();
        assertThat(simulation.unpayableDebtIds()).containsExactly("debt-1");
        assertThat(simulation.schedule().size()).isLessThanOrEqualTo(600);
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void fiftyDebtsAtEveryMaximumValidValueDoNotOverflowOverSixHundredMonths() {
        List<Debt> debts = new java.util.ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            debts.add(debt(i, Money.ofDollars(1_000_000_000L), "100", Money.ofDollars(1_000_000_000L)));
        }
        SimulationInput input = new SimulationInput(
                debts,
                Money.ofDollars(1_000_000_000L),
                Money.ofDollars(1_000_000_000L),
                Strategy.AVALANCHE.ordering(),
                600);

        PayoffSimulation simulation = simulator.simulate(input);

        assertThat(simulation.schedule()).isNotEmpty();
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void aRunawayDebtCoexistingWithAPayableDebtStillReportsTheLatterSMonthsToDebtFreedom() {
        Debt runaway = debt(1, Money.ofDollars(1_000_000_000L), "100", Money.ZERO);
        Debt payable = debt(2, Money.ofDollars(50), "0", Money.ofDollars(50));
        SimulationInput input = new SimulationInput(
                List.of(runaway, payable),
                Money.ZERO,
                Money.ZERO,
                new FixedOrder(List.of(payable, runaway)),
                600);

        PayoffSimulation simulation = simulator.simulate(input);

        assertThat(simulation.truncated()).isTrue();
        assertThat(simulation.unpayableDebtIds()).containsExactly("debt-1");
        assertThat(simulation.monthRetired("debt-2")).contains(1);
        assertThat(simulation.monthsToDebtFreedom()).isEqualTo(1);
        assertAccountingInvariant(input, simulation);
    }

    @Test
    void noDebtsProducesAnEmptySchedule() {
        SimulationInput input = new SimulationInput(
                List.of(), Money.ZERO, Money.ZERO, Strategy.SNOWBALL.ordering(), 12);

        PayoffSimulation simulation = simulator.simulate(input);

        assertThat(simulation.schedule()).isEmpty();
        assertThat(simulation.monthsToDebtFreedom()).isEqualTo(0);
        assertThat(simulation.totalInterestPaid()).isEqualTo(Money.ZERO);
        assertThat(simulation.truncated()).isFalse();
        assertThat(simulation.unpayableDebtIds()).isEmpty();
        assertAccountingInvariant(input, simulation);
    }
}
