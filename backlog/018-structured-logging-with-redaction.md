---
status: approved
---

# Structured logging with money redaction

Structured JSON logs that carry correlation IDs and derived aggregates, and never
a household's money values or snapshot contents.

## Description

NFR-2, backed by NFR-1: the service stores nothing, and household figures appear
in request bodies and in nothing else. Logging is the most likely place for that
guarantee to leak — an exception handler that dumps the request body, or a debug
statement added during a projection bug hunt, quietly turns a stateless service
into one with PII at rest in a log aggregator.

CSV import widens the surface: an uploaded file is household data in bulk, and a
parse failure is exactly the moment someone reaches for logging the offending
line. The `ImportReport` echoes raw rejected rows back to the user (story 021) —
that is a response, not a log, and the distinction has to hold.

So this story is as much about making the leak hard as about emitting the logs.

## Acceptance Criteria

- Logs are emitted as structured JSON.
- Every request carries a correlation ID, propagated to logs and available for
  the response (see story 007's open question).
- No money value, debt balance, debt label, income figure, uploaded filename, raw
  CSV row, or raw snapshot appears in any log line at any level.
- Exception handling logs the error class and correlation ID, never the request
  body or the uploaded bytes.
- CSV parse failures log a reason code and line number only — never the line
  content.
- `Money` and the snapshot types have redaction-safe `toString` implementations,
  or are structurally prevented from being logged.
- Derived aggregates (stage verdict, debt count, row counts, latency) may be
  logged.
- A test asserts that a full assessment request and a full CSV import produce no
  log line containing any input figure or row content.
- Log level configuration cannot be changed to defeat the redaction.

## Size

M

## Open Questions

- ~~Is debt count safe to log?~~ **Resolved.** Yes, as an aggregate.
- ~~Inbound or generated correlation id?~~ **Resolved.** A well-formed inbound
  `X-Request-Id` is reused; otherwise one is generated. Always echoed.
- ~~Log aggregator conventions?~~ **Resolved.** None targeted; ECS structured format.

Full requirements: [spec/018-structured-logging-with-redaction/requirements.md](../spec/018-structured-logging-with-redaction/requirements.md).
