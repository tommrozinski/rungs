# Requirements Specification

<!-- Feature name: 033-accessibility-acceptance-suite -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 033) -->
<!-- Link: backlog/033-accessibility-acceptance-suite.md -->
<!-- Branch: main -->

## Feature Overview

The design spec's accessibility rules (§6), verified as a set — "treated as acceptance
criteria, not aspiration". Automated checks run in `./mvnw verify` on the JVM with no
browser: a contrast test computed from the color tokens, and markup assertions over every
rendered screen (labels, error association, table headers, ladder semantics, live regions,
reduced motion). A browser-based axe scan is available as an optional local run, and
manual keyboard and screen-reader passes are recorded per screen.

## User Stories

### Story 1: Usable by everyone
**As** a user relying on a keyboard or screen reader
**I want** every screen operable and understandable
**So that** I get the same answers as anyone else

**Acceptance Criteria:**
- [ ] Every screen is fully keyboard operable and announces results changes.
- [ ] Status is never conveyed by color alone.

### Story 2: Checked by the build
**As** the engineering reviewer
**I want** accessibility rules enforced automatically where possible
**So that** regressions fail the build

**Acceptance Criteria:**
- [ ] Contrast and markup checks run in `./mvnw verify` without a browser.
- [ ] Manual passes are recorded in `docs/accessibility.md`.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: Contrast test: computes WCAG contrast from story 024 token hex values for
  every text/background pair the CSS uses, requiring AA (4.5:1 body, 3:1 large text and UI
  components); asserts gold is never a text color on white or `surface`.
- **REQ-002**: Markup tests (MockMvc + Jsoup) over S-01 to S-04 and the import report:
  - every input has a `<label>`; errors tied by `aria-describedby`;
  - the ladder is an `<ol>` with exactly one `aria-current="step"`;
  - chips and discs carry text, not color only;
  - tables use `<th scope="col">` and `<th scope="row">`;
  - projection results region has `aria-live="polite"`; progress bars have
    `role="progressbar"` and values;
  - one `<h1>` per page and no skipped heading levels;
  - every interactive element is a native control or has a role and accessible name.
- **REQ-003**: The CSS includes a `prefers-reduced-motion: reduce` rule that disables the
  progress-bar transition; a test asserts the rule exists.
- **REQ-004**: Focus ring (story 025) present on all interactive elements; a test asserts
  the `:focus-visible` rule uses the focus tokens.
- **REQ-005**: `docs/accessibility.md` records manual passes per screen: keyboard-only in
  Chrome, and VoiceOver with Safari on macOS, with date and findings.

### Should Have (P1)
- **REQ-006**: An opt-in Maven profile `-Paxe` runs axe-core via Selenium against a locally
  installed Chrome for every screen. It is not part of `./mvnw verify` or CI, so the build
  still needs only a JDK.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- Automated checks add under 15 seconds to `verify`.

### Security
- Not applicable.

### Accessibility / Browser Support
- Target WCAG 2.2 AA throughout, including the verdict block.

## Constraints and Assumptions

### Technical Constraints
- JVM-only in the build; no Node tooling (AC-H).

### Assumptions
- Manual screen-reader passes are sufficient for dynamic behavior not observable in static
  markup.

## Out of Scope
- WCAG AAA; screen readers beyond VoiceOver.

## Success Metrics
- Contrast and markup tests pass and fail on planted fixtures.
- `docs/accessibility.md` has a recorded pass for every screen before increment 3.5 closes.
