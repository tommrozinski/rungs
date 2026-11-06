# Design: 027 + 029 — Snapshot form screen and assessment screen

<!-- Source: requirements.md in spec/027 and spec/029; prd/design-spec.md §4 S-01, S-02, §6, §7 -->
<!-- Ticket: none (backlog stories 027, 029) -->
<!-- Date: 2026-09-13 -->

Combined design (lean process). The requirements hold the field lists, copy, and acceptance criteria.

## 1. Layout

```
application/
  ExampleHouseholds              three preset SnapshotInputs by id (local-model task)
  SnapshotInput.forForm()        FR-4 display normalization that keeps $0 rows (blank → "0", months 3, "Debt N")
  AssessmentPage                 AssessmentResponse + totalDebt + emergencyFund (MoneyResponse), built in-process
  AssessmentService.assessPage(SnapshotInput)
adapter/in/web/view/
  SnapshotFormController         GET /, POST /, POST /snapshot/debts/add, POST /snapshot/debts/remove/{index},
                                 POST /snapshot/field, POST /snapshot/example/{id}
  AssessmentPageController       POST /assessment
  FormErrors                     violations → pointer → ValidationCode message (same text as the API)
templates/
  layout.html                    head (rungs.css, font preload, vendored htmx), noscript notice, main slot
  snapshot/form.html             S-01; fragments: debts-table, field
  assessment/page.html           S-02
  fragments/hidden-snapshot.html the snapshot as hidden inputs (feeds the 026 CTA slot and "Change my numbers")
static/js/vendor/htmx.min.js     pinned htmx 2.x, version and SHA-256 recorded in progress
static/js/snapshot-form.js       mortgage section toggle only (027 REQ-011); counts toward the 200-line budget
```

## 2. Decisions

- **Presets without CSV.** Story 021, the import service, is deferred, so "Load an example" reads from
  `ExampleHouseholds` in `application` instead of CSV files (a deviation from 027 REQ-008). When 020 and 021
  land, the presets move to `src/main/resources/examples/*.csv`, but the endpoint stays the same.
- **htmx.** The vendoring part of story 034 is pulled forward: one pinned file under `static/js/vendor/`,
  loaded by `layout.html`. This group skips 034's CSP and policy tests.
- **Page state.**
  - Form field names follow `SnapshotInput`'s property paths (`monthlyTakeHomeIncome`,
    `debts[2].balance`, `mortgage.homeValue`, `householdFlags.rents`).
  - Posts bind to `SnapshotInput` through Spring's constructor binding for nested records and lists, via
    `@ModelAttribute`. The implementer confirms Spring 7 supports this and otherwise adds a small form
    record in the view.
  - Unchecked checkboxes send nothing, which binds to null, and null means false.
  - No session, no cookies, and no figures in URLs. Row indexes are allowed in paths because they aren't
    figures.
- **Validation.**
  - Whole-form validation uses `validator.validate(input, FieldsThenWholeForm.class)`.
  - On blur, `validateProperty` checks only the posted field, on `SnapshotInput`, `DebtInput`, or
    `MortgageInput`.
  - Messages come from `ValidationCode` through the same rejection mapping the API uses, so the form
    shows identical text. `FormErrors` keys messages by JSON Pointer, and each field fragment looks up
    its pointer.
  - A failed submit re-renders the form with every error, plus a C-09 problem summary listing
    "{n} field(s) need attention." with the same count wording as the API.
- **Form re-render (027 REQ-007).**
  - `SnapshotInput.forForm()` returns a copy where blank numbers become `"0"`, a null months value
    becomes `3`, and blank labels become `Debt N` by position.
  - Debts with a `$0` balance are kept. Only `toSnapshot()` removes them.
  - Every rule lives in `application`.
- **Add and remove row.**
  - htmx posts the whole form and swaps the debts-table fragment.
  - Adding a 51st debt with a balance above zero is refused with the 011 `TOO_MANY_DEBTS` message on
    the table.
  - Rows with a `$0` or blank balance don't count toward the limit (011 REQ-003).
- **On-blur.**
  - `hx-post="/snapshot/field"` with `hx-trigger="blur"` on each input, sending the field name and value
    (`hx-include` of the field itself).
  - It swaps the field's wrapper by its stable id with `outerHTML`. htmx 2 restores focus to an element
    with the same id.
  - `hx-sync="this:replace"`, per design spec §7.
