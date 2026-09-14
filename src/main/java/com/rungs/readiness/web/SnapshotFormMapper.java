package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.Debt;
import com.rungs.readiness.domain.model.DebtType;
import com.rungs.readiness.domain.model.FinancialSnapshot;
import com.rungs.readiness.domain.model.Housing;
import com.rungs.readiness.domain.model.HouseholdFlags;
import com.rungs.readiness.domain.model.Money;
import com.rungs.readiness.domain.model.MoneyFormatException;
import com.rungs.readiness.domain.model.Mortgage;
import com.rungs.readiness.domain.model.Rent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Converts the raw {@link SnapshotForm} the browser posts into a {@link FinancialSnapshot}, leniently: a blank or
 * unparseable amount or percent becomes zero, a negative one becomes zero, percentages are clamped to 0-100 and
 * rounded to the scale the domain requires (rather than rejected for having too many decimal places), a debt row
 * with a zero balance is dropped, a blank label becomes "Debt N" (the row's 1-based position as submitted), and an
 * unrecognized debt type becomes {@link DebtType#OTHER}. Only the first {@value #MAX_DEBT_ROWS} non-zero-balance
 * rows, in submitted order, become debts; any further rows are dropped, keeping the payoff schedule's rendered size
 * bounded. A blank or unrecognized {@code housing} becomes {@code MORTGAGE}; whichever mode is not selected has its
 * fields ignored. The form has no maturity fields, matching {@link Mortgage}, which has no maturity to set.
 * Nothing here rejects input or throws; every bad case resolves to a plain default instead, so a submission can
 * never 500.
 *
 * <p>The form collects no college contribution amount, so the snapshot always has a $0 monthly college
 * contribution; it also has no field for the emergency-fund target's month count, so the snapshot always targets
 * the domain's minimum of 3 months. {@code asOf} is supplied by the caller (the controller, from its injected
 * {@link java.time.Clock}), not decided here.
 */
final class SnapshotFormMapper {

    /** The form has no field for this; the snapshot always targets the domain's minimum, a 3-month fund. */
    private static final int EMERGENCY_FUND_MONTHS = 3;

    /**
     * Only the first this many non-zero-balance debt rows become debts; later rows are dropped, keeping a huge
     * submission (e.g. hundreds of rows) from rendering a page-crushing payoff schedule. Also
     * {@link SnapshotController}'s cap on rows the form itself will grow to, so "Add a debt" and the binder agree.
     */
    static final int MAX_DEBT_ROWS = 20;

    /** Decimal places {@link FinancialSnapshot#retirementContributionPercent()} is normalized to. */
    private static final int RETIREMENT_PERCENT_SCALE = 2;

    /** Decimal places {@link Debt#annualInterestRate()} and {@link Mortgage#annualInterestRate()} normalize to. */
    private static final int INTEREST_RATE_SCALE = 3;

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    /**
     * Every parsed money amount is capped at this value so an enormous submitted amount can never reach the
     * domain and overflow its arithmetic; larger amounts are capped, not rejected.
     */
    private static final Money MAX_MONEY = Money.ofDollars(1_000_000_000);

    /** {@link #MAX_MONEY} expressed as dollars, for capping before a {@link BigDecimal} amount is even in cents. */
    private static final BigDecimal MAX_MONEY_DOLLARS = BigDecimal.valueOf(1_000_000_000);

    private static final String RENT_MODE = "RENT";

    /**
     * Longest text {@link #parseDecimal} accepts, mirroring {@link Money#parse}'s own cap; this keeps the
     * hostile-but-short case (scientific notation such as {@code "1e-999999999"}) out of {@link BigDecimal}
     * entirely and rejects the pathological case (thousands of digits) before it is even inspected.
     */
    private static final int MAX_TEXT_LENGTH = 32;

    /**
     * A plain decimal number: an optional leading {@code -}, one or more digits, and an optional {@code .} followed
     * by one or more digits. No scientific notation, no {@code $}, no comma grouping, no {@code NaN}/{@code
     * Infinity} — anything else becomes zero before it ever reaches {@link BigDecimal}.
     */
    private static final Pattern PLAIN_DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");

    private SnapshotFormMapper() {
    }

    /** Builds the snapshot the seven-stage ladder assesses from the form as submitted, as of {@code asOf}. */
    static FinancialSnapshot toSnapshot(SnapshotForm form, YearMonth asOf) {
        List<Debt> debts = new ArrayList<>();
        List<SnapshotForm.DebtRow> rows = form.getDebts();
        for (int index = 0; index < rows.size() && debts.size() < MAX_DEBT_ROWS; index++) {
            SnapshotForm.DebtRow row = rows.get(index);
            Money balance = money(row.getBalance());
            if (balance.isZero()) {
                continue;
            }
            String label = row.getLabel() == null || row.getLabel().isBlank()
                    ? "Debt " + (index + 1)
                    : row.getLabel();
            debts.add(new Debt(
                    "debt-" + (debts.size() + 1),
                    label,
                    debtType(row.getType()),
                    balance,
                    percent(row.getAnnualInterestRate(), INTEREST_RATE_SCALE),
                    money(row.getMinimumPayment())));
        }

        return new FinancialSnapshot(
                money(form.getMonthlyTakeHomeIncome()),
                money(form.getMonthlyEssentialExpenses()),
                money(form.getStarterEmergencyFund()),
                money(form.getFullEmergencyFund()),
                Money.ZERO,
                percent(form.getRetirementContributionPercent(), RETIREMENT_PERCENT_SCALE),
                EMERGENCY_FUND_MONTHS,
                debts,
                housing(form),
                new HouseholdFlags(form.isRetired(), form.isIntendsToFundCollege()),
                asOf);
    }

    /**
     * Returns the household's housing: {@link Rent} when {@code housing} is exactly {@code "RENT"}, ignoring the
     * mortgage fields; otherwise a {@link Mortgage} built from the mortgage fields, ignoring the rent field. A
     * blank or unrecognized {@code housing} is treated as the mortgage mode.
     */
    private static Housing housing(SnapshotForm form) {
        if (RENT_MODE.equals(form.getHousing())) {
            return new Rent(money(form.getMonthlyRent()));
        }
        return new Mortgage(
                money(form.getMortgageBalance()),
                percent(form.getMortgageRate(), INTEREST_RATE_SCALE),
                money(form.getMortgagePayment()));
    }

    /**
     * Parses {@code text} as a dollar amount, accepting everything {@link Money#parse} does — including a leading
     * {@code $} and comma-grouped thousands — plus amounts with more than two decimal places, which round half-up
     * to the cent rather than being rejected (the more forgiving of {@link Money#parse}'s two options for those).
     * Blank, unparseable, or negative input becomes zero; an amount above {@link #MAX_MONEY} is capped to it rather
     * than rejected, so nothing here ever throws or reaches the domain able to overflow its arithmetic.
     */
    private static Money money(String text) {
        if (text == null) {
            return Money.ZERO;
        }
        String trimmed = text.strip();
        try {
            Money parsed = Money.parse(trimmed);
            return parsed.isNegative() ? Money.ZERO : parsed.min(MAX_MONEY);
        } catch (MoneyFormatException notMoneyFormat) {
            return moneyFromLenientDecimal(trimmed);
        }
    }

    /** Falls back for amounts {@link Money#parse} rejects only for having more than two decimal places (or being
     * too large for it to accumulate at all); also caps at {@link #MAX_MONEY} rather than reporting overflow. */
    private static Money moneyFromLenientDecimal(String text) {
        BigDecimal amount = parseDecimal(text);
        if (amount.signum() < 0) {
            return Money.ZERO;
        }
        if (amount.compareTo(MAX_MONEY_DOLLARS) > 0) {
            return MAX_MONEY;
        }
        try {
            return Money.ofCents(amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact());
        } catch (ArithmeticException overflow) {
            return Money.ZERO;
        }
    }

    /**
     * Parses {@code text} as a percentage at exactly {@code scale} decimal places; blank or unparseable input
     * becomes zero, a negative value becomes zero, a value above 100 is clamped to 100, and extra decimal places
     * beyond {@code scale} round half-up rather than being rejected.
     */
    private static BigDecimal percent(String text, int scale) {
        BigDecimal value = parseDecimal(text);
        if (value.signum() < 0) {
            value = BigDecimal.ZERO;
        } else if (value.compareTo(ONE_HUNDRED) > 0) {
            value = ONE_HUNDRED;
        }
        try {
            return value.setScale(scale, RoundingMode.HALF_UP);
        } catch (ArithmeticException overflow) {
            return BigDecimal.ZERO.setScale(scale);
        }
    }

    /**
     * Returns {@code text} parsed as a plain decimal, or zero when it is null, blank, longer than
     * {@link #MAX_TEXT_LENGTH}, or anything other than an optionally-negative plain decimal number — which rules
     * out scientific notation (e.g. {@code "1e-999999999"}), {@code NaN}/{@code Infinity}, and oversized digit
     * strings before they ever reach {@link BigDecimal}. Any {@link ArithmeticException} or
     * {@link NumberFormatException} the construction still manages to throw resolves to zero as a last resort.
     */
    private static BigDecimal parseDecimal(String text) {
        if (text == null) {
            return BigDecimal.ZERO;
        }
        String trimmed = text.strip();
        if (trimmed.isEmpty() || trimmed.length() > MAX_TEXT_LENGTH || !PLAIN_DECIMAL.matcher(trimmed).matches()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(trimmed);
        } catch (ArithmeticException | NumberFormatException invalid) {
            return BigDecimal.ZERO;
        }
    }

    /** Returns the matching {@link DebtType}, or {@link DebtType#OTHER} when {@code text} names none. */
    private static DebtType debtType(String text) {
        if (text == null) {
            return DebtType.OTHER;
        }
        try {
            return DebtType.valueOf(text);
        } catch (IllegalArgumentException unknownType) {
            return DebtType.OTHER;
        }
    }
}
