# Requirements Specification

<!-- Feature name: 003-financial-snapshot-domain-model -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 003) -->
<!-- Link: backlog/003-financial-snapshot-domain-model.md -->
<!-- Branch: main -->

## Feature Overview

The immutable domain types that describe a household at a point in time —
`FinancialSnapshot`, `Debt`, `DebtType`, `Mortgage`, and `HouseholdFlags` — in
`domain.model`. Every evaluator, the payoff simulation, and CSV import/export read these
types, and the CSV round-trip property (AC-7.1) depends on their value equality, so
equality and scale handling are hard requirements, not incidentals. The types are pure
data with no framework coupling; mapping from JSON, forms, and CSV lives in `application`
and the adapters. This spec also fixes four model decisions made on 2026-09-13: interest
rates are percentages, debt ids are server-assigned by position, stage 5 reads a new
`monthlyCollegeContribution`, and stage 3 reads a new `emergencyFundMonths`.

## User Stories

### Story 1: One trustworthy model of a household
**As** a developer writing evaluators and the payoff simulation
**I want** immutable, always-valid snapshot types
**So that** no rule has to defend against nulls, mutation, or impossible values

**Acceptance Criteria:**
- [ ] All five types are records (or an enum) in `com.rungs.readiness.domain.model`.
- [ ] Constructors reject nulls (except `mortgage`) and structurally impossible values
  with `IllegalArgumentException` or `NullPointerException`.
- [ ] The debt list is defensively copied and unmodifiable, and preserves the order
  supplied.
- [ ] No Spring, Jakarta, or Jackson import appears in any of these types.

### Story 2: Equality that survives a round trip
**As** the CSV round-trip property (story 023)
**I want** value equality that ignores representation noise
**So that** export-then-import compares equal whenever the household is the same

**Acceptance Criteria:**
- [ ] Record equality holds across the whole graph, including the debt list and mortgage.
- [ ] `annualInterestRate` is normalized to scale 3 and `retirementContributionPercent`
  to scale 2 on construction, so `7.25`, `7.250`, and `7.2500` construct equal values.
- [ ] Values with more precision than the scale allows are rejected, not rounded.

### Story 3: Fields the stage rules actually need
**As** the stage 3 and stage 5 evaluators (stories 005, 006)
**I want** explicit fields for the emergency-fund multiple and the college contribution
**So that** both stages can be evaluated as the PRD describes

**Acceptance Criteria:**
- [ ] `FinancialSnapshot.emergencyFundMonths` is an `int`, at least 3.
- [ ] `FinancialSnapshot.monthlyCollegeContribution` is a `Money`.
- [ ] `dependentsInCollegePlan` does not exist.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `FinancialSnapshot` has components `monthlyTakeHomeIncome`,
  `monthlyEssentialExpenses`, `starterEmergencyFund`, `fullEmergencyFund`,
  `monthlyCollegeContribution` (all `Money`), `retirementContributionPercent`
  (`BigDecimal`), `emergencyFundMonths` (`int`), `debts` (`List<Debt>`), `mortgage`
  (`Mortgage`, nullable), and `householdFlags` (`HouseholdFlags`).
- **REQ-002**: `Debt` has components `id` (`String`), `label` (`String`), `type`
  (`DebtType`), `balance` (`Money`), `annualInterestRate` (`BigDecimal`), and
  `minimumPayment` (`Money`).
- **REQ-003**: `DebtType` is an enum: `STUDENT_LOAN`, `AUTO`, `CREDIT_CARD`, `MEDICAL`,
  `PERSONAL`, `OTHER`. There is no `MORTGAGE` value; a mortgage is only ever the separate
  `mortgage` component (decided 2026-09-13 in story 006).
- **REQ-004**: `Mortgage` has components `balance`, `monthlyPayment`, `homeValue` (all
  `Money`) and `annualInterestRate` (`BigDecimal`).
