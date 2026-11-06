#!/usr/bin/env python3
"""Independent golden generator for spec 008's AC-2.1 three-debt snowball scenario.

Re-implements requirements.md REQ-002 directly from the requirements text, in Python's
`decimal` module, deliberately not translated line-by-line from the Java
`PayoffSimulator`. The Java test parses the CSV this script writes and compares every
line to the simulation exactly, so an independent implementation is the point: if the
two disagree, the requirements text (not either implementation) is the tiebreaker.

Writes src/test/resources/golden/payoff-three-debts-snowball.csv, one row per month per
debt, header: month,debtId,openingBalance,interest,payment,closingBalance,retired.

`--strategy avalanche` (story 010) runs the same scenario in avalanche order and prints
its months-to-debt-freedom and total interest instead of writing a file, for a reviewer
to cross-check against the committed `projection-three-debts-avalanche.json` golden;
`--strategy snowball` (the default) is unchanged.
"""
import csv
import pathlib
import sys
from decimal import Context, Decimal, ROUND_HALF_EVEN, getcontext

# The ambient precision for every plain arithmetic operation below (addition, subtraction,
# and the balance * factor multiplication). High enough that it never rounds a result on
# its own; the only intentional rounding is the explicit quantize() to the cent.
getcontext().prec = 60

CENT = Decimal("0.01")

# MathContext.DECIMAL128 in the Java implementation: 34 significant decimal digits,
# half-even rounding. REQ-002 step 1 requires the annual-rate-to-monthly-factor division
# to use exactly this.
DECIMAL128 = Context(prec=34, rounding=ROUND_HALF_EVEN)


class Debt:
    """One debt's mutable simulation state."""

    def __init__(self, debt_id, balance, annual_rate_percent, minimum_payment):
        self.debt_id = debt_id
        self.balance = balance
        self.annual_rate_percent = annual_rate_percent
        self.minimum_payment = minimum_payment
        self.retired = False


def monthly_interest(opening_balance, annual_rate_percent):
    """REQ-002 step 1: opening balance * (annual rate / 1200), rounded once, half-even, to the cent."""
    monthly_rate_factor = DECIMAL128.divide(annual_rate_percent, Decimal(1200))
    exact_interest = opening_balance * monthly_rate_factor
    return exact_interest.quantize(CENT, rounding=ROUND_HALF_EVEN)


def simulate(debts, payoff_order, monthly_surplus, extra_monthly_payment, horizon_months):
    """Runs REQ-002 to REQ-004 month by month and returns one row per debt per month."""
    rows = []
    pending_freed_minimum = Decimal("0.00")
    month = 0
    while month < horizon_months and any(not debt.retired for debt in debts):
        month += 1
        opening_balance = {}
        interest = {}
        payment = {}
        unused_minimum_this_month = Decimal("0.00")

        # Steps 1 and 2: accrue interest, then pay the minimum, on every debt unretired at
        # the start of the month.
        for debt in debts:
            if debt.retired:
                continue
            opening_balance[debt.debt_id] = debt.balance
            debt_interest = monthly_interest(debt.balance, debt.annual_rate_percent)
            interest[debt.debt_id] = debt_interest
            debt.balance += debt_interest

            minimum_paid = min(debt.minimum_payment, debt.balance)
            debt.balance -= minimum_paid
            payment[debt.debt_id] = minimum_paid
            unused_minimum_this_month += debt.minimum_payment - minimum_paid

        # Step 3: this month's pool.
        pool = (
            max(monthly_surplus, Decimal("0.00"))
            + extra_monthly_payment
            + pending_freed_minimum
            + unused_minimum_this_month
        )

        # Step 4: cascade the pool through the fixed payoff order.
        for debt in payoff_order:
            if debt.retired or pool <= 0:
                continue
            applied = min(pool, debt.balance)
            debt.balance -= applied
            payment[debt.debt_id] += applied
            pool -= applied

        # Step 5: retire any debt whose balance reached $0; record this month's rows in
        # supplied order.
        newly_freed_minimum = Decimal("0.00")
        for debt in debts:
            if debt.debt_id not in opening_balance:
                continue
            retired_this_month = debt.balance == 0
            rows.append(
                {
                    "month": month,
                    "debtId": debt.debt_id,
                    "openingBalance": opening_balance[debt.debt_id],
                    "interest": interest[debt.debt_id],
                    "payment": payment[debt.debt_id],
                    "closingBalance": debt.balance,
                    "retired": retired_this_month,
                }
            )
            if retired_this_month:
                debt.retired = True
                newly_freed_minimum += debt.minimum_payment
        pending_freed_minimum += newly_freed_minimum

    return rows


def format_money(value):
    return str(value.quantize(CENT, rounding=ROUND_HALF_EVEN))


def main():
    # design.md §3: three debts in supplied order; "Medical bill" maps to the closest
    # existing DebtType, MEDICAL. --strategy avalanche|snowball (default snowball) lets a
    # reviewer cross-check the avalanche scenario (010) against the committed JSON golden
    # without writing a second committed CSV fixture; only snowball is written to disk.
    strategy = "snowball"
    if "--strategy" in sys.argv:
        strategy = sys.argv[sys.argv.index("--strategy") + 1].lower()

    credit_card = Debt("debt-1", Decimal("3500.00"), Decimal("23.99"), Decimal("90.00"))
    medical_bill = Debt("debt-2", Decimal("900.00"), Decimal("0"), Decimal("50.00"))
    car_loan = Debt("debt-3", Decimal("7200.00"), Decimal("6.9"), Decimal("210.00"))
    debts = [credit_card, medical_bill, car_loan]

    if strategy == "avalanche":
        # Descending annual rate, ties keep supplied order: card (23.99) > car (6.9) > medical (0).
        payoff_order = [credit_card, car_loan, medical_bill]
    else:
        # Snowball: ascending starting balance. medical (900) < card (3500) < car (7200).
        payoff_order = [medical_bill, credit_card, car_loan]

    rows = simulate(
        debts,
        payoff_order,
        monthly_surplus=Decimal("300.00"),
        extra_monthly_payment=Decimal("0.00"),
        horizon_months=360,
    )

    total_interest = sum((row["interest"] for row in rows), Decimal("0.00"))
    months_to_debt_freedom = max((row["month"] for row in rows if row["retired"]), default=0)
    print(f"{strategy}: {months_to_debt_freedom} months, {format_money(total_interest)} total interest")

    if strategy != "snowball":
        return

    repo_root = pathlib.Path(__file__).resolve().parents[2]
    output_path = repo_root / "src/test/resources/golden/payoff-three-debts-snowball.csv"
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["month", "debtId", "openingBalance", "interest", "payment", "closingBalance", "retired"])
        for row in rows:
            writer.writerow(
                [
                    row["month"],
                    row["debtId"],
                    format_money(row["openingBalance"]),
                    format_money(row["interest"]),
                    format_money(row["payment"]),
                    format_money(row["closingBalance"]),
                    "true" if row["retired"] else "false",
                ]
            )
    print(f"Wrote {len(rows)} rows to {output_path}")


if __name__ == "__main__":
    main()
