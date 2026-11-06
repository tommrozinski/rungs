# Requirements Specification

<!-- Feature name: 006-stage-evaluators-4-to-7 -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 006) -->
<!-- Link: backlog/006-stage-evaluators-4-to-7.md -->
<!-- Branch: main -->

## Feature Overview

The four lifecycle stage evaluators: stage 4 (retirement contribution of at least 15%),
stage 5 (college funding), stage 6 (mortgage paid off), and stage 7 (wealth building, once
stages 1–6 are all satisfied or skipped). This is where `SKIPPED` earns its place: renters
skip stage 6, households with no college plans skip stage 5, and retired households skip
stages 4 and 5. Skip reasons are product copy rendered on the ladder ("You rent, so there's
no mortgage to pay down."), so they are explanations, not codes. AC-1.2 — a renter with no
college plans who satisfies stages 1–4 reaches stage 7 — is the headline case.

## User Stories

### Story 1: Stage 4 — retirement contributions
**As** a household saving for retirement
**I want** to see my contribution rate against the 15% target
**So that** I know how far I am from stage 4

**Acceptance Criteria:**
- [ ] Skipped when `retired` is true.
- [ ] Satisfied at exactly 15% and above.
- [ ] Below 15%: `percentComplete` is contribution ÷ 15 (rounded down), `IN_PROGRESS` above
  0% and `NOT_STARTED` at 0%; `remainingAmount` is null.

### Story 2: Stage 5 — college funding
**As** a household with or without college plans
**I want** stage 5 to apply only when I intend to fund college
**So that** the ladder never demands something irrelevant to me

**Acceptance Criteria:**
- [ ] Skipped when `intendsToFundCollege` is false, or when `retired` is true.
- [ ] Satisfied when `monthlyCollegeContribution` is above $0.
- [ ] Otherwise `NOT_STARTED` at 0.0 with a blocker.

### Story 3: Stage 6 — mortgage
**As** a homeowner or renter
**I want** stage 6 to reflect my housing situation
**So that** renters skip it and homeowners see what is left on the mortgage

**Acceptance Criteria:**
- [ ] Skipped when `rents` is true (even if a mortgage object is also supplied).
- [ ] Satisfied when there is no mortgage or its balance is $0.
- [ ] Otherwise `percentComplete` is home equity, (home value − balance) ÷ home value,
  rounded down; `remainingAmount` is the mortgage balance.

### Story 4: Stage 7 — wealth building
**As** a household that has done the earlier work
**I want** stage 7 to recognize it
**So that** the ladder shows I have reached the top

**Acceptance Criteria:**
- [ ] Satisfied only when stages 1–6 are each `SATISFIED` or `SKIPPED`.
- [ ] Otherwise `percentComplete` is the share of stages 1–6 that are satisfied or skipped.
- [ ] AC-1.2 passes: stages 1–4 satisfied, renting, no college plans → current stage 7,
  with stages 5 and 6 `SKIPPED` and populated skip reasons.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `RetirementEvaluator` (4), `CollegeEvaluator` (5), `MortgageEvaluator` (6),
  and `WealthBuildingEvaluator` (7) in `com.rungs.readiness.domain.stage`, implementing
  `StageEvaluator` with the lower-stages signature from story 004.
- **REQ-002**: Stage 4 skip precedence: `retired` → skipped. Otherwise satisfied at
  `retirementContributionPercent` ≥ 15; else progress via `Percentages.ofRatio`.
- **REQ-003**: Stage 5 skip precedence: `intendsToFundCollege == false` → skipped with the
  no-plans reason; else `retired` → skipped with the retired reason. Otherwise satisfied
  when `monthlyCollegeContribution` > $0, else `NOT_STARTED` at 0.0.
- **REQ-004**: Stage 6: `rents` → skipped. Else no mortgage or balance $0 → satisfied.
  Else `percentComplete` = `Percentages.of(homeValue − balance, homeValue)`; when home
  value is $0 or does not exceed the balance, progress is 0.0 (`NOT_STARTED`).
- **REQ-005**: Stage 7 reads the six lower-stage evaluations passed by the ladder. It is
  satisfied when all six are `SATISFIED` or `SKIPPED`; otherwise `percentComplete` is the
  count of satisfied-or-skipped stages ÷ 6, rounded down, `IN_PROGRESS` above 0 and
  `NOT_STARTED` at 0. Stage 7 is never skipped and has no `remainingAmount`.
- **REQ-006**: Skip reasons:
  - Stage 4, retired: "You're retired, so you're no longer saving for retirement from
    income."
  - Stage 5, no plans: "You don't plan to fund college, so there's nothing to save for."
  - Stage 5, retired: "You're retired, so there's no college fund to build."
  - Stage 6, renting: "You rent, so there's no mortgage to pay down."
- **REQ-007**: Blockers (percentages written without trailing zeros, e.g. "6%", "6.5%";
  amounts from `Money.toDisplayString()`):
  - Stage 4: "You're contributing {percent}% of income to retirement, short of 15%."
  - Stage 5: "You plan to fund college but aren't contributing to it yet."
  - Stage 6: "You have {balance} left on your mortgage."
  - Stage 7: "{n} of the 6 earlier stages are complete."
- **REQ-008**: Stages 4–6 each report their true status regardless of the current stage
  (PRD 5.3); for example, a household at stage 2 already contributing 15% reports stage 4
  `SATISFIED`.

### Should Have (P1)
- **REQ-009**: Copy templates are private constants in each evaluator, as in story 005.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- Each evaluator is constant time with no I/O.

### Security
- Blockers contain household figures and are response content only; never logged (PRD
  NFR-2).

### Accessibility / Browser Support
- Skip reasons and blockers are complete sentences that read correctly aloud.

## Constraints and Assumptions

### Technical Constraints
- Pure domain code (PRD AC-A, AC-F). Uses stories 002–004.
- Debts of type `MORTGAGE` no longer exist (PRD FR-4), so stage 6 reads only the
  `mortgage` object.
- Mortgage payments are part of essential expenses (PRD FR-1); stage 6 does not use
  `mortgage.monthlyPayment`.

### Assumptions
- Validation guarantees percentages within 0–100 and non-negative amounts.
- A household that owns its home outright supplies no mortgage and `rents` false, and
  stage 6 reports `SATISFIED`.

## Out of Scope
- Stages 1–3 — story 005.
- The ladder, `StageEvaluation`, and `Percentages` — story 004.
- Projecting when stages 4–7 would be reached — not modeled (see story 008).
- UI rendering of skipped rows — story 026.

## Success Metrics
- One test class per evaluator, including the 14.99% / 15.00% boundary for stage 4, both
  stage 5 skip reasons and their precedence, renter-with-mortgage for stage 6, home value
  of $0, and stage 7 at 0, 5, and 6 of 6.
- AC-1.2 passes at the domain level through the full `StageLadder`.
- Every skip reason and blocker in REQ-006 and REQ-007 is asserted verbatim.
- 100% line and branch coverage on the four evaluators.
