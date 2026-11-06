# Requirements Specification

<!-- Feature name: 014-property-based-invariant-suite -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 014) -->
<!-- Link: backlog/014-property-based-invariant-suite.md -->
<!-- Branch: main -->

## Feature Overview

A jqwik property-based test suite that checks the engine's invariants across thousands of
generated households, not just hand-picked examples. Golden files prove specific
scenarios; properties prove the shape of the whole input space, which is what makes the
PRD's "correct to the cent, and provably so" (G5) more than a slogan, and is the cheapest
way to find cascade and clamping bugs in the payoff simulation. The story also builds the
reusable snapshot generator that the CSV round-trip property (story 023) extends.

## User Stories

### Story 1: Invariants across all inputs
**As** the engineering reviewer
**I want** the arithmetic invariants asserted on generated inputs
**So that** correctness does not depend on which examples someone thought of

**Acceptance Criteria:**
- [ ] The PRD §9 invariants and the stage-ladder invariant each run as a jqwik property.
- [ ] A failing case shrinks to a minimal household and prints its seed.

### Story 2: A generator others can extend
**As** the author of the CSV round-trip property (story 023)
**I want** a reusable generator of valid, normalized snapshots
**So that** I can add adversarial labels without rebuilding generation

**Acceptance Criteria:**
- [ ] `SnapshotArbitraries` produces valid normalized snapshots.
- [ ] Label generation is a separate, replaceable arbitrary.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `net.jqwik:jqwik` is a test dependency and runs under `./mvnw verify` with
  the JUnit Platform.
- **REQ-002**: `SnapshotArbitraries` (test sources) generates normalized snapshots: 0–50
  debts with positional ids; balances from $0.01 to $500,000; rates 0–36% with up to 3
  decimals, including 0; minimums $0–$5,000; all debt types; optional mortgage; all flag
  combinations; `emergencyFundMonths` 3–9; money fields $0–$50,000 per month.
- **REQ-003**: Properties:
  1. **Accounting**, every schedule including truncated ones: for each debt, total
     payments = total interest + (starting balance − final balance).
  2. **Full payoff**, schedules that are not truncated and have no unpayable debts: total
     principal paid equals the sum of starting balances.
  3. **No negative balance** in any period.
  4. **Avalanche never costs more**: when neither plan is truncated, avalanche
     `totalInterestPaid` ≤ snowball's (AC-2.2).
  5. **Horizon bound**: schedule length ≤ `horizonMonths` (AC-2.5).
  6. **Current stage**: equals the lowest stage neither `SATISFIED` nor `SKIPPED`, or 7.
  7. **Evaluators never throw** and always return results satisfying the story 004
     invariants.
  8. **Money round trip**: `Money.parse(m.toBareString())` equals `m`.
- **REQ-004**: Seed policy: random seed each run; jqwik prints the failing seed and stores
  it in its local database (`.jqwik-database`, git-ignored) so the next run replays it.
  A regression found by a property is also pinned as an example test.
- **REQ-005**: Simulation properties use horizons of 1–120 months and at most 1,000 tries,
  keeping the whole suite under 60 seconds locally.

### Should Have (P1)
- **REQ-006**: Statistics (`Statistics.collect`) report the distribution of debt counts and
  truncated plans, so generator coverage is visible.

### Nice to Have (P2)
- **REQ-007**: An optional Maven profile `-Pproperties-long` raises tries tenfold for
  occasional deep runs.

## Non-Functional Requirements

### Performance
- Default suite completes in under 60 seconds on a developer laptop.

### Security
- Generated data only; no real household data in tests or fixtures.

### Accessibility / Browser Support
- Not applicable.

## Constraints and Assumptions

### Technical Constraints
- Test sources only; `domain` main code gains no test-only hooks.
- `.jqwik-database` is added to `.gitignore`.

### Assumptions
- If property 4 finds a cent-level rounding counterexample, it is investigated as a
  defect first; any tolerance would be a documented PRD change, not a silent test edit.

## Out of Scope
- CSV round-trip property and adversarial labels — story 023.
- Golden-file examples — stories 008, 010.

## Success Metrics
- All eight properties pass at default tries.
- A deliberately injected cascade bug is caught by property 1 or 2 during development.
- Suite runtime under 60 seconds locally and in CI.
