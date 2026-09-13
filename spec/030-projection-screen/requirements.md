# Requirements Specification

<!-- Feature name: 030-projection-screen -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 030) -->
<!-- Link: backlog/030-projection-screen.md -->
<!-- Branch: main -->

## Feature Overview

S-03 (design spec §4): an extra-payment slider and a strategy toggle in one control card,
three outcome stats, and the month-by-month schedule. Moving either control re-renders the
results from a fresh server-side projection via htmx, debounced by 250ms, with any
in-flight request cancelled so the screen always shows the latest values. Rows where a
debt is retired are tinted green — the emotional payload of the screen — and a truncated
plan says so at the foot of the table.

## User Stories

### Story 1: What if I paid more?
**As** a household at stage 2
**I want** to drag an extra-payment slider and see the plan change
**So that** I can judge whether the extra money is worth it

**Acceptance Criteria:**
- [ ] Slider $0–$2,000 in $25 steps; strategy toggle snowball/avalanche.
- [ ] Changes re-render results via htmx after 250ms; stale responses are discarded.

### Story 2: When is each debt gone
**As** a household
**I want** a schedule with paid-off months highlighted
**So that** I can see the milestones

**Acceptance Criteria:**
- [ ] Outcome stats and a schedule table with retired-debt rows tinted green.
- [ ] A truncated plan is stated at the foot of the table.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `ProjectionPageController` in `adapter.in.web.view`: `POST /projection`
  renders the full page; `POST /projection/results` renders only the results fragment.
  Both call the projection application service in-process.
- **REQ-002**: Control card: range input `extraMonthlyPayment` 0–2000 step 25 with a
  visible value label rendered by the server on each response; C-03 toggle for strategy.
- **REQ-003**: htmx on the control form: `hx-post="/projection/results"`,
  `hx-trigger="input changed delay:250ms"`, `hx-target="#results"`,
  `hx-sync="this:replace"`. The snapshot travels in hidden fields.
- **REQ-004**: Outcome stats: "Debt-free in" (`monthsToDebtFreedom` as "34 months", or
  "Not within 30 years" when null), "Total interest" (`totalInterestPaid`), and "First debt
  paid off" (month of the first retirement, from the application view model).
- **REQ-005**: Schedule table (C-10): one row per month with each debt's payment and closing
  balance; retired-debt rows tinted. Only the first 24 months render initially, with a
  "Show all {n} months" button that loads the rest via htmx.
- **REQ-006**: When `truncated`, the table foot reads "This plan doesn't finish within
  {horizon} months." Unpayable debts are named in a caution banner via warnings (story 012).
- **REQ-007**: A link-styled form submit "Compare snowball and avalanche" posts to
  `POST /comparison` (story 031) with the current controls and snapshot.
- **REQ-008**: The results region has `aria-live="polite"` so updated outcomes are
  announced; the slider has an accessible name and value text.

### Should Have (P1)
- **REQ-009**: A figures-match test compares the page's schedule and stats with the
  `/v1/projections` JSON for the same input.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- Results fragment round trip under 250ms p95 for a 360-month projection (NFR-3).

### Security
- No figures in URLs; results computed server-side only (AC-G).

### Accessibility / Browser Support
- Slider operable by keyboard with $25 arrow steps; live region announces changes.

## Constraints and Assumptions

### Technical Constraints
- JavaScript required (htmx); no client-side interpolation or math.

### Assumptions
- Horizon fixed at the 360-month default on this screen.

## Out of Scope
- Projection endpoint — story 010.
- Comparison screen — story 031.
- Mobile table behavior — story 032.

## Success Metrics
- View tests for full page, results fragment, truncated plan, and "show all".
- Figures-match test passes; the stale-response rule is covered by a test of
  `hx-sync` markup.
