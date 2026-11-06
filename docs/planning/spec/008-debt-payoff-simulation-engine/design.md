# Design: 008 + 009 + 010 — Payoff simulation, strategies, projection endpoint

<!-- Source: requirements.md in spec/008, spec/009, spec/010; prd/readiness-engine-prd.md FR-2 -->
<!-- Ticket: none (backlog stories 008, 009, 010) -->
<!-- Date: 2026-09-13 -->

Combined design (lean process). The requirements hold the rules and acceptance criteria; this page adds
layout, the decisions the requirements leave open, and the test list.

## 1. Layout

```
domain/projection/
  PayoffOrdering          interface: List<Debt> order(List<Debt>)                        (009)
  Strategy                enum SNOWBALL, AVALANCHE; ordering()                            (009)
  SimulationInput         debts, monthlySurplus, extraMonthlyPayment, ordering, horizon   (008)
  PayoffSimulator         simulate(SimulationInput) → PayoffSimulation; stateless         (008)
  PayoffSimulation        schedule, monthsToDebtFreedom, totalInterestPaid, truncated,
                          unpayableDebtIds; monthRetired(id)                              (008)
  MonthlyPeriod, DebtLine                                                                 (008)
application/
  ProjectionInput         snapshot (SnapshotInput), extraMonthlyPayment, strategy, horizonMonths (010)
  ProjectionService       project(ProjectionInput) → ProjectionResponse                   (010)
  validation/HorizonMonths, StrategyName (+ validators)                                   (010)
  validation/SnapshotValidator.validate(ProjectionInput)                                  (010)
  response/ProjectionResponse, ProjectionDebtResponse, MonthlyPeriodResponse, DebtLineResponse (010)
adapter/in/web/api/
  ProjectionController    POST /v1/projections                                            (010)
config/ApplicationConfiguration  StageLadder bean shared by AssessmentService and ProjectionService
```

## 2. Decisions

- **Records, immutable.** All domain types are records with defensive `List.copyOf`; constructors reject
  nulls and out-of-range values (`horizonMonths` 1–600, negative extra payment) with
  IllegalArgumentException, following `Debt` and `FinancialSnapshot`.
- **Interest step.** `Money.times(BigDecimal)` already rounds half-even to the cent; the rate factor is
  `annualInterestRate.divide(1200, MathContext.DECIMAL128)`. The implementer confirms `times` rounds once
  (not twice) and uses DECIMAL128; if not, a new `Money` method does exactly that.
- **Month loop.** A fixed order from `ordering.order(debts)` at month 0 (009 REQ-005). Balances live in a
  `long[]` of cents indexed by supplied position; only interest touches `BigDecimal`. `lines` in a month
  follow supplied order (not payoff order), one per debt unretired at the start of the month.
- **Pool.** Per 008 REQ-002 step 3, freed minimums join from the month after retirement; this month's
  unused minimum (minimum > balance) joins this month's pool.
- **Unpayable.** 008 REQ-007 as written: unretired at the horizon with closing balance ≥ starting balance.
  No early detection or skipping; the horizon bounds the loop (600 × 50 is small).
