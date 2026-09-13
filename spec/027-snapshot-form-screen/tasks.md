# Tasks: 027 + 029 — Snapshot form screen and assessment screen

<!-- Source: spec/027-snapshot-form-screen/design.md -->
<!-- Ticket: none (backlog stories 027, 029) -->
<!-- Date: 2026-09-13 -->

## Phase 1: Example households

- [x] Add application ExampleHouseholds with the three fixed SnapshotInput presets from design §3 (byId returning Optional, ids in order) and ExampleHouseholdsTests asserting ids, every value, unknown id, and each preset's current stage via AssessmentService
  - ID 1.1 · Depends on: none

## Phase 2: Snapshot form (027)

- [x] Add layout.html with vendored htmx, SnapshotInput.forForm, FormErrors, SnapshotFormController (GET/POST /, add/remove debt row, on-blur field, example presets), snapshot form templates and debts-table/field fragments, the mortgage toggle script, and MockMvc + Jsoup view tests including API message parity
  - ID 2.1 · Depends on: 1.1

## Phase 3: Assessment screen (029)

- [x] Add AssessmentPage and AssessmentService.assessPage, AssessmentPageController (POST /assessment, form re-render on validation failure), the assessment page template with verdict, stat cards, warnings, ladder, and hidden snapshot fields, change CHANGE_NUMBERS_ACTION to "/", and the figures-match test for the three example households
  - ID 3.1 · Depends on: 2.1
