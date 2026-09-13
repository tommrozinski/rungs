# Requirements Specification

<!-- Feature name: 007-assessment-endpoint -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 007) -->
<!-- Link: backlog/007-assessment-endpoint.md -->
<!-- Branch: main -->

## Feature Overview

`POST /v1/assessments` (PRD FR-1): the first endpoint, and the one that makes the stage
work reachable. It accepts a financial snapshot as JSON and returns the full seven-stage
ladder, the current stage, the monthly surplus, and a warnings list. It establishes the
path every later endpoint follows: a REST controller in `adapter.in.web.api`, request and
response DTOs, an `application` input model that applies the PRD FR-4 normalization rules
(blank numbers to zero, positional ids and labels, zero-balance debts removed), an
application service that runs the `StageLadder`, and a Jackson mapping that writes `Money`
in the fixed wire format. Validation rules and problem details are story 011; warnings are
story 012.

## User Stories

### Story 1: Ask "what stage am I at?"
**As** a client application developer
**I want** one request that returns the full ladder and the current stage
**So that** I can render every stage row without embedding any rules

**Acceptance Criteria:**
- [ ] `POST /v1/assessments` with a snapshot returns 200 and a `ReadinessAssessment`.
- [ ] The response carries `currentStage`, all seven stages in order, `monthlySurplus`,
  and `warnings` (an empty list until story 012).
- [ ] AC-1.1 through AC-1.4 pass as contract tests against the endpoint.

### Story 2: Forgiving input, exact output
**As** a client sending partly filled data
**I want** blank numbers treated as zero and paid-off debts ignored
**So that** incomplete forms still produce a sensible assessment

**Acceptance Criteria:**
- [ ] Missing, null, or blank numbers are treated as zero; `emergencyFundMonths` defaults
  to 3.
- [ ] A zero-balance debt is accepted and affects no figure in the response.
- [ ] Debts get server-assigned ids `debt-N` and, when their label is blank, `Debt N`.

### Story 3: A stable, exact contract
**As** a client and the golden-file tests
**I want** the response to be byte-identical for identical input
**So that** results are reproducible and diffs are meaningful

**Acceptance Criteria:**
- [ ] Money appears as `{"amount": "1234.56", "display": "$1,234.56"}` in responses.
- [ ] Every field is always present, with `null` where a value does not apply.
- [ ] Two identical requests produce byte-identical response bodies.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `AssessmentController` in `com.rungs.readiness.adapter.in.web.api` handles
  `POST /v1/assessments`, consuming and producing `application/json`, using constructor
  injection only (AC-D).
- **REQ-002**: **Request money format.** In request bodies, a money value is a plain
  decimal string (`"1234.56"`, parsed with `Money.parse`), not an object. Percentages are
  decimal strings (`"7.25"`). `emergencyFundMonths` is a JSON integer. Responses use the
  `{"amount", "display"}` object (PRD §5).
- **REQ-003**: Request shape: `monthlyTakeHomeIncome`, `monthlyEssentialExpenses`,
  `starterEmergencyFund`, `fullEmergencyFund`, `monthlyCollegeContribution`,
  `retirementContributionPercent`, `emergencyFundMonths`, `debts` (each `label`, `type`,
  `balance`, `annualInterestRate`, `minimumPayment`), `mortgage` (`balance`,
  `annualInterestRate`, `monthlyPayment`, `homeValue`, or null), and `householdFlags`
  (`rents`, `retired`, `intendsToFundCollege`).
- **REQ-004**: An `application` input model receives the mapped request and applies PRD
  FR-4 normalization before building the domain snapshot: blank/null numbers → zero
  (`emergencyFundMonths` → 3), ids `debt-N` and blank labels `Debt N` by supplied
  position, then zero-balance debts removed. Story 011 adds the validation constraints to
  this same model.
- **REQ-005**: `AssessmentService` in `application` builds a `StageLadder` with the seven
  evaluators, assesses the snapshot, computes the monthly surplus, and returns a
  `ReadinessAssessment` result.
- **REQ-006**: `FinancialSnapshot.monthlySurplus()` (added to the domain model by this
  story) returns income − essential expenses − the sum of debt minimum payments. The
  mortgage payment is not subtracted (PRD FR-1). The result may be negative.
- **REQ-007**: Response shape:
  `{"currentStage": 1, "stages": [{"stage", "status", "percentComplete",
  "remainingAmount", "blocker", "skipReason"}, …], "monthlySurplus": {…}, "warnings": []}`.
  `percentComplete` is a scale-1 decimal string (`"50.0"`). `remainingAmount` is a money
  object or `null`. All keys are always present.
- **REQ-008**: A Jackson 3 serializer for `Money` lives in `adapter.in.web.api`; domain
  types carry no Jackson annotations (AC-A). Property order is fixed so output is
  byte-identical (NFR-4).
- **REQ-009**: MockMvc contract tests assert the exact JSON for AC-1.1, AC-1.2, AC-1.3,
  and AC-1.4, blank-as-zero for each numeric field, zero-balance debt removal, and
  positional ids and labels.

### Should Have (P1)
- **REQ-010**: A golden-file response for the AC-1.2 household is committed under
  `src/test/resources`, so response-shape changes show up as reviewable diffs.

### Nice to Have (P2)
- **REQ-011**: A microbenchmark-free latency check: a test asserting a warm assessment
  completes well under 50ms, as a smoke signal for NFR-3.

## Non-Functional Requirements

### Performance
- p95 under 50ms for `/v1/assessments`, measured locally (PRD NFR-3).

### Security
- No household figures are logged; request bodies are never logged (PRD NFR-2, story 018).
- Nothing is stored; the request is discarded after the response (PRD NFR-1).

### Accessibility / Browser Support
- Not applicable: JSON API. Blockers and skip reasons are rendered verbatim by clients.

## Constraints and Assumptions

### Technical Constraints
- Spring Boot 4.1 with Jackson 3 (`tools.jackson`) in the adapter only.
- PRD AC-C: the REST adapter depends on `application`, never on the view adapter.
- Money and percentage wire formats are fixed by PRD §5 and REQ-002.

### Assumptions
- Until story 011 lands, malformed input may produce a generic 400; problem details and
  field-level errors are added there.
- Story 012 fills `warnings`; the key is present and empty until then.

## Out of Scope
- Validation constraints, unknown-property rejection, and RFC 9457 problem details —
  story 011.
- Warning detection — story 012.
- `X-Request-Id` correlation header and structured logging — story 018.
- OpenAPI documentation — story 016.
- Metrics — story 017.
- The assessment web page — story 029.

## Success Metrics
- AC-1.1 to AC-1.4 pass through the HTTP layer.
- The AC-1.2 golden file matches byte-for-byte on repeated runs.
- No Jackson import exists in `domain` (verified later by story 015).
- 100% line coverage on the controller, input model normalization, and service.