- **Runaway-balance ceiling (code review fix).** A debt whose interest permanently outgrows every payment
  compounds exponentially and can overflow a `long` well before `horizonMonths` (e.g. the largest valid
  single debt, $1,000,000,000.00 at 100% APR with nothing paid toward it, by ~month 230). After a month's
  steps complete (so REQ-009's accounting invariant still holds for that month), any unretired debt whose
  closing balance reaches or exceeds `RUNAWAY_BALANCE_CENTS` ($1,000,000,000,000.00 — 1000x the largest
  valid balance, far below `Long.MAX_VALUE` even after one more month at 100% APR) stops the simulation
  early: `truncated = true`, `unpayableDebtIds`/`monthsToDebtFreedom` computed exactly as at the horizon.
  The schedule is then shorter than `horizonMonths`, which REQ-004/AC-2.5 only forbid exceeding.
- **Orderings.** Stable sorts (`List.sort` is stable) over a copy, so supplied order is the tie-break with
  no explicit index. Avalanche compares with `BigDecimal.compareTo` so `7.25` equals `7.250`.
- **Request model (010).** `ProjectionInput` is a record in `application`, mirroring `SnapshotInput`:
  `@Valid @NotNull(message = "INVALID_TYPE") SnapshotInput snapshot`, `@MoneyAmount String
  extraMonthlyPayment`, `@StrategyName String strategy` (null allowed → SNOWBALL; unknown → the existing
  `INVALID_STRATEGY` code), `@HorizonMonths Integer horizonMonths` (null → 360; outside 1–600 →
  `HORIZON_RANGE`). `SnapshotValidator` gets `validate(ProjectionInput)` with `FieldsThenWholeForm`, so the
  50-debt rule applies under `/snapshot`. Pointers come out as `/snapshot/debts/0/balance` naturally from
  the property path; the contract test pins it (011 REQ-011).
- **Response.** Records under `application.response`, keys in the REQ-006 order via record component
  order (the 007 Jackson configuration). Money as `MoneyResponse`. `warnings: []` until 012.
- **Defaults.** Applied in `ProjectionService` (blank extra → $0, null strategy → SNOWBALL, null horizon →
  360), not in the controller.
- **Wiring.** `ApplicationConfiguration` gains a `StageLadder` bean (the seven evaluators, built once) used
  by `AssessmentService` and `ProjectionService`; `PayoffSimulator` is constructed inside the service.
- **Golden files, generated independently.** The AC-2.1 fixture values come from a small Python script
  using `decimal` (ROUND_HALF_EVEN) that re-implements REQ-002 from the requirements text, not from the
  Java code. The script is committed at `spec/008-debt-payoff-simulation-engine/golden-payoff.py` as
  review evidence; the CSV and both JSON goldens are committed under `src/test/resources/golden/`. Tests
  never rewrite them. REQ-012 (regeneration flag) is skipped; rerunning the script is the documented path.
- **Performance.** A plain JUnit test times a warm 360-month, 10-debt projection through the service
  (median of 20 runs under 250 ms), tagged so it fails loudly rather than flakily (generous bound).

## 3. AC-2.1 scenario (fixed values)

Three debts, in supplied order, chosen so snowball and avalanche orders differ:

| Supplied # | Label | Type | Balance | Rate | Minimum |
| --- | --- | --- | --- | --- | --- |
| 1 | Credit card | CREDIT_CARD | 3500.00 | 23.99 | 90.00 |
| 2 | Medical bill | closest existing type (e.g. MEDICAL, else OTHER) | 900.00 | 0 | 50.00 |
| 3 | Car loan | AUTO | 7200.00 | 6.9 | 210.00 |

Monthly surplus 300.00, extra 0, horizon 360. Snowball order: medical, card, car. Avalanche order: card,
car, medical. For the endpoint goldens, the snapshot is chosen so `monthlySurplus()` is 300.00 with these
debts (the implementer derives income and expenses from `FinancialSnapshot.monthlySurplus()`), with an
emergency fund that puts the household at stage 2, so `monthsToNextStage` is non-null.

## 4. Tests

- **009:** order per strategy; `snowballBreaksBalanceTiesBySuppliedOrder`,
  `avalancheBreaksRateTiesBySuppliedOrder`, three-way ties, `7.25` vs `7.250`; input list unchanged; empty
  and single-debt lists.
- **008:** each REQ-002 step alone (half-cent rounding, minimum above balance, two-debt cascade in one
  month, freed minimum next month); negative surplus as $0; AC-2.3 (unpayable, 600-month horizon
  terminates), AC-2.4 (minimums only), AC-2.5 (truncation, null months); no debts → month 0, empty
  schedule; REQ-009 accounting invariant and no negative balance asserted by a helper on every schedule;
  determinism; AC-2.1 CSV fixture; AC-2.2 avalanche ≤ snowball; single-debt schedules identical;
  a fixed-order-not-reselected case; `FixedOrder` test ordering; `monthRetired`.
- **010:** golden JSON byte-for-byte for both strategies; defaults; stage 2 vs other stage for
  `monthsToNextStage`; no debts; truncated; negative amortization → 200 with `unpayableDebtIds`;
  validation (`HORIZON_RANGE` at 0 and 601, `INVALID_CHOICE` strategy, `/snapshot/...` pointer, 51 debts);
  identical requests byte-identical; performance.
- **Gates:** ArchUnit (domain framework-free, no floating point AC-F), JaCoCo (100% on
  `domain.projection` per 008 success metrics), existing scans.

## 5. Phases

| Phase | Work | Engineer |
| --- | --- | --- |
| 1 | 009 orderings + 008 value records (`SimulationInput`, `PayoffSimulation`, `MonthlyPeriod`, `DebtLine`) | Sonnet |
| 2 | 008 `PayoffSimulator`, Python golden script, CSV fixture, AC-2.1 to AC-2.5 tests | Sonnet |
| 3 | 010 input, validation, service, response, controller, JSON goldens, perf test | Sonnet |

Phases run in sequence in the main checkout. One Sonnet code-reviewer pass over 008–010 together, focused
on the month-loop arithmetic and the golden independence.

## 6. Risks

| Risk | Mitigation |
| --- | --- |
| Golden computed by the same logic it checks | Independent Python `decimal` script written from the requirements text, committed as evidence |
| `Money.times` rounding differs from REQ-002 | Implementer checks it first; half-cent test pins behavior |
| Large 600 × 50 JSON responses | Accepted for v1 (010 assumption); perf test covers 360 × 10 |