- **REQ-005**: `HouseholdFlags` has boolean components `rents`, `retired`, and
  `intendsToFundCollege`.
- **REQ-006**: Interest rates are annual percentages (`7.25` means 7.25%), normalized to
  scale 3, between 0 and 100 inclusive. `retirementContributionPercent` is normalized to
  scale 2, between 0 and 100 inclusive. More decimal places than the scale is rejected.
- **REQ-007**: `Debt.id` matches `debt-<n>` with `n` ≥ 1, and ids are unique within a
  snapshot. Ids are assigned by the `application` input models from each debt's 1-based
  position in the list as supplied, before zero-balance debts are removed — the same
  position used for generated labels (PRD FR-4). The domain never generates ids.
- **REQ-008**: Structural invariants enforced on construction: no money component is
  negative; each debt's `balance` is above zero (zero-balance debts never reach the
  domain); `label` is not blank; `emergencyFundMonths` ≥ 3; every component except
  `mortgage` is non-null.
- **REQ-009**: `emergencyFundMonths` stores the value as supplied, with no upper clamp.
  The stage 3 evaluator caps it at 6, and warning detection (story 012) flags values
  above 6.
- **REQ-010**: `debts` is copied with `List.copyOf` on construction; order is preserved
  exactly, because tie-breaking (story 009) and positional ids depend on it.
- **REQ-011**: Equality is plain record equality, made reliable by the normalization in
  REQ-006.

### Should Have (P1)
- **REQ-012**: `FinancialSnapshot.hasMortgage()` returns whether a `mortgage` is present,
  so callers do not null-check the component directly.
- **REQ-013**: Test fixtures provide a builder (in test sources only) that produces a
  valid default snapshot, so evaluator tests set only the fields they care about.

### Nice to Have (P2)
- **REQ-014**: `FinancialSnapshot.totalDebtBalance()` returns the sum of all debt
  balances, since stage 2 and the projection summaries both need it.

## Non-Functional Requirements

### Performance
- Construction is cheap and allocation-light; a snapshot of 50 debts is built in
  microseconds, since projections and property tests construct many.

### Security
- These types are household data. Log redaction, including whether record `toString`
  must hide values, is decided in story 018; nothing in this story logs a snapshot.
- Invariant-violation messages name the component and the rule, never the value.

### Accessibility / Browser Support
- Not applicable: no UI.

## Constraints and Assumptions

### Technical Constraints
- Pure domain code: no Spring, Jakarta, or Jackson (PRD AC-A); records only (AC-E); no
  floating-point types (AC-F).
- Money uses the `Money` type from story 002.
- Wire and CSV representations (PRD §5): percentages are decimal strings; debt ids are
  never accepted from clients; CSV keys `emergency_fund_months` and `college_contribution`.

### Assumptions
- Input rules that produce user-facing errors (negative values, over 50 debts, percent
  ranges) are enforced first by validation in `application` (story 011); the domain
  invariants in REQ-008 are a second line of defense and indicate a programming error if
  they ever fire.
- Blank-as-zero, zero-balance debt removal, positional labels, and positional ids all
  happen in `application` before construction (PRD FR-4).
- `HouseholdFlags.retired` has no stage rule yet; whether it causes a skip is decided in
  story 006.

## Out of Scope
- Mapping from JSON, form posts, and CSV rows — stories 007, 011, 020, 027.
- Validation messages and problem details — story 011.
- Stage evaluation using these fields — stories 004–006.
- Warning detection — story 012.
- Mortgage representation in CSV — story 020.
- Log redaction — story 018.

## Success Metrics
- 100% line and branch coverage on the five types.
- Equality tests prove `7.25`, `7.250`, and `7.2500` construct equal debts, and that
  reordering the debt list produces an unequal snapshot.
- Every invariant in REQ-008 has a test that expects rejection.
- No Spring, Jakarta, Jackson, `double`, or `float` reference exists in the types or
  their tests.
