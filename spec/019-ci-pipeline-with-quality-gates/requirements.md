# Requirements Specification

<!-- Feature name: 019-ci-pipeline-with-quality-gates -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 019) -->
<!-- Link: backlog/019-ci-pipeline-with-quality-gates.md -->
<!-- Branch: main -->

## Feature Overview

A GitHub Actions pipeline that runs `./mvnw verify` on JDK 25 for every push and pull
request and fails on the quality gates the PRD sets: JaCoCo coverage thresholds, the
architecture suite (story 015), golden-file drift, OpenAPI drift, and every other test.
The pipeline is a thin wrapper around the single local command — nothing "green" in CI
that can't be reproduced by running `./mvnw verify` on a laptop — and installs no Node or
frontend toolchain. Increment 3 is done when this pipeline enforces its gates.

## User Stories

### Story 1: Every push is verified
**As** the maintainer working directly on `main`
**I want** CI to run the full build on every push
**So that** a broken gate is visible immediately

**Acceptance Criteria:**
- [ ] `.github/workflows/ci.yml` runs on push to any branch and on pull requests.
- [ ] The only build step is `./mvnw -B verify`.
- [ ] A failing test, rule, or gate fails the workflow.

### Story 2: Coverage that means something
**As** the engineering reviewer
**I want** coverage gates enforced by the build
**So that** the claimed test discipline is real

**Acceptance Criteria:**
- [ ] The build fails below 85% line coverage on `com.rungs.readiness.domain` or below
  80% line coverage on the whole module.
- [ ] The JaCoCo HTML report is uploaded as a workflow artifact.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: Workflow `.github/workflows/ci.yml`: `actions/checkout`,
  `actions/setup-java` with Temurin 25 and Maven dependency caching, then
  `./mvnw -B verify`. Actions referenced by major version tag.
- **REQ-002**: JaCoCo Maven plugin in `pom.xml` with a `check` goal bound to `verify`:
  line coverage ≥ 85% for the `domain` package (PRD §9) and ≥ 80% for the whole module
  (the standing PR guidance minimum). Thresholds are gates, not warnings.
- **REQ-003**: All suites run inside `verify`: unit, property (014), architecture (015),
  contract, golden-file (008, 010, 013), OpenAPI drift (016), logging leak (018), and
  later accessibility (033) and htmx/JavaScript-policy (034) tests.
- **REQ-004**: No Node.js setup or frontend step exists in the workflow (AC-H).
- **REQ-005**: The JaCoCo report (`target/site/jacoco/`) is uploaded as an artifact on
  every run, including failed runs.
- **REQ-006**: Tests are reproducible: no network calls beyond dependency download, no
  wall-clock-dependent assertions.

### Should Have (P1)
- **REQ-007**: A CI status badge in the README.
- **REQ-008**: The workflow uses `concurrency` to cancel superseded runs on the same ref.

### Nice to Have (P2)
- **REQ-009**: Surefire and failsafe reports uploaded on failure for quick diagnosis.

## Non-Functional Requirements

### Performance
- A full CI run completes in under 10 minutes with a warm dependency cache.

### Security
- The workflow uses the default read-only `GITHUB_TOKEN` permissions (`contents: read`);
  no secrets are required.

### Accessibility / Browser Support
- Not applicable.

## Constraints and Assumptions

### Technical Constraints
- GitHub Actions, since the repository is hosted at `github.com/tommrozinski/rungs`.
- JDK 25 matches the Maven Enforcer rule from story 001.

### Assumptions
- The project is committed directly to `main` by a single maintainer, so there is no
  required-review branch protection; CI still runs on every push.
- Container image builds are not part of CI (story 037 decides separately).

## Out of Scope
- Release, publish, or deployment steps.
- Container image build — story 037.
- Dependency update automation.

## Success Metrics
- A deliberately failing test, a coverage drop below either threshold, and an ArchUnit
  violation each fail the workflow during development.
- A clean push produces a green run and an uploaded coverage report.
- The workflow file contains no Node or frontend tooling.
