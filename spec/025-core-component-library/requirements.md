# Requirements Specification

<!-- Feature name: 025-core-component-library -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 025) -->
<!-- Link: backlog/025-core-component-library.md -->
<!-- Branch: main -->

## Feature Overview

The general-purpose UI components every screen composes from (design spec §3): button
(C-01), text field (C-02), toggle group (C-03), checkbox (C-04), stat card (C-08), warning
banner (C-09), and schedule table (C-10). Each is a parameterized Thymeleaf fragment
styled only with story 024's tokens, built in every state the spec lists, so no screen
improvises a state later. Two details are easy to skip and expensive to retrofit: the
visible focus ring on every interactive element, and the green tint on schedule rows where
a debt is retired.

## User Stories

### Story 1: Components with every state
**As** a developer building screens
**I want** ready fragments for each component in each state
**So that** screens are assembled, not styled from scratch

**Acceptance Criteria:**
- [ ] Each component in REQ-002 exists as a Thymeleaf fragment with its listed states.
- [ ] Every interactive element shows the 3px `blue-100` focus ring outside a `blue-600`
  border.

### Story 2: Review them all in one place
**As** the designer or reviewer
**I want** a gallery rendering every component in every state
**So that** visual regressions are easy to spot

**Acceptance Criteria:**
- [ ] `/dev/components` renders the gallery when the `dev` profile is active and returns
  404 otherwise.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: Fragments live in `src/main/resources/templates/fragments/`, one file per
  component, taking parameters (label, name, value, state, error message).
- **REQ-002**: Components and states:
  - C-01 Button: primary (pill), secondary, quiet × default, hover, focus, disabled.
  - C-02 Text field: default, focused, error (message slot tied via `aria-describedby`),
    read-only; label always visible.
  - C-03 Toggle group: two options, one selected, as a radio group.
  - C-04 Checkbox: on, off.
  - C-08 Stat card: neutral, positive; figure uses tabular numerals.
  - C-09 Warning banner: caution (gold surface) for warnings; problem (red) only for
    validation summaries and failed imports.
  - C-10 Schedule table: header row with `<th scope="col">`, row header per month with
    `<th scope="row">`, standard row, and debt-retired row tinted `green-100`.
- **REQ-003**: All styling uses story 024 tokens; no inline `style` attributes.
- **REQ-004**: Components render server-supplied strings only; no fragment formats or
  computes a number (PRD AC-G).
- **REQ-005**: A `dev`-profile-only gallery controller in `adapter.in.web.view` renders
  every component and state at `/dev/components`.

### Should Have (P1)
- **REQ-006**: Fragment tests render each component with Thymeleaf and assert key markup
  (labels, `aria-describedby`, `scope` attributes, state classes) using Jsoup.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- Fragments add no JavaScript.

### Security
- All dynamic text uses Thymeleaf's escaped output (`th:text`), never `th:utext`.

### Accessibility / Browser Support
- Focus ring on every interactive element; gold never used as text on white; status never
  conveyed by color alone.

## Constraints and Assumptions

### Technical Constraints
- Thymeleaf fragments only; no JavaScript component framework (design spec §7).

### Assumptions
- The ladder and verdict components are story 026.

## Out of Scope
- Stage ladder, status chip, progress bar, verdict block — story 026.
- Screen layouts — stories 027–031.

## Success Metrics
- Gallery shows every state; fragment tests pass.
- A search of templates finds no `th:utext` and no inline `style`.
