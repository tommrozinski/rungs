---
status: approved
---

# S-04 Comparison screen

Two aligned columns, a green header on the cheaper strategy, and a dark delta
block stating the difference in plain sentences.

> **Approved 2026-09-13.** Full requirements, which supersede the criteria and open
> questions below: [spec/031-comparison-screen/requirements.md](../spec/031-comparison-screen/requirements.md).
> Decided: no chart; closing line is fixed copy (no threshold); the screen has its own
> extra-payment control; delta block on `navy-900`.

## Description

Design spec section 4, S-04. Two columns with identical row labels, aligned so
the eye can scan across — the alignment is the entire argument of the screen.

The closing line notes that when the gap is small, the order matters less than
the habit. The spec ties this directly to PRD non-goal NG-4: the engine reports
arithmetic and does not push a recommendation. So the screen is allowed to say
which strategy costs less, because that is arithmetic, but not which one the
household should choose.

Which column gets the green header comes from the API (story 013), not from the
client comparing two numbers.

## Acceptance Criteria

- Two columns with identical row labels, horizontally aligned across.
- The column costing less interest gets the green header, driven by the API's
  verdict rather than a client-side comparison.
- A dark delta block states the difference in plain sentences.
- The closing line notes that a small gap means the order matters less than the
  habit.
- No recommendation is made — the screen reports the arithmetic only (NG-4).
- The screen renders from a single `/v1/projections/comparison` response.
- The AC-3.2 identical-schedule case renders the all-zero delta with its
  explanatory note rather than an empty or broken block.
- A truncated projection on either side is stated, not hidden (story 013).
- No chart.
- Renders correctly with JavaScript disabled.
- Tabular numerals and server-formatted currency throughout.

## Size

S

## Open Questions

- Design spec open question, answered in the spec but worth confirming: no chart.
  "A chart of two nearly identical curves adds pixels and no information."
- What threshold makes a gap "small" enough for the closing line, and does that
  come from the API (story 013) or is it a fixed client-side string shown always?
- Does the comparison screen carry its own extra-payment control, or does it
  inherit whatever the projection screen was last set to?
- "Dark delta block" — which token? The palette has `navy-900` for headings but
  no dedicated dark surface.
