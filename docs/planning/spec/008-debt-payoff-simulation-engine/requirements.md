# Requirements Specification

<!-- Feature name: 008-debt-payoff-simulation-engine -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 008) -->
<!-- Link: backlog/008-debt-payoff-simulation-engine.md -->
<!-- Branch: main -->

## Feature Overview

The month-by-month debt payoff simulation (PRD FR-2 mechanics), as pure domain logic in
`domain.projection`. Given the household's debts, monthly surplus, an extra monthly
payment, a debt ordering, and a horizon, it accrues interest, pays minimums, directs the
remaining money at the target debt, cascades any overpayment within the month, rolls
retired debts' minimums forward, and stops when every debt is paid or the horizon is
reached. It is the most arithmetic-dense part of the engine and the core of the PRD's
"correct to the cent, and provably so" goal (G5). The mortgage is not simulated, a
negative surplus is treated as zero, and stage progress is not projected — all decided
2026-09-13. Debt ordering is story 009; the HTTP endpoint is story 010.

## User Stories

### Story 1: A payoff schedule correct to the cent
**As** a household deciding whether extra payments are worth it
**I want** a month-by-month schedule of what each debt is paid and charged
**So that** I can see exactly when each debt disappears and what interest it costs

**Acceptance Criteria:**
- [ ] Each month lists, per debt: opening balance, interest, payment, closing balance.
- [ ] Each month lists the debts retired that month.
- [ ] A hand-computed three-debt scenario matches to the cent (AC-2.1).

### Story 2: The mechanics the PRD specifies
**As** the engineering reviewer
**I want** every payoff rule implemented exactly and tested in isolation
**So that** the schedule can be trusted as documentation

**Acceptance Criteria:**
- [ ] Interest accrues on the opening balance before payments, rounded half-even.
- [ ] Minimums apply to every unretired debt; surplus plus extra goes to the target.
- [ ] A retired debt's minimum joins the pool from the following month.
- [ ] Overpayment is clamped and the remainder cascades to the next target that month.

### Story 3: Always terminates, never lies
**As** a client requesting a projection
**I want** the simulation to stop at the horizon and say so
**So that** impossible plans return a bounded, honest answer

**Acceptance Criteria:**
- [ ] No schedule exceeds `horizonMonths` entries; reaching it sets `truncated` (AC-2.5).
- [ ] A debt whose minimum never covers its interest does not loop and is excluded from
  the completion estimate (AC-2.3).
- [ ] Zero surplus and zero extra still produce a valid, minimums-only schedule (AC-2.4).

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `PayoffSimulator.simulate(SimulationInput)` in
  `com.rungs.readiness.domain.projection` returns a `PayoffSimulation`. `SimulationInput`
  holds `debts` (`List<Debt>`, in supplied order), `monthlySurplus` (`Money`),
  `extraMonthlyPayment` (`Money`, ≥ 0), `ordering` (`PayoffOrdering`, story 009), and
  `horizonMonths` (1–600).
- **REQ-002**: Each simulated month, in this order:
  1. For each unretired debt, interest = opening balance × `annualInterestRate` ÷ 1200,
     computed with `MathContext.DECIMAL128` and rounded once, half-even, to the cent;
     added to the balance.
  2. Each unretired debt is paid `min(minimumPayment, balance)`.
  3. The pool for the month is `max(monthlySurplus, $0)` + `extraMonthlyPayment` + the
     minimums of debts retired in earlier months + any unused part of this month's
     minimums (a minimum larger than the remaining balance).
  4. The pool is applied to unretired debts in the ordering's sequence; each receives
     `min(pool, balance)` and the remainder cascades to the next debt in the same month.
  5. Debts whose balance reaches $0 are retired this month.
- **REQ-003**: A negative `monthlySurplus` is treated as $0 (PRD FR-2).
- **REQ-004**: The simulation stops after the month in which the last debt is retired, or
  after `horizonMonths` months; if any debt remains unretired at the horizon, `truncated`
  is true.
- **REQ-005**: `PayoffSimulation` carries `schedule` (`List<MonthlyPeriod>`),
  `monthsToDebtFreedom` (`Integer`, nullable), `totalInterestPaid` (`Money`), `truncated`
  (`boolean`), and `unpayableDebtIds` (`List<String>`).
- **REQ-006**: `MonthlyPeriod` carries `month` (1-based), `lines` (one `DebtLine` per debt
  unretired at the start of the month: `debtId`, `openingBalance`, `interest`, `payment`,
  `closingBalance`), and `retiredDebtIds`.
- **REQ-007**: `unpayableDebtIds` lists debts still unretired at the horizon whose closing
  balance is at or above their starting balance — they never amortized (AC-2.3).
  `monthsToDebtFreedom` is the month the last *payable* debt was retired, ignoring
  unpayable debts; it is null if any payable debt is still unretired at the horizon, and
  0 when there are no debts.
- **REQ-008**: `totalInterestPaid` is the sum of all interest in the schedule. The mortgage
  is never part of the simulation.
- **REQ-009**: Accounting invariant, per debt: total payments = total interest + (starting
  balance − final balance). No balance is ever negative.
- **REQ-010**: No wall-clock, random, or environment read (PRD NFR-4); identical input
  yields an identical `PayoffSimulation`.

### Should Have (P1)
- **REQ-011**: The AC-2.1 expected schedule is a committed CSV fixture
  (`src/test/resources/golden/payoff-three-debts-snowball.csv`) with one row per month and
  debt, hand-computed and reviewed like code.
- **REQ-012**: A `FixedOrder` test ordering (test sources) so the simulator is tested
  independently of story 009's strategies.

### Nice to Have (P2)
- **REQ-013**: `PayoffSimulation.monthRetired(String debtId)` returns the month a debt was
  retired, or empty.

## Non-Functional Requirements

### Performance
- A 360-month simulation of 50 debts completes well within the 250ms p95 budget for the
  projection endpoint (PRD NFR-3); money arithmetic stays on `long` except the single
  `BigDecimal` interest step per debt per month.

### Security
- Pure computation on in-memory values; no logging of balances (PRD NFR-2).

### Accessibility / Browser Support
- Not applicable.

## Constraints and Assumptions

### Technical Constraints
- Pure domain code (PRD AC-A, AC-B, AC-F); uses `Money` (002) and `Debt` (003).
- Rates are percentages (`7.25` = 7.25%), hence the ÷ 1200.

### Assumptions
- The caller passes debts already normalized (zero-balance debts removed, no mortgage
  type) and a validated horizon.
- Interest-first ordering, negative-surplus handling, and mortgage exclusion follow PRD
  FR-2 as amended 2026-09-13.

## Out of Scope
- `SNOWBALL` and `AVALANCHE` orderings and the tie-break rule — story 009.
- `POST /v1/projections`, defaults, `monthsToNextStage`, and JSON — story 010.
- The `NEGATIVE_AMORTIZATION` warning — story 012.
- Property-based invariants across generated inputs — story 014.
- Projecting savings, retirement, or stage progress — not modeled.

## Success Metrics
- The AC-2.1 fixture matches to the cent.
- Dedicated tests for each REQ-002 step: interest rounding at a half cent, a minimum above
  the remaining balance, a cascade across two debts in one month, and a freed minimum
  appearing the following month.
- AC-2.3, AC-2.4, and AC-2.5 each have a named test; a negative-amortizing debt at a 600
  horizon terminates.
- REQ-009's accounting invariant is asserted on every test schedule.
- 100% line and branch coverage on `domain.projection`.
