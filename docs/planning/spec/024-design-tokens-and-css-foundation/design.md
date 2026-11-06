# Design: 024–026 — Tokens, core components, stage ladder and verdict

<!-- Source: requirements.md in spec/024, 025, 026; prd/design-spec.md §2, §3, §6, §7; prd/design-board.html -->
<!-- Ticket: none (backlog stories 024, 025, 026) -->
<!-- Date: 2026-09-13 -->

Combined design (lean process). The requirements and the design spec hold the exact states, copy and
token values.

## 1. Layout

```
src/main/resources/static/
  css/rungs.css                  tokens (:root), reset, utilities, components (025, 026); about 400 lines max
  fonts/InterVariable.woff2      Inter 4.1 variable font (weights 100–900)
  fonts/LICENSE.txt              OFL, from the same release
src/main/resources/templates/
  fragments/button.html, text-field.html, toggle-group.html, checkbox.html, stat-card.html,
            warning-banner.html, schedule-table.html                        (025)
  fragments/status-chip.html, stage-row.html, progress-bar.html, stage-ladder.html,
            verdict.html                                                    (026)
  dev/components.html            gallery page
src/main/java/com/rungs/readiness/adapter/in/web/view/
  ComponentGalleryController     @Profile("dev") GET /dev/components; sample data only
  GallerySamples                 server-side sample strings and view records for the gallery
src/test/java/com/rungs/readiness/adapter/in/web/view/
  FragmentRenderer (test helper), *FragmentTests, ComponentGalleryTests
src/test/java/com/rungs/readiness/architecture/
  ColorLiteralScanTests          (024 REQ-007)
```

## 2. Story 024 — tokens and foundation

