package com.rungs.readiness.domain.projection;

import static com.rungs.readiness.domain.model.FinancialSnapshotBuilder.aSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Mortgage;
import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

/** {@link MortgagePayoffCalculator}. */
class MortgagePayoffCalculatorTests {

    private static final YearMonth AS_OF = YearMonth.of(2024, 1);

    @Test
    void rejectsANullMortgage() {
        assertThatThrownBy(() -> MortgagePayoffCalculator.payoff(null, AS_OF))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("mortgage");
    }

    @Test
    void rejectsANullAsOf() {
        Mortgage mortgage = new Mortgage(Money.ofDollars(1_000), BigDecimal.ZERO, Money.ofDollars(100));

        assertThatThrownBy(() -> MortgagePayoffCalculator.payoff(mortgage, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("asOf");
    }

    // Balance $0

    @Test
    void aZeroBalancePaysOffImmediately() {
        Mortgage paidOff = new Mortgage(Money.ZERO, new BigDecimal("5"), Money.ZERO);

        MortgagePayoff payoff = MortgagePayoffCalculator.payoff(paidOff, AS_OF);

        assertThat(payoff).isEqualTo(new MortgagePayoff.PaysOff(0, AS_OF));
    }

    // Not covered: the payment does not exceed the first month's interest

    @Test
    void isNotCoveredWhenThePaymentDoesNotExceedTheFirstMonthInterest() {
        Mortgage mortgage = new Mortgage(Money.ofDollars(100_000), new BigDecimal("12"), Money.ofDollars(900));

        MortgagePayoff payoff = MortgagePayoffCalculator.payoff(mortgage, AS_OF);

        assertThat(payoff).isEqualTo(new MortgagePayoff.NotCovered(Money.ofDollars(900), Money.ofDollars(1_000)));
    }

    @Test
    void isNotCoveredWhenThePaymentExactlyEqualsTheFirstMonthInterest() {
        Mortgage mortgage = new Mortgage(Money.ofDollars(100_000), new BigDecimal("12"), Money.ofDollars(1_000));

        MortgagePayoff payoff = MortgagePayoffCalculator.payoff(mortgage, AS_OF);

        assertThat(payoff).isInstanceOf(MortgagePayoff.NotCovered.class);
    }

    // Hand-verified small loan, exact to the cent: balance $500 at 12% annual (1% monthly), $260 payment, no taxes.
    // Month 1: interest 500.00 * 0.01 = 5.00; owed 505.00; payment 260.00; balance 245.00.
    // Month 2: interest 245.00 * 0.01 = 2.45; owed 247.45; payment capped at 247.45; balance 0.00. Paid off after 2
    // payments, the first one due in asOf itself, so the second (and last) is due the month after asOf.

    @Test
    void payoffOfAHandVerifiedSmallLoanMatchesTheCentByCentAmortization() {
        Mortgage mortgage = new Mortgage(Money.ofDollars(500), new BigDecimal("12"), Money.ofDollars(260));

        MortgagePayoff payoff = MortgagePayoffCalculator.payoff(mortgage, AS_OF);

        assertThat(payoff).isEqualTo(new MortgagePayoff.PaysOff(2, YearMonth.of(2024, 2)));
    }

    // 0% rate: no interest accrues, so the balance divides evenly by the payment.

    @Test
    void payoffAtAZeroPercentRateDividesTheBalanceByThePayment() {
        Mortgage mortgage = new Mortgage(Money.ofDollars(1_200), BigDecimal.ZERO, Money.ofDollars(300));

        MortgagePayoff payoff = MortgagePayoffCalculator.payoff(mortgage, AS_OF);

        assertThat(payoff).isEqualTo(new MortgagePayoff.PaysOff(4, YearMonth.of(2024, 4)));
    }

    // 600-month cap: covered, but the payment is so close to the first month's interest that the loan never
    // amortizes within 600 months. Balance $500,000 at 20% annual (1.6667% monthly, first month interest $8,333.33);
    // a $8,333.40 payment stays covered but leaves the loop still short of zero after 600 iterations.

    @Test
    void isCappedWhenTheLoanRunsPastSixHundredMonths() {
        Mortgage mortgage = new Mortgage(Money.ofDollars(500_000), new BigDecimal("20"), Money.ofCents(833_340));

        MortgagePayoff payoff = MortgagePayoffCalculator.payoff(mortgage, AS_OF);

        assertThat(payoff).isInstanceOf(MortgagePayoff.Capped.class);
    }

    @Test
    void payoffAtExactlySixHundredMonthsIsNotCapped() {
        // 0% rate, so the balance divides exactly: $600 at $1 a month pays off in exactly 600 months.
        Mortgage mortgage = new Mortgage(Money.ofDollars(600), BigDecimal.ZERO, Money.ofDollars(1));

        MortgagePayoff payoff = MortgagePayoffCalculator.payoff(mortgage, AS_OF);

        assertThat(payoff).isInstanceOf(MortgagePayoff.PaysOff.class);
        assertThat(((MortgagePayoff.PaysOff) payoff).months()).isEqualTo(600);
    }

    // Overflow during amortization: a balance this close to Long.MAX_VALUE cents, at 100% annual interest, adds
    // more than a month's interest before the loop's first payment is even applied, overflowing Money.plus. That
    // must be treated the same as running past 600 months rather than propagating the ArithmeticException.

    @Test
    void overflowDuringAmortizationIsTreatedAsTheSixHundredMonthCap() {
        Money hugeBalance = Money.ofCents(Long.MAX_VALUE - 1000);
        Mortgage mortgage = new Mortgage(hugeBalance, new BigDecimal("100"), hugeBalance);

        MortgagePayoff payoff = MortgagePayoffCalculator.payoff(mortgage, AS_OF);

        assertThat(payoff).isInstanceOf(MortgagePayoff.Capped.class);
    }

    // Cross-checked against the closed-form amortization formula n = -ln(1 - rB/P) / ln(1+r), computed with double
    // in this test only. A 30-year (360-month) loan at 5.75% whose payment is rounded to the cent takes one month
    // longer than the closed-form answer once that rounding is amortized out cent by cent — the off-by-one the
    // design allows for.

    @Test
    void payoffMonthsAreWithinOneMonthOfTheClosedFormAmortizationFormula() {
        double balance = 220_000;
        double annualRatePercent = 5.75;
        double monthlyRate = annualRatePercent / 1200;
        int targetMonths = 360;
        double payment = balance * monthlyRate / (1 - Math.pow(1 + monthlyRate, -targetMonths));
        long paymentCents = Math.round(payment * 100);
        Money paymentMoney = Money.ofCents(paymentCents);

        Mortgage mortgage = new Mortgage(Money.ofDollars((long) balance), new BigDecimal("5.75"), paymentMoney);

        MortgagePayoff payoff = MortgagePayoffCalculator.payoff(mortgage, AS_OF);

        double roundedPayment = paymentCents / 100.0;
        double rB = monthlyRate * balance;
        double closedFormMonths = -Math.log(1 - rB / roundedPayment) / Math.log(1 + monthlyRate);

        assertThat(payoff).isInstanceOf(MortgagePayoff.PaysOff.class);
        int months = ((MortgagePayoff.PaysOff) payoff).months();
        assertThat(Math.abs(months - closedFormMonths)).isLessThanOrEqualTo(1.0);
        // Pinned exactly, so a change to the amortization loop is caught even though the formula above tolerates it.
        assertThat(months).isEqualTo(361);
    }

    // A1 fix: MortgagePayoffCalculator and PayoffScheduleCalculator must agree on which calendar month a loan's
    // first payment is due, so the same mortgage amortized alone reaches zero in the same month either way.

    @Test
    void aLoneMortgagePayoffMatchesTheScheduleCalculatorForTheSameLoan() {
        Mortgage mortgage = new Mortgage(Money.ofDollars(500), new BigDecimal("12"), Money.ofDollars(260));

        MortgagePayoff currentPayoff = MortgagePayoffCalculator.payoff(mortgage, AS_OF);

        FinancialSnapshot snapshot = aSnapshot().withAsOf(AS_OF).withHousing(mortgage).build();
        PayoffSchedule schedule = PayoffScheduleCalculator.schedule(snapshot);

        assertThat(currentPayoff).isInstanceOf(MortgagePayoff.PaysOff.class);
        assertThat(((MortgagePayoff.PaysOff) currentPayoff).month())
                .isEqualTo(schedule.mortgagePayoffMonth().orElseThrow());
    }
}
