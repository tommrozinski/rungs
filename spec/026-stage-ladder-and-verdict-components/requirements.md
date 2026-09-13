# Requirements Specification

<!-- Feature name: 026-stage-ladder-and-verdict-components -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 026) -->
<!-- Link: backlog/026-stage-ladder-and-verdict-components.md -->
<!-- Branch: main -->

## Feature Overview

The components the design hangs on (design spec §3): the status chip (C-05), stage row
(C-06), progress bar (C-07), and verdict block (C-11). The ladder shows all seven stages as
an ordered list, every row gives a reason, and exactly one row — the current stage — is
expanded with its blocker, progress bar, and single call to action. The verdict block is
the design's one bold moment: the current stage's blocker in large type, white on solid
blue. All text comes from the assessment result verbatim.

## User Stories

### Story 1: See every stage and why
**As** a household reading my assessment
**I want** all seven stages with a reason on each
**So that** I understand where I stand and what doesn't apply to me

**Acceptance Criteria:**
- [ ] All seven stages render in order as an `<ol>`, the current one with
  `aria-current="step"`.
- [ ] In-progress and not-started rows show the blocker; skipped rows show the skip reason.
- [ ] Every chip has a text label and every disc shows a number, check, or dash.

### Story 2: One clear verdict
**As** a household
**I want** one sentence naming what stands between me and the next stage
**So that** the most important fact is unmissable

**Acceptance Criteria:**
- [ ] The verdict block shows the current stage's blocker.
- [ ] When the current stage is 7 and satisfied, it shows fixed copy instead.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: C-05 status chip variants `SATISFIED` ("Done"), `IN_PROGRESS`
  ("In progress"), `NOT_STARTED` ("Not started"), `SKIPPED` ("Doesn't apply"), colored by
  story 024 status tokens.
- **REQ-002**: C-06 stage row: disc (number, check for satisfied, dash for skipped), stage
  title, chip, and reason text. The expanded variant (current stage only) adds a gold
  border, the blocker, a C-07 progress bar, and one call to action.
- **REQ-003**: Call to action on the expanded row: "See the payoff plan" when the current
  stage is 2; "Change my numbers" otherwise. Both are form submits carrying the snapshot in
  hidden fields (design spec §7).
- **REQ-004**: C-07 progress bar shows `percentComplete`: gold below 100, green at 100,
  with `role="progressbar"` and `aria-valuenow`. It fills with a transition, disabled under
  `prefers-reduced-motion`.
- **REQ-005**: The connector line runs behind the discs and stops at the first and last
  row (CSS only).
- **REQ-006**: C-11 verdict block: Display type (34px), white on `blue-600`, text = current
  stage blocker. For current stage 7 with status `SATISFIED`, the text is "Every earlier
  stage is complete or doesn't apply to you."
- **REQ-007**: Stage titles are fixed copy: 1 "Starter emergency fund", 2 "Pay off debt",
  3 "Full emergency fund", 4 "Save for retirement", 5 "Save for college", 6 "Pay off your
  home", 7 "Build wealth".
- **REQ-008**: Blocker, skip reason, and percentages are rendered verbatim from the
  assessment result; no rewording or formatting in templates (AC-G).

### Should Have (P1)
- **REQ-009**: Fragment tests assert the `<ol>`, `aria-current`, chip labels, one expanded
  row, and the stage 7 verdict copy.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- CSS only; no JavaScript.

### Security
- Escaped output only (`th:text`).

### Accessibility / Browser Support
- Status never by color alone; progress bar exposed to assistive technology; motion
  respects reduced-motion.

## Constraints and Assumptions

### Technical Constraints
- Uses tokens (024) and base components (025).

### Assumptions
- Stage titles are neutral vocabulary with no trademarked names (PRD §1).

## Out of Scope
- Assessment page composition — story 029.
- Mobile compression of rows — story 032.

## Success Metrics
- Fragment tests pass for all four statuses, the expanded row, and both verdict cases.
- Visual review in the component gallery (025).
