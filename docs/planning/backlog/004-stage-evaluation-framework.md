---
status: approved
---

# Stage evaluation framework and progression rules

The `StageEvaluator` contract, the `StageEvaluation` result shape, and the ladder
logic that picks the current stage from seven independent verdicts.

## Description

PRD section 5.3 defines progression: stages are evaluated in order, and the
current stage is the lowest-numbered stage that is neither `SATISFIED` nor
`SKIPPED`. A skipped stage never blocks progression. The response carries the
full ladder so a client can render all seven regardless of which is current — and
the design spec is emphatic that the UI shows all seven, because "a hidden stage
looks like a bug."

The engine is stateless and has no memory of prior assessments, so regression is
ordinary: a household that drains its emergency fund is simply reported at the
lower stage with no special handling.

This story builds the frame and the ladder. The seven concrete evaluators are
stories 005 and 006 so each rule gets its own boundary tests.

## Acceptance Criteria

- A `StageEvaluator` port exists that maps a `FinancialSnapshot` to a
  `StageEvaluation` for one stage.
- `StageEvaluation` carries `stage`, `status`, `percentComplete`,
  `remainingAmount` (nullable), `blocker` (nullable), `skipReason` (nullable).
- `status` is one of `SATISFIED | IN_PROGRESS | NOT_STARTED | SKIPPED`.
- The ladder returns the lowest-numbered stage that is neither `SATISFIED` nor
  `SKIPPED` as the current stage.
- When all seven are satisfied or skipped, the current stage is 7.
- Skipped stages do not block progression past them.
- `percentComplete` is `BigDecimal` scale 1 in the range 0–100; exactly `100.0`
  for a satisfied stage, and `0.0` — not an error — when the denominator is zero
  (AC-1.4).
- Every `IN_PROGRESS` stage carries a non-null `blocker`; every `SKIPPED` stage
  carries a non-null `skipReason` and a null `blocker` (AC-1.3).
- Evaluation is deterministic and reads no wall clock (NFR-4).
- A missing evaluator or an out-of-order stage fails loudly rather than silently
  producing a short ladder.

## Size

M

## Open Questions

- ~~`NOT_STARTED` vs `IN_PROGRESS`?~~ **Resolved.** By progress: above 0% is
  `IN_PROGRESS`, exactly 0% is `NOT_STARTED` (PRD 5.3).
- ~~Do stages above the current one report their true status?~~ **Resolved.** Yes;
  a met condition reports `SATISFIED` regardless of position.

Full requirements: [spec/004-stage-evaluation-framework/requirements.md](../spec/004-stage-evaluation-framework/requirements.md).
