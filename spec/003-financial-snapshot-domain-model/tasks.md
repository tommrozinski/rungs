# Tasks: 003–006 + 015 — Domain model, stage ladder, evaluators, architecture rules

<!-- Source: spec/003-financial-snapshot-domain-model/design.md -->
<!-- Ticket: none (backlog stories 003, 004, 005, 006, 015) -->
<!-- Date: 2026-09-13 -->

## Phase 1: Architecture rules (015)

- [x] Add archunit-junit5 1.5.0 and ArchitectureRules (AC-A to AC-F, determinism) built from a root package, with ArchitectureTests on main classes and ArchitectureFixtureTests proving each rule fails on its archfixtures case
  - ID 1.1 · Depends on: none
- [x] Add WebClientScanTests (AC-G patterns, literal stripping, vendor exclusion, 200-line JS budget) with resource fixtures, and SingleModuleScanTests (AC-H) with a @TempDir violation fixture
  - ID 1.2 · Depends on: none

## Phase 2: Snapshot model (003)

- [x] Add DebtType, HouseholdFlags, Mortgage, Debt, FinancialSnapshot with Invariants (percent normalization per design §2), hasMortgage, totalDebtBalance, and FinancialSnapshotBuilder, with tests for every invariant, scale equality (7.25/7.250/7.2500), over-precision rejection, and debt order
  - ID 2.1 · Depends on: none

## Phase 3: Stage framework (004)

- [x] Add StageStatus, StageEvaluation (REQ-004 combinations), Percentages.of/ofRatio, StageEvaluator, StageLadder, LadderResult, and FixedStageEvaluator, with rejecting tests per forbidden combination and ladder tests (all satisfied, skipped below unsatisfied, satisfied above current, missing/duplicate/wrong-stage evaluator)
  - ID 3.1 · Depends on: 2.1

## Phase 4: Evaluators (005, 006)

- [ ] Add StarterFundEvaluator, DebtFreeEvaluator, FullEmergencyFundEvaluator with boundary tests and verbatim blockers
  - ID 4.1 · Depends on: 3.1
- [ ] Add RetirementEvaluator, CollegeEvaluator, MortgageEvaluator, WealthBuildingEvaluator with boundary, skip-precedence, and verbatim copy tests
  - ID 4.2 · Depends on: 3.1
- [ ] Add LadderAcceptanceTests for AC-1.1 and AC-1.2 through the real seven-evaluator StageLadder
  - ID 4.3 · Depends on: 4.1, 4.2
