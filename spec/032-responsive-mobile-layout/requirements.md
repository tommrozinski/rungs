# Requirements Specification

<!-- Feature name: 032-responsive-mobile-layout -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 032) -->
<!-- Link: backlog/032-responsive-mobile-layout.md -->
<!-- Branch: main -->

## Feature Overview

S-05 (design spec §4): the layout at phone width, targeting 375px. Everything stacks, stat
cards go from three across to two, stage rows compress, and the primary action becomes
full width and sticks to the bottom of the ladder. Non-current stage rows hide their reason
behind a disclosure instead of dropping it, keeping the "every row shows a reason" rule
intact on a small screen. The schedule table scrolls horizontally inside its own container.

## User Stories

### Story 1: Use it on my phone
**As** a household on a phone
**I want** every screen readable and operable at 375px
**So that** I don't need a computer

**Acceptance Criteria:**
- [ ] No horizontal page scroll from 320px to 480px.
- [ ] Stat cards two across; stage rows compressed; sticky full-width primary action.

### Story 2: Reasons still reachable
**As** a mobile user
**I want** each stage's reason available on tap
**So that** compression never hides information completely

**Acceptance Criteria:**
- [ ] Non-current rows show a "Why?" disclosure revealing the blocker or skip reason.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: Breakpoints: mobile ≤ 480px, tablet 481–960px, desktop > 960px; mobile-first
  CSS in `rungs.css` using story 024 tokens.
- **REQ-002**: Mobile: single column; stat cards two across; stage discs 30px, titles 13px;
  current-row reason always visible; other rows' reasons inside `<details>` with summary
  "Why?".
- **REQ-003**: The primary action is full width and `position: sticky` at the bottom of the
  ladder on mobile.
- **REQ-004**: Schedule and comparison tables scroll horizontally within a focusable
  container (`tabindex="0"`, labelled), never the page.
- **REQ-005**: File bar actions stack full width on mobile, both visible.
- **REQ-006**: Touch targets at least 44×44px.
- **REQ-007**: The verdict block wraps without truncating its figure at 320px.

### Should Have (P1)
- **REQ-008**: A Jsoup test asserts the `<details>` disclosure markup on non-current rows.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- CSS only; no layout JavaScript.

### Security
- Not applicable.

### Accessibility / Browser Support
- Disclosure is native `<details>`, keyboard and screen-reader operable; scroll containers
  are reachable by keyboard.

## Constraints and Assumptions

### Technical Constraints
- Tokens only; no framework.

### Assumptions
- Visual checks at 320, 375, 768, and 1280px are done manually and recorded in
  `docs/accessibility.md` (story 033).

## Out of Scope
- Native apps; landscape-specific layouts.

## Success Metrics
- Manual checks at the four widths recorded with no horizontal page scroll.
- Disclosure markup test passes.
