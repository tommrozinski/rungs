# Requirements Specification

<!-- Feature name: 027-snapshot-form-screen -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 027) -->
<!-- Link: backlog/027-snapshot-form-screen.md -->
<!-- Branch: main -->

## Feature Overview

S-01, the only input screen (design spec §4): cards for money in and out, debts, and
household (including the mortgage when the household does not rent), with a subhead that
states nothing is stored. Debts are a table with add and remove actions swapped in by htmx.
Leaving a field validates that field on the server; adding a row checks the 50-debt limit;
submitting runs every rule and moves to the assessment. "Load an example" loads one of
three preset households through the same import path a user file takes. The form is the
working copy of the snapshot, carried between pages in hidden fields.

## User Stories

### Story 1: Enter my household quickly
**As** a household
**I want** one form for my income, expenses, debts, and household situation
**So that** I can see my stage without typing more than necessary

**Acceptance Criteria:**
- [ ] Three cards plus a mortgage section shown when "I rent" is off.
- [ ] Blank number fields count as zero; paid-off ($0) debt rows stay visible and are
  ignored.
- [ ] "Check my stage" submits the form and shows the assessment.

### Story 2: Errors that match the rules exactly
**As** a household making a typo
**I want** an inline error as soon as I leave a field
**So that** I can fix it before submitting

**Acceptance Criteria:**
- [ ] On blur, htmx posts the field and swaps in the server-rendered field with any error.
- [ ] Messages are identical to the API's (story 011).

### Story 3: See it working in one click
**As** a reviewer
**I want** preset example households
**So that** I can see results without typing

**Acceptance Criteria:**
- [ ] "Load an example" offers three households: deep in debt, mid-journey, nearly done.
- [ ] Each loads through the same import service as a user file.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `SnapshotFormController` in `adapter.in.web.view`: `GET /` renders an empty
  form; `POST /` re-renders the form from posted fields (used by "Change my numbers").
- **REQ-002**: Cards and fields:
  - Money in and out: take-home income; essential expenses, labeled "including rent or
    mortgage payment".
  - Debts: table with label, type (select without mortgage), balance, rate (%), minimum
    payment; add-row and remove-row buttons.
  - Household: starter fund, full emergency fund, emergency fund months (default 3),
    retirement contribution (%), "I rent", "I'm retired", "I plan to fund college",
    monthly college contribution.
  - Mortgage (shown when "I rent" is off): balance, rate, monthly payment, home value.
- **REQ-003**: Subhead above the fold: "Nothing you enter is stored."
- **REQ-004**: Add-row and remove-row post the form with htmx and swap the debts table
  fragment rendered by the server; adding a 51st real debt is refused with the story 011
  message on the table.
- **REQ-005**: On blur, htmx posts the field's value and swaps the server-rendered field
  fragment; only field-only rules run (story 011 REQ-008).
- **REQ-006**: "Check my stage" posts the whole form to `POST /assessment` (story 029). On
  validation failure the form re-renders with all errors and a C-09 problem summary.
- **REQ-007**: After any server render, blank number fields show `0`, ignored $0 debt rows
  remain visible, and blank labels show their generated "Debt N" name (PRD FR-4).
- **REQ-008**: "Load an example" posts a preset id to `POST /snapshot/example/{id}`; the
  presets are committed CSV files under `src/main/resources/examples/` read through the
  story 021 import service in-process.
- **REQ-009**: Page state is carried only in form fields; no cookies, no session, no
  figures in URLs.
- **REQ-010**: Buttons say what happens ("Check my stage", "Add a debt", "Remove"), never
  "Submit".

### Should Have (P1)
- **REQ-011**: The mortgage section's visibility toggles with a small script when "I rent"
  changes, and is also rendered correctly by the server on every response.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- Field validation round trips complete well under 100ms locally.

### Security
- No figures in URLs or logs; all output escaped.

### Accessibility / Browser Support
- Every field has a visible label; errors are tied with `aria-describedby`; the debts
  table has column headers; focus stays on the field after an on-blur swap.

## Constraints and Assumptions

### Technical Constraints
- View controller calls application services in-process (design spec §7); JavaScript
  required (htmx), hand-written JS within the story 034 budget.

### Assumptions
- Presets are neutral example data, not real households.

## Out of Scope
- File bar and import report — story 028.
- Assessment screen — story 029.
- Mobile layout — story 032.

## Success Metrics
- View tests (MockMvc + Jsoup) for empty form, re-render with errors, add/remove row,
  51st-debt refusal, on-blur fragment, and each preset.
- Rendered errors match API messages for the same input.
