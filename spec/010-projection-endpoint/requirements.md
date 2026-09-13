# Requirements Specification

<!-- Feature name: 010-projection-endpoint -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 010) -->
<!-- Link: backlog/010-projection-endpoint.md -->
<!-- Branch: main -->

## Feature Overview

`POST /v1/projections` (PRD FR-2): the web surface over the payoff simulation (story 008)
and the strategies (story 009). It accepts a snapshot nested under `snapshot` — the same
shape `/v1/assessments` accepts — plus an extra monthly payment, a strategy, and a
horizon, and returns the month-by-month schedule, total interest, months to debt freedom,
months to the next stage (only when the household is at stage 2), and whether the horizon
cut the plan short. This story owns the request contract, the defaults, and the committed
golden-file fixtures that make every schedule change a reviewable diff. It also carries
the tightest user-facing latency target: the projection screen re-renders on every slider
move.

## User Stories

### Story 1: See the payoff plan
**As** a household at stage 2
**I want** a month-by-month plan for my debts under an extra payment and a strategy
**So that** I can see when I'll be debt-free and what interest it costs

**Acceptance Criteria:**
- [ ] The response has `monthsToNextStage`, `monthsToDebtFreedom`, `totalInterestPaid`,
  `truncated`, `unpayableDebtIds`, `debts`, `schedule`, and `warnings`.
- [ ] At stage 2, `monthsToNextStage` equals `monthsToDebtFreedom`; otherwise it is null.
- [ ] AC-2.1: the three-debt snowball scenario matches a committed golden file.

### Story 2: Sensible defaults
**As** a client sending only a snapshot
**I want** omitted options to use documented defaults
**So that** the simplest request still returns a useful plan

**Acceptance Criteria:**
- [ ] Omitted `extraMonthlyPayment` is $0, `strategy` is `SNOWBALL`, and `horizonMonths`
  is 360.
- [ ] No schedule exceeds `horizonMonths` entries (AC-2.5).

### Story 3: Reviewable changes
**As** the engineering reviewer
**I want** full responses committed as golden files
**So that** any change to a schedule shows up as a diff, never silently

**Acceptance Criteria:**
- [ ] Golden JSON responses are committed for the AC-2.1 scenario under both strategies.
- [ ] A mismatch fails the build; tests never rewrite the fixtures.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `ProjectionController` in `adapter.in.web.api` handles
  `POST /v1/projections` (JSON in and out), constructor injection only.
- **REQ-002**: Request shape:
  `{"snapshot": {…assessment request…}, "extraMonthlyPayment": "300.00",
  "strategy": "SNOWBALL", "horizonMonths": 360}`. Money is a plain decimal string;
  `strategy` is `SNOWBALL` or `AVALANCHE`; `horizonMonths` is a JSON integer.
- **REQ-003**: Defaults: missing/blank `extraMonthlyPayment` → $0 (FR-4); missing
  `strategy` → `SNOWBALL`; missing `horizonMonths` → 360. Range checks (1–600) are story
  011.
- **REQ-004**: `ProjectionService` in `application` normalizes the snapshot through the
  story 007 input model, runs the `StageLadder` to find the current stage, and runs
  `PayoffSimulator` with the snapshot's debts, `monthlySurplus()`, the extra payment, the
  chosen strategy's ordering, and the horizon.
- **REQ-005**: `monthsToNextStage` equals `monthsToDebtFreedom` when the current stage is
  2, and is null for any other current stage (PRD FR-2).
- **REQ-006**: Response shape (all keys always present, money as response objects):
  - `monthsToNextStage`, `monthsToDebtFreedom`: integer or `null`
  - `totalInterestPaid`: money
  - `truncated`: boolean
  - `unpayableDebtIds`: array of ids
  - `debts`: `[{"id", "label", "type", "startingBalance"}]` in supplied order, so clients
    can label schedule lines without holding the request
  - `schedule`: `[{"month", "lines": [{"debtId", "openingBalance", "interest",
    "payment", "closingBalance"}], "retiredDebtIds"}]`
  - `warnings`: array (story 012)
- **REQ-007**: `monthsToDebtFreedom` is `0` and `schedule` is empty when the snapshot has
  no debts.
- **REQ-008**: Golden files `src/test/resources/golden/projection-three-debts-snowball.json`
  and `…-avalanche.json` are compared byte-for-byte; the test fails with a diff and never
  regenerates them.
- **REQ-009**: Identical requests produce byte-identical responses (NFR-4).

### Should Have (P1)
- **REQ-010**: A contract test for a truncated plan (horizon too short) asserting
  `truncated` true and null months.
- **REQ-011**: A contract test for a negative-amortizing debt asserting it appears in
  `unpayableDebtIds` and the request still returns 200.

### Nice to Have (P2)
- **REQ-012**: A documented, opt-in script or test flag to regenerate golden files
  locally, used only deliberately and reviewed as a diff.

## Non-Functional Requirements

### Performance
- p95 under 250ms for a 360-month projection, measured locally (PRD NFR-3). This is a
  user-facing target because the projection screen re-renders on each slider change.

### Security
- No household figures or request bodies logged (PRD NFR-2).

### Accessibility / Browser Support
- Not applicable: JSON API. The schedule table's accessibility is story 030/033.

## Constraints and Assumptions

### Technical Constraints
- Follows the story 007 patterns: input model normalization, `Money` serializer, fixed
  property order.
- Full schedule is always returned; no pagination or summary mode (decided 2026-09-13).

### Assumptions
- A 600-month, 50-debt response is large but acceptable for v1.
- Validation (story 011) rejects `horizonMonths` outside 1–600 and unknown strategies.

## Out of Scope
- Payoff mechanics and orderings — stories 008, 009.
- Validation and problem details — story 011.
- Warnings content — story 012.
- Strategy comparison — story 013.
- OpenAPI documentation of the tie-break rule — story 016.
- The projection screen — story 030.

## Success Metrics
- Both golden files match on repeated runs.
- Defaults, truncation, no-debt, and negative-amortization cases each have a contract
  test.
- A warm 360-month, 10-debt projection completes under 250ms locally.
- 100% line coverage on the controller and `ProjectionService`.
