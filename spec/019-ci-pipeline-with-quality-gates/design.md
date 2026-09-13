# Design: 019 — CI pipeline with quality gates

<!-- Source: spec/019-ci-pipeline-with-quality-gates/requirements.md -->
<!-- Ticket: none (backlog story 019) -->
<!-- Date: 2026-09-13 -->

## Approach

CI is a thin wrapper: every gate lives in `pom.xml` and runs in `./mvnw verify`, so a green
laptop build and a green CI run mean the same thing. Later suites (014–018, 033, 034) join
the gate by being ordinary tests; nothing in the workflow changes for them.

## `pom.xml` — JaCoCo 0.8.15 (REQ-002)

| Execution | Phase | Purpose |
| --- | --- | --- |
| `prepare-agent` | `initialize` (default) | Sets `argLine`; Surefire's `@{argLine}` (story 002) picks it up |
| `report` | `verify` | HTML/CSV/XML under `target/site/jacoco/` |
| `check` | `verify`, after `report` | Fails the build (`haltOnFailure` default) |

Rules, both `LINE` `COVEREDRATIO`:
- `BUNDLE` ≥ 0.80 (whole module).
- `PACKAGE` ≥ 0.85 for `com.rungs.readiness.domain` and `com.rungs.readiness.domain.*`.
  JaCoCo cannot aggregate a package subset, so the rule applies per package, which implies
  ≥ 85% for `domain` as a whole. Packages with only `package-info.java` have no lines and
  are not evaluated.

Version pinned explicitly (Enforcer `requirePluginVersions`). 0.8.15 supports Java 25 in CI
and the local JDK 26.

## `.github/workflows/ci.yml` (REQ-001, 004, 005, 007–009)

- Triggers: `push` (all branches), `pull_request`.
- `permissions: contents: read`; no secrets.
- `concurrency: ci-${{ github.ref }}`; superseded runs are cancelled except on `main`, so
  every `main` commit keeps its own result (reviewer finding, 2026-09-13).
- Steps: `actions/checkout@v7` → `actions/setup-java@v6` (Temurin 25, `cache: maven`) →
  `./mvnw -B verify` → on failure, if `target/jacoco.exec` exists, render the report with
  `jacoco:report` (report-only, not a second build; a failing test stops before `verify`, so
  REQ-005 "including failed runs" needs it) → upload `target/site/jacoco/` with
  `if: always()` → upload Surefire/Failsafe reports with `if: failure()`.
- `timeout-minutes: 15` (NFR target: under 10 min warm).
- No Node or frontend step.

README gains the CI badge and a sentence on the coverage gates. The repository is private,
so the badge renders only for viewers with access.

## Verification

- Local `./mvnw -B clean verify` green; report produced; the check logs "All coverage checks
  have been met".
- Negative check: temporarily raising the domain minimum above actual coverage fails
  `verify`; restored.
- Push produces a green run with a `jacoco-report` artifact.
- REQ-006 (no network or wall-clock assertions) holds for today's tests; later stories keep it.
- ArchUnit-violation failure is proven in story 015 (the suite doesn't exist yet).