- **Assessment page (029).**
  - `POST /assessment` validates with `FieldsThenWholeForm`. If validation fails, the response renders
    `snapshot/form.html` with the errors, at status 200 or 422 (the implementer picks one; the test pins
    it). It does not redirect, because a redirect would lose the posted figures.
  - If validation passes, `AssessmentService.assessPage(input)` returns an `AssessmentPage` built from
    the same `AssessmentResponse` the API serializes, plus:
    - `totalDebt`: `MoneyResponse` of `snapshot.totalDebtBalance()`, computed in `application`
      (029 REQ-002, AC-G).
    - `emergencyFund`: `MoneyResponse` of the full emergency fund.
  - The monthly-surplus stat card uses its positive variant when `amount` is above zero. The Java view
    model supplies that decision as a boolean, so the template does no comparison.
  - The view maps `StageResponse` values to 026's `StageRowViews.row(...)` and `verdict(...)`.
  - The call to action, "Change my numbers", posts to `/`.
  - Change 026's placeholder `CHANGE_NUMBERS_ACTION` from `"/snapshot"` to `"/"`. `PAYOFF_PLAN_ACTION`
    stays `"/projection"` until story 030. The stage 2 call to action therefore has no handler until 030
    lands (a tier 2 risk, see §5).
  - Warnings render as C-09 caution banners above the ladder. The list is always empty until story 012,
    but the rendering path is tested with a stubbed response.
  - The title and H1 are "Your stage", and the document title includes the stage number, for example
    "Your stage: 2 of 7 · Rungs". That string is built in Java.
- **Mortgage toggle.** `snapshot-form.js` shows or hides the mortgage section when "I rent" changes.
  The server also renders the correct visibility on every response, so the form works if the script
  fails. It uses no arithmetic and no fetch, which the AC-G scan checks.

## 3. Example households (`ExampleHouseholds`, fixed values)

Every value is a `SnapshotInput` string. Flags not listed are false, and unlisted money fields are `"0"`.

| id | Values | Lands on |
| --- | --- | --- |
| `deep-in-debt` | income 4200, expenses 3100, starter 400, full 400, months 3, retirement 0; rents true; debts: "Store card" CREDIT_CARD 1850 @ 24.99 min 55; "Car loan" AUTO 9400 @ 6.9 min 285; "Student loan" STUDENT_LOAN 21300 @ 5.5 min 240; no mortgage | stage 1 |
| `mid-journey` | income 6800, expenses 4200, starter 1000, full 12600, months 3, retirement 8.5, college contribution 0; intendsToFundCollege true; no debts; mortgage balance 186000 @ 6.25, payment 1450, home value 310000 | stage 4 |
| `nearly-done` | income 9500, expenses 5000, starter 1000, full 30000, months 6, retirement 15, college contribution 250; intendsToFundCollege true; no debts; mortgage balance 42000 @ 3.75, payment 900, home value 420000 | stage 6 |

`byId(String)` returns `Optional<SnapshotInput>`, and `ids()` returns the three ids in this order. An
unknown id on `POST /snapshot/example/{id}` returns 404.

## 4. Tests

- **027:**
  - MockMvc + Jsoup view tests for: the empty form (labels, subhead, button copy); a re-render with errors
    (each error tied through `aria-describedby`, plus the summary); add row; remove row; refusing a 51st
    real debt; the on-blur fragment with and without an error; each preset; and `forForm` normalization
    after submit (`0`, visible `$0` row, `Debt N`).
  - Messages must equal the API's for the same input: call `/v1/assessments` and compare the error
    messages by pointer.
- **029:**
  - A figures-match test for the three example households. It compares every rendered amount,
    percentage, blocker, and skip reason with the `/v1/assessments` JSON.
  - Section order (verdict, stats, warnings, ladder).
  - All seven stages, including the skipped ones.
  - Warnings banner present or absent.
  - A validation failure re-renders the form.
  - Hidden snapshot fields round-trip, so "Change my numbers" reproduces the same form.
- **`ExampleHouseholds`:** unit tests of ids, values, and unknown id, plus each preset's current stage
  through `AssessmentService`.
- **Gates:** WebClientScan (AC-G), TemplateMarkupScan (no inline style or `th:utext`), ColorLiteralScan,
  ArchUnit AC-C (the view package depends only on `application`), and JaCoCo.

## 5. Phases

| Phase | Work | Engineer |
| --- | --- | --- |
| 1 | `ExampleHouseholds` + tests (§3) | local `qwen3-coder:30b` through the workflow's local-model step; Sonnet if it falls back |
| 2 | 027: layout, htmx vendoring, `forForm`, `FormErrors`, form controller and templates, mortgage toggle | Sonnet implementation-engineer |
| 3 | 029: `AssessmentPage`, `assessPage`, page controller and template, 026 action constant, figures-match test | Sonnet implementation-engineer |

The phases run in sequence in the main checkout. There is one Sonnet code-reviewer pass for 027 + 029
together, since they contain view logic and validation wiring. Phase 1 gets no separate review, because
it is data plus tests and CI covers it.

## 6. Risks

| Risk | Mitigation |
| --- | --- |
| Spring 7 constructor binding for nested record lists | The implementer verifies it early; the fallback is a view form record with setters (no public fields, per AC-E) |
| Stage 2's "See the payoff plan" has no handler until 030 | Tier 2 builds 030. If it isn't done by the demo, the stage 2 call to action renders as "Change my numbers" (a one-line constant change) |
| AC-G scan rejects htmx attributes or templates | `hx-post`, `hx-trigger`, `hx-swap` and `hx-sync` are allowed; `hx-on` is banned, and no inline JS is used |
| On-blur focus loss | htmx 2 restores focus to an element with the same id; a view test pins the ids |
