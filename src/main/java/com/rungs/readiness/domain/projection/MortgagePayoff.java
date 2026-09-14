package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Money;
import java.time.YearMonth;
import java.util.Objects;

/**
 * The result of amortizing a mortgage at its current payment alone, with no extra payments, computed by
 * {@link MortgagePayoffCalculator}.
 */
public sealed interface MortgagePayoff {

    /** The payment doesn't exceed the first month's interest, so the loan never amortizes at all. */
    record NotCovered(Money principalAndInterest, Money firstMonthInterest) implements MortgagePayoff {

        /** Validates the components; throws NullPointerException naming the component. */
        public NotCovered {
            Objects.requireNonNull(principalAndInterest, "principalAndInterest");
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
