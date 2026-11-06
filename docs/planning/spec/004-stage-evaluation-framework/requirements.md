# Requirements Specification

<!-- Feature name: 004-stage-evaluation-framework -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 004) -->
<!-- Link: backlog/004-stage-evaluation-framework.md -->
<!-- Branch: main -->

## Feature Overview

The frame every stage rule plugs into: the `StageEvaluator` interface, the
`StageEvaluation` result each evaluator returns, and the `StageLadder` that runs all seven
and picks the household's current stage. It lives in `domain.stage` as pure domain code.
It enforces the progression rules from PRD 5.3 — the current stage is the lowest one that
is neither satisfied nor skipped, every stage reports its true status regardless of
position, and status follows measurable progress — so the seven concrete evaluators
(stories 005 and 006) only have to answer "how far along is this one stage?". It serves
those evaluator stories, the assessment endpoint (story 007), and the ladder UI, which
renders all seven results verbatim.

## User Stories

### Story 1: A contract every stage rule follows
**As** a developer writing a stage evaluator
**I want** a small interface and a result type that refuses inconsistent results
**So that** every stage reports status, progress, and its reason the same way

**Acceptance Criteria:**
- [ ] `StageEvaluator` has `int stage()` and
  `StageEvaluation evaluate(FinancialSnapshot, List<StageEvaluation> lowerStages)`.
- [ ] `StageEvaluation` carries `stage`, `status`, `percentComplete`, `remainingAmount`,
  `blocker`, and `skipReason`, and its constructor rejects combinations the rules forbid.
- [ ] A shared percentage helper turns "part of whole" into a scale-1 percentage that
  rounds down and never errors on a zero whole.

### Story 2: One ladder, one current stage
**As** the assessment use case (story 007)
**I want** to run all seven evaluators and get the full ladder plus the current stage
**So that** every client can render all seven rows and highlight the right one

**Acceptance Criteria:**
- [ ] The ladder returns all seven evaluations in stage order and the current stage.
- [ ] The current stage is the lowest stage that is neither `SATISFIED` nor `SKIPPED`,
  or 7 when every stage is satisfied or skipped.
- [ ] A stage above the current one whose condition is met reports `SATISFIED`.
- [ ] Constructing a ladder without exactly one evaluator for each stage 1–7 fails
  immediately.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `StageStatus` enum: `SATISFIED`, `IN_PROGRESS`, `NOT_STARTED`, `SKIPPED`.
- **REQ-002**: `StageEvaluator` interface in `com.rungs.readiness.domain.stage` with
  `int stage()` and
  `StageEvaluation evaluate(FinancialSnapshot snapshot, List<StageEvaluation> lowerStages)`,
  where `lowerStages` holds the results for stages 1 to `stage() − 1`, in order. Only
  stage 7 uses it (story 006); the others ignore it. It is an internal domain interface,
  not a port. (Amended 2026-09-13 while specifying story 006.)
- **REQ-003**: `StageEvaluation` record with `stage` (`int`, 1–7), `status`,
  `percentComplete` (`BigDecimal`, scale 1, 0.0–100.0), `remainingAmount` (`Money`,
  nullable), `blocker` (`String`, nullable), and `skipReason` (`String`, nullable).
- **REQ-004**: `StageEvaluation` enforces on construction:
  - `SATISFIED`: `percentComplete` is exactly `100.0`; `blocker`, `skipReason`, and
    `remainingAmount` are null.
  - `IN_PROGRESS`: `percentComplete` is at most `99.9`; `blocker` is non-blank;
    `skipReason` is null.
  - `NOT_STARTED`: `percentComplete` is exactly `0.0`; `blocker` is non-blank;
    `skipReason` is null.
  - `SKIPPED`: `percentComplete` is `0.0`; `skipReason` is non-blank; `blocker` and
    `remainingAmount` are null.
