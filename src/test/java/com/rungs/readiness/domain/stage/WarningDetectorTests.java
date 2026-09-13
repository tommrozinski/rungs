package com.rungs.readiness.domain.stage;

import static com.rungs.readiness.domain.model.FinancialSnapshotBuilder.aSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.Severity;
import com.rungs.readiness.domain.model.Warning;
import com.rungs.readiness.domain.model.WarningCode;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 012 REQ-003: each condition, its verbatim message, and the fixed code order when several fire together. */
class WarningDetectorTests {

    @Test
    void noConditionsMeansAnEmptyList() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(4000))
                .withMonthlyEssentialExpenses(Money.ofDollars(3000))
                .build();

        assertThat(WarningDetector.detect(snapshot)).isEmpty();
    }

    @Test
    void expensesExceedIncomeWhenSurplusIsNegative() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(3000))
                .withMonthlyEssentialExpenses(Money.ofDollars(3200))
                .build();

        List<Warning> warnings = WarningDetector.detect(snapshot);

        assertThat(warnings).containsExactly(new Warning(WarningCode.EXPENSES_EXCEED_INCOME, Severity.CAUTION,
                "Your essential expenses and minimum payments are $200.00 more than your take-home pay each month.",
                List.of()));
    }

    @Test
    void aSurplusOfExactlyZeroIsNotAWarning() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(3000))
                .withMonthlyEssentialExpenses(Money.ofDollars(3000))
                .build();

        assertThat(WarningDetector.detect(snapshot)).isEmpty();
    }

    @Test
    void oneNegativelyAmortizingDebtNamesItInTheSingularMessage() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(5000))
                // Interest: 10000 * (24 / 1200) = 200.00; minimum 100 doesn't cover it.
                .withDebt("Card", DebtType.CREDIT_CARD, Money.ofDollars(10000), "24", Money.ofDollars(100))
                .build();

        List<Warning> warnings = WarningDetector.detect(snapshot);

        assertThat(warnings).containsExactly(new Warning(WarningCode.NEGATIVE_AMORTIZATION, Severity.CAUTION,
                "The minimum payment on Card doesn't cover its monthly interest, so the balance grows.",
                List.of("debt-1")));
    }

    @Test
    void aMinimumPaymentExactlyEqualToInterestIsNotAWarning() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(5000))
                // Interest: 10000 * (12 / 1200) = 100.00 exactly; minimum equals it.
                .withDebt("Card", DebtType.CREDIT_CARD, Money.ofDollars(10000), "12", Money.ofDollars(100))
                .build();

        assertThat(WarningDetector.detect(snapshot)).isEmpty();
    }

    @Test
    void twoNegativelyAmortizingDebtsAreJoinedWithAnd() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(5000))
                .withDebt("Card", DebtType.CREDIT_CARD, Money.ofDollars(10000), "24", Money.ofDollars(100))
                .withDebt("Loan", DebtType.PERSONAL, Money.ofDollars(5000), "24", Money.ofDollars(50))
                .build();

        List<Warning> warnings = WarningDetector.detect(snapshot);

        assertThat(warnings).containsExactly(new Warning(WarningCode.NEGATIVE_AMORTIZATION, Severity.CAUTION,
                "The minimum payments on Card and Loan don't cover their monthly interest, so those balances grow.",
                List.of("debt-1", "debt-2")));
    }

    @Test
    void threeOrMoreNegativelyAmortizingDebtsUseAnOxfordComma() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(5000))
                .withDebt("Card", DebtType.CREDIT_CARD, Money.ofDollars(10000), "24", Money.ofDollars(100))
                .withDebt("Loan", DebtType.PERSONAL, Money.ofDollars(5000), "24", Money.ofDollars(50))
                .withDebt("Bill", DebtType.MEDICAL, Money.ofDollars(2000), "24", Money.ofDollars(20))
                .build();

        List<Warning> warnings = WarningDetector.detect(snapshot);

        assertThat(warnings).containsExactly(new Warning(WarningCode.NEGATIVE_AMORTIZATION, Severity.CAUTION,
                "The minimum payments on Card, Loan, and Bill don't cover their monthly interest, "
                        + "so those balances grow.",
                List.of("debt-1", "debt-2", "debt-3")));
    }

    @Test
    void aPayableDebtDoesNotJoinTheNegativeAmortizationWarning() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(5000))
                .withDebt("Card", DebtType.CREDIT_CARD, Money.ofDollars(10000), "24", Money.ofDollars(100))
                .withDebt("Auto", DebtType.AUTO, Money.ofDollars(5000), "6", Money.ofDollars(500))
                .build();

        List<Warning> warnings = WarningDetector.detect(snapshot);

        assertThat(warnings).containsExactly(new Warning(WarningCode.NEGATIVE_AMORTIZATION, Severity.CAUTION,
                "The minimum payment on Card doesn't cover its monthly interest, so the balance grows.",
                List.of("debt-1")));
    }

    @Test
    void emergencyFundTargetCappedWhenMonthsExceedSix() {
        FinancialSnapshot snapshot = aSnapshot().withEmergencyFundMonths(9).build();

        List<Warning> warnings = WarningDetector.detect(snapshot);

        assertThat(warnings).containsExactly(new Warning(WarningCode.EMERGENCY_FUND_TARGET_CAPPED, Severity.CAUTION,
                "Emergency fund targets are capped at 6 months, so your target uses 6 months instead of 9.",
                List.of()));
    }

    @Test
    void exactlySixEmergencyFundMonthsIsNotAWarning() {
        FinancialSnapshot snapshot = aSnapshot().withEmergencyFundMonths(6).build();

        assertThat(WarningDetector.detect(snapshot)).isEmpty();
    }

    @Test
    void allThreeConditionsTogetherAreReturnedInCodeOrder() {
        FinancialSnapshot snapshot = aSnapshot()
                .withMonthlyTakeHomeIncome(Money.ofDollars(3000))
                .withMonthlyEssentialExpenses(Money.ofDollars(3200))
                .withDebt("Card", DebtType.CREDIT_CARD, Money.ofDollars(10000), "24", Money.ofDollars(100))
                .withEmergencyFundMonths(9)
                .build();

        List<Warning> warnings = WarningDetector.detect(snapshot);

        assertThat(warnings).extracting(Warning::code).containsExactly(
                WarningCode.EXPENSES_EXCEED_INCOME, WarningCode.NEGATIVE_AMORTIZATION,
                WarningCode.EMERGENCY_FUND_TARGET_CAPPED);
    }
}
