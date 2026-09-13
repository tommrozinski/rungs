# Tasks: 024–026 — Tokens, core components, stage ladder and verdict

<!-- Source: spec/024-design-tokens-and-css-foundation/design.md -->
<!-- Ticket: none (backlog stories 024, 025, 026) -->
<!-- Date: 2026-09-13 -->

## Phase 1: Tokens and foundation (024)

- [ ] Add static/css/rungs.css with the board's :root tokens verbatim, semantic status tokens, type/spacing/radius/elevation tokens, reset and .num; self-host Inter 4.1 InterVariable.woff2 with its license; add ColorLiteralScanTests with planted-literal fixtures
  - ID 1.1 · Depends on: none

## Phase 2: Core components (025)

- [ ] Add Jsoup, fragments for button, text field, toggle group, checkbox, stat card, warning banner, and schedule table in every state, the dev-profile component gallery, FragmentRenderer, fragment tests, gallery 200/404 tests, and a th:utext/style scan
  - ID 2.1 · Depends on: 1.1

## Phase 3: Stage ladder and verdict (026)

- [ ] Add StageRowView, VerdictView, StageRowViews (titles, chip labels, CTA, stage 7 verdict copy), fragments for status chip, stage row, progress bar, ladder, and verdict, gallery entries, and fragment tests for all four statuses, the expanded row, CTAs, and both verdict cases
  - ID 3.1 · Depends on: 2.1
