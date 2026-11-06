# Requirements Specification

<!-- Feature name: 028-file-bar-and-import-report-ui -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 028) -->
<!-- Link: backlog/028-file-bar-and-import-report-ui.md -->
<!-- Branch: main -->

## Feature Overview

The load/save bar (C-12) and the import report (C-13) on the snapshot form (design spec
§4). The file bar makes the difference between the saved file and the working form visible
at a glance — empty, loaded, or edited — because that distinction is what stops people
losing work. Loading posts the chosen file to the server and re-renders the form with the
imported snapshot and a report of what was accepted, skipped, and rejected; saving posts
the form and downloads the CSV. A small script tracks unsaved edits and confirms before a
load replaces them.

## User Stories

### Story 1: Load and save my file
**As** a household that keeps a snapshot file
**I want** to load it into the form and save the form back to a file
**So that** my data persists without the service storing anything

**Acceptance Criteria:**
- [ ] "Load a file" uploads a CSV and re-renders the form filled from it.
- [ ] "Save a file" downloads `rungs-snapshot.csv` from the current form.

### Story 2: Never lose unsaved work
**As** a household editing a loaded file
**I want** to see when the form differs from the file, and be warned before replacing it
**So that** I don't overwrite work by accident

**Acceptance Criteria:**
- [ ] The file bar shows empty, loaded (filename and debt count), or edited (filename +
  "edited", with "Save a file" promoted to primary).
- [ ] Loading while edited asks for confirmation first.

### Story 3: Understand the import
**As** a household loading a hand-edited file
**I want** a clear report of what loaded and what didn't
**So that** I fix only the rejected rows

**Acceptance Criteria:**
- [ ] Clean, partial, and failed reports render as the design spec describes.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: C-12 file bar under the screen title with "Load a file" (file input +
  submit) and "Save a file", and a state line:
  - Empty: "No file loaded. Fill in the form below, or load a saved one."
  - Loaded: "{filename} — {n} debt(s) loaded."
  - Edited: "{filename} — edited." and "Save a file" becomes the primary button.
- **REQ-002**: Load posts `multipart/form-data` (the `file` part plus the current form's
  hidden fields) to `POST /snapshot/import`, which calls the story 021 import service
  in-process and re-renders the form from the imported snapshot, with the filename kept in a
  hidden field.
- **REQ-003**: C-13 report between the file bar and the first card, only after a load:
  - Clean: one green, dismissible line with the server summary (including skipped paid-off
    debts).
  - Partial: gold banner with the summary, then a table of rejected rows (line, raw text,
    reason) and unknown-key warnings.
  - Failed: red banner with the problem detail; the form is re-rendered from its previous
    hidden-field state, unchanged.
- **REQ-004**: Save posts the form to `POST /snapshot/export`, which calls the story 022
  export service in-process and returns the CSV download. If the snapshot is invalid, the
  form re-renders with validation errors and no download.
- **REQ-005**: `static/js/file-state.js` marks the bar "edited" on any form `input` event
  after a load, and before a load while edited shows `confirm("Loading a file replaces
  everything in the form. Your unsaved changes will be lost.")`. It holds page state only
  (story 034 policy).
- **REQ-006**: Save-in-place via the File System Access API is not implemented (design
  spec §8).

### Should Have (P1)
- **REQ-007**: After a successful save, the bar returns to the loaded state (server
  render sets the filename to `rungs-snapshot.csv`).

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- A 64 KB load re-renders the form in well under 500ms locally.

### Security
- Uploads held in memory only (story 022); filename rendered escaped and never logged.

### Accessibility / Browser Support
- Report banners use text labels and a live region so the result is announced after load.

## Constraints and Assumptions

### Technical Constraints
- In-process calls to the import/export services (design spec §7); JavaScript required.

### Assumptions
- A partial import counts as "loaded"; edits afterward make it "edited".

## Out of Scope
- Import/export endpoints and CSV format — stories 020–022.
- Form fields — story 027.

## Success Metrics
- View tests for clean, partial, failed load; valid and invalid save; each bar state.
- The script stays within the JS budget and passes story 015's scan.
