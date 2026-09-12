---
status: proposed
---

# Debt payoff simulation engine

The month-by-month amortization core: accrue interest, apply minimums, apply
surplus to the target debt, roll retired payments forward, and stop at the
horizon.

## Description

FR-2's payoff mechanics, as pure domain logic with no HTTP concerns. This is the
most arithmetic-dense story in the backlog and the one the PRD's "correct to the
cent, and provably so" goal (G5) is really about.

The mechanics, from the PRD:

- Interest accrues monthly at `annualInterestRate / 12` on the opening balance,
  rounded half-even to the cent.
- Minimum payments apply to every debt. Surplus plus `extraMonthlyPayment` goes
  to the target debt.
- A retired debt's minimum payment rolls into surplus from the following month.
- Overpayment is clamped: a final payment never exceeds the payoff amount, and
  the remainder cascades to the next target **in the same month**.
- A debt whose minimum does not cover monthly interest is excluded from the
  completion estimate rather than looping forever (AC-2.3).

Debt ordering is story 009, behind a small interface, so this story can be tested
against a fixed ordering.

## Acceptance Criteria

- A simulation produces a `List<MonthlyPeriod>`, each carrying opening balances,
  per-debt payments applied, interest accrued, debts retired this month, and the
  stage reached.
- Interest is `annualInterestRate / 12` on the opening balance, half-even to the
  cent.
- A retired debt's minimum payment is available as surplus from the *following*
  month, not the same month.
- A final payment is clamped to the payoff amount and the remainder cascades to
  the next target within the same month.
- AC-2.3: a negative-amortizing debt does not cause an infinite loop and is
  excluded from the completion estimate.
- AC-2.4: zero surplus and zero extra payment still produces a valid schedule
  driven by minimums alone.
- AC-2.5: no schedule exceeds `horizonMonths` entries; hitting the horizon sets
  `truncated`.
- Invariants hold: no balance goes negative; total principal paid equals the sum
  of starting balances.
- Mortgage debt is excluded from `monthsToDebtFreedom` (non-mortgage only).
- A hand-computed golden file for a three-debt scenario matches to the cent
  (AC-2.1).
- No wall-clock read anywhere in the simulation (NFR-4).

## Size

L

## Open Questions

- Does a mortgage participate in the schedule at all (accruing interest, showing
  balances) while being excluded from `monthsToDebtFreedom`, or is it omitted
  entirely?
- Is interest applied before or after the month's payments? "Applied to the
  opening balance" implies before, but this needs pinning explicitly since it
  moves every figure in the golden file.
- When surplus is negative (expenses exceed income), is it clamped to zero or
  does it erode balances upward?
- `MonthlyPeriod.stageReached` — is the full ladder re-evaluated each simulated
  month, and against what projected emergency-fund and retirement values? The
  simulation models debt balances only, so stages 1, 3, and 4 have no projected
  inputs. This also drives `monthsToNextStage` in story 010 and the schedule
  table in story 031.
