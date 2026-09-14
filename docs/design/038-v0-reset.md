# Design: 038 — v0 reset (one form, one result page)

<!-- Source: docs/scope.md, docs/mockups/snapshot-form.html, docs/mockups/assessment-result.html -->
<!-- Ticket: none (backlog story 038) -->
<!-- Date: 2026-09-13 -->

The app shrinks to the v0 scope: the seven-stage evaluator, a form at `GET /`, and a result page from
`POST /assess`. Pages are plain server-rendered HTML that work without JavaScript. The two mockup HTML
files are the markup contract. Templates are those files with `th:` attributes added, never rewritten from
a description.

## 1. Decisions (user, 2026-09-13)

- Strip everything outside v0 (list in §2). Git history keeps it.
- Use `docs/mockups/snapshot-form.html` as it stands:
  - money fields;
  - a debts table (Name, Type, Balance, Rate %, Minimum, ✕);
  - checkboxes for rent, college, and retired.

  No mortgage, no college amount, no "Load an example".
- No warning banner. Stage 2 shows only "N debts remain, totalling $X.", with no progress bar and no
  payoff button.
- The app name is **Rungs**. The mock's "Gazelle" wordmark becomes "Rungs" and the glyph "R".

## 2. Keep and strip

**Keep:**
- `domain/model`: Money, Debt, DebtType, FinancialSnapshot, HouseholdFlags, Mortgage, Invariants, MoneyFormatException.
- `domain/stage`: the seven evaluators, StageLadder, LadderResult, StageEvaluation, StageStatus, Percentages.
- Their JUnit tests.
- `RungsApplication`, `RungsApplicationTests`, the Maven wrapper, and a plain CI `./mvnw verify`.

**Strip:**
- `domain/projection`, `domain/port`, `domain/stage/WarningDetector`, `domain/model` Warning,
  WarningCode, Severity, MonthlyInterest.
- `application` in full: input, validation, response, services, ExampleHouseholds. A thin
  `AssessmentService` may stay if the controller uses it.
- `adapter/in/web/api`, `adapter/out`, and the current `adapter/in/web/view` contents.
- `config` beans except what the new controller needs, and the security headers filter.
- Every template, fragment, script, vendored htmx, and the current CSS. The Inter font may stay.
- Test side: ArchUnit, the architecture fixtures, scans, golden files, contract tests, Actuator tests,
  and view tests.
- In `pom.xml`: JaCoCo, jqwik, ArchUnit, Actuator, and validation dependencies if they are now unused.
  Keep the Enforcer plugin only if it's harmless.
- The Dockerfile, .dockerignore, and the README Podman section.
- Old specs stay as history.

## 3. Web layer

- `SnapshotController` with the following routes:
  - `GET /`: an empty form with one empty debt row.
  - `POST /assess`: renders the result page.
  - `POST /debts/add` and `POST /debts/remove/{index}`: re-render the form with the posted values kept.
    The Add button and the ✕ buttons are `<button formaction=… formnovalidate>`, so they need no JS.
- **Binding.** Form names come from the mock (`monthlyTakeHomeIncome`, `debts[0].label`, `rents`, …).
  A small form record binds them. Blank or unparseable numbers become 0, per addendum item 5, so there
  are no rejection messages. A debt row with a zero balance is dropped. A blank label becomes "Debt N".
  Unchecked boxes mean false. Emergency fund months are fixed at 3. The mortgage is always absent.
- **CSS.** The two mock `<style>` blocks merge into `static/css/rungs.css`. The mock-only `.echo` and
  `.note` rules are dropped. Inline `style="…"` attributes in the mock markup stay as they are.
- **Templates.** `templates/form.html` comes from snapshot-form.html. `templates/result.html` comes from
  assessment-result.html. Delete the build notes, the echo panels, and every `<script>`. Keep all
  other markup and classes.

## 4. Result page copy (view model, built in Java)

**Eyebrow:** "You're on stage N of 7", or "All seven stages are done or skipped" when none remain.

**Stats:**
- Monthly surplus: positive green when above $0.
- Non-mortgage debt.
- Stages complete: "K of 7", where K counts Done stages only.

**Verdict by current stage:**

| Stage | Line | Sub |
| --- | --- | --- |
| 1 | "$X more and your starter fund is done." | "Save $1,000 before anything else." |
| 2 | "$D stands between you and debt freedom." | "At your current surplus of $S a month, that's about N months of work." (N = ceil(D ÷ S); if S ≤ $0: "Right now your expenses and minimums use up your take-home pay.") |
| 3 | "$X more and three months of expenses are covered." | Same surplus sentence using X |
| 4 | "Raise retirement saving to 15%." | "You're at P%." |
| 5 | "Start saving for college." | "Set aside something every month." |
| 6 | "Pay off the house." | "Every earlier stage is done or skipped." |
| 7 | "Build wealth and give." | "Every earlier stage is done or skipped." |

Money shows no cents when it's whole ("$12,480"). N and "months" agree in number.

**Stage titles:**
1. "Save $1,000 to start"
2. "Pay off everything but the house"
3. "Save three months of expenses"
4. "Put 15% toward retirement"
5. "Save for college"
6. "Pay off the house"
7. "Build wealth and give"

**Chips:** Done, In progress, Not started, Skipped.

**Discs:**
- Done: ✓ (`done`).
- The current stage or In progress: its number (`active`).
- Not started: its number (plain).
- Skipped: – (`skipped`).

The current stage renders inside `.stage-open` with the blocker paragraph.

**Stage lines (the small text, or the blocker in the open card):**

| Stage | Done | In progress / Not started | Skipped |
| --- | --- | --- | --- |
| 1 | "$F of $1,000." | "$F of $1,000." plus the bar when in progress | — |
| 2 | "No debts besides the house." | "Three debts remain, totalling **$D**." ("One debt remains, …"; spelled-out counts for one to ten) | — |
| 3 | "$F of $T." | "$F of $T." (T = 3 × expenses) plus the bar when in progress | — |
| 4 | "You're at P%." | "You're at P%. Nine points to go." (spelled-out whole numbers one to ten, else digits) plus the bar | "You told us you're retired." |
| 5 | "You're saving for college." | "You plan to fund college." | "You told us you're not funding college." / "You told us you're retired." |
| 6 | "No mortgage to pay down." | — | "You rent, so there's no mortgage to pay down." |
| 7 | "Every earlier stage is done or skipped." | "Opens once stages 1 through 6 are done or skipped." | — |

**Checkpoint.** The board household (6200, 3500, 1000, 0, 6%, rents, the three board debts) must
reproduce `docs/mockups/assessment-result.html` text exactly, except the removed banner, the stage 2
payoff line, the bar, and the button.

## 5. Tests (basic, per addendum item 4)

- The existing per-stage JUnit tests, updated for any copy moves.
- A view-model test for the copy table.
- One controller test: GET / renders the form. POST /assess with the board household matches the
  checkpoint text. Add and remove keep the typed values. A blank or bad number becomes 0.
- **Visual check.** Take headless Chrome screenshots of the prd files and the running app at the same
  width, then compare them by eye.

## 6. Phases

| Phase | Work |
| --- | --- |
| 1 | Strip (§2); build green with the remaining domain tests |
| 2 | Form template, CSS, controller with GET /, add/remove, POST /assess echoing values onto a static copy of the result page; screenshot check |
| 3 | Real assessment on the result page with the §4 copy and the checkpoint test; screenshot check |

Each phase runs on a Sonnet implementer and ends in a commit. After phase 3: one Sonnet review, then push.
