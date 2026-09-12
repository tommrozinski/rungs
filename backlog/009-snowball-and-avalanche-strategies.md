---
status: proposed
---

# Snowball and avalanche payoff strategies

The two debt-ordering strategies, with a documented and tested tie-breaking rule.

## Description

FR-2. `SNOWBALL` targets the smallest balance first; `AVALANCHE` targets the
highest rate first. The PRD is emphatic that ties are broken by the order the
debts appear in the request, and that this must be documented and tested — which
is why ordering is its own story rather than a detail buried in the simulation.

Request order as tie-break also makes the strategies deterministic (NFR-4), so
this story is what lets the golden files in story 010 stay stable.

## Acceptance Criteria

- A strategy interface selects the next target debt from the set of unretired
  non-mortgage debts.
- `SNOWBALL` selects the smallest current balance.
- `AVALANCHE` selects the highest `annualInterestRate`.
- Ties under either strategy are broken by the debt's index in the request list.
- The tie-break rule is documented in the OpenAPI description and in a test whose
  name states the rule.
- Re-selection happens each month against *current* balances, so snowball
  ordering can shift as balances change.
- `SNOWBALL` is the default when no strategy is supplied.
- AC-2.2 holds: for identical inputs, avalanche's `totalInterestPaid` is less
  than or equal to snowball's.
- A single-debt snapshot produces identical schedules under both strategies.

## Size

S

## Open Questions

- Does snowball re-evaluate "smallest balance" every month against current
  balances, or lock the order once at month zero? These diverge when a large
  high-minimum debt amortizes past a smaller one.
- Are negative-amortizing debts (excluded from the completion estimate per
  AC-2.3) still eligible to be selected as a target?
