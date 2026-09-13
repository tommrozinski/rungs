# Backlog — Financial Readiness Engine

Candidate user stories drafted from [`prd/readiness-engine-prd.md`](../prd/readiness-engine-prd.md)
and [`prd/design-spec.md`](../prd/design-spec.md).

**This file is generated.** It is rebuilt in full every time stories are drafted —
edit the individual story files, not this index.

Nothing here is approved. Every story is `proposed` until a human reviews it and
changes the status in the story file itself.

## Engine — increments 1 to 3

| ID | Title | Status | Summary |
| --- | --- | --- | --- |
| [001](001-project-scaffold-and-package-layout.md) | Project scaffold and package layout | approved | Single-module Maven / Java 25 / Spring Boot 4.1 + Thymeleaf skeleton; no frontend toolchain. |
| [002](002-money-value-type.md) | Money value type | approved | Whole-cent `Money` backed by `long`, owning rounding, parsing, and display format. |
| [003](003-financial-snapshot-domain-model.md) | Financial snapshot domain model | proposed | Immutable `FinancialSnapshot`, `Debt`, `Mortgage`, `HouseholdFlags` with value equality. |
| [004](004-stage-evaluation-framework.md) | Stage evaluation framework and progression rules | proposed | `StageEvaluator` contract and the ladder logic that picks the current stage. |
| [005](005-stage-evaluators-1-to-3.md) | Stage evaluators 1–3: starter fund, non-mortgage debt, full emergency fund | proposed | The three cash-and-debt stages with percent-complete and verdict-ready blockers. |
| [006](006-stage-evaluators-4-to-7.md) | Stage evaluators 4–7: retirement, college, mortgage, wealth building | proposed | The four lifecycle stages, including skip rules for renters and non-funders. |
| [007](007-assessment-endpoint.md) | Assess readiness endpoint | proposed | `POST /v1/assessments` returning the full ladder, current stage, and surplus. |
| [008](008-debt-payoff-simulation-engine.md) | Debt payoff simulation engine | proposed | Month-by-month amortization: interest, minimums, surplus, cascade, horizon. |
| [009](009-snowball-and-avalanche-strategies.md) | Snowball and avalanche payoff strategies | proposed | The two debt orderings with a documented, tested tie-break rule. |
| [010](010-projection-endpoint.md) | Project the path forward endpoint | proposed | `POST /v1/projections` with defaults, horizon clamp, and golden fixtures. |
| [011](011-request-validation-and-problem-details.md) | Request validation and RFC 9457 problem details | proposed | Boundary validation whose error copy the client renders verbatim. |
| [012](012-assessment-warnings.md) | Non-fatal warnings | proposed | The four conditions that qualify an assessment without failing it. |
| [013](013-strategy-comparison-endpoint.md) | Compare strategies endpoint | proposed | `POST /v1/projections/comparison` with a delta derived from both projections. |
| [014](014-property-based-invariant-suite.md) | Property-based invariant test suite | proposed | jqwik generator plus the projection invariants across all inputs. |
| [015](015-archunit-architecture-suite.md) | ArchUnit architecture constraint suite | proposed | Section 8's eight constraints, including AC-G (no client logic) and AC-H (one module). |
| [016](016-openapi-document.md) | OpenAPI 3.1 document generation | proposed | Generated and committed contract covering all five endpoints, plus ADRs. |
| [017](017-observability-actuator-and-metrics.md) | Observability: health probes and metrics | proposed | Actuator probes and Micrometer metrics including a stage-verdict counter. |
| [018](018-structured-logging-with-redaction.md) | Structured logging with money redaction | proposed | JSON logs with correlation IDs and provably no figures or CSV row content. |
| [019](019-ci-pipeline-with-quality-gates.md) | CI pipeline with coverage and architecture gates | proposed | A thin wrapper around `mvn verify` that gates coverage, architecture, fixtures. |
| [037](037-container-image.md) | Container image | proposed | Multi-stage Dockerfile, non-root, read-only filesystem, built and run with Podman. |

## CSV snapshot import/export — increment 3.5a