- **Colors.** The design board's `:root` block is copied verbatim, which gives 20 color tokens: the 15
  in the design spec table plus `--blue-500`, `--gold-600`, `--green-700`, `--red-100` and `--white`.
  Story 024's "fifteen" undercounts; REQ-002 and design spec §7 both say to copy verbatim, and that
  takes precedence.
  - The board's `--font` token and radius tokens are copied too.
  - The board's two shadows are `--shadow-card` and `--shadow-frame`. REQ-004 asks for exactly two
    elevations named flat and card, so they are renamed `--elevation-flat` (`none`) and
    `--elevation-card` (the board's card shadow). `--shadow-frame` belongs to the design board's
    page frame, not the product, and is dropped.
- **Semantic tokens** (REQ-006):
  - `--status-satisfied: var(--green-600)`
  - `--status-in-progress: var(--gold-500)`
  - `--status-not-started: var(--ink-500)`
  - `--status-skipped: var(--blue-600)`
  - `--validation-error: var(--red-600)`
- **Type** (REQ-003) and **spacing** (REQ-004) are rem tokens at a 16px root. Small type is
  0.78125rem, the design spec's 12.5px.
- **Font** (REQ-005):
  - Inter 4.1 `InterVariable.woff2` covers 400, 700 and 800 in one file, which meets "at most two font
    files".
  - `@font-face` uses `font-weight: 100 900` and `font-display: swap`.
  - Layouts preload it later (029). The gallery page preloads it now.
  - The implementer downloads the official release zip from GitHub rsms/inter v4.1 and commits only the
    WOFF2 file and the license.
- **Base.** A small reset, then `body` using the Body role on `--surface`, and the `.num` utility for
  tabular numerals.
- **`ColorLiteralScanTests`:**
  - Hex colors (`#rgb`, `#rrggbb`, `#rrggbbaa`), `rgb(`/`rgba(` and `hsl(`/`hsla(` fail outside the
    first `:root { … }` block of each CSS file under `static/css`.
  - The same patterns fail anywhere in templates, including in `th:` attributes.
  - Violations are proven with `@TempDir` fixtures: one planted literal per pattern, plus a literal
    inside `:root` that passes. It follows the style of the existing architecture scans.

## 3. Story 025 — core components

- **One fragment file per component** (REQ-001). Parameters are plain strings or booleans supplied by
  the server. Each state is a class modifier chosen by a parameter (e.g. `state='error'`), mapped with
  `th:classappend` from literal-only expressions.
  - No template arithmetic or formatting is allowed. The AC-G scan (015) already rejects operators
    inside or between expressions, `#numbers`, inline scripts, `on*=`/`hx-on` handlers and inline
    `style`.
  - Only `th:text` is used; `th:utext` is never used.
- **Focus.** One shared rule: `:focus-visible { outline: 3px solid var(--blue-100); outline-offset: 0;
  border-color: var(--blue-600) }` on buttons, inputs, radios and checkboxes. Hover and disabled states
  are CSS pseudo-classes. The gallery shows the "hover" and "focus" states with forced modifier classes
  (`is-hover`, `is-focus`) that share those rules, so they can be reviewed statically.
- **Text field:** always has a `<label for>`. In the error state it adds `aria-invalid="true"` and an
  `aria-describedby` pointing to `<p id="{name}-error">`. The id is built server-side as a parameter,
  because string concatenation in the template would trip the AC-G scan.
- **Toggle group:** a `fieldset` of radios with a visually hidden legend.
- **Schedule table:** `<th scope="col">` headers and `<th scope="row">` for the month. The debt-retired
  row gets the `is-retired` class, which uses the `--green-100` background.
- **Gallery** (REQ-005):
  - `ComponentGalleryController` has `@Profile("dev")`, so without the profile the mapping doesn't
    exist and the route returns 404.
  - The model comes from `GallerySamples`, with every string prepared server-side.
- **Tests** (REQ-006):
  - `FragmentRenderer` builds a standalone `SpringTemplateEngine` with a classpath resolver, so no
    Spring context is needed. It renders a fragment with given variables and returns a Jsoup
    `Document`.
  - Tests assert labels, `aria-describedby`, `scope`, state classes and escaping (a `<b>` in a label
    renders as text).
  - `ComponentGalleryTests` uses `@SpringBootTest` + MockMvc: 200 under `dev` and 404 otherwise.
  - A template scan fails on `th:utext` and on any `style=` attribute.
- **New test dependency:** `org.jsoup:jsoup` 1.23.2.

## 4. Story 026 — stage ladder and verdict

- **Fragment inputs are view records built server-side** (in `adapter.in.web.view`). Story 029 maps
  them from `application.response.AssessmentResponse`, the same objects the API returns; for now the
  gallery builds them from samples.
  - `StageRowView(int stage, String title, String status, String chipLabel, String percentComplete,
    String reason, boolean current, String ctaLabel, String ctaAction)`
  - `VerdictView(String text)`
  - The title (REQ-007), the chip label (REQ-001), the reason (blocker or skip reason), the call to
    action ("See the payoff plan" when the current stage is 2, otherwise "Change my numbers") and the
    stage 7 verdict copy are all chosen in Java, not in templates. Templates only print.
  - The mapping from `AssessmentResponse` to these views is story 029's job. 026 provides the records
    and a `StageRowViews` factory with fixed copy that 029 will call.
- **Ladder:** an `<ol class="ladder">`. Each `<li>` holds the disc, title, chip and reason. The disc
  shows a check glyph for SATISFIED, a dash for SKIPPED and the number otherwise, with visually hidden
  text such as "Stage 3". The current row has `aria-current="step"` and the expanded class, with a
  gold border, the progress bar and the call-to-action form.
  - The call-to-action form posts to `ctaAction` with a hidden-fields slot, a fragment-expression
    parameter that 029 fills with the snapshot fields.
  - The connector line is a `::before` on each `li`, clipped on `:first-child` and `:last-child`
    (REQ-005).
- **Progress bar** (REQ-004): no inline style is allowed (025 REQ-003, and 034's CSP), so the fill
  can't be set with a `style` width.
  - Use a `<div role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="{percent}">`
    containing an inline SVG `<rect>` whose `width` attribute is a server-supplied percent string (for
    example `"50.0%"`).
  - Color comes from a status class: gold below 100, green at 100.
  - `transition: width` is applied where the browser supports CSS geometry properties, and removed under
    `prefers-reduced-motion: reduce`.
  - If SVG width can't be set without template string building, the implementer supplies the complete
    attribute value from Java.
- **Verdict:** the Display role, white on `--blue-600`, hero radius. The text comes from `VerdictView`.
- **Tests** (REQ-009): the `<ol>` has 7 `li`, exactly one `aria-current="step"` and one expanded row;
  chip labels for all four statuses; the disc glyph and hidden text; the stage 2 call to action versus
  the others; the stage 7 satisfied verdict copy; and the progress bar ARIA values.

## 5. Phases and execution

| Phase | Story | Notes |
| --- | --- | --- |
| 1 | 024 | CSS tokens, font, color-literal scan |
| 2 | 025 | Needs phase 1; adds Jsoup |
| 3 | 026 | Needs phase 2 |

- The three phases run in sequence as one implementation-engineer, in a worktree (branched from
  `origin/main` at `b21d832`), in parallel with 007/011 in the main checkout. Nothing overlaps except
  `pom.xml`: this group adds Jsoup and 011 adds the validation starter. The merge applies each as a
  patch.
- One code review and one security review for the group.
- Visual check: run the app with `--spring.profiles.active=dev` and open `/dev/components`.

## 6. Risks

| Risk | Mitigation |
| --- | --- |
| The AC-G scan rejects a needed template idiom | Supply values from Java; never weaken the scan (015 assumption) |
| SVG width transition unsupported in some browsers | Motion is decoration; the bar is still correct without it |
| CSS exceeds about 400 lines | Shared rules for focus and state; review at the end of each phase |
| Font download needs network | Implementer fetches the official v4.1 release; checksum noted in progress |
