---
status: proposed
---

# Property-based invariant test suite

A jqwik suite asserting the arithmetic invariants that must hold for every
possible snapshot, not just the hand-picked ones.

## Description

PRD section 9 names the invariants that hold across all inputs:

- total principal paid equals the sum of starting balances
- no balance ever goes negative
- avalanche interest is never greater than snowball interest for the same inputs
- schedule length never exceeds the horizon

Golden files prove specific scenarios; properties prove the shape of the whole
space, and they are what turns G5's "provably so" into something more than a
slogan. This is also the cheapest place to find the cascade and clamping bugs
that story 008's mechanics invite.

This story builds the generator infrastructure as well as the projection
properties. The CSV round-trip property (AC-7.1) reuses that generator and lives
in story 023, because it needs labels containing commas, quotes, and unicode.

## Acceptance Criteria

- jqwik is wired into the build and runs in CI.
- A reusable generator produces arbitrary valid `FinancialSnapshot`s: varied debt
  counts (0–50), balances, rates, minimums, and household flags.
- The generator is extensible with adversarial label content for story 023.
- Property: total principal paid equals the sum of starting balances for every
  completed schedule.
- Property: no debt balance is ever negative in any period.
- Property: avalanche `totalInterestPaid` is never greater than snowball's for
  the same inputs.
- Property: schedule length never exceeds `horizonMonths`.
- Property: the current stage is always the lowest stage that is neither
  satisfied nor skipped.
- Failing cases shrink to a minimal reproducer and are reported readably.
- The suite runs in reasonable time for local development.

## Size

M

## Open Questions

- Do the principal and interest invariants need to exclude negative-amortizing
  debts (which never complete) and truncated schedules?
- What seed policy — fixed for reproducibility in CI, or random with the failing
  seed reported?