- **REQ-005**: Status follows progress (PRD 5.3): an evaluator reports `IN_PROGRESS` when
  its progress is above 0% and the stage is not satisfied, and `NOT_STARTED` at exactly
  0%. **Stage 2 is the one exception**: it reports `IN_PROGRESS` at `0.0` whenever
  non-mortgage debt remains, because the engine sees only current balances and has no
  starting balance to measure against. `IN_PROGRESS` therefore allows `0.0` (REQ-004).
- **REQ-006**: `Percentages.of(Money part, Money whole)` returns a scale-1 `BigDecimal`
  rounded **down**, capped at `99.9` unless `part` ≥ `whole`, and `0.0` when `whole` is
  zero (AC-1.4). Rounding down guarantees an unmet stage never displays `100.0`. An
  overload `Percentages.ofRatio(BigDecimal part, BigDecimal whole)` applies the same rules
  to non-money quantities (stage 4's contribution percent, stage 7's stage count).
- **REQ-007**: `StageLadder` is constructed with a collection of evaluators and fails
  with `IllegalArgumentException` unless it contains exactly one evaluator for each stage
  1–7.
- **REQ-008**: `StageLadder.assess(FinancialSnapshot)` returns a `LadderResult` record with
  `currentStage` (`int`) and `stages` (`List<StageEvaluation>`, unmodifiable, ordered
  1–7). It runs evaluators in stage order, passes each the results of the stages below it,
  and verifies that each evaluator returned a result for its own stage number.
- **REQ-009**: The current stage is the lowest-numbered stage whose status is neither
  `SATISFIED` nor `SKIPPED`; if all seven are satisfied or skipped, it is 7.
- **REQ-010**: Every evaluator runs for every assessment; no stage is short-circuited
  because a lower stage is incomplete.
- **REQ-011**: Evaluation is deterministic and reads no clock, random source, or
  environment (PRD NFR-4).

### Should Have (P1)
- **REQ-012**: A reusable test-fixture evaluator (test sources only) that returns a
  configured result, so ladder tests do not depend on the real evaluators.

### Nice to Have (P2)
- **REQ-013**: `LadderResult.evaluation(int stage)` returns one stage's result by number.

## Non-Functional Requirements

### Performance
- Assessing a snapshot runs seven evaluators with no I/O; the ladder adds negligible
  overhead to the 50ms p95 budget for `/v1/assessments` (PRD NFR-3).

### Security
- Invariant-violation messages name the stage and the rule broken, never snapshot values.

### Accessibility / Browser Support
- Not applicable. `blocker` and `skipReason` are user-facing copy rendered verbatim;
  their wording rules belong to stories 005 and 006.

## Constraints and Assumptions

### Technical Constraints
- Pure domain code in `domain.stage` (PRD AC-A, AC-B, AC-E, AC-F).
- The domain has no dependency injection; `application` constructs the ladder with the
  seven evaluators explicitly (story 007).

### Assumptions
- `remainingAmount` is populated by evaluators that have a dollar shortfall; stages
  without one (for example stage 4, a percentage) leave it null.
- Regression needs no handling: the engine is stateless and simply reports the lower
  stage (PRD 5.3).

## Out of Scope
- The seven concrete evaluators and their blocker and skip-reason copy — stories 005, 006.
- The assessment endpoint, `monthlySurplus`, and wiring — story 007.
- Warnings — story 012.
- The stage-verdict metrics counter — story 017.
- The property that the current stage is always the lowest unsatisfied, unskipped stage
  across generated snapshots — story 014.

## Success Metrics
- 100% line and branch coverage on `StageEvaluation`, `Percentages`, `StageLadder`, and
  `LadderResult`.
- Every forbidden status combination in REQ-004 has a rejecting test.
- Ladder tests cover: all satisfied (current 7), a skipped stage below an unsatisfied one,
  a satisfied stage above the current one, and a missing or duplicate evaluator.
- `Percentages.of` tests cover zero whole, part equal to whole, part above whole, and a
  value that would round to 100.0 (for example 9,999 of 10,000 → 99.9).
