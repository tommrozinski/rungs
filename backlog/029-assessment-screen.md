---
status: proposed
---

# S-02 Assessment screen

The primary screen: verdict block, three stat cards, warnings, then the
seven-stage ladder.

## Description

Design spec section 4, S-02. This is the screen the product is for, and its
ordering is argued rather than arbitrary:

- The **verdict block** leads, carrying the one bold moment in the design.
- **Warnings appear above the ladder** "because they change how the numbers below
  should be read." A warning under the fold would be a warning nobody acts on.
- **The ladder always shows all seven stages.** Skipped stages are visible and
  explained, not hidden — "a hidden stage looks like a bug."

The components are stories 025 and 026; this story composes them against a real
assessment result — the same response object `/v1/assessments` returns, obtained
in-process per design spec section 7 — and owns the screen's layout, empty
states, and navigation onward to the projection screen.

## Acceptance Criteria

- Layout order is verdict block, three stat cards, warnings, seven-stage ladder.
- The view controller calls the assessment application service in-process (not
  `/v1/assessments` over HTTP) and renders its result with no additional
  computation.
- The page model is built from the same response object `/v1/assessments`
  returns, and a test asserts the rendered figures match the endpoint's JSON for
  the same input.
- All seven stages render in every case, including skipped ones with their
  explanations.
- Warnings render above the ladder using C-09, with severity from the API
  (story 012).
- The verdict block renders the current stage's blocker.
- Stat cards use tabular numerals and server-formatted currency strings.
- The single call to action leads to the projection screen.
- No stage rules or arithmetic exist in client code; every number rendered comes
  from the response.
- The screen renders correctly with JavaScript disabled.
- Copy follows the voice rules; no exclamation marks, no encouragement, no
  coaching tone.

## Size

M

## Open Questions

- Which three figures are the stat cards? The spec says three but does not name
  them — monthly surplus, current stage, and months to debt freedom would need
  the projection endpoint, not the assessment endpoint.
- What does the screen show for a household at stage 7, where there is no blocker
  and no next stage? The verdict block has no obvious content.
- How does a user get here — does submitting S-01 navigate, or does the
  assessment render on the same page below the form?
- ~~In-process call or HTTP to `/v1/assessments`?~~ **Resolved.** Design spec
  section 7: in-process, guarded by an ArchUnit rule (story 015) and a
  figures-match test.
