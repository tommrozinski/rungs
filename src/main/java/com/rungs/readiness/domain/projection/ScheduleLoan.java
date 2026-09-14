package com.rungs.readiness.domain.projection;

import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * One loan (a debt or the mortgage) as it enters a {@link PayoffSchedule}, in payoff order (design.md 040 section 3
 * for the month-by-month pool mechanics; design.md 041 section 1 for the snowball order: every debt smallest
 * starting balance first, ties in supplied order, then the mortgage last).
 *
 * @param name the loan's label; "Mortgage" for the home loan
 * @param startingBalance the balance owed as of the schedule's first month
 * @param annualRate the annual interest rate as a percentage ({@code 6.125} means 6.125%)
 * @param minimumPayment the minimum monthly payment; the mortgage's is its full payment
 * @param mortgage whether this loan is the household's mortgage rather than a debt
 */
public record ScheduleLoan(String name, Money startingBalance, BigDecimal annualRate, Money minimumPayment, boolean mortgage) {

    /** Validates every component; throws NullPointerException naming the component. */
    public ScheduleLoan {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(startingBalance, "startingBalance");
        Objects.requireNonNull(annualRate, "annualRate");
        Objects.requireNonNull(minimumPayment, "minimumPayment");
    }
}
