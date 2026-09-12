---
status: proposed
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

- Does this endpoint accept a `strategy` field at all? It runs both, so the field
  is meaningless — is it rejected as an unknown property (per FR-4) or ignored?
- What does "months saved" mean when one strategy truncates and the other does
  not — null, or the horizon as a floor?
- Is the AC-3.2 note free text or a coded reason like the warnings? The design
  spec's closing line ("when the gap is small, the order matters less than the
  habit") suggests the API may need to supply a small-gap note too — is that in
  scope, and what threshold counts as small?
