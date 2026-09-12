# Requirements Specification

<!-- Feature name: 001-project-scaffold-and-package-layout -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-12 -->
<!-- Ticket: none (backlog story 001) -->
<!-- Link: backlog/001-project-scaffold-and-package-layout.md -->
<!-- Branch: main -->

## Feature Overview

Stand up the Rungs application skeleton: a single-module Maven build of a Java 25 /
Spring Boot 4.1 application with server-rendered Thymeleaf, laid out in the
ports-and-adapters package structure under `com.rungs.readiness`. Every later story adds
code into this structure, so it has to exist and build cleanly first. It serves the
engineer building the backlog, who needs somewhere to put code on day one, and the
engineering reviewer, who should be able to clone the repo and run `./mvnw verify` with
nothing installed but a JDK. The story also creates the public README that states what
the project is and names its conscious deferrals. It delivers structure only; the tests
that enforce the layout come in story 015, and the CI gates in story 019.

## User Stories

### Story 1: Build into a ready-made structure
**As** the engineer building the backlog
**I want** a single-module Maven project with the package tree already in place
**So that** every later story has a defined home for its code

**Acceptance Criteria:**
- [ ] `./mvnw verify` succeeds.
- [ ] The build is one Maven module: no parent/aggregator POM and no second module.
- [ ] Under `com.rungs.readiness`, these packages exist: `domain.model`, `domain.stage`,
  `domain.projection`, `domain.port`, `application`, `adapter.in.web.api`,
  `adapter.in.web.view`, and `adapter.out.ai`.
- [ ] Each of those packages contains a `package-info.java` whose Javadoc states in one
  line what belongs in it.
- [ ] `spring-boot-starter-thymeleaf` is a dependency, and `src/main/resources/templates`
  and `src/main/resources/static` exist.
- [ ] The test source set is wired up, and a minimal test run (application context
  loads) passes.

### Story 2: Verify with only a JDK
**As** the engineering reviewer
**I want** to clone the repo and verify it with nothing but JDK 25 installed
**So that** I can evaluate the project without any setup

