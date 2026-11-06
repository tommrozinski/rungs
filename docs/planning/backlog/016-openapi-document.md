---
status: approved
---

# OpenAPI 3.1 document generation

Generate the OpenAPI document from the code and commit it, so contract changes
arrive as reviewable diffs.

## Description

NFR-6. The PRD originally made this the *only* interface (NG-3); NG-3 has since
been amended to put a read-only web client in scope. The document is still
the contract of record — the client is a consumer of the API like any other, and
holds no rules of its own.

Generated from the code rather than hand-maintained, committed to the repo, with
a build check that fails on drift.

The PRD also asks for ADRs on every structural decision, which belongs here as
the documentation increment.

## Acceptance Criteria

- OpenAPI 3.1 is generated from the code (annotations or equivalent), not
  hand-maintained.
- The document is committed and a build check fails when the committed copy
  drifts from the generated one.
- All endpoints are documented: `/v1/assessments`, `/v1/projections`,
  `/v1/projections/comparison`, `/v1/snapshots/import`, `/v1/snapshots/export`.
- The CSV media types and `Content-Disposition` behaviour of the snapshot
  endpoints are documented.
- Request and response schemas include every field, with the `Money` wire format,
  its formatted display string, and `BigDecimal` scales stated explicitly.
- Problem-detail responses are documented per endpoint with their status codes
  and field-pointer format.
- Enum values are enumerated: stage status, debt type, strategy, warning codes,
  import-report reason codes.
- The snowball/avalanche tie-break rule appears in the strategy field
  description (story 009).
- ADRs exist for the structural decisions: ports-and-adapters layering,
  cents-as-`long` money, statelessness, server-side CSV parsing, and the
  server-rendered client choice.
- The client-choice ADR records design spec section 7's decision as final:
  Thymeleaf in one Maven module, no SPA, no `package.json` or frontend build
  tool, vendored htmx for server round trips — including the stated rationale
  for deliberately not using React, and the alternatives rejected.
- ADRs also record the other section 7 decisions: view controllers call
  application services in-process rather than the `/v1` API over HTTP, and the
  JavaScript policy (external files, page state only, ~200-line budget).
- Public-facing descriptions use neutral vocabulary and avoid the trademarked
  framework names.

## Size

S

## Open Questions

- ~~Swagger UI?~~ **Resolved.** No; the committed document is enough, and the
  running app exposes no docs endpoint.
- ~~ADR location and template?~~ **Resolved.** `docs/adr/NNNN-title.md` with
  context / decision / alternatives / consequences.
- ~~ADR for the NG-3 reversal?~~ **Resolved.** Yes, ADR 0008.

Full requirements: [spec/016-openapi-document/requirements.md](../spec/016-openapi-document/requirements.md).
