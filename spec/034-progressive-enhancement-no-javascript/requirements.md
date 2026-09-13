# Requirements Specification

<!-- Feature name: 034-progressive-enhancement-no-javascript (retitled: htmx integration and JavaScript policy) -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 034) -->
<!-- Link: backlog/034-progressive-enhancement-no-javascript.md -->
<!-- Branch: main -->

## Feature Overview

Originally "every screen works with JavaScript disabled"; on 2026-09-13 that rule was
dropped, and JavaScript is now required. This story now owns how JavaScript enters the web
client: vendored htmx, the hand-written script budget, a strict Content-Security-Policy,
htmx configuration that works under that policy, the carrying of page state in hidden
fields, and a clear notice for browsers with scripting off. Pages stay server-rendered
Thymeleaf; scripts only manage page state and htmx requests (design spec §7).

## User Stories

### Story 1: A small, safe script footprint
**As** the engineering reviewer
**I want** JavaScript limited to vendored htmx and a small audited budget under a strict CSP
**So that** the client stays a viewer and resists script injection

**Acceptance Criteria:**
- [ ] htmx is vendored at a pinned version and served locally.
- [ ] A strict Content-Security-Policy header is sent on every page.
- [ ] Hand-written JavaScript stays within ~200 lines.

### Story 2: Clear expectations without JavaScript
**As** a user with scripting disabled
**I want** to be told the app needs JavaScript
**So that** I'm not left with a broken page

**Acceptance Criteria:**
- [ ] Every page includes a `<noscript>` notice: "Rungs needs JavaScript turned on."

## Functional Requirements

### Must Have (P0)
- **REQ-001**: htmx vendored as `src/main/resources/static/js/vendor/htmx.min.js` at a pinned
  version recorded in `static/js/vendor/VERSIONS.md` with its license.
- **REQ-002**: Response header on all view routes: `Content-Security-Policy: default-src
  'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self';
  connect-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'self'`.
- **REQ-003**: htmx configured via `<meta name="htmx-config">` with `allowEval: false`,
  `includeIndicatorStyles: false`, and response handling that swaps 400 validation
  fragments, so it runs under the CSP.
- **REQ-004**: No inline `<script>` blocks or `on*=` attributes; hand-written scripts live in
  `static/js/*.js` (outside `vendor/`), page state only, ≤ 200 lines total — enforced by
  story 015's scan.
- **REQ-005**: A layout fragment includes htmx, hand-written scripts (`defer`), the CSP-safe
  config meta tag, and the `<noscript>` notice on every page.
- **REQ-006**: Page state travels in hidden form fields (design spec §7): every page form
  includes the snapshot; no cookies are set and no figures appear in URLs. A test asserts
  responses set no `Set-Cookie` header and view routes accept POST only for results pages.

### Should Have (P1)
- **REQ-007**: `X-Content-Type-Options: nosniff` and `Referrer-Policy: no-referrer` on view
  routes.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- htmx plus hand-written JS under 20 KB gzipped; scripts deferred.

### Security
- Strict CSP with no `unsafe-inline` or `unsafe-eval`; no third-party origins.

### Accessibility / Browser Support
- htmx swaps preserve focus where the user is working (story 027) and use live regions for
  results (story 030).

## Constraints and Assumptions

### Technical Constraints
- No frontend build or package manager (AC-H).

### Assumptions
- Dropping the no-JavaScript requirement removes the need for full-page fallbacks such as a
  submit-button slider and server round-trip-only row editing.

## Out of Scope
- No-JavaScript operation (dropped 2026-09-13).
- Screen behaviors — stories 027–031.

## Success Metrics
- Tests assert the CSP and security headers, the `<noscript>` notice, no `Set-Cookie`, and
  the htmx config meta tag on every page.
- Browser console shows no CSP violations during a manual pass (recorded in story 033's
  document).
