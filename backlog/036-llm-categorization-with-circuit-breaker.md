---
status: proposed
---

# LLM categorization with fallback and circuit breaker

Add the Spring AI primary path to `/v1/categorize`, with schema validation, a
circuit breaker that degrades to rules, a token ceiling, and response caching.

## Description

The rest of FR-6, and the only story in the backlog that reaches outside the
process. The PRD's design is defensive throughout, and the acceptance criteria
below keep that posture: every model response is schema-validated, malformed or
low-confidence responses fall back to the deterministic table from story 035, a
circuit breaker opens after repeated failures, per-request tokens are capped, and
responses are cached by normalized description hash.

The model client sits behind a `CategorizationPort` owned by the domain
(`adapter.out.ai` implements it), so the architecture constraints in story 015
hold: the domain never sees Spring AI.

Stretch scope. Not started before increments 1–3.5 are complete.

## Acceptance Criteria

- A `CategorizationPort` is owned by `domain.port`; the Spring AI client lives in
  `adapter.out.ai` and is the only thing that imports it (AC-C).
- The prompt is versioned and the version is recorded with each response.
- Every model response is schema-validated before use.
- A malformed response falls back to the rule table and returns `source: RULES`.
- A response below a configured confidence threshold falls back to the rule table.
- A circuit breaker opens after repeated model failures; while open, the endpoint
  serves rules-only without attempting a call.
- Breaker state transitions are covered by tests, including half-open recovery.
- A per-request token ceiling is enforced; a request exceeding it fails over to
  rules rather than truncating.
- Responses are cached by normalized description hash; a cache hit makes no model
  call.
- Responses carry `source` distinguishing model-derived from rules-derived
  answers.
- Transaction descriptions are never logged (NFR-2); cache keys are hashes, not
  raw text.
- Model unavailability never fails the request.

## Size

L

## Open Questions

- Which model and provider? The PRD says Spring AI but names no model.
- What is the low-confidence threshold?
- Is the cache in-memory only? A persistent cache would conflict with NFR-1's "no
  PII at rest" if descriptions are recoverable from it.
- What is the token ceiling, and is it per-transaction or per-request?
- What are the breaker's thresholds — failure count, window, and open duration?
- Sending transaction descriptions to a third-party model is a meaningful change
  to the privacy posture NFR-1 otherwise states strongly — and the design spec
  puts "nothing is stored" above the fold as a product claim (story 027). Does
  that claim need qualifying once this ships, and does it need calling out in the
  README as a conscious tradeoff?
