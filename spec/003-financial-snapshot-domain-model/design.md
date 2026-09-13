# Design: 003–006 + 015 — Domain model, stage ladder, evaluators, architecture rules

<!-- Source: requirements.md in spec/003, 004, 005, 006, 015 -->
<!-- Ticket: none (backlog stories 003, 004, 005, 006, 015) -->
<!-- Date: 2026-09-13 -->

Combined design (lean process). The requirements files hold the detail — exact copy, boundaries,
REQ lists — and are not repeated here. This covers structure, the non-obvious decisions, and
the test plan.

## 1. Layout

```
src/main/java/com/rungs/readiness/domain/
├── model/   FinancialSnapshot, Debt, DebtType, Mortgage, HouseholdFlags, Invariants (pkg-private)
└── stage/   StageStatus, StageEvaluation, Percentages, StageEvaluator, StageLadder, LadderResult,
             StarterFundEvaluator, DebtFreeEvaluator, FullEmergencyFundEvaluator,
             RetirementEvaluator, CollegeEvaluator, MortgageEvaluator, WealthBuildingEvaluator
src/test/java/com/rungs/readiness/
├── domain/model/  …Tests, FinancialSnapshotBuilder (fixture)
├── domain/stage/  …Tests, FixedStageEvaluator (fixture), LadderAcceptanceTests (AC-1.1, AC-1.2)
└── architecture/  ArchitectureRules, ArchitectureTests, ArchitectureFixtureTests,
                   WebClientScanTests, SingleModuleScanTests
    archfixtures/  one sub-package per violated constraint
```

## 2. Story 003 — snapshot model

- Records with compact constructors. Package-private `Invariants` holds the shared checks:
  `nonNull`, `nonNegative(Money, name)`, `positive(Money, name)`, `notBlank`, and
  `percent(BigDecimal, scale, name)`.
- `percent` normalization, in this order so no step can be made expensive:
  1. `requireNonNull`.
  2. Range 0–100 by `compareTo`. This rejects `1E+999999999` before any rescale.
  3. `stripped = value.stripTrailingZeros()`. This is cheap even for `1E-999999999`, and 0 strips to scale 0.
  4. If `stripped.scale() > scale`, reject ("has more than N decimal places").
  5. Return `stripped.setScale(scale)`. The stripped scale is between −2 and `scale`, so this is exact and small.
- Messages name the component and the rule, never the value. Nulls throw
  `NullPointerException(name)`; every other rule throws `IllegalArgumentException`.
- `Debt`: the id matches `debt-[1-9][0-9]*`, `balance` is above 0, `minimumPayment` is at least 0,
  and the rate is scale 3.
- `Mortgage`: every Money component is at least 0 (a balance of $0 is allowed), and the rate is scale 3.
- `FinancialSnapshot`:
  - `debts = List.copyOf(debts)`, which also throws an NPE on a null element; order is kept.
  - Ids must be unique, checked with a `HashSet`.
  - `emergencyFundMonths` is at least 3, and `retirementContributionPercent` is scale 2.
  - `hasMortgage()` and `totalDebtBalance()` (via `Money.sum`).
- The test builder `FinancialSnapshotBuilder` defaults to a valid snapshot:
  - every Money component is $0, percent is 0, months is 3, there are no debts and no mortgage,
    and every flag is false;
  - it has `withDebt(label, type, balance, rate, min)`, which assigns positional ids.

## 3. Story 004 — framework

- `StageEvaluation` validates REQ-004:
  - `percentComplete` must be exactly scale 1, from 0.0 to 100.0, and is not normalized, since
    evaluators always get it from `Percentages`;
  - `stage` is 1–7.
- `Percentages.of(Money part, Money whole)` delegates to
  `ofRatio(BigDecimal part, BigDecimal whole)`, which:
  1. rejects a null or negative argument;
  2. returns `0.0` if `whole` is 0;
  3. returns `100.0` if `part ≥ whole`;
  4. otherwise returns `min(part × 100 ÷ whole` at scale 1 with `RoundingMode.DOWN`, `99.9)`.
- `StageLadder(Collection<StageEvaluator>)`:
  - copies the collection into an array indexed by stage;
  - throws `IllegalArgumentException` for a missing, duplicate or out-of-range stage.
