package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Money;
import java.time.YearMonth;
import java.util.Objects;

/**
 * The result of amortizing a mortgage at its current payment alone, with no extra payments, computed by
 * {@link MortgagePayoffCalculator}.
 */
public sealed interface MortgagePayoff {

    /**
     * The payment doesn't exceed the first month's interest, so the loan never amortizes at all.
     *
     * @param payment the loan's full monthly payment, taxes and insurance included (see {@link
     *     com.rungs.readiness.domain.model.Mortgage#monthlyPayment}); not principal and interest alone
     */
    record NotCovered(Money payment, Money firstMonthInterest) implements MortgagePayoff {

        /** Validates the components; throws NullPointerException naming the component. */
        public NotCovered {
            Objects.requireNonNull(payment, "payment");
            Objects.requireNonNull(firstMonthInterest, "firstMonthInterest");
        }
    }

    /** The payment covers the interest, but the loan still hasn't reached zero within 600 months. */
    record Capped() implements MortgagePayoff {
    }

    /** The loan reaches zero after {@code months} payments, the last one made in {@code month}. */
    record PaysOff(int months, YearMonth month) implements MortgagePayoff {

        /** Validates the components; throws NullPointerException naming the component. */
        public PaysOff {
            Objects.requireNonNull(month, "month");
        }
    }
}
