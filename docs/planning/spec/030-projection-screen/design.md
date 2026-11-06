# Design: 012 + 034 + 030 — Warnings, htmx and JavaScript policy, projection screen

<!-- Source: requirements.md in spec/012, spec/034, spec/030; prd/design-spec.md §4 S-03, §7 -->
<!-- Ticket: none (backlog stories 012, 030, 034) -->
<!-- Date: 2026-09-13 -->

Combined design (lean process). The requirements hold the rules, copy, and acceptance criteria.

## 1. Layout

```
domain/model/
  WarningCode, Severity, Warning        (012 REQ-001, REQ-002; replaces application.Warning)
  MonthlyInterest.on(Money, BigDecimal) the one interest formula, used by the simulator and the detector
domain/stage/
  WarningDetector.detect(FinancialSnapshot)                                     (012 REQ-003)
application/
  AssessmentService, ProjectionService  call the detector; response.WarningResponse gains severity, debtIds
  ProjectionPage                        ProjectionResponse + firstRetirementMonth + debt labels   (030)
  ProjectionService.projectPage(ProjectionInput)                                                 (030)
config/ (or adapter/in/web)
  SecurityHeadersFilter                 CSP, nosniff, Referrer-Policy on every response           (034)
adapter/in/web/view/
  ProjectionPageController              POST /projection, /projection/results, /projection/schedule (030)
  ProjectionPageViews + view records    formatted stats, schedule rows, value label
templates/
  layout.html                           htmx-config meta, noscript "Rungs needs JavaScript turned on." (034)
  projection/page.html                  control card, #results (fragment: results), schedule rows fragment
static/js/vendor/VERSIONS.md            htmx 2.0.10, SHA-256, 0BSD license                        (034)
```

## 2. Decisions

### 012 warnings
- `Warning(WarningCode code, Severity severity, String message, List<String> debtIds)` in `domain.model`;
  `application.Warning` is deleted and `ReadinessAssessment` carries the domain type.
- `MonthlyInterest.on(balance, annualRate)` = balance × rate ÷ 1200 (DECIMAL128, one half-even round to the
  cent). `PayoffSimulator` is refactored to call it, so warning and schedule agree (012 constraint).
- `NEGATIVE_AMORTIZATION` when `minimumPayment < MonthlyInterest.on(balance, rate)`. Label lists: one
  label; two → "A and B"; three or more → "A, B, and C" (Oxford comma, per the three-label example).
- `{shortfall}` is `monthlySurplus().negate().toDisplayString()`; `{n}` is the supplied months.
- `EMERGENCY_FUND_TARGET_CAPPED` says the target uses 6 months. The implementer first confirms
  `FullEmergencyFundEvaluator` caps at 6; if it does not, stop and report (a real contradiction).
- Wire: `WarningResponse(code, severity, message, debtIds)`. Assessment and projection responses both
  carry the detector's list; the existing goldens keep `warnings: []` because their scenarios trigger
  nothing (the implementer confirms, never edits a golden to pass).
- The 029 page banner and 030 page use the message as the banner body.

### 034 policy
- One servlet filter sets the REQ-002 CSP, `X-Content-Type-Options: nosniff`, and
  `Referrer-Policy: no-referrer` on every response (API included; harmless for JSON, simpler than route
  matching).
- `layout.html` head adds `<meta name="htmx-config" content='{"allowEval":false,
  "includeIndicatorStyles":false,"responseHandling":[…swap 2xx, swap 400 and 422, error otherwise…]}'>`.
  `includeIndicatorStyles:false` matters: htmx otherwise injects an inline style the CSP blocks.
- noscript copy becomes "Rungs needs JavaScript turned on." (tests from 027 that pin the old copy change).
- `VERSIONS.md` next to the vendored file. The existing AC-G scan already enforces REQ-004's budget.
- REQ-006 tests: no `Set-Cookie` on any view route; results routes reject GET (405).

