# Requirements Specification

<!-- Feature name: 012-assessment-warnings -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 012) -->
<!-- Link: backlog/012-assessment-warnings.md -->
<!-- Branch: main -->

## Feature Overview

Non-fatal warnings (PRD FR-5): conditions that make an answer questionable without making
the request invalid. There are three — expenses exceed income, a debt whose minimum
payment does not cover its interest, and an emergency-fund target capped at 6 months. (The
fourth, `MORTGAGE_IN_DEBT_LIST`, was removed on 2026-09-13 because mortgage-type debts are
now rejected.) Detection is domain logic; every response from the assessment, projection,
and comparison endpoints carries the same `warnings` list. Messages are user-facing copy
rendered in the gold caution banner above the ladder, so they name the numbers and never
alarm.

## User Stories

### Story 1: Know when the numbers need a caveat
**As** a household whose inputs describe an unsustainable situation
**I want** a plain warning above my results
**So that** I read the ladder and payoff plan in the right light

**Acceptance Criteria:**
- [ ] Each of the three conditions produces its warning with a specific message.
- [ ] A warned response is still 200; warnings never fail a request.
- [ ] A response with no conditions has an empty `warnings` array, never null.

### Story 2: Machine-readable and stable
**As** a client application developer
**I want** warnings with stable codes and a severity
**So that** I can style and handle them without parsing text

**Acceptance Criteria:**
- [ ] Each warning has `code`, `severity`, `message`, and `debtIds`.
- [ ] Warning order is deterministic.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `WarningCode` enum (in `domain.model`): `EXPENSES_EXCEED_INCOME`,
  `NEGATIVE_AMORTIZATION`, `EMERGENCY_FUND_TARGET_CAPPED`, in that order.
- **REQ-002**: `Warning` record (in `domain.model`): `code`, `severity` (`Severity` enum
  with the single value `CAUTION`), `message`, `debtIds` (`List<String>`, empty when not
  debt-specific).
- **REQ-003**: `WarningDetector.detect(FinancialSnapshot)` in `domain.stage` returns warnings
  sorted by code order:
  - `EXPENSES_EXCEED_INCOME` when `monthlySurplus()` is negative. Message: "Your essential
    expenses and minimum payments are {shortfall} more than your take-home pay each month."
  - `NEGATIVE_AMORTIZATION` when one or more debts have a minimum payment below one month's
    interest on their current balance (same interest formula as story 008). One warning
    lists every affected debt id in supplied order. Message (one debt): "The minimum
    payment on {label} doesn't cover its monthly interest, so the balance grows."; (several):
    "The minimum payments on {label}, {label}, and {label} don't cover their monthly
    interest, so those balances grow."
  - `EMERGENCY_FUND_TARGET_CAPPED` when `emergencyFundMonths` > 6. Message: "Emergency fund
    targets are capped at 6 months, so your target uses 6 months instead of {n}."
- **REQ-004**: `warnings` is populated on `/v1/assessments`, `/v1/projections`, and once at
  the top level of `/v1/projections/comparison`.
- **REQ-005**: JSON shape: `{"code", "severity", "message", "debtIds"}`; all keys present.
- **REQ-006**: Severity is always `CAUTION`: red is reserved for input validation, and a
  household behind on its goals is never shown red (design spec §2.1).

### Should Have (P1)
- **REQ-007**: A test per condition, plus one combining all three to prove order.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- Detection is O(number of debts), no I/O.

### Security
- Messages include household figures and debt labels; they are response content only and
  never logged (PRD NFR-2).

### Accessibility / Browser Support
- Messages are complete sentences; the banner conveys severity with text, not color alone
  (story 033).

## Constraints and Assumptions

### Technical Constraints
- Pure domain code for detection (PRD AC-A, AC-F).
- Interest calculation matches story 008 exactly, so the warning agrees with the schedule.

### Assumptions
- CSV unknown-key warnings are a separate concept owned by the `ImportReport` (story 021)
  and do not use this type.
- `Severity` may gain values later; clients must treat unknown severities as caution.

## Out of Scope
- Excluding negative-amortizing debts from the completion estimate — story 008.
- The warning banner component and placement — stories 025, 029.
- Import-report warnings — story 021.

## Success Metrics
- Each message in REQ-003 asserted verbatim, including the one-debt and three-debt
  variants of `NEGATIVE_AMORTIZATION`.
- Contract tests show warnings on all three endpoints.
- 100% line and branch coverage on `WarningDetector`.
