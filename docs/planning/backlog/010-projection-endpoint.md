---
status: approved
---

# Project the path forward endpoint

`POST /v1/projections` — run the payoff simulation against a snapshot and return
the schedule, the months to the next stage, and the months to debt freedom.

## Description

FR-2's web surface. Accepts a `FinancialSnapshot` plus a `ProjectionRequest`
(`extraMonthlyPayment` defaulting to zero, `strategy` defaulting to `SNOWBALL`,
`horizonMonths` defaulting to 360 with a max of 600) and returns a `Projection`.

The heavy lifting is stories 008 and 009; this story owns the contract, the
defaults, the horizon clamp, and the golden-file fixtures that section 9 wants
committed and reviewed like code.

Latency matters more here than anywhere else in the API: the design spec's
projection screen re-renders from a fresh server-side projection on every slider
change, via htmx with a 250ms debounce, so the NFR-3 budget of 250ms for a 360-month projection is a
user-facing responsiveness target, not just a number.

## Acceptance Criteria

- `POST /v1/projections` accepts a snapshot plus a `ProjectionRequest` and
  returns `monthsToNextStage`, `monthsToDebtFreedom`, `totalInterestPaid`,
  `schedule`, and `truncated`.
- Defaults apply when fields are omitted: zero extra payment, `SNOWBALL`,
  360-month horizon.
- `horizonMonths` above 600 is rejected by validation.
- `monthsToNextStage` and `monthsToDebtFreedom` are null — not zero — when not
  reached within the horizon, with `truncated` true.
- Every money field carries a server-formatted display string (story 002).
- AC-2.1: a three-debt snowball scenario matches a committed golden file to the
  cent.
- AC-2.5: the serialized schedule never exceeds `horizonMonths` entries.
- Golden fixtures are committed and a diff in them fails the build until
  reviewed.
- p95 under 250ms for a 360-month projection, measured locally (NFR-3).
- Warnings from story 012 appear on this response too.

## Size

M

## Open Questions

- ~~What does `monthsToNextStage` mean?~~ **Resolved** (spec 008). Populated only
  when the current stage is 2, equal to `monthsToDebtFreedom`; null otherwise.
- ~~Full schedule or summary/pagination?~~ **Resolved.** Always the full schedule;
  a summary mode can be added later without breaking clients.
- ~~Mortgage interest in `totalInterestPaid`?~~ **Resolved.** Excluded.
- **Also decided:** the request nests the snapshot under `snapshot` (PRD FR-2).

Full requirements: [spec/010-projection-endpoint/requirements.md](../spec/010-projection-endpoint/requirements.md).