### 030 projection screen
- **Binding.** The page posts the 029 hidden snapshot fields at the root (same names as the form), plus
  `extraMonthlyPayment` and `strategy`. The controller binds `@ModelAttribute SnapshotInput` and the two
  request params, and builds `ProjectionInput` with horizon 360. It validates through
  `SnapshotValidator.validate(ProjectionInput)`. `SnapshotBindingAdvice` (027 fixes) covers this controller too.
- **Failures.** On `POST /projection`, invalid input re-renders the snapshot form with errors (like 029).
  On `/projection/results` and `/projection/schedule`, invalid input returns 400 with a small caution
  fragment; 034's htmx config swaps it.
- **Controls.** Range 0–2000 step 25; the server accepts any valid money amount (the slider only produces
  steps, and the API has no cap beyond 011's). C-03 toggle for strategy.
  The form carries `hx-post="/projection/results" hx-trigger="input changed delay:250ms"
  hx-target="#results" hx-sync="this:replace"`.
  - **Deviation (review fix):** dropped `changed` from the trigger — htmx's `changed` modifier
    caches the last value per dispatching element, and a radio's `value` never changes, so
    re-clicking an already-checked strategy radio (Snowball → Avalanche → Snowball) was silently
    suppressed and the results went stale. Trigger is now `hx-trigger="input delay:250ms"`;
    `hx-sync="this:replace"` still discards any now-stale in-flight request.
- **Value label.** "$300 extra a month" is rendered by the server inside the results fragment with
  `hx-swap-oob="true"` into the control card, so it updates on every response. The slider has
  `aria-describedby` pointing to it. `aria-valuetext` is omitted because it would go stale without script.
- **Stats.**
  - Debt-free in: "34 months", "1 month", "0 months", or "Not within 30 years" when null.
  - Total interest: the display money.
  - First debt paid off: "Month N", or "Not within 30 years" when no debt retires.
  - Every string is built in Java.
- **Schedule.**
  - Columns are Month, then "{label} payment" and "{label} balance" for each debt in supplied order.
  - A debt with no line that month (already retired) shows "—" in both cells.
  - A row is tinted when `retiredDebtIds` is non-empty.
  - The first 24 rows render initially. A "Show all {n} months" button (`hx-post="/projection/schedule"`,
    `hx-include` the control form) swaps the table body with every row and removes the button.
- **Truncated.** The table foot reads "This plan doesn't finish within 360 months."; warnings render as
  C-09 banners above the stats.
- **Compare link (REQ-007).** Left out until story 031, which is deferred. Recorded as a deviation.
- **Stage 2 entry.** 029's stage-2 CTA already posts the hidden snapshot to `/projection`; the page opens
  with extra $0 and SNOWBALL.
- **Live region.** `#results` has `aria-live="polite"`.

## 3. Tests

- **012:** each condition with its verbatim message (NEGATIVE_AMORTIZATION with one, two, and three
  debts); all three together, in code order; no conditions → empty list; `MonthlyInterest` half-cent
  rounding; simulator goldens unchanged after the refactor; contract tests showing warnings on
  `/v1/assessments` and `/v1/projections` with `code`, `severity`, `message`, and `debtIds`.
- **034:** headers on `/`, `/assessment`, `/projection`, and `/v1/assessments`; the htmx-config meta and
  noscript notice on every page; no `Set-Cookie`; GET on results routes is 405; the AC-G scan stays
  green; `VERSIONS.md` hash matches the file.
- **030:**
  - The full page and the results fragment, including the OOB label.
  - Stats strings.
  - Schedule columns and "—" cells.
  - The 24-row cap and show-all.
  - The truncated foot.
  - The warnings banner.
  - The `hx-*` markup (stale-response rule).
  - Invalid input on each route.
  - A figures-match test against `/v1/projections` for the AC-2.1 household.
  - The 029 stage-2 CTA landing.

## 4. Phases

| Phase | Work | Engineer | Where |
| --- | --- | --- | --- |
| 1 | 012 warnings | Sonnet | main checkout |
| 1 | 034 policy | Sonnet | worktree (parallel; touches only layout, filter, vendor docs, tests) |
| 2 | 030 projection screen | Sonnet | main checkout, after both phase 1 tasks merge |

One Sonnet code-reviewer pass over the batch, covering CSP, binding, and warning copy.
