---
status: approved
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

- ~~Does the mortgage participate?~~ **Resolved.** Excluded from the schedule and
  from `totalInterestPaid`.
- ~~Interest before or after payments?~~ **Resolved.** Interest accrues on the
  opening balance first, then payments apply (PRD FR-2).
- ~~Negative surplus?~~ **Resolved.** Treated as $0: minimums plus
  `extraMonthlyPayment` only.
- ~~`MonthlyPeriod.stageReached`?~~ **Resolved.** Removed. `monthsToNextStage` is
  populated only when the current stage is 2.

Full requirements: [spec/008-debt-payoff-simulation-engine/requirements.md](../spec/008-debt-payoff-simulation-engine/requirements.md).