**Acceptance Criteria:**
- [ ] The Maven Wrapper is committed (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/`).
- [ ] `./mvnw verify` passes on a fresh clone with only JDK 25 installed.
- [ ] No `package.json`, `node_modules`, lockfile, or frontend build plugin exists
  anywhere in the repo.
- [ ] The application starts, and `/actuator/health` reports `UP`.
- [ ] An automated test asserts the health response is exactly `{"status":"UP"}` and that
  no other Actuator endpoint is exposed.

### Story 3: A README that explains the gaps
**As** someone reading the public repository
**I want** a README that explains the project and names what it deliberately leaves out
**So that** missing features read as decisions, not oversights

**Acceptance Criteria:**
- [ ] The README summarizes the project in neutral vocabulary and states that nothing is
  stored.
- [ ] It gives prerequisites (JDK 25) and build/run commands using `./mvnw`.
- [ ] It lists the deferred features: authentication, rate limiting, persistence,
  multi-tenancy, and internationalization (PRD §12); saved households, printing/PDF,
  dark mode, charting, and save-in-place (design spec §8).
- [ ] No trademarked framework names appear in the repo name, package names, or README.

## Functional Requirements

<!-- P0 = Must have for launch
     P1 = Should have, workarounds exist
     P2 = Nice to have
     Use REQ-001, REQ-002 etc. as stable identifiers -->

### Must Have (P0)
- **REQ-001**: A single Maven module with `groupId` `com.rungs` and `artifactId` `rungs`.
  No parent/aggregator POM.
- **REQ-002**: The Spring Boot starter parent is pinned to `4.1.1`, the latest GA release
  (milestones excluded). Java 25 is set with the `java.version` property, from which the
  Boot parent derives `maven.compiler.release` (the parent's default is 17).
- **REQ-003**: The Maven Wrapper is committed and pins a specific Maven version.
- **REQ-004**: The main class `com.rungs.readiness.RungsApplication` sits in the root
  package.
- **REQ-005**: The eight architecture packages (`domain.model`, `domain.stage`,
  `domain.projection`, `domain.port`, `application`, `adapter.in.web.api`,
  `adapter.in.web.view`, `adapter.out.ai`) exist, each with a `package-info.java` whose
  Javadoc states in one line what belongs there.
- **REQ-006**: Dependencies are limited to `spring-boot-starter-webmvc`,
  `spring-boot-starter-thymeleaf`, and `spring-boot-starter-actuator`, plus the test
  starters `spring-boot-starter-webmvc-test` and `spring-boot-starter-actuator-test`. The
  deprecated `spring-boot-starter-web` is not used. Later stories add their own
  dependencies (validation, jqwik, ArchUnit, and so on).
- **REQ-007**: `src/main/resources/templates` and `src/main/resources/static` exist and
  are tracked in git with a `.gitkeep` until real files arrive.
- **REQ-008**: Actuator exposes the `health` endpoint only, and it reports `UP`. An
  automated test asserts that `/actuator/health` returns exactly `{"status":"UP"}` and
  that another Actuator endpoint (e.g. `/actuator/env`) returns 404.
- **REQ-009**: A test confirms the Spring application context loads.
- **REQ-010**: The README covers the project summary, the no-storage claim, prerequisites
  (JDK 25), `./mvnw` build and run commands, and the deferred-features list.
- **REQ-011**: No `package.json`, lockfile, `node_modules`, or frontend build plugin exists
  anywhere in the repository.
- **REQ-012**: The Maven Enforcer plugin (version managed by the Boot parent) runs in
  `./mvnw verify` and fails the build with a clear message when the JDK is older than 25,
  the Maven version is below the wrapper's pinned version, or any `SNAPSHOT` dependency is
  present. Story 015 may extend it with the no-second-module and no-Node checks.

### Should Have (P1)
- **REQ-013**: `application.properties` sets the application name and the Actuator
  exposure, and nothing else.

### Nice to Have (P2)
- **REQ-014**: An `.editorconfig` for consistent indentation and line endings.

## Non-Functional Requirements

### Performance
- No endpoint performance targets apply; PRD NFR-3 covers the assessment and projection
  endpoints delivered by later stories.
- The build is reproducible: every dependency and plugin version is pinned, with no
  `SNAPSHOT` versions or version ranges.

### Security
- Actuator exposes `health` only, with `show-details` set to `never`; the response is
  `{"status":"UP"}` and nothing more.
- No secrets, credentials, or environment-specific values exist in the repository or in
  `application.properties`.
- No database, datasource, or session starters are included, so nothing can be persisted
  (PRD NFR-1).

### Accessibility / Browser Support
- Not applicable: the scaffold has no UI. Stories 024–034 own accessibility and browser
  support.

## Constraints and Assumptions

### Technical Constraints
- Java 25 and Spring Boot 4.1.1 (PRD target stack, updated 2026-09-12).
- One Maven module with no frontend toolchain (PRD AC-H, design spec §7).
- Maven runs only through the committed wrapper, pinned to Maven 3.9.16; REQ-012's
  Enforcer minimum matches it.
- The package layout follows PRD §8 under `com.rungs.readiness`; the empty packages are
  real, each with its `package-info.java`.
- Neutral vocabulary only: no trademarked framework names in the repo, packages, or
  README (PRD §1).
- Constructor injection only (AC-D), from the first class written.
- Work is committed directly to `main`.

### Assumptions
- Reviewers have a JDK 25 from any vendor, and internet access on the first build so the
  wrapper can download Maven and dependencies.
- The development machine has no JDK or Maven yet. At the start of implementation,
  Claude runs the installs with the user approving each prompt:
  `brew install --cask temurin@25` (JDK 25) and `brew install maven` (temporary, only to
  generate the wrapper with `mvn wrapper:wrapper -Dmaven=3.9.16`). After that, everything
  runs through `./mvnw`.
- Spring Boot 4.1 uses Jackson 3 (`tools.jackson`). Nothing in this story touches it, but
  stories 007 and 015 depend on it.
- Architecture packages without classes are expected; later stories fill them.
- The repository already has a `.gitignore` from the first commit. The scaffold keeps its
  Maven and IDE entries and adds to it only if needed, rather than recreating it.

## Out of Scope
- ArchUnit rules and the no-second-module / no-Node build checks — story 015. REQ-012's
  Enforcer covers only JDK, Maven, and `SNAPSHOT` rules.
- CI pipeline, JaCoCo coverage gate, and golden-file checks — story 019.
- Domain code: `Money`, the snapshot model, stage evaluators, payoff simulation — stories
  002–009.
- HTTP endpoints, request validation, and problem details — stories 007, 010, 011, 013.
  Bean Validation is not added as a dependency yet.
- OpenAPI document and ADRs — story 016.
- Probes beyond basic `health`, metrics, and structured logging — stories 017 and 018.
- Any page, template, CSS, or JavaScript, including vendored htmx — stories 024–034. The
  scaffold only creates the empty `templates/` and `static/` folders.
- CSV import and export — stories 020–023.
- The Spring AI dependency — story 036. `adapter.out.ai` is an empty package only.
- Container image (Dockerfile, Podman) — story 037.
- Deployment and release process — not covered by any story.
- A LICENSE file — decide before the repository is made public.

## Success Metrics
- **Clean first run:** on a fresh clone with only JDK 25 installed, `./mvnw verify` passes
  the first time, with no manual steps.
- **Wrong JDK fails clearly:** on an older JDK the build stops with the Enforcer's
  "requires Java 25" message, not a compiler error.
- **Starts and reports healthy:** `./mvnw spring-boot:run` starts the app, and
  `curl localhost:8080/actuator/health` returns `{"status":"UP"}`.
- **Structure is complete:** all eight architecture packages have a `package-info.java`.
- **No forbidden files:** a search of the repository finds no `package.json`, lockfile,
  `node_modules`, or second module.
- **No trademarked names in the product:** a search of source code, package names,
  `pom.xml`, the README, and the repository name finds no trademarked framework name. The
  planning documents in `prd/` and `backlog/` are excluded, since the PRD's trademark note
  necessarily names them.
- **README stands alone:** a newcomer can build and run the app from the README alone,
  and it lists every deferred feature.
- No build-time target is set: timing on a skeleton says little, and the first run is
  dominated by downloads.
