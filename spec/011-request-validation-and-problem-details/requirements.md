# Requirements Specification

<!-- Feature name: 011-request-validation-and-problem-details -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 011) -->
<!-- Link: backlog/011-request-validation-and-problem-details.md -->
<!-- Branch: main -->

## Feature Overview

Every validation rule in PRD FR-4, declared once with Jakarta Bean Validation on the input
models in `application` and applied by both inbound adapters, plus the RFC 9457 problem
detail responses the API returns when a request is rejected. Each field-level error
carries a JSON Pointer to the field, a stable code, and a user-facing message that the web
form renders verbatim, so the API and the form can never disagree. Rules are classified
as field-only (checked on blur by the form) or whole-form (checked on add-row and submit).
Nothing in an error body or a log contains a household figure.

## User Stories

### Story 1: Precise, fixable errors
**As** a client application developer
**I want** every invalid field reported with a pointer, a code, and a message
**So that** I can highlight exactly the inputs that need fixing

**Acceptance Criteria:**
- [ ] A rejected request returns 400 `application/problem+json` with `type`, `title`,
  `status`, `detail`, and `errors: [{"pointer", "code", "message"}]`.
- [ ] All violations are reported in one response, ordered by pointer.
- [ ] Pointers are JSON Pointers (`/debts/2/balance`; `/snapshot/debts/2/balance` on
  projection endpoints).

### Story 2: One rule set for API and form
**As** the web form (story 027)
**I want** to validate a single field with the same rules and messages as the API
**So that** on-blur errors match submit errors exactly

**Acceptance Criteria:**
- [ ] Constraints live only on `application` input models.
- [ ] Any field-only rule can be evaluated for one property and yields the same message.
- [ ] The 50-debt rule is whole-form and counts debts after zero-balance removal.

### Story 3: Strict about shape, forgiving about blanks
**As** an API consumer who mistypes a field name
**I want** unknown properties rejected
**So that** a typo never silently becomes zero

**Acceptance Criteria:**
- [ ] Unknown JSON properties, including a client-supplied debt `id`, are rejected.
- [ ] Missing, null, or blank numbers are still treated as zero (or their documented
  default) and are not errors.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: Constraints are declared on input models in `application` (never on domain
  records or REST DTOs). Adapters trigger validation; they define no rules.
- **REQ-002**: Field-only rules, each with a code and message:
  | Rule | Code | Message |
  | --- | --- | --- |
  | Any money value negative | `NEGATIVE` | "Enter zero or more." |
  | Money string not parseable | `INVALID_AMOUNT` | "Enter an amount like 1,234.56." |
  | Percentage outside 0–100 | `PERCENT_RANGE` | "Enter a percentage from 0 to 100." |
  | Rate with more than 3 decimals / retirement percent with more than 2 | `TOO_PRECISE` | "Use at most {n} decimal places." |
  | `emergencyFundMonths` below 3 | `MONTHS_MIN` | "Enter 3 months or more." |
  | `horizonMonths` outside 1–600 | `HORIZON_RANGE` | "Enter a horizon from 1 to 600 months." |
  | Unknown `strategy` | `INVALID_CHOICE` | "Choose snowball or avalanche." |
  | Debt `type` of `MORTGAGE` | `MORTGAGE_AS_DEBT` | "Enter your mortgage in the mortgage section, not as a debt." |
  | Unknown debt `type` | `INVALID_CHOICE` | "Choose a debt type from the list." |
- **REQ-003**: Whole-form rule: more than 50 debts with a balance above zero → code
  `TOO_MANY_DEBTS`, pointer `/debts`, message "Enter 50 debts or fewer."
- **REQ-004**: Adapter-only rules (REST JSON binding): unknown properties (including debt
  `id`) → code `UNKNOWN_FIELD` with that property's pointer; wrong JSON type (e.g. a
  number where a string is expected) → `INVALID_TYPE`.
- **REQ-005**: Malformed JSON returns 400 with `type` `urn:rungs:problem:malformed-request`
  and no `errors` array; never a stack trace.
- **REQ-006**: Validation failures use `type` `urn:rungs:problem:validation`, `title`
  "Some fields need attention.", and `detail` "{n} field(s) need attention."
- **REQ-007**: Order of operations: JSON binding → blank-to-default normalization (FR-4)
  → field-only rules → positional ids/labels and zero-balance removal → whole-form rules.
  Negative balances are rejected even on debts that would be removed.
- **REQ-008**: Each field-only constraint can be evaluated alone (e.g.
  `Validator.validateProperty`), producing the same code and message as full validation.
- **REQ-009**: Codes are a bounded enum, so metrics (017) and logs (018) can reference
  them without free text.
- **REQ-010**: Problem bodies and logs never include submitted values.
- **REQ-011**: Applies to `/v1/assessments`, `/v1/projections`, and
  `/v1/projections/comparison`; pointers are prefixed with `/snapshot` where the snapshot
  is nested.

### Should Have (P1)
- **REQ-012**: A single `@RestControllerAdvice` in `adapter.in.web.api` produces every
  problem response, so the shape cannot drift between endpoints.

### Nice to Have (P2)
- **REQ-013**: Message templates live in one resource bundle-free constants class, so copy
  review happens in one file.

## Non-Functional Requirements

### Performance
- Validation adds no measurable cost against the NFR-3 budgets; no I/O.

### Security
- Unknown-property rejection is enabled globally for request bodies (FR-4).
- No submitted value appears in any response error or log line (PRD NFR-2).

### Accessibility / Browser Support
- Messages are plain, specific, and actionable, following the design spec voice rules;
  the form associates each with its input (story 033).

## Constraints and Assumptions

### Technical Constraints
- Jakarta Bean Validation (`spring-boot-starter-validation`) is allowed in `application`
  and adapters, never in `domain` (PRD AC-A).
- JSON Pointer per RFC 6901; problem details per RFC 9457.

### Assumptions
- The CSV import has its own row-level report (story 021) and problem type for a bad
  version marker; it reuses the codes here where they apply.
- Form-post validation for the Thymeleaf pages reuses these models (story 027).

## Out of Scope
- CSV row validation and `ImportReport` — stories 020, 021.
- On-blur form wiring — story 027.
- OpenAPI documentation of problem responses — story 016.

## Success Metrics
- A contract test per code in REQ-002–REQ-004, asserting status, `type`, pointer, code,
  and message.
- A request with several violations returns them all, in pointer order.
- A test proves a misspelt numeric property is rejected, not defaulted to zero.
- A test proves no submitted value appears in the problem body.
- 100% line coverage on the input models' constraints and the controller advice.
