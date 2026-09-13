---
status: approved
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
- The order is fixed once at month zero from starting balances and rates, and
  does not change as balances shrink (decided 2026-09-13).
- `SNOWBALL` is the default when no strategy is supplied.
- AC-2.2 holds: for identical inputs, avalanche's `totalInterestPaid` is less
  than or equal to snowball's.
- A single-debt snapshot produces identical schedules under both strategies.

## Size

S

## Open Questions

- ~~Re-evaluate monthly or lock at month zero?~~ **Resolved.** Locked at month
  zero.
- ~~Are negative-amortizing debts eligible targets?~~ **Resolved.** Yes; they are
  excluded from the completion estimate only if they never amortize (spec 008).

Full requirements: [spec/009-snowball-and-avalanche-strategies/requirements.md](../spec/009-snowball-and-avalanche-strategies/requirements.md).
