# Requirements Specification

<!-- Feature name: 022-snapshot-export-and-in-memory-upload-handling -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 022) -->
<!-- Link: backlog/022-snapshot-export-and-in-memory-upload-handling.md -->
<!-- Branch: main -->

## Feature Overview

Two halves of the save mechanism. `POST /v1/snapshots/export` turns a snapshot into a
downloadable CSV file. And the multipart configuration that makes the NFR-1 claim true:
uploaded files are parsed in memory and discarded with the request, never written to disk
— not even to a temp directory. Spring's default multipart handling spools large parts to
a temp folder, so "stores nothing" is false by default; this story sets the limits
explicitly and proves the guarantee with a test that watches the temp folder during an
upload, rather than trusting configuration.

## User Stories

### Story 1: Save my snapshot
**As** a user who has filled in the form
**I want** to download my snapshot as a file
**So that** I can load it again later

**Acceptance Criteria:**
- [ ] Export returns `text/csv` with `Content-Disposition: attachment;
  filename="rungs-snapshot.csv"`.
- [ ] Identical snapshots export byte-identical files.

### Story 2: Uploads never touch disk
**As** a household uploading financial data
**I want** my file held in memory only
**So that** "nothing is stored" is literally true

**Acceptance Criteria:**
- [ ] A test uploading a file at the 64 KB limit observes no file created in the
  configured temp location.
- [ ] Uploads over 64 KB are refused before being buffered.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `SnapshotExportController` in `adapter.in.web.api` handles
  `POST /v1/snapshots/export`, consuming the `/v1/assessments` request JSON.
- **REQ-002**: The snapshot goes through the story 007/011 input model (normalization and
  validation). An invalid snapshot returns the story 011 problem response, so every
  exported file imports cleanly.
- **REQ-003**: The response is 200 `text/csv; charset=UTF-8`, body from
  `SnapshotCsvWriter` (story 020), with headers `Content-Disposition: attachment;
  filename="rungs-snapshot.csv"` and `Cache-Control: no-store`.
- **REQ-004**: Zero-balance debts are not written (they were removed by normalization).
- **REQ-005**: Multipart configuration:
  `spring.servlet.multipart.max-file-size=64KB`, `max-request-size=128KB`, and
  `file-size-threshold` set above the maximum file size so parts are never written to
  disk. The container's request-size limits are set consistently, and oversize requests
  are rejected before the body is buffered beyond the limit.
- **REQ-006**: A test configures the multipart location (and `java.io.tmpdir`) to a fresh
  directory, uploads files of 1 KB and exactly 64 KB, and asserts the directory stays
  empty during and after each request; a 65 KB upload is rejected with 413.
- **REQ-007**: Handlers read uploaded bytes into local variables only; nothing retains a
  reference to a `MultipartFile` or its bytes after the response.
- **REQ-008**: The README states the no-disk guarantee as a product claim, alongside
  "nothing is stored".

### Should Have (P1)
- **REQ-009**: The same multipart configuration covers the web client's file-upload form
  (story 028); the no-disk test exercises both the API and the view route once that route
  exists.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- Export of a 50-debt snapshot completes well under 50ms.

### Security
- `Cache-Control: no-store` keeps the household file out of browser and proxy caches.
- No upload or export content appears in logs (story 018).

### Accessibility / Browser Support
- The download works as a normal form post and browser file download.

## Constraints and Assumptions

### Technical Constraints
- Multipart transport (decided 2026-09-13).
- Fixed filename, so no clock is read (NFR-4).

### Assumptions
- Exporting only valid snapshots is acceptable; the form shows validation errors before
  saving (story 028).

## Out of Scope
- The format and writer — story 020.
- Import endpoint and report — story 021.
- File bar UI — story 028.
- Container-level read-only filesystem proof — story 037.

## Success Metrics
- The no-disk test passes at 1 KB and 64 KB and fails if `file-size-threshold` is lowered.
- Export contract tests assert headers, content type, and byte-identical bodies.
- An exported file imports with status `CLEAN`.
