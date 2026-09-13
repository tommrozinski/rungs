---
status: approved
---

# Accessibility acceptance suite

The design spec's accessibility rules, built and verified — "treated as
acceptance criteria, not aspiration."

> **Approved 2026-09-13.** Full requirements, which supersede the criteria and open
> questions below: [spec/033-accessibility-acceptance-suite/requirements.md](../spec/033-accessibility-acceptance-suite/requirements.md).
> Decided: JVM-only checks in `verify` (token contrast test and Jsoup markup tests);
> axe via Selenium as an opt-in local profile, not in CI; WCAG 2.2 AA; manual
> keyboard and VoiceOver passes; live region on projection results.

## Description

Design spec section 6. The spec states these as requirements rather than
intentions, so this story makes them checkable. Several are already embedded in
the component stories (focus rings in 025, the ordered-list ladder and
non-color-dependent status in 026); this story is where they are verified as a
set and where the remainder are built.

The substantive ones:

- **Contrast**: all text meets WCAG AA. Gold is a surface and border only, never
  text on white, because it fails at body sizes.
- **Status is never carried by color alone.** Every chip has a text label; every
  stage disc shows a number, a check, or a dash.
- **The schedule table uses real `<th>` scope attributes** — a screen reader
  should announce "month 3, store card, cleared."
- **`prefers-reduced-motion` respected.** The only motion in the product is the
  progress bar filling on load, and it is skipped entirely under that query.
- **Fully keyboard operable**, with the slider on a $25 arrow-key step.

## Acceptance Criteria

- All text meets WCAG AA contrast against its background, verified by an
  automated check across every screen.
- Gold never appears as text on a white or near-white surface.
- Every status chip carries a text label; every stage disc carries a number, a
  check, or a dash.
- Every interactive element has a visible focus ring (3px `blue-100` outside a
  `blue-600` border).
- The stage ladder is an ordered list with `aria-current="step"` on the current
  stage.
- The schedule table uses `<th>` with correct `scope` attributes; a screen reader
  announces row and column context.
- `prefers-reduced-motion` suppresses the progress-bar animation entirely.
- Every screen is fully keyboard operable, including load, save, slider, and
  toggle.
- The slider responds to arrow keys with a $25 step.
- Form fields have associated labels, and error messages are programmatically
  associated with their field.
- An automated accessibility check runs over every screen in the build.
- Manual keyboard and screen-reader passes are recorded for each screen.

## Size

M

## Open Questions

- Which automated tool, and does it run in `mvn verify` or as a separate CI job
  (story 019)? Design spec section 7 bans `package.json` and any frontend
  toolchain, which rules out Pa11y and Lighthouse CI as normally installed. The
  JVM-friendly option is axe-core via its Java bindings (Selenium or Playwright
  for Java) driving the real Thymeleaf pages — confirm that is acceptable.
- Is AA the target throughout, or AAA for the verdict block given it is the one
  piece of large display type?
- Are live regions needed when the projection screen re-renders after a slider
  change? A screen reader user gets no notification that the numbers changed
  otherwise.
- What screen reader and browser combinations are in scope for the manual pass?
