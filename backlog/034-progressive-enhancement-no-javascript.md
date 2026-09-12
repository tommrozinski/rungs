---
status: proposed
---

# Progressive enhancement and no-JavaScript operation

Every screen renders and works with JavaScript disabled, with the slider
degrading to a number input.

## Description

Design spec section 7: "Every screen must render correctly with JavaScript
disabled except the slider, which degrades to a number input."

This is a cross-cutting constraint rather than a feature, and it is listed as its
own story because it is the kind of requirement that is cheap to hold if it
shapes the screens from the start and expensive to retrofit once four screens
assume a fetch-and-patch model.

Design spec section 7 now makes that model the default rather than a risk:
server-rendered Thymeleaf in one Maven module, no SPA, no frontend build. Every
screen starts life as ordinary HTML forms and links, so the no-JS path is the
baseline, not a fallback. htmx, which is part of this increment, layers on top of
forms that already work — its `hx-` attributes must never be the only way a
control functions.

This story also owns the JavaScript policy from design spec section 7 as a
whole: external files only, page state only, enhancement only, and roughly 200
hand-written lines in total. Story 015 automates the check. It also reinforces the spec's central rule — a
client that holds no logic has little reason to need JavaScript in the first
place, so any screen that *cannot* work without it is a signal that logic leaked
client-side.

The genuinely hard cases are the ones the screen stories flag as open questions:
add-row on the debts table (story 027), the download and unsaved-changes confirm
in the file bar (story 028), and the live re-render on slider change (story 030).

## Acceptance Criteria

- S-01 through S-04 render and complete their primary task with JavaScript
  disabled.
- The projection slider degrades to a number input with a submit action.
- Form submission works without JavaScript on every screen.
- Adding and removing debt rows works without JavaScript.
- CSV export download works without JavaScript.
- CSV import works without JavaScript, including rendering the import report.
- htmx is vendored as a single pinned file in `src/main/resources/static/js` and
  served by the application — no CDN.
- Every `hx-` enhanced control also works as a plain form submission or link
  with scripting disabled.
- No inline `<script>` blocks or inline event handlers; all JavaScript is in
  external files, and a strict Content-Security-Policy header is set.
- Hand-written JavaScript holds page state only and stays within the ~200-line
  budget.
- No screen shows an empty state, a broken layout, or a dead control with
  JavaScript disabled.
- A test pass covers every screen with scripting disabled.
- Where a no-JS path is meaningfully worse, the degradation is documented rather
  than hidden.

## Size

M

## Open Questions

- The unsaved-changes confirm before loading a file (story 028) cannot work
  without JavaScript. Is a server-side interstitial acceptable, or is this an
  accepted degradation?
- Does the no-JS add-row path require a server round trip per row, or does the
  form ship with a fixed number of blank rows?
- ~~Does no-JS validation fall back to submit only?~~ **Resolved.** Yes — on-blur
  validation is an htmx enhancement, and submit checks every rule (story 027).
- Is the no-JS pass automated in CI (story 019) or manual? If automated, it runs
  inside `mvn verify` with a JVM browser driver (e.g. HtmlUnit with scripting
  off, or Selenium/Playwright for Java), since no Node toolchain is permitted.
