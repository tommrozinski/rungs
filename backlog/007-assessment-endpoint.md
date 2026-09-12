---
status: proposed
---

# Assess readiness endpoint

`POST /v1/assessments` — accept a financial snapshot, return the full stage
ladder, the current stage, and the household's monthly surplus.

## Description

FR-1. This is the first endpoint and the one that makes the stage work reachable.
It owns the web adapter: request and response DTOs, mapping to and from the
domain (the domain imports no Jackson, per AC-A), and the application service
that orchestrates the seven evaluators.

`monthlySurplus` is income minus essential expenses minus minimum debt payments —
the same figure the projection stories feed into the payoff simulation, so it
belongs in the domain rather than the controller.

Per the design spec, every money field in the response carries a server-formatted
display string alongside its machine value (story 002). The client renders what
it is given and formats nothing.

Validation is story 011 and warnings are story 012; this story delivers the happy
path and the response shape.

## Acceptance Criteria

- `POST /v1/assessments` accepts a `FinancialSnapshot` and returns a
  `ReadinessAssessment`.
- The response carries `currentStage`, all seven `StageEvaluation`s in stage
  order, `monthlySurplus`, and a `warnings` list (empty until story 012).
- `monthlySurplus` equals income minus essential expenses minus the sum of
  minimum debt payments. Zero-balance debts are ignored (PRD FR-4), so their
  minimum payments do not count.
- Missing, null, or blank numeric fields in the request are treated as zero
  (PRD FR-4), with a contract test for each.
- A zero-balance debt in the request is accepted, not rejected, and has no
  effect on any figure in the response.
- Every money field carries a server-formatted display string.
- AC-1.1 through AC-1.4 pass as contract tests against the endpoint, not just
  against the domain.
- Domain types carry no Jackson annotations; mapping lives in `adapter.in.web`.
- Constructor injection only, no field injection (AC-D).
- A MockMvc or `@SpringBootTest` slice test asserts the exact JSON shape.
- Identical request bodies produce byte-identical responses (NFR-4).
- p95 under 50ms measured locally (NFR-3).

## Size

M

## Open Questions

- Can `monthlySurplus` be negative, and how is that formatted for display (see
  story 002)? The `EXPENSES_EXCEED_INCOME` warning implies yes.
- Do minimum payments for `MORTGAGE`-type debts count against `monthlySurplus`,
  or only the separate `mortgage.monthlyPayment`? Both can be present — that is
  the `MORTGAGE_IN_DEBT_LIST` warning case.
- Should the response echo a correlation ID for the structured-logging story?
