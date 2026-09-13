---
status: approved
---

# Compare strategies endpoint

`POST /v1/projections/comparison` — run both orderings against one snapshot and
report the difference in months and dollars.

## Description

FR-3, and goal G4: make the snowball-versus-avalanche tradeoff visible in dollars
and months rather than leaving it as folklore. The endpoint runs both strategies
against one snapshot and returns both projections plus a `delta` covering months
saved, interest saved, and which strategy retires the first debt sooner.

The PRD's constraint is worth respecting precisely: the delta is *computed from
the two projections*, never recalculated independently (AC-3.1). That rules out a
second pass over the snapshot and keeps the two halves of the response provably
consistent.

The design spec's comparison screen needs one thing the PRD does not name: which
column gets the green header, i.e. which strategy costs less interest. That is a
verdict the API should supply rather than the client deriving it — client code
does no arithmetic, comparison included.

## Acceptance Criteria

- `POST /v1/projections/comparison` returns both projections plus a `delta`.
- The delta carries months saved, interest saved, and which strategy retires the
  first debt sooner.
- The response names which strategy costs less interest, so the client renders
  emphasis without comparing numbers itself.
- AC-3.1: delta values are derived arithmetically from the two returned
  projections; a test proves no independent recalculation path exists.
- AC-3.2: a single-debt snapshot yields an all-zero delta plus a note explaining
  that both strategies are identical.
- Interest saved is never negative — avalanche is the floor (consistent with
  AC-2.2).
- Both projections honour the same `extraMonthlyPayment` and `horizonMonths`.
- If either projection truncates, the delta reports what is comparable and flags
  the truncation rather than silently comparing a complete run to a partial one.
- Every money field carries a server-formatted display string (story 002).

## Size

M

## Open Questions

- ~~Accept a `strategy` field?~~ **Resolved.** Rejected as an unknown property.
- ~~Months saved when one truncates?~~ **Resolved.** Null.
- ~~Note format and small-gap threshold?~~ **Resolved.** Coded note
  (`IDENTICAL_SCHEDULES`, `TRUNCATED`) with a message. No small-gap threshold: the
  closing line is fixed page copy in story 031 (NG-4).

Full requirements: [spec/013-strategy-comparison-endpoint/requirements.md](../spec/013-strategy-comparison-endpoint/requirements.md).
