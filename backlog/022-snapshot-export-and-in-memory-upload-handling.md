---
status: proposed
---

# Snapshot export and in-memory upload handling

`POST /v1/snapshots/export` returning a downloadable CSV, plus the configuration
that guarantees an uploaded file never touches disk.

## Description

FR-7's write direction, paired with the NFR-1 amendment that arrived with it.

Export accepts a `FinancialSnapshot` and returns `text/csv` with a
`Content-Disposition` filename. The design spec's save flow is a browser
download, since a web page cannot silently overwrite a file on disk.

The NFR-1 half is the part that is easy to get wrong and hard to notice:
"Uploaded CSV files are parsed in memory and discarded with the request; they are
never written to disk, not even to a temp directory. Multipart temp-file spooling
must be disabled explicitly rather than left at its default." Spring Boot's
default multipart resolver spools to a temp directory above a size threshold, so
the statelessness claim in NFR-1 is false by default. That makes this a
configuration story with a test, not a line in a properties file.

## Acceptance Criteria

- `POST /v1/snapshots/export` accepts a `FinancialSnapshot` and returns
  `text/csv`.
- The response sets `Content-Disposition` with a filename.
- Exported currency values carry no `$` and no thousands separators (AC-7.4).
- Exported labels are RFC 4180 quoted where required (AC-7.5).
- The export is byte-identical for identical input (NFR-4).
- Multipart temp-file spooling is explicitly disabled; the file-size threshold is
  configured so nothing spools.
- The 64 KB limit (AC-7.6) is enforced before any buffering decision is made.
- A test proves no temp file is created during an import — asserted against the
  configured temp directory, not assumed from configuration.
- Uploaded bytes are unreachable after the request completes.
- The README states the no-disk guarantee as a product claim.

## Size

M

## Open Questions

- What filename does export suggest? A fixed name, a timestamp (which would need
  `Clock` injection per NFR-4), or one the client supplies?
- If the upload is raw `text/csv` rather than multipart (story 021's open
  question), the spooling concern changes shape — the container may still buffer
  large bodies. What is the guarantee against *that*?
- Does export need to round-trip a mortgage, and what happens if the format
  cannot represent one (story 020)?
- Is a `Cache-Control: no-store` header wanted on the export response, given the
  file is household data?