| ID | Title | Status | Summary |
| --- | --- | --- | --- |
| [020](020-csv-snapshot-format-and-parser.md) | CSV snapshot format and parser | proposed | The versioned format and its domain-side reader and writer, RFC 4180 quoting. |
| [021](021-snapshot-import-endpoint.md) | Snapshot import endpoint | proposed | `POST /v1/snapshots/import` returning a snapshot plus a per-row `ImportReport`. |
| [022](022-snapshot-export-and-in-memory-upload-handling.md) | Snapshot export and in-memory upload handling | proposed | `POST /v1/snapshots/export` plus the config proving no upload touches disk. |
| [023](023-csv-round-trip-property-test.md) | CSV round-trip property test | proposed | AC-7.1 as a property: export-then-import is the identity on valid snapshots. |

## Web client — increment 3.5b

| ID | Title | Status | Summary |
| --- | --- | --- | --- |
| [024](024-design-tokens-and-css-foundation.md) | Design tokens and CSS foundation | proposed | The `:root` token block, type scale, spacing, and the semantic color rule. |
| [025](025-core-component-library.md) | Core component library | proposed | Buttons, fields, toggles, stat cards, warning banners, schedule table. |
| [026](026-stage-ladder-and-verdict-components.md) | Stage ladder and verdict components | proposed | The status chip, stage row, progress bar, and verdict block. |
| [027](027-snapshot-form-screen.md) | S-01 Snapshot form screen | proposed | The only input screen, with inline validation bound to the API's problem details. |
| [028](028-file-bar-and-import-report-ui.md) | File bar and import report UI | proposed | Load/save with a visible saved-vs-working distinction, and the 3-state report. |
| [029](029-assessment-screen.md) | S-02 Assessment screen | proposed | Verdict, stats, warnings, then the seven-stage ladder. |
| [030](030-projection-screen.md) | S-03 Projection screen | proposed | Slider and strategy toggle re-rendering live via htmx, debounced 250ms. |
| [031](031-comparison-screen.md) | S-04 Comparison screen | proposed | Two aligned columns, green header on the cheaper strategy, delta block. |
| [032](032-responsive-mobile-layout.md) | S-05 Responsive mobile layout | proposed | 375pt: stacked content, compressed stage rows, sticky primary action. |
| [033](033-accessibility-acceptance-suite.md) | Accessibility acceptance suite | proposed | Contrast, non-color status, table semantics, reduced motion, keyboard operation. |
| [034](034-progressive-enhancement-no-javascript.md) | Progressive enhancement and no-JavaScript operation | proposed | Every screen works with scripting disabled; the slider degrades to a number input. |

## Stretch — increment 4

| ID | Title | Status | Summary |
| --- | --- | --- | --- |
| [035](035-rules-based-transaction-categorization.md) | Rules-based transaction categorization | proposed | `POST /v1/categorize` on a deterministic rule table, built before the model path. |
| [036](036-llm-categorization-with-circuit-breaker.md) | LLM categorization with fallback and circuit breaker | proposed | Spring AI primary path degrading to rules under failure. |

---

## Document conflicts to resolve

These are places where the two source documents disagree with each other or with
themselves. Each is recorded in the affected stories, but they are decisions
rather than details.

- ~~NG-3 is contradicted in the PRD's own text.~~ **Resolved.** PRD NG-3, the
  summary, users, section 8, section 9, and section 12 now describe the thin
  read-only client, and design spec section 0 says the PRD was amended
  (story 016).
- ~~FR-7 cites the wrong constraint.~~ **Resolved.** PRD section 8 gains AC-G
  (no business logic in the web client) and FR-7 now cites it (story 015).
- ~~Trademark exposure in the project name.~~ **Resolved.** The project was
  renamed from `Gazelle` to **Rungs** before any code, git remote, or saved
  snapshot file existed. The CSV version marker is `rungs_snapshot_v1`. The
  original name borrowed vocabulary from the same published framework the PRD is
  otherwise careful not to name; the new one is drawn from the seven-stage ladder
  the product is built around (stories 001, 020).
