---
status: proposed
---

# Rules-based transaction categorization

`POST /v1/categorize` backed by a deterministic rule table — the fallback path,
built first and standing on its own.

## Description

Part of FR-6, which the PRD marks as a stretch goal explicitly out of scope for
the first two increments.

The PRD describes the LLM as the primary path and the rule table as the fallback.
Building the fallback first inverts that order deliberately: it gives the
endpoint a complete, deterministic, testable implementation before any model
dependency exists, and it means the circuit breaker in story 036 has something
real to degrade *to* rather than something stubbed.

The endpoint is also the only place in the service where a `source` field
distinguishes how an answer was reached.

## Acceptance Criteria

- `POST /v1/categorize` accepts a list of transaction descriptions with amounts
  and returns a budget category plus a confidence score per transaction.
- A deterministic rule table maps normalized descriptions to categories.
- Responses carry `source: RULES`.
- Description normalization is defined and tested (case, whitespace, punctuation,
  merchant noise).
- An unmatched description returns a documented default category rather than an
  error.
- Confidence scores are deterministic for a given input.
- Results are stable for identical input (NFR-4).
- Transaction descriptions and amounts are never logged (NFR-2).
- The endpoint appears in the OpenAPI document.

## Size

M

## Open Questions

- What is the budget category set? The PRD never enumerates it.
- Where does the rule table live — code, a committed resource file, or config?
- How is confidence scored for a rules match — always 1.0, or graded by match
  quality?
- Is there a batch size limit, as there is a 50-debt limit elsewhere?
- NG-4 says the engine reports arithmetic, not recommendations. Does categorizing
  a transaction sit comfortably inside that boundary?
- Does this endpoint get a UI? Design spec section 8 does not list it as out of
  scope for the UI, but no screen covers it either.
