---
status: approved
---

# Snapshot import endpoint

`POST /v1/snapshots/import` — accept a CSV upload, return the parsed snapshot
plus a report of exactly what was and was not accepted.

## Description

FR-7. The endpoint accepts `text/csv` and returns a `FinancialSnapshot` alongside
an `ImportReport` listing accepted rows, rejected rows with line numbers and
reasons, and unknown-key warnings.

The `ImportReport` is not a diagnostic afterthought — the design spec renders it
as component C-13 in three states, and the partial state is the interesting one:
accepted rows are already in the form, and the user fixes the rest by typing
rather than editing the file and starting over. That only works if the report
carries the raw rejected row text back for display, with enough structure for the
UI to table it.

Note the tension with NFR-2 that story 018 has to hold: rejected row content goes
in the *response*, never in a log line.

## Acceptance Criteria

- `POST /v1/snapshots/import` accepts `text/csv` and returns a snapshot plus an
  `ImportReport`.
- The report lists accepted row counts by type, rejected rows with line number,
  raw row text, and a reason code plus user-facing reason copy.
- Unknown `FIELD` and `FLAG` keys appear as warnings distinct from rejections.
- AC-7.7: `DEBT` rows with a zero balance are not added to the snapshot and are
  listed as **skipped** (paid off) with their line number — a third category,
  distinct from accepted and rejected.
- Blank values read as zero rather than producing a rejected row.
- An import whose only non-accepted rows are skipped paid-off debts is a clean
  import, not a partial one. The report carries the skipped count so the clean
  line can say so (e.g. "Loaded 3 debts and 8 values. Skipped 1 paid-off debt.").
- AC-7.2: three valid debt rows and one malformed row import the three, and the
  report names the fourth with its line number and reason.
- AC-7.3: a missing or unrecognized version marker is rejected with an RFC 9457
  problem detail naming the expected marker.
- AC-7.6: files above 64 KB are rejected before parsing, not after.
- A wholly unparseable file returns a problem detail and no partial snapshot.
- Reason copy follows the design spec voice rules and renders verbatim.
- The imported snapshot is returned in the same shape `/v1/assessments` accepts,
  so the client can round-trip it without transformation.
- Contract tests cover clean, partial, and failed imports.

## Size

M

## Open Questions

- ~~Raw body or multipart?~~ **Resolved.** `multipart/form-data`, part `file`
  (PRD FR-7).
- ~~64 KB limit status?~~ **Resolved.** 413 with problem type
  `urn:rungs:problem:file-too-large`, before parsing.
- ~~Echo the filename?~~ **Resolved.** Yes, sanitized, in the response only; never
  logged.
- ~~What counts as accepted?~~ **Resolved.** Per row type: fields, flags, debts,
  mortgage; the summary counts debts and values.
- ~~Clean or partial when only paid-off debts were skipped?~~ **Resolved.**
  Clean.

Full requirements: [spec/021-snapshot-import-endpoint/requirements.md](../spec/021-snapshot-import-endpoint/requirements.md).