- `assess` runs stages 1–7 in order:
  - passes `List.copyOf(results so far)` to each evaluator;
  - throws `IllegalStateException` if an evaluator returns another stage's number;
  - computes `currentStage` per REQ-009.
- `LadderResult(int currentStage, List<StageEvaluation> stages)` copies the list, validates 7
  entries in order and `currentStage` 1–7, and has `evaluation(int)`.

## 4. Stories 005–006 — evaluators

Stateless final classes, each with its copy in private constants built with `formatted()` and
amounts from `Money.toDisplayString()`. The rules follow the requirements exactly. Notes:

- **Stage 3:**
  - target = `expenses.times(BigDecimal.valueOf(min(months, 6)))`, which is exact because the
    factor is an integer;
  - a target of $0 is satisfied before `Percentages` is called.
- **Stage 4:** the blocker percent is `percent.stripTrailingZeros().toPlainString()` ("6",
  "6.5", "0").
- **Stage 6:** `rents` gives skipped; no mortgage or a $0 balance gives satisfied; a home value of
  $0 or at most the balance gives `NOT_STARTED` at 0.0; otherwise `Percentages.of(homeValue.minus(balance), homeValue)`.
- **Stage 7:** takes `n` = satisfied or skipped among `lowerStages` and returns
  `Percentages.ofRatio(n, 6)`. At n = 0 the status is `NOT_STARTED`.
- **Stage 2:** `IN_PROGRESS` at 0.0; the blocker reads "1 debt" or "N debts".

## 5. Story 015 — architecture rules

- Test dependency: `com.tngtech.archunit:archunit-junit5` 1.5.0.
- `ArchitectureRules` builds each rule from a root package, for example
  `AC_A_domainHasNoFrameworkImports(root)`:
  - the real test passes `com.rungs.readiness` and imports main classes with
    `ImportOption.DoNotIncludeTests`;
  - fixture tests pass `com.rungs.readiness.archfixtures.<case>`, whose sub-packages mirror
    `domain`, `application` and `adapter`, and import those fixtures only.
  - So each rule is proven to fail on its fixture and to pass on the real code (REQ-011) without
    fixtures ever leaking into the real check.
- Rules for layers that are still empty (the adapters) use `allowEmptyShould(true)`. No freeze
  store is used.
- **AC-G** (`WebClientScanTests`):
  - scans `templates/` and `static/` for the patterns in REQ-009, with `static/js/vendor/`
    excluded;
  - for the `${…}` arithmetic check, removes quoted string literals first, so a CSS class like
    `'is-current'` is not flagged;
  - hand-written JS must total at most 200 lines;
  - fixtures live in `src/test/resources/archfixtures/ac-g/`.
- **AC-H** (`SingleModuleScanTests`):
  - the scan takes a root path;
  - the real test scans the repository, skipping `target/`, `.git/` and `src/test/resources/archfixtures/`;
  - the violation fixture is built in a `@TempDir` at test time (a `package.json`,
    `node_modules/`, a nested `pom.xml`) instead of being committed.
  - **Deviation from REQ-011:** a committed nested `pom.xml` could be imported as a second project
    by IDEs.

## 6. Implementation phases

| Phase | Stories | Can run in parallel with |
| --- | --- | --- |
| 1 | 015 architecture rules | 2 (separate worktree) |
| 2 | 003 model | 1 |
| 3 | 004 framework | — (needs 2) |
| 4a / 4b | 005 evaluators 1–3 / 006 evaluators 4–7 | each other (needs 3) |
| 4c | Ladder acceptance tests AC-1.1, AC-1.2 | — (needs 4a+4b) |

Each phase gets one implementation-engineer (TDD), and each story group gets one code-reviewer
pass. The coverage gate from 019 enforces at least 85% per domain package; the requirements
target 100% for these types.

## 7. Risks

| Risk | Mitigation |
| --- | --- |
| ArchUnit 1.5.0 can't read Java 25 class files | Confirmed by the first phase 1 build; if not, drop to 1.4.1 |
| Pattern scan false positives in later templates | Literal stripping; fix by moving logic server-side (015 assumption) |
| Pathological `BigDecimal` scale in percent inputs | Range check and `stripTrailingZeros` before `setScale` (§2) |
| Parallel worktrees diverge on `pom.xml` | Only phase 1 edits `pom.xml` |