- ~~Web client module structure.~~ **Resolved.** Design spec section 7 now
  decides it as final: server-rendered Thymeleaf in the same Spring Boot
  application, **one Maven module**. No SPA, no `readiness-web` module, no
  `package.json`, no frontend build tool; htmx allowed later as a single script
  tag. Knock-on effects: every web-client check (CSS token lint, accessibility,
  no-JS pass, client arithmetic ban) must run inside `mvn verify` on JVM tooling
  (stories 001, 015, 016, 019, 024, 025, 027, 028, 030, 033, 034).
- ~~How view controllers reach the domain.~~ **Resolved** (design spec section
  7). In-process calls to application services, with `adapter.in.web.api` and
  `adapter.in.web.view` as sibling adapters. Guarded by ArchUnit and a
  figures-match test (stories 001, 015, 029, 030).
- ~~Which JavaScript is allowed.~~ **Resolved** (design spec section 7).
  Hand-written JS in external `static/js` files, page state only, no arithmetic,
  formatting, or JSON fetching, roughly 200 lines. File System Access API
  save-in-place is cut (stories 015, 028, 034).
- ~~Whether htmx and live slider re-render ship.~~ **Resolved** (design spec
  section 7). htmx is part of 3.5b, vendored, driving the live slider and on-blur
  validation (stories 027, 030, 034).
- ~~PRD section 8 package layout is stale.~~ **Resolved.** It now shows
  `in.web.api`, `in.web.view`, and the `templates`/`static` resources, and adds
  AC-H (one Maven module). The PRD's Kotlin-module open question is closed as
  "no" on the same grounds (story 001).
- ~~Blur validation scope and stale slider responses.~~ **Resolved** (design spec
  S-01 and section 7). Blur checks field-only rules; the 50-debt limit runs on
  add-row and submit. A newer slider request cancels an in-flight one
  (stories 027, 030).
- ~~Where validation rules live.~~ **Resolved.** PRD FR-4 now states rules are
  declared once on input models in `application`; both web adapters trigger them
  (story 011).
- ~~Blank values and zero-balance debts.~~ **Resolved.** PRD FR-4: a missing,
  null, or blank number is zero everywhere — form, API, and CSV — except
  documented defaults (`horizonMonths` 360). A zero-balance debt is paid off and
  ignored, never an error; CSV import lists it as skipped (new AC-7.7)
  (stories 005, 007, 011, 020, 021, 023, 027). Follow-ups: an ignored row stays
  visible in the form showing $0; the 50-debt limit counts only debts above $0;
  an import that only skipped paid-off debts is clean; a blank debt label is
  named "Debt N" by position (stories 011, 020, 021, 023, 027, 028).
- **Coverage threshold.** PRD section 9 says 85% on `domain`; the standing PR
  guidance says 80% minimum, 100% desired (story 019).
- ~~Client-side constraint enforcement.~~ **Resolved.** Automated in
  `mvn verify` under PRD AC-G; design spec section 0 no longer says "enforced by
  review" (story 015).

## Unresolved questions that block multiple stories

- **The emergency-fund multiple has no home** (005, 012, 020) — section 5.2 calls
  it configurable and the `EMERGENCY_FUND_TARGET_CAPPED` warning implies a caller
  supplies it, but there is no field for it on `FinancialSnapshot` and no key for
  it in the CSV format.
- **Stage 5 can never be satisfied as written** (006, 026, 027) — the rule
  references "a college contribution", but no such field exists in the domain
  model or the CSV format.
- **`NOT_STARTED` vs `IN_PROGRESS` is undefined** (004, 026) — and it determines
  what every ladder row renders.
- **`MonthlyPeriod.stageReached`** (008, 010, 030) — requires re-evaluating the
  ladder each simulated month, but the simulation models debt balances only, so
  stages 1, 3, and 4 have no projected inputs. Needs pinning before golden files
  are committed.
- **Can a mortgage round trip through CSV?** (020, 022, 023) — the format has no
  mortgage row, so export is lossy for any household with one, and AC-7.1 cannot
  hold for those snapshots.
- **Upload transport** (021, 022) — FR-7 says the endpoint accepts `text/csv`,
  but NFR-1's multipart-spooling instruction and the design spec's file picker
  both imply `multipart/form-data`. The no-disk guarantee is shaped differently
  in each case.
