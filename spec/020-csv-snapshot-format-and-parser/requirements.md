# Requirements Specification

<!-- Feature name: 020-csv-snapshot-format-and-parser -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 020) -->
<!-- Link: backlog/020-csv-snapshot-format-and-parser.md -->
<!-- Branch: main -->

## Feature Overview

The versioned CSV snapshot format and its reader and writer, as pure domain code (PRD
FR-7). The service stores nothing, so a CSV file the user keeps is the save mechanism;
the format is therefore the hardest thing in the system to change once files exist. This
spec pins it completely: a version marker line, typed `FIELD`, `FLAG`, `DEBT`, and
`MORTGAGE` rows, RFC 4180 quoting in both directions, per-row rejection with line numbers
so one bad row never discards the rest, and warnings for unknown keys so newer files
degrade gracefully. The reader returns raw row values; the `application` input model
applies the same normalization as JSON requests (blank numbers to zero, positional ids and
labels, zero-balance debts skipped). The HTTP endpoints are stories 021 and 022.

## User Stories

### Story 1: A file that saves the whole household
**As** a user who keeps a snapshot file
**I want** every part of my snapshot, including a mortgage, written to one CSV
**So that** loading the file restores exactly what I saved

**Acceptance Criteria:**
- [ ] The writer emits the marker, all known `FIELD` and `FLAG` rows, every debt, and the
  mortgage when present.
- [ ] Reading the written file yields the same values.

### Story 2: Forgiving, honest reading
**As** a user loading a hand-edited file
**I want** good rows accepted and bad rows reported with their line and reason
**So that** I can fix only what's wrong

**Acceptance Criteria:**
- [ ] A malformed row is rejected individually with its line number, raw text, and a
  reason code; parsing continues (AC-7.2).
- [ ] Unknown `FIELD`/`FLAG` keys are warnings, not rejections.
- [ ] A missing or wrong version marker rejects the whole file (AC-7.3).

### Story 3: Labels that survive anything
**As** a user with a debt named `Mom's "loan", part 2`
**I want** labels with commas, quotes, newlines, and unicode to round trip intact
**So that** saving never corrupts my data (AC-7.5)

**Acceptance Criteria:**
- [ ] RFC 4180 quoting and escaping is applied on write and honored on read.
- [ ] Multi-line quoted labels parse, and are reported by their starting line.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: Package `com.rungs.readiness.domain.csv` with `SnapshotCsvReader` and
  `SnapshotCsvWriter`; no Spring, Jakarta, or Jackson imports; no floating point.
- **REQ-002**: **Format.**
  - Line 1 is exactly `rungs_snapshot_v1` (a leading UTF-8 byte-order mark is tolerated).
  - `FIELD,<key>,<value>` with money keys `monthly_take_home`, `essential_expenses`,
    `starter_fund`, `full_fund`, `college_contribution`; percent key
    `retirement_percent`; integer key `emergency_fund_months`.
  - `FLAG,<key>,<value>` with keys `rents`, `retired`, `funds_college`; values `true` or
    `false`, case-insensitive; blank means `false`.
  - `DEBT,<label>,<type>,<balance>,<annual_rate>,<minimum_payment>`.
  - `MORTGAGE,<balance>,<annual_rate>,<monthly_payment>,<home_value>`, at most one.
  - Row order is not significant except that `DEBT` rows keep their file order.
- **REQ-003**: **Reading rules.** Encoding is strict UTF-8; lines end in LF or CRLF; empty
  lines are ignored; keys are exact lowercase; unquoted values are trimmed, quoted values
  are kept exactly; blank values are returned as blank (normalized later). Money uses
  `Money.parse` rules (AC-7.4); percentages allow up to 3 decimals for rates and 2 for
  `retirement_percent`.
- **REQ-004**: **Rejection reason codes** (bounded enum): `WRONG_COLUMN_COUNT`,
  `UNKNOWN_ROW_TYPE`, `INVALID_AMOUNT`, `INVALID_PERCENT`, `INVALID_INTEGER`,
  `INVALID_FLAG`, `INVALID_DEBT_TYPE`, `MORTGAGE_AS_DEBT`, `DUPLICATE_KEY` (a second
  `FIELD` or `FLAG` with the same key — the later row is rejected), `DUPLICATE_MORTGAGE`,
  `UNTERMINATED_QUOTE`. Each rejection carries the starting line number, the raw row
  text, and the code.
- **REQ-005**: **Whole-file failures** (no partial result): missing or unrecognized marker
  (`MISSING_VERSION_MARKER`, naming the expected marker), invalid UTF-8
  (`INVALID_ENCODING`), input over 64 KB (`FILE_TOO_LARGE`).
- **REQ-006**: `SnapshotCsvReader.read(byte[])` returns a `CsvReadResult`: the raw snapshot
  draft (field values, flags, debts in file order, optional mortgage), accepted row counts
  by type, rejections, and unknown-key warnings (line number and key). No row is ever
  silently dropped.
- **REQ-007**: **Writing rules.** UTF-8 without byte-order mark; CRLF line endings; marker
  first; then `FIELD` rows in the REQ-002 key order, `FLAG` rows in key order, `DEBT` rows
  in snapshot order, and the `MORTGAGE` row last when present. Money uses
  `Money.toBareString()`; percentages are written from their normalized value with
  trailing zeros removed but at least 2 decimals (`26.99`, `6.40`, `7.125`, `6.00`);
  `emergency_fund_months` as an integer; flags as `true`/`false`.
- **REQ-008**: **Quoting.** A value is quoted when it contains a comma, double quote, CR,
  LF, or leading or trailing whitespace; embedded double quotes are doubled.
- **REQ-009**: The writer is deterministic: identical snapshots produce byte-identical
  output (NFR-4).

### Should Have (P1)
- **REQ-010**: A committed sample file (`src/test/resources/csv/example-household.csv`)
  that both reader and writer tests use, and that "Load an example" (story 027) reuses.

### Nice to Have (P2)
- **REQ-011**: Rejection raw text is truncated to 200 characters, so a pathological row
  cannot bloat the report.

## Non-Functional Requirements

### Performance
- Reading or writing a 64 KB file takes well under 50ms.

### Security
- Input is bounded (64 KB) before parsing; the parser never allocates proportionally to
  anything but that bound.
- Rejection reasons are codes; the raw text travels only in the response report, never in
  logs (PRD NFR-2, story 018).

### Accessibility / Browser Support
- Not applicable.

## Constraints and Assumptions

### Technical Constraints
- Pure domain code (PRD AC-A, AC-F); parsing is server-side only (AC-G).
- The format is versioned; any incompatible change requires a new marker.

### Assumptions
- Debt ids are not stored in the file; they are reassigned by position on import, which
  round-trips exactly because file order is preserved.
- Zero-balance `DEBT` rows parse successfully; the application layer skips and reports
  them (AC-7.7, story 021).

## Out of Scope
- HTTP import and the `ImportReport` response — story 021.
- HTTP export and no-disk upload handling — story 022.
- The round-trip property — story 023.
- Normalization rules (blanks, ids, labels, zero-balance) — PRD FR-4, story 007/011.

## Success Metrics
- Reader tests for every reason code and whole-file failure.
- Quoting tests for commas, doubled quotes, CRLF and LF inside quotes, leading and trailing
  spaces, and non-ASCII labels.
- AC-7.2, AC-7.3, AC-7.4, AC-7.5 pass at the domain level.
- 100% line and branch coverage on the reader and writer.
