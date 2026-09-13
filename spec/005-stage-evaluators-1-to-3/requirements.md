# Requirements Specification

<!-- Feature name: 005-stage-evaluators-1-to-3 -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 005) -->
<!-- Link: backlog/005-stage-evaluators-1-to-3.md -->
<!-- Branch: main -->

## Feature Overview

The three cash-and-debt stage evaluators: stage 1 (starter emergency fund of $1,000),
stage 2 (all debt eliminated — the debts list never contains a mortgage), and stage 3
(full emergency fund of `emergencyFundMonths` × monthly essential expenses, capped at 6
months). Each implements the `StageEvaluator` contract from story 004 and returns status,
percent complete, remaining amount, and a blocker sentence with real dollar figures. The
blocker for the current stage becomes the assessment screen's verdict line, so the copy is
product copy: second person, sentence case, names the number, no encouragement. These
stages decide AC-1.1 and most households' current stage.

## User Stories

### Story 1: Stage 1 — starter emergency fund
**As** a household with little saved
**I want** to see how far I am from a $1,000 starter fund
**So that** I know the exact amount standing between me and stage 2

**Acceptance Criteria:**
- [ ] Satisfied at exactly $1,000.00 and above.
- [ ] Below $1,000.00: `percentComplete` is fund ÷ $1,000 (rounded down), `remainingAmount`
  is the shortfall, status is `IN_PROGRESS` above $0 and `NOT_STARTED` at $0.
- [ ] Blocker: "Your starter emergency fund is $500.00 short of $1,000.00."

### Story 2: Stage 2 — debt eliminated
**As** a household paying off debt
**I want** to see the total debt and how many debts remain
**So that** the size of stage 2 is concrete

**Acceptance Criteria:**
- [ ] Satisfied when the debts list is empty (zero-balance debts never reach the domain,
  and a mortgage is never in the list).
- [ ] Otherwise `IN_PROGRESS` with `percentComplete` 0.0 (PRD 5.3 exception),
  `remainingAmount` equal to the total balance.
- [ ] Blocker: "You have $12,480.00 of debt across 3 debts." (singular "1 debt").

### Story 3: Stage 3 — full emergency fund
**As** a debt-free household
**I want** to see my emergency fund against my months-of-expenses target
**So that** I know the shortfall in dollars and which target it is measured against

**Acceptance Criteria:**
- [ ] Target is `min(emergencyFundMonths, 6)` × `monthlyEssentialExpenses`.
- [ ] Satisfied when `fullEmergencyFund` ≥ target, including exactly at the target and
  whenever the target is $0.
- [ ] Otherwise `percentComplete` is fund ÷ target (rounded down), `remainingAmount` is the
  shortfall, `IN_PROGRESS` above $0 and `NOT_STARTED` at $0.
- [ ] Blocker: "Your emergency fund is $2,400.00 short of a 3-month target of $10,500.00."

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `StarterFundEvaluator` (stage 1), `DebtFreeEvaluator` (stage 2), and
  `FullEmergencyFundEvaluator` (stage 3) in `com.rungs.readiness.domain.stage`, each
  implementing `StageEvaluator`.
- **REQ-002**: Stage 1 target is a fixed constant of $1,000.00; it is not configurable.
- **REQ-003**: Stage 1 and stage 3 compute `percentComplete` with `Percentages.of` (story
  004): rounded down, scale 1, never 100.0 while unmet.
- **REQ-004**: Stage 2 is satisfied exactly when `debts` is empty; otherwise it is
  `IN_PROGRESS`, `percentComplete` 0.0, `remainingAmount` = `Money.sum` of balances.
- **REQ-005**: Stage 3 caps `emergencyFundMonths` at 6 when computing the target; it does
  not raise the warning (story 012).
- **REQ-006**: A stage 3 target of $0 (zero essential expenses) is satisfied, since any
  fund meets it; `Percentages.of` is never called with a zero whole in that case.
- **REQ-007**: Blocker copy, with amounts from `Money.toDisplayString()`:
  - Stage 1: "Your starter emergency fund is {shortfall} short of $1,000.00."
  - Stage 2: "You have {total} of debt across {n} debt(s)." — "1 debt", "3 debts".
  - Stage 3: "Your emergency fund is {shortfall} short of a {months}-month target of
    {target}." — `{months}` is the capped value.
- **REQ-008**: `NOT_STARTED` rows use the same blocker sentences as `IN_PROGRESS` rows,
  since every ladder row shows a reason.
- **REQ-009**: Satisfied stages carry no blocker and no remaining amount (story 004).

### Should Have (P1)
- **REQ-010**: Blocker templates are private constants in each evaluator, so copy changes
  touch one line and one test.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- Each evaluator is O(number of debts) with no I/O.

### Security
- Blocker text is response content and intentionally contains household figures; it must
  never be logged (PRD NFR-2, story 018).

### Accessibility / Browser Support
- Blockers are complete sentences that read correctly aloud; no symbols stand in for words.

## Constraints and Assumptions

### Technical Constraints
- Pure domain code (PRD AC-A, AC-F). Uses `Money` (story 002), `FinancialSnapshot`
  (story 003), and `StageEvaluation` / `Percentages` (story 004).
- English copy only; internationalization is out of scope (PRD §12).

### Assumptions
- Validation (story 011) guarantees non-negative amounts and `emergencyFundMonths` ≥ 3
  before evaluation.
- Displayed amounts keep cents (`$12,480.00`), consistent with `Money`'s single display
  format.

## Out of Scope
- Stages 4–7 — story 006.
- The `EMERGENCY_FUND_TARGET_CAPPED` warning — story 012.
- Rendering blockers in the verdict block and ladder — stories 026, 029.

## Success Metrics
- One test class per evaluator with boundary cases: $999.99, $1,000.00, and $0 for stage
  1; empty list, one debt, and several debts for stage 2; exactly at target, one cent
  short, $0 fund, zero expenses, and `emergencyFundMonths` of 7 (capped to 6) for stage 3.
- AC-1.1 passes at the domain level: $500 saved, $12,000 credit-card debt, no mortgage →
  current stage 1.
- Every blocker in REQ-007 is asserted verbatim, including the singular "1 debt".
- 100% line and branch coverage on the three evaluators.
