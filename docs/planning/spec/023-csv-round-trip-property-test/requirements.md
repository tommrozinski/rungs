# Requirements Specification

<!-- Feature name: 023-csv-round-trip-property-test -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 023) -->
<!-- Link: backlog/023-csv-round-trip-property-test.md -->
<!-- Branch: main -->

## Feature Overview

AC-7.1 as a property: for any valid, normalized snapshot, exporting it to CSV and importing
the file back yields an equal snapshot. It extends the story 014 generator with
adversarial labels — commas, quotes, newlines, surrounding spaces, and unicode — and runs
through the real writer, reader, and application normalization, not a shortcut. It is
the test most likely to find the format's real defects: quoting bugs, scale mismatches,
and fields that silently fail to round trip. Snapshots with a mortgage are included, now
that the format has a `MORTGAGE` row.

## User Stories

### Story 1: Saving never changes my data
**As** a user who saves and reloads snapshots
**I want** every save-then-load to return exactly what I had
**So that** the file is a trustworthy save mechanism

**Acceptance Criteria:**
- [ ] `import(export(s))` equals `s` for generated normalized snapshots, with and without
  a mortgage.
- [ ] Equality covers the whole graph, including debt order, ids, and labels.

### Story 2: Failures that explain themselves
**As** the developer fixing a round-trip bug
**I want** a shrunk example and the CSV that broke it
**So that** I can see the defect immediately

**Acceptance Criteria:**
- [ ] A failure shrinks to a minimal snapshot and prints the intermediate CSV.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: A jqwik property drives `SnapshotCsvWriter` → bytes → `SnapshotCsvReader` →
  the `application` import normalization, and asserts the result equals the original
  snapshot and that the import report has no rejections, skips, or warnings.
- **REQ-002**: The generator extends `SnapshotArbitraries` (story 014) with labels
  containing commas, double quotes, single quotes, CR and LF, leading and trailing spaces
  around non-blank text, and non-ASCII unicode (accents, CJK, emoji, right-to-left
  script).
- **REQ-003**: Generated snapshots are normalized, as the story 002/003 decisions require:
  debt balances from $0.01 (no zero-balance debts), labels never blank or whitespace-only,
  positional ids, no `MORTGAGE` debt type; they include 0 and 50 debts, every debt type,
  and mortgage present or absent.
- **REQ-004**: Money values include the cent boundary and large magnitudes (up to
  $10,000,000.00); rates include 0, whole numbers, and 3-decimal values.
- **REQ-005**: A second property asserts writer stability:
  `export(import(export(s)))` is byte-identical to `export(s)`.
- **REQ-006**: A third property covers skipping: a snapshot with zero-balance debts
  inserted imports as the same snapshot without them, with each skip reported by line
  number (AC-7.7), and remaining ids still positional by original file order.
- **REQ-007**: On failure, the property's report includes the intermediate CSV text.
- **REQ-008**: No field is excluded from equality; if one cannot round trip, the defect is
  fixed rather than excluded.

### Should Have (P1)
- **REQ-009**: Statistics report how many generated labels needed quoting and how many
  snapshots had a mortgage.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- The round-trip properties add under 20 seconds at default tries.

### Security
- Generated data only.

### Accessibility / Browser Support
- Not applicable.

## Constraints and Assumptions

### Technical Constraints
- Runs in `./mvnw verify` (story 019); reuses story 014's generator and seed policy.

### Assumptions
- Ids are positional and file order is preserved, so ids round-trip without being stored;
  REQ-006 confirms the behavior when skipped rows shift nothing but the skipped debts.

## Out of Scope
- Example-based reader and writer tests — story 020.
- HTTP endpoints — stories 021, 022.

## Success Metrics
- All three properties pass at default tries.
- A deliberately broken quote-escaping change is caught during development.
- Statistics show quoted labels and mortgages generated in a meaningful share of cases.
