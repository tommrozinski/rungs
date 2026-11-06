---
status: approved
---

# Design tokens and CSS foundation

The `:root` token block, type scale, spacing, and reset — the one place any color
or measurement is defined.

> **Approved 2026-09-13.** Full requirements, which supersede the criteria and open
> questions below: [spec/024-design-tokens-and-css-foundation/requirements.md](../spec/024-design-tokens-and-css-foundation/requirements.md).
> Decided: Inter self-hosted; rem sizes; display size 34px; no dark-mode structuring.

## Description

Design spec section 2. Tokens are CSS custom properties and "nothing downstream
hardcodes a color, so the whole palette can be re-themed from one block." The
board file's `:root` block is the source and is copied verbatim rather than
retyped — retyping is how palettes drift.

Fifteen color tokens, five type roles on a single family (Inter with a system
fallback), a seven-step spacing scale, four radii, and exactly two elevations.
The spec says to resist adding a third, which is the kind of constraint that only
survives if it is established before the components arrive.

Two rules here carry real weight beyond aesthetics:

- **Color carries status semantically.** Green satisfied, gold in progress, grey
  not started, blue skipped or actionable. Red is reserved for input validation
  and never appears as a stage status — "a user who is behind on their goals is
  not shown red, because being at stage 2 is progress, not an error."
- **Tabular numerals** on all currency and duration figures, so columns align and
  figures do not jitter when a projection re-renders.

No CSS framework. The spec budgets roughly 300 hand-written lines. Per design
spec section 7 there is also no frontend build step: the stylesheet is a plain
file served from `src/main/resources/static` by the Spring Boot application — no
preprocessor, no PostCSS, no bundler, no `package.json`.

## Acceptance Criteria

- A `:root` block defines all fifteen color tokens from design spec section 2.1
  at the specified hex values.
- Type roles are defined for Display (34/800), H1 (24/700), H2 (17/700), Body
  (14/400, line height 1.6, max 60ch), and Small (12.5/400).
- Inter is loaded with a system fallback stack that does not shift layout when
  Inter is unavailable.
- `font-variant-numeric: tabular-nums` applies to all currency and duration
  figures.
- Spacing scale (4, 8, 12, 16, 22, 32, 44), radii (6 fields, 10 cards, 16 hero,
  pill buttons), and two elevations are defined as tokens.
- No color value is hardcoded anywhere outside the token block — enforced by a
  check that runs in `mvn verify` (a JUnit scan or Maven step; stylelint is
  unavailable without a Node toolchain).
- No CSS framework, preprocessor, or CSS build step is added; the stylesheet is
  served as a static resource from `src/main/resources/static`.
- Total hand-written CSS stays in the order of magnitude the spec budgets.
- Palette values are approximated, not sampled from any third party's brand
  tokens, and no brand asset is included.

## Size

S

## Open Questions

- Is Inter self-hosted or loaded from a CDN? Self-hosting avoids a third-party
  request on a page handling household financial data.
- The spec fixes type sizes in px. Should these be rem for user font-size
  preferences, which story 033's accessibility criteria would normally require?
- Dark mode is explicitly out of scope (design spec section 8) — should the token
  block still be structured so it could be added later, or is that the "tempting
  weekend detour" the spec warns against?
