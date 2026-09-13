# Requirements Specification

<!-- Feature name: 021-snapshot-import-endpoint -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 021) -->
<!-- Link: backlog/021-snapshot-import-endpoint.md -->
<!-- Branch: main -->

## Feature Overview

`POST /v1/snapshots/import` (PRD FR-7): upload a saved CSV file as a multipart form part
and get back the snapshot it describes plus an `ImportReport` saying exactly what was
accepted, skipped, and rejected. The report is what makes a partial import useful: good
rows are already in the returned snapshot, and each rejected row comes back with its line
number, raw text, and a plain reason, so the user fixes the rest by typing rather than
editing the file and starting over. Whole-file problems — wrong version marker, bad
encoding, oversized file — return problem details and no snapshot. The upload never
touches disk (story 022).

## User Stories

### Story 1: Load a saved file
**As** a user with a snapshot file
**I want** to upload it and get my snapshot back
**So that** I can pick up where I left off

**Acceptance Criteria:**
- [ ] A `multipart/form-data` request with a `file` part returns 200 with `filename`,
  `snapshot`, and `report`.
- [ ] The snapshot is in the same shape `/v1/assessments` accepts, so it can be posted
  back unchanged.

### Story 2: Know exactly what happened
**As** a user whose file has a few problems
**I want** a report of accepted, skipped, and rejected rows
**So that** I can fix only what went wrong (AC-7.2)

**Acceptance Criteria:**
- [ ] Three valid debt rows and one malformed row import the three and report the fourth
  with its line number and reason.
- [ ] Paid-off debts are listed as skipped, and alone keep the import clean (AC-7.7).
- [ ] Unknown keys are listed as warnings.

### Story 3: Bad files fail clearly
**As** a user who picked the wrong file
**I want** a specific error and no half-loaded data
**So that** my form is left untouched

**Acceptance Criteria:**
- [ ] A missing or unrecognized version marker returns a problem naming
  `rungs_snapshot_v1` (AC-7.3).
- [ ] A file over 64 KB is rejected before parsing (AC-7.6).

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `SnapshotImportController` in `adapter.in.web.api` handles
  `POST /v1/snapshots/import`, consuming `multipart/form-data` with one required part
  named `file`, producing `application/json`.
- **REQ-002**: The service reads the bytes with `SnapshotCsvReader` (story 020), then
  passes the draft through the same `application` input model as JSON requests
  (blank-to-default, positional ids and labels, zero-balance skip, validation). Rows that
  parse but fail validation (for example a negative amount or a 51st real debt) become
  rejected rows with the story 011 code, and their values are left out of the snapshot.
- **REQ-003**: Response (200):
  `{"filename", "snapshot": {…request shape…}, "report": {"status", "summary",
  "accepted": {"fields", "flags", "debts", "mortgage"}, "skipped": [{"line", "code",
  "message"}], "rejected": [{"line", "rawText", "code", "message"}],
  "warnings": [{"line", "key", "message"}]}}`. All keys always present.
- **REQ-004**: `report.status` is `CLEAN` when there are no rejections and no warnings
  (skipped paid-off debts allowed), otherwise `PARTIAL`.
- **REQ-005**: `report.summary` is server copy, e.g. "Loaded 3 debts and 8 values." plus
  "Skipped 1 paid-off debt." when applicable; values count accepted `FIELD`, `FLAG`, and
  `MORTGAGE` rows.
- **REQ-006**: Reason messages per code (user-facing, voice rules), e.g.
  `WRONG_COLUMN_COUNT` "This row has the wrong number of values.", `INVALID_AMOUNT`
  "This amount isn't a number, like 1,234.56.", `DUPLICATE_KEY` "This value already
  appears earlier in the file.", `PAID_OFF` "This debt has a $0 balance, so it's paid off
  and wasn't loaded."
- **REQ-007**: Whole-file failures return problem details and no snapshot:
  - Over 64 KB → 413, `type` `urn:rungs:problem:file-too-large`, rejected before parsing.
  - Missing marker → 400, `type` `urn:rungs:problem:invalid-snapshot-file`, code
    `MISSING_VERSION_MARKER`, detail naming `rungs_snapshot_v1`.
  - Invalid UTF-8 → 400, same type, code `INVALID_ENCODING`.
  - Missing `file` part → 400, `type` `urn:rungs:problem:validation`, pointer `/file`,
    code `MISSING_FILE`.
- **REQ-008**: `filename` is the uploaded name reduced to its base name, control
  characters removed, truncated to 255 characters. It appears only in the response, never
  in logs.
- **REQ-009**: Increments `rungs.snapshot.imports` with `outcome` `CLEAN`, `PARTIAL`, or
  `FAILED` (story 017); logs only codes and line numbers (story 018).

### Should Have (P1)
- **REQ-010**: Contract tests for clean, partial, skipped-only, unknown-key, oversized,
  missing-marker, bad-encoding, and missing-part requests.

### Nice to Have (P2)
- **REQ-011**: `rawText` is truncated to 200 characters (story 020).

## Non-Functional Requirements

### Performance
- A 64 KB import completes well under 250ms.

### Security
- Upload held in memory only (story 022); size bounded before parsing.
- The declared part content type is not trusted; the bytes are always parsed as the
  snapshot format.
- Row content and filename travel only in the response (PRD NFR-2).

### Accessibility / Browser Support
- Plain HTML file-upload forms post this request without JavaScript (story 034).

## Constraints and Assumptions

### Technical Constraints
- Transport is multipart only (decided 2026-09-13).
- The returned snapshot uses the request format (plain money strings), not response money
  objects.

### Assumptions
- The web client posts the file to a view controller that calls the same application
  service in-process (design spec §7); this endpoint is the API form.

## Out of Scope
- Parsing and the format — story 020.
- In-memory multipart configuration and the no-disk test — story 022.
- The file bar and import report UI — story 028.

## Success Metrics
- AC-7.2, AC-7.3, AC-7.6, and AC-7.7 pass as contract tests.
- Every reason code has a message asserted verbatim.
- 100% line coverage on the controller and import service.
