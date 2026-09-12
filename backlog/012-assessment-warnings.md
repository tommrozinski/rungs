---
status: proposed
---

# Non-fatal warnings

Surface the four conditions that make an assessment questionable without making
it invalid.

## Description

FR-5. These are conditions a caller needs to know about but that should not fail
the request — the engine still produces a usable answer, it just qualifies it.
The four codes:

- `EXPENSES_EXCEED_INCOME` — essential expenses plus minimums exceed take-home income.
- `NEGATIVE_AMORTIZATION` — a minimum payment is below monthly interest accrual.
- `EMERGENCY_FUND_TARGET_CAPPED` — requested months of expenses above 6.
- `MORTGAGE_IN_DEBT_LIST` — a `MORTGAGE`-type debt supplied alongside a `mortgage` object.

Detection is domain logic; the `warnings` list on the response is populated by the
application service. `NEGATIVE_AMORTIZATION` is detected here but also drives
behaviour in story 008, which must exclude such debts from the completion
estimate rather than loop forever.

The design spec renders these as a banner above the ladder, "because they change
how the numbers below should be read" — so the message text is user-facing copy,
and the caution/problem distinction in component C-09 implies warnings carry a
severity the API must supply rather than the client inferring one.

## Acceptance Criteria

- A `Warning` type carries a stable machine-readable code, a severity, and a
  human-readable message with the relevant figures.
- Each of the four conditions is detected and emitted with its documented code.
- `NEGATIVE_AMORTIZATION` names the specific debt(s) affected.
- Warnings never change the HTTP status; a warned response is still 200.
- Multiple warnings can appear on one response.
- An unwarned request returns an empty list, never null.
- Warnings appear on `/v1/assessments` and both projection endpoints alike.
- Warning order is deterministic so output stays byte-identical (NFR-4).
- Message copy follows the design spec voice rules and renders verbatim.
- Detection is covered by a test per condition.

## Size

S

## Open Questions

- `EMERGENCY_FUND_TARGET_CAPPED` presumes a caller-supplied multiple, but
  `FinancialSnapshot` has no such field. Same unresolved question as story 005.
- When `MORTGAGE_IN_DEBT_LIST` fires, which figure wins for surplus and
  projection purposes — the debt entry or the `mortgage` object?
- Which of the four map to C-09 "caution" (gold) versus "problem" (red)? The
  design spec reserves red for input validation and says a user behind on goals
  is never shown red — does any warning qualify as red at all?
- Does the CSV import unknown-key warning (FR-7) share this `Warning` type, or is
  it separate and confined to `ImportReport`?
