# Requirements Specification

<!-- Feature name: 013-strategy-comparison-endpoint -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 013) -->
<!-- Link: backlog/013-strategy-comparison-endpoint.md -->
<!-- Branch: main -->

## Feature Overview

`POST /v1/projections/comparison` (PRD FR-3, goal G4): run snowball and avalanche against
one snapshot and report the difference in months and dollars, so the tradeoff between the
two payoff orders is visible rather than folklore. The response holds both full
projections plus a `delta` computed only from those two projections (AC-3.1), including
which strategy costs less interest — a verdict the API supplies so the client never
compares numbers itself. The engine reports arithmetic and makes no recommendation
(NG-4).

## User Stories

### Story 1: Compare both orders side by side
**As** a household choosing a payoff order
**I want** both plans and the difference between them in one response
**So that** I can see what the choice actually costs

**Acceptance Criteria:**
- [ ] The response contains `snowball`, `avalanche`, `delta`, and `warnings`.
- [ ] `delta` has `monthsSaved`, `interestSaved`, `cheaperStrategy`,
  `firstDebtRetiredSooner`, `truncated`, and `note`.
- [ ] Both projections use the same extra payment and horizon.

### Story 2: A delta that can't disagree with the plans
**As** the engineering reviewer
**I want** the delta derived only from the two returned projections
**So that** the response is internally consistent by construction

**Acceptance Criteria:**
- [ ] AC-3.1: the delta is computed by a pure function whose only inputs are the two
  simulation results.
- [ ] AC-3.2: a single-debt snapshot yields an all-zero delta with an explanatory note.
- [ ] If either plan is truncated, fields that would compare unequal spans are null and
  the note says why.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `ComparisonController` in `adapter.in.web.api` handles
  `POST /v1/projections/comparison`.
- **REQ-002**: Request shape: `{"snapshot": {…}, "extraMonthlyPayment": "300.00",
  "horizonMonths": 360}` with the story 010 defaults. A `strategy` property is rejected as
  unknown (`UNKNOWN_FIELD`, story 011).
- **REQ-003**: The service normalizes the snapshot once, runs the simulator twice (one per
  strategy), and detects warnings once.
- **REQ-004**: `ComparisonDelta.of(PayoffSimulation snowball, PayoffSimulation avalanche)`
  in `domain.projection` computes:
  - `monthsSaved` = snowball `monthsToDebtFreedom` − avalanche `monthsToDebtFreedom`; null
    if either is null. Not clamped.
  - `interestSaved` = snowball `totalInterestPaid` − avalanche `totalInterestPaid`; null if
    either plan is truncated. Not clamped, so a violation of AC-2.2 would be visible.
  - `cheaperStrategy` = `SNOWBALL`, `AVALANCHE`, or `SAME` by total interest; null if
    either plan is truncated.
  - `firstDebtRetiredSooner` = `SNOWBALL`, `AVALANCHE`, or `SAME` by the month each plan
    first retires a debt; null if neither retires any.
  - `truncated` = either plan truncated.
  - `note` = `{"code", "message"}` or null:
    - `IDENTICAL_SCHEDULES` when both schedules are identical: "Both orders pay your debts
      the same way, so the results are identical."
    - `TRUNCATED` when either plan is truncated: "At least one plan doesn't finish within
      {horizon} months, so interest isn't compared."
    - `IDENTICAL_SCHEDULES` takes precedence.
- **REQ-005**: Response shape: `{"snowball": {…}, "avalanche": {…}, "delta": {…},
  "warnings": […]}`. Each projection uses the story 010 response shape without its
  `warnings` key.
- **REQ-006**: For identical schedules, `monthsSaved` is 0, `interestSaved` is $0,
  `cheaperStrategy` and `firstDebtRetiredSooner` are `SAME` (AC-3.2).
- **REQ-007**: Byte-identical output for identical input (NFR-4).

### Should Have (P1)
- **REQ-008**: A golden JSON response for the three-debt scenario, compared byte-for-byte
  like story 010's fixtures.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- Two 360-month simulations; target p95 under 500ms locally (twice the single-projection
  budget).

### Security
- No household figures logged (PRD NFR-2).

### Accessibility / Browser Support
- Not applicable: JSON API. The comparison screen is story 031.

## Constraints and Assumptions

### Technical Constraints
- `ComparisonDelta.of` takes no snapshot, rates, or balances — only the two results — which
  is how AC-3.1 is enforced structurally.
- No recommendation language anywhere (NG-4). The "order matters less than the habit" line
  is fixed page copy in story 031, not an API field.

### Assumptions
- Snowball and avalanche usually finish in the same month under a constant total payment,
  so `monthsSaved` is often 0 while `interestSaved` is positive.

## Out of Scope
- Simulation and orderings — stories 008, 009.
- Validation — story 011; warnings — story 012.
- The comparison screen and its closing line — story 031.

## Success Metrics
- `ComparisonDelta` unit tests for identical schedules, truncated plans (each side), and a
  normal three-debt case.
- AC-3.1 and AC-3.2 contract tests; the golden file matches.
- A test proves a `strategy` property is rejected.
- 100% line and branch coverage on `ComparisonDelta` and the service.
