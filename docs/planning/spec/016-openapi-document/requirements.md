# Requirements Specification

<!-- Feature name: 016-openapi-document -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 016) -->
<!-- Link: backlog/016-openapi-document.md -->
<!-- Branch: main -->

## Feature Overview

An OpenAPI 3.1 document generated from the code and committed to the repository, so every
contract change arrives as a reviewable diff (PRD NFR-6), plus the Architecture Decision
Records the PRD asks for on every structural decision. The document is the contract of
record for API consumers; the web client is a consumer of the same application services.
A test regenerates the document and fails the build when the committed copy has drifted.

## User Stories

### Story 1: A contract that can't drift
**As** a client application developer
**I want** a committed OpenAPI document that always matches the running code
**So that** I can rely on it as the API's interface

**Acceptance Criteria:**
- [ ] `docs/openapi/openapi.json` is generated from the code and committed.
- [ ] A test fails when the generated document differs from the committed one, and says
  how to update it.

### Story 2: Decisions written down
**As** the engineering reviewer
**I want** an ADR for each structural decision
**So that** I can see why the system is shaped the way it is

**Acceptance Criteria:**
- [ ] ADRs exist in `docs/adr/` for every decision listed in REQ-006.
- [ ] Each ADR states context, decision, alternatives considered, and consequences.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `springdoc-openapi` (the release line supporting Spring Boot 4.1) generates
  OpenAPI 3.1 during tests. The docs endpoint is enabled only in the test profile; the
  running application exposes no docs endpoint and no Swagger UI.
- **REQ-002**: A test fetches the generated document, normalizes it (sorted keys, stable
  formatting), and compares it with `docs/openapi/openapi.json`. On mismatch it fails with
  a diff summary and the command to regenerate (`./mvnw test -Dopenapi.update=true`),
  which rewrites the file only when that flag is set.
- **REQ-003**: The document covers every endpoint that exists: `/v1/assessments`,
  `/v1/projections`, `/v1/projections/comparison`, and, once stories 021–022 land,
  `/v1/snapshots/import` and `/v1/snapshots/export` with their `text/csv` media types and
  `Content-Disposition` header.
- **REQ-004**: Schemas state the contract decisions explicitly: request money as a decimal
  string; response money as `{amount, display}`; percentages as decimal strings; debt ids
  server-assigned and never accepted; nullable fields marked; enums enumerated (stage
  status, debt type without `MORTGAGE`, strategy, warning codes, severity, validation
  codes, delta verdicts, note codes).
- **REQ-005**: Every endpoint documents its 400 problem responses: `type` URNs, the
  `errors` array, and JSON Pointer field locations. The `strategy` description states the
  tie-break rule (ties keep supplied order; order fixed at month zero).
- **REQ-006**: ADRs in `docs/adr/NNNN-title.md` (context / decision / alternatives /
  consequences):
  0001 ports-and-adapters layering; 0002 money as `long` cents and its wire format;
  0003 statelessness and no persistence; 0004 server-side CSV parsing; 0005 server-rendered
  Thymeleaf client with htmx in one Maven module (including the reason for not using
  React); 0006 view controllers call application services in-process; 0007 JavaScript
  policy; 0008 reversal of the "no front end" non-goal; 0009 Java 25 and Spring Boot 4.1;
  0010 blank-as-zero and zero-balance debt normalization; 0011 server-assigned positional
  debt ids; 0012 fixed-order payoff strategies and interest-first accrual.
- **REQ-007**: Public-facing descriptions use neutral vocabulary; no trademarked framework
  names appear in the document or ADRs.

### Should Have (P1)
- **REQ-008**: `docs/adr/README.md` indexes the ADRs with one-line summaries.

### Nice to Have (P2)
- **REQ-009**: Request and response examples in the document for the AC-1.2 household.

## Non-Functional Requirements

### Performance
- Document generation adds under 5 seconds to the test run.

### Security
- No docs endpoint or Swagger UI in the running application, keeping the exposed surface
  to the API and Actuator endpoints.

### Accessibility / Browser Support
- Not applicable.

## Constraints and Assumptions

### Technical Constraints
- OpenAPI 3.1; JSON format for the committed document.
- The design phase must confirm the springdoc release compatible with Spring Boot 4.1 and
  Jackson 3.

### Assumptions
- The CSV endpoints are added to the document by stories 021–022 when they land; this
  story's drift test catches their omission.

## Out of Scope
- Hosting or rendering the document.
- Client SDK generation.

## Success Metrics
- The drift test passes on a clean checkout and fails after any controller or DTO change
  until the document is regenerated.
- All twelve ADRs exist and are linked from the index.
- A search of `docs/` finds no trademarked framework names.
