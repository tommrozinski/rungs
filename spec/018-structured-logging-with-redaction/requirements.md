# Requirements Specification

<!-- Feature name: 018-structured-logging-with-redaction -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 018) -->
<!-- Link: backlog/018-structured-logging-with-redaction.md -->
<!-- Branch: main -->

## Feature Overview

Structured JSON logs with a correlation id on every request, built so that no household
figure, debt label, filename, CSV row, or request body can reach a log line (PRD NFR-2,
backed by NFR-1). Logging is the most likely place for the "stores nothing" guarantee to
leak — an exception handler that dumps a body, or a debug statement added during a bug
hunt — so this story makes leaks structurally hard: domain and request types redact
themselves in `toString`, parser exception messages are never logged, and a test drives
full requests with distinctive values through the app and proves none appear in the logs,
even at debug level.

## User Stories

### Story 1: Traceable requests
**As** an operator investigating a failure
**I want** every log line tagged with a request id that is also returned to the client
**So that** a user's report can be matched to the logs without any household data

**Acceptance Criteria:**
- [ ] Every response carries an `X-Request-Id` header.
- [ ] Every log line during a request includes that id.

### Story 2: Logs that cannot leak
**As** a household using the service
**I want** my figures never written to logs
**So that** "nothing is stored" is true in practice

**Acceptance Criteria:**
- [ ] A test with distinctive figures and labels finds none of them in any log output,
  at INFO and at DEBUG for Spring web and the application packages.
- [ ] Domain, input-model, and DTO `toString` output shows no values.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: Console logs use Spring Boot's structured logging in ECS format
  (`logging.structured.format.console=ecs`).
- **REQ-002**: A servlet filter in `adapter.in.web` reads `X-Request-Id`; if it matches
  `^[A-Za-z0-9._-]{1,64}$` it is used, otherwise a random UUID is generated. The id is put
  in the MDC for the request and echoed in the `X-Request-Id` response header.
- **REQ-003**: `toString` on `Money`, `Debt`, `Mortgage`, `FinancialSnapshot`, request
  DTOs, and application input models shows structure only — ids, types, counts — and
  `<redacted>` in place of amounts, percentages, and labels (e.g.
  `Debt[id=debt-1, type=AUTO, balance=<redacted>]`).
- **REQ-004**: Tests register an AssertJ representation that prints real values, so test
  failure messages stay readable without weakening production `toString`.
- **REQ-005**: The problem-details advice (story 011) and any error logging record the
  exception class, the bounded code, the request id, and the route — never exception
  messages from JSON binding, parsing, or CSV reading, and never the request body or
  uploaded bytes.
- **REQ-006**: No request-body logging filter is registered (for example
  `CommonsRequestLoggingFilter`), in any profile.
- **REQ-007**: CSV parse failures log a reason code and line number only (stories 020–021).
- **REQ-008**: Allowed in logs: stage verdicts, debt count, row counts, validation codes,
  latency, route, status code.
- **REQ-009**: A leak test posts an assessment, a projection, and (once story 021 lands) a
  CSV import containing the sentinel values `987654.32`, `12.345`-style rates, and the
  label `ZebraSentinelLoan`, with DEBUG enabled for `org.springframework.web` and
  `com.rungs`, and asserts no captured log line contains any sentinel.

### Should Have (P1)
- **REQ-010**: A malformed-JSON request containing a sentinel is part of the leak test,
  proving parser messages are not logged.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- The filter and MDC add negligible latency.

### Security
- Inbound request ids are length- and character-restricted, so they cannot inject log
  structure or carry data.
- Redaction is structural (`toString`, no body logging, no parser messages), so changing
  log levels cannot defeat it; the leak test runs at DEBUG to prove that.

### Accessibility / Browser Support
- Not applicable.

## Constraints and Assumptions

### Technical Constraints
- Random UUID generation happens in the adapter; `domain` stays free of randomness
  (story 015).
- No specific log aggregator; ECS is a widely supported default.

### Assumptions
- Debt count is an acceptable aggregate to log (decided 2026-09-13).
- Blocker and warning messages contain figures and are response content only.

## Out of Scope
- Metrics — story 017.
- Log shipping or retention.
- Tracing (`traceparent`).

## Success Metrics
- The leak test passes at DEBUG for all covered requests.
- Tests assert `X-Request-Id` echo for a valid inbound id, replacement of an invalid one,
  and generation when absent.
- `toString` tests for each redacted type.
