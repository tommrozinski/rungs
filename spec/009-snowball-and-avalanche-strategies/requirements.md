# Requirements Specification

<!-- Feature name: 009-snowball-and-avalanche-strategies -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 009) -->
<!-- Link: backlog/009-snowball-and-avalanche-strategies.md -->
<!-- Branch: main -->

## Feature Overview

The two debt-ordering strategies the payoff simulation (story 008) uses to decide which
debt receives extra payments: `SNOWBALL` (smallest starting balance first) and
`AVALANCHE` (highest interest rate first). The order is fixed once, at month zero, and
ties are broken by each debt's position in the list as supplied — a rule the PRD requires
to be documented and tested, and the one that keeps golden files deterministic (NFR-4).
Comparing the two orderings in dollars and months is goal G4; this story supplies the
orderings, and story 013 compares them.

## User Stories

### Story 1: Choose a payoff order
**As** a household comparing payoff approaches
**I want** to run the plan smallest-balance-first or highest-rate-first
**So that** I can see what each approach costs

**Acceptance Criteria:**
- [ ] `SNOWBALL` orders debts by ascending starting balance.
- [ ] `AVALANCHE` orders debts by descending annual interest rate.
- [ ] The order is computed once from the debts at month zero and never changes.

### Story 2: Deterministic ties
**As** the golden-file tests and the reviewer
**I want** ties broken by a single documented rule
**So that** identical inputs always produce identical schedules

**Acceptance Criteria:**
- [ ] Equal balances (snowball) or equal rates (avalanche) keep their supplied order.
- [ ] A test named for the rule proves it for each strategy.
- [ ] A single-debt snapshot produces identical schedules under both strategies.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `PayoffOrdering` interface in `com.rungs.readiness.domain.projection` with
  `List<Debt> order(List<Debt> debts)`, returning every debt exactly once in payoff
  sequence.
- **REQ-002**: `Strategy` enum `SNOWBALL`, `AVALANCHE`, each providing its
  `PayoffOrdering`.
- **REQ-003**: Snowball ordering: ascending `balance`; ties by ascending supplied index.
- **REQ-004**: Avalanche ordering: descending `annualInterestRate` (compared numerically,
  so `7.25` equals `7.250`); ties by ascending supplied index. Balance is not a
  tie-breaker.
- **REQ-005**: The ordering is applied once, before month 1, from starting values; the
  simulator walks that fixed sequence every month, skipping retired debts.
- **REQ-006**: Negative-amortizing debts are included in the ordering like any other debt
  (decided 2026-09-13).
- **REQ-007**: Orderings do not mutate or reorder the input list; the supplied order
  remains the tie-break reference.

### Should Have (P1)
- **REQ-008**: Tests named for the rule, e.g.
  `snowballBreaksBalanceTiesBySuppliedOrder` and `avalancheBreaksRateTiesBySuppliedOrder`.
- **REQ-009**: A domain test runs the three-debt AC-2.1 scenario under both strategies and
  asserts avalanche `totalInterestPaid` ≤ snowball (AC-2.2).

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- Ordering is a single O(n log n) sort of at most 50 debts, done once per simulation.

### Security
- Not applicable beyond PRD NFR-2: no logging of debt values.

### Accessibility / Browser Support
- Not applicable.

## Constraints and Assumptions

### Technical Constraints
- Pure domain code (PRD AC-A, AC-F).
- Tie-break by supplied order is mandated by PRD FR-2 and must appear in the OpenAPI
  `strategy` description (story 016).

### Assumptions
- The default strategy (`SNOWBALL` when omitted) is applied by the request layer in story
  010, not by the domain.
- Debt list order is preserved from the request through normalization (story 003,
  REQ-010), so supplied index is meaningful.

## Out of Scope
- The monthly payoff mechanics — story 008.
- Request defaults and JSON — story 010.
- The side-by-side comparison and delta — story 013.
- Proving AC-2.2 across generated inputs — story 014.

## Success Metrics
- Tie-break tests for both strategies, including three-way ties.
- A test where snowball's fixed order differs from what monthly re-selection would pick,
  proving the order does not change mid-schedule.
- AC-2.2 holds on the three-debt scenario; single-debt schedules are identical.
- 100% line and branch coverage on the orderings.
