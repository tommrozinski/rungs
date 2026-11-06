# Requirements Specification

<!-- Feature name: 031-comparison-screen -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 031) -->
<!-- Link: backlog/031-comparison-screen.md -->
<!-- Branch: main -->

## Feature Overview

S-04 (design spec §4): snowball and avalanche side by side in two aligned columns, a green
header on whichever costs less interest, and a dark delta block that states the difference
in plain sentences. The closing line notes that when the gap is small, the order matters
less than the habit — fixed copy, because the engine reports arithmetic and never
recommends (NG-4). Every verdict and figure comes from the comparison result.

## User Stories

### Story 1: See both orders together
**As** a household choosing a payoff order
**I want** both plans aligned row by row
**So that** I can compare them by scanning across

**Acceptance Criteria:**
- [ ] Two columns with identical row labels: debt-free in, total interest, first debt paid
  off.
- [ ] The cheaper strategy's column header is green, driven by `cheaperStrategy`.

### Story 2: The difference in words
**As** a household
**I want** the gap stated plainly
**So that** I don't have to subtract

**Acceptance Criteria:**
- [ ] A delta block states the difference in sentences composed by the server.
- [ ] Identical and truncated cases show their notes.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `ComparisonPageController` in `adapter.in.web.view` handles `POST /comparison`,
  calling the comparison application service in-process, with its own extra-payment control
  (same range as story 030) that re-renders via htmx.
- **REQ-002**: Columns "Snowball" and "Avalanche", rows "Debt-free in", "Total interest",
  "First debt paid off"; green header on the `cheaperStrategy` column; no green header when
  `SAME` or null.
- **REQ-003**: Delta block on a `navy-900` surface with white text. Sentences come from the
  application view model, e.g. "Avalanche costs $412.30 less in interest." and "Both orders
  finish in 34 months." When `note` is present, its message is shown instead of any
  comparison it replaces.
- **REQ-004**: Closing line (fixed copy): "When the gap is small, the order matters less than
  the habit."
- **REQ-005**: No chart (design spec §9).
- **REQ-006**: "Back to the payoff plan" posts to `POST /projection` with the snapshot and
  controls.

### Should Have (P1)
- **REQ-007**: A figures-match test against `/v1/projections/comparison` for the three-debt
  scenario.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- Page render within the comparison endpoint budget (< 500ms locally).

### Security
- No figures in URLs.

### Accessibility / Browser Support
- The two columns are a table with column headers; the cheaper column is also marked in
  text ("Costs less"), not by color alone.

## Constraints and Assumptions

### Technical Constraints
- No arithmetic or comparison in templates (AC-G); sentences composed server-side.

### Assumptions
- `navy-900` is an acceptable dark surface; white on it meets AA.

## Out of Scope
- Comparison endpoint — story 013.

## Success Metrics
- View tests for normal, identical, and truncated comparisons.
- Figures-match test passes.
