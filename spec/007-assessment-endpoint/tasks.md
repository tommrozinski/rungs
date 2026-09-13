# Tasks: 007 + 011 — Assessment endpoint, validation, and problem details

<!-- Source: spec/007-assessment-endpoint/design.md -->
<!-- Ticket: none (backlog stories 007, 011) -->
<!-- Date: 2026-09-13 -->

## Phase 1: Assessment endpoint (007)

- [ ] Add FinancialSnapshot.monthlySurplus, application input records with FR-4 normalization, AssessmentService and ReadinessAssessment, ApplicationConfiguration, AssessmentController with response records and Jackson configuration, MockMvc contract tests (AC-1.1–1.4, blank-as-zero, zero-balance removal, positional ids/labels, byte-identical output), and the AC-1.2 golden file
  - ID 1.1 · Depends on: none

## Phase 2: Validation and problem details (011)

- [ ] Add spring-boot-starter-validation, ValidationCode, field and whole-form constraints with group sequence, ProblemDetailsAdvice (validation, unknown field, wrong type, malformed), numeric pointer ordering, and a contract test per code plus ordering, no-value-leak, misspelt-field, and validateProperty parity tests
  - ID 2.1 · Depends on: 1.1
