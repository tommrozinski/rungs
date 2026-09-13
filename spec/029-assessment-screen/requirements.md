# Requirements Specification

<!-- Feature name: 029-assessment-screen -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 029) -->
<!-- Link: backlog/029-assessment-screen.md -->
<!-- Branch: main -->

## Feature Overview

S-02, the primary screen (design spec §4): the verdict block, three stat cards, any
warnings, then the seven-stage ladder. Its order is deliberate — the verdict leads,
warnings sit above the ladder because they change how the numbers read, and every stage is
always shown. The page is rendered from the same assessment result the API returns,
obtained in-process, and a test proves its figures match the endpoint's JSON.

## User Stories

### Story 1: Where am I and what's next
**As** a household that just entered my numbers
**I want** my verdict, key figures, warnings, and full ladder on one screen
**So that** I understand my position at a glance

**Acceptance Criteria:**
- [ ] Order: verdict, stat cards, warnings, ladder.
- [ ] All seven stages render, including skipped ones with their reasons.
- [ ] Warnings render as caution banners above the ladder.

### Story 2: Numbers I can trust
**As** the engineering reviewer
**I want** the page to show exactly what the API returns
**So that** the client provably holds no logic

**Acceptance Criteria:**
- [ ] A test renders the page and the `/v1/assessments` JSON for the same input and
  asserts every figure matches.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `AssessmentPageController` in `adapter.in.web.view` handles
  `POST /assessment`, calling the assessment application service in-process; the page model
  is built from the same response object the API serializes.
- **REQ-002**: Stat cards: "Monthly surplus" (`monthlySurplus`, positive variant when above
  $0), "Total debt" (sum of debt balances, computed by the application layer), and
  "Emergency fund" (full emergency fund). All figures are server display strings.
- **REQ-003**: Warnings (story 012) render with C-09 caution above the ladder; none renders
  no banner.
- **REQ-004**: Verdict (story 026) and ladder (story 026) with the snapshot carried in
  hidden fields for the expanded row's call to action and for "Change my numbers".
- **REQ-005**: Validation failures on `POST /assessment` re-render the form (story 027)
  instead of this page.
- **REQ-006**: A figures-match test compares every rendered amount, percentage, blocker,
  and skip reason with the `/v1/assessments` JSON for three households.

### Should Have (P1)
- **REQ-007**: The page title and H1 are "Your stage", and the current stage number is in
  the document title for screen reader orientation.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- Page render within the NFR-3 assessment budget plus template time (< 100ms locally).

### Security
- Snapshot only in hidden fields; no figures in URLs.

### Accessibility / Browser Support
- Heading order: H1 page title, H2 per section; ladder semantics from story 026.

## Constraints and Assumptions

### Technical Constraints
- No arithmetic in templates; "Total debt" comes from the application view model (AC-G).

### Assumptions
- Stage 7 satisfied uses the story 026 fixed verdict copy.

## Out of Scope
- Components — stories 025, 026.
- Projection screen — story 030.

## Success Metrics
- Figures-match test passes for the three example households.
- View tests for with and without warnings, and a validation-failure redirect to the form.
