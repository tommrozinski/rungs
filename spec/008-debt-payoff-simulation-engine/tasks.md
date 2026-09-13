# Tasks: 008 + 009 + 010 — Payoff simulation, strategies, projection endpoint

<!-- Source: spec/008-debt-payoff-simulation-engine/design.md -->
<!-- Ticket: none (backlog stories 008, 009, 010) -->
<!-- Date: 2026-09-13 -->

## Phase 1: Orderings and value records

- [x] Add domain.projection PayoffOrdering, Strategy (SNOWBALL, AVALANCHE with stable supplied-order tie-break), SimulationInput, PayoffSimulation, MonthlyPeriod, DebtLine records with validation, plus ordering and record tests (009 REQ-001 to REQ-008)
  - ID 1.1 · Depends on: none

## Phase 2: Payoff simulator (008)

- [x] Add PayoffSimulator implementing 008 REQ-002 to REQ-010, the independent Python golden script and AC-2.1 CSV fixture, FixedOrder test ordering, per-step, AC-2.2 to AC-2.5, invariant, and determinism tests
  - ID 2.1 · Depends on: 1.1

## Phase 3: Projection endpoint (010)

- [x] Add ProjectionInput with HorizonMonths and StrategyName validation, SnapshotValidator.validate(ProjectionInput), StageLadder bean, ProjectionService, projection response records, ProjectionController POST /v1/projections, both golden JSON files, contract and performance tests
  - ID 3.1 · Depends on: 2.1
