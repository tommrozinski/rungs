# Requirements Specification

<!-- Feature name: 024-design-tokens-and-css-foundation -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 024) -->
<!-- Link: backlog/024-design-tokens-and-css-foundation.md -->
<!-- Branch: main -->

## Feature Overview

The single hand-written stylesheet foundation for the web client (design spec §2): the
`:root` design tokens copied verbatim from the design board, the type scale, spacing,
radii, the two elevations, and a reset. Every later component consumes these tokens, so
no color or measurement is ever defined anywhere else. Two rules carry product meaning:
color carries status (green satisfied, gold in progress, grey not started, blue skipped or
actionable, red only for input validation), and all currency and duration figures use
tabular numerals so values don't jitter when results re-render.

## User Stories

### Story 1: One source for every visual value
**As** a developer building components
**I want** every color and measurement available as a token
**So that** the palette can change in one place and never drifts

**Acceptance Criteria:**
- [ ] A `:root` block defines all fifteen color tokens at the design spec hex values,
  copied from `prd/design-board.html`.
- [ ] Type, spacing, radius, and elevation tokens exist.
- [ ] A build check fails if a color literal appears outside the token block.

### Story 2: Readable, stable figures
**As** a user comparing numbers on screen
**I want** figures aligned and steady
**So that** columns scan cleanly and re-renders don't shift layout

**Acceptance Criteria:**
- [ ] A `.num` utility applies `font-variant-numeric: tabular-nums`.
- [ ] Inter is self-hosted with a system fallback stack.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: One stylesheet, `src/main/resources/static/css/rungs.css`, served as a static
  resource; no framework, preprocessor, or CSS build step.
- **REQ-002**: Color tokens (`--blue-700` … `--surface`) copied verbatim from the design
  board's `:root` block.
- **REQ-003**: Type roles as rem values at a 16px root: Display 2.125rem/800 with
  -0.025em tracking (34px, per the §2.2 type table, which is authoritative over the 30px
  mentioned in §1), H1 1.5rem/700, H2 1.0625rem/700, Body 0.875rem/400 with line height
  1.6 and max 60ch, Small 0.78125rem/400.
- **REQ-004**: Spacing tokens 4, 8, 12, 16, 22, 32, 44 px (as rem); radius tokens field
  6px, card 10px, hero 16px, pill 999px; exactly two elevation tokens (flat, card).
- **REQ-005**: Inter is self-hosted as WOFF2 under `static/fonts/` with its OFL license
  file, `font-display: swap`, and fallback `system-ui, -apple-system, "Segoe UI", Roboto,
  sans-serif`.
- **REQ-006**: Semantic status tokens map to palette tokens (`--status-satisfied`,
  `--status-in-progress`, `--status-not-started`, `--status-skipped`, `--validation-error`)
  so components use meaning, not hues.
- **REQ-007**: A JUnit scan fails if a hex, `rgb(`, or `hsl(` color literal appears in
  `static/css` outside the `:root` token block, or in any template.

### Should Have (P1)
- **REQ-008**: The stylesheet stays under about 400 lines including components added by
  stories 025–026, in line with the design spec's ~300-line budget.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- One CSS file and at most two font files; fonts preloaded for the regular and bold
  weights.

### Security
- No third-party requests: fonts and CSS are served from the application (fits the
  strict Content-Security-Policy in story 034).

### Accessibility / Browser Support
- rem-based sizes respect the user's browser font-size setting.
- Gold is never a text color on white (enforced in story 033's contrast test).
- Current evergreen Chrome, Firefox, Safari, and Edge.

## Constraints and Assumptions

### Technical Constraints
- No CSS framework or build tooling (design spec §7, PRD AC-H).
- Palette values are approximations; no third-party brand asset is included.

### Assumptions
- Dark mode is out of scope (design spec §8); tokens are not pre-structured for it.

## Out of Scope
- Components — stories 025, 026.
- Responsive rules — story 032.

## Success Metrics
- The color-literal scan passes and fails on a planted literal fixture.
- Every token from the design board appears in `rungs.css` with an identical value.
