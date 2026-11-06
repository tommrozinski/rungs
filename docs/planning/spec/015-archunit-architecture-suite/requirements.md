# Requirements Specification

<!-- Feature name: 015-archunit-architecture-suite -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 015) -->
<!-- Link: backlog/015-archunit-architecture-suite.md -->
<!-- Branch: main -->

## Feature Overview

The build-enforced architecture constraints of PRD §8 — AC-A through AC-H — as ArchUnit
rules and JUnit file scans that run in `./mvnw verify`. The PRD treats these as testable
constraints, not preferences: the domain stays framework-free and deterministic, adapters
depend only on ports and the application layer, the web client holds no business logic,
and the repository stays a single Maven module with no frontend toolchain. Each rule is
proven to fire by a committed violation fixture, and every failure names the file and the
constraint.

## User Stories

### Story 1: Layering enforced by the build
**As** the engineering reviewer
**I want** architectural rules to fail the build when broken
**So that** the layering is guaranteed, not trusted to review

**Acceptance Criteria:**
- [ ] One named rule per constraint AC-A to AC-H.
- [ ] Every rule has a committed fixture that violates it and a test proving the rule
  fails on that fixture.
- [ ] Failures name the offending class or file and the constraint.

### Story 2: No logic leaks into the web client
**As** the maintainer of the Thymeleaf client
**I want** templates and scripts scanned for arithmetic, parsing, and data fetching
**So that** the client stays a viewer (AC-G)

**Acceptance Criteria:**
- [ ] A scan of `templates/` and `static/` fails on forbidden patterns.
- [ ] Hand-written JavaScript stays within the line budget; vendored htmx is excluded.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: `com.tngtech.archunit:archunit-junit5` is a test dependency; rules run in
  `./mvnw verify` against main classes (test classes are not subject to the rules).
- **REQ-002**: AC-A: `domain` depends on no class in `org.springframework..`, `jakarta..`,
  `tools.jackson..`, or `com.fasterxml.jackson..`.
- **REQ-003**: AC-B: `domain` depends on no class in `application..` or `adapter..`.
- **REQ-004**: AC-C: adapters do not depend on other adapters (`adapter.in.web.api`,
  `adapter.in.web.view`, `adapter.out.ai` are mutually independent); `adapter.out` depends
  on `domain.port`; `adapter.in.web.view` depends on `application` and never on
  `domain.stage` or `domain.projection`.
- **REQ-005**: AC-D: no field injection (`@Autowired`/`@Inject` on fields) anywhere.
- **REQ-006**: AC-E: `domain` classes are records, enums, interfaces, or final classes with
  only final fields; no public non-final fields anywhere.
- **REQ-007**: AC-F: no `double`, `float`, `Double`, or `Float` in `domain` fields, method
  parameters, return types, or detectable method calls.
- **REQ-008**: Determinism (NFR-4): `domain` calls no `now()` on `java.time` types, no
  `System.currentTimeMillis`/`nanoTime`, and uses no `java.util.Random`,
  `ThreadLocalRandom`, `SecureRandom`, or `UUID.randomUUID`.
- **REQ-009**: AC-G scan (JUnit) over `src/main/resources/templates` and
  `src/main/resources/static`, excluding `static/js/vendor/`: fails on `parseFloat`,
  `parseInt`, `Number(`, `toFixed`, `Math.`, `fetch(`, `XMLHttpRequest`, `<script>`
  without `src`, inline `on*=` handlers, Thymeleaf `#numbers`, and arithmetic operators
  inside `${…}` expressions. Hand-written `.js` totals at most 200 lines.
- **REQ-010**: AC-H scan (JUnit) of the repository tree (excluding `target/` and `.git/`):
  fails on `package.json`, `package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`,
  `node_modules/`, or any `pom.xml` other than the root one.
- **REQ-011**: Violation fixtures live in test sources under `…/archfixtures/` (Java) and
  `src/test/resources/archfixtures/` (templates, scripts, files), and each rule's test
  asserts the rule fails on its fixture and passes on the real code.

### Should Have (P1)
- **REQ-012**: Rule names match constraint ids (e.g. `AC_A_domainHasNoFrameworkImports`)
  so a failure reads as the PRD constraint it breaks.

### Nice to Have (P2)
- **REQ-013**: ArchUnit's freeze store is not used; the codebase starts compliant, so no
  violations are grandfathered.

## Non-Functional Requirements

### Performance
- The whole suite adds under 10 seconds to `./mvnw verify`.

### Security
- Not applicable beyond keeping `jakarta..` out of `domain`.

### Accessibility / Browser Support
- Not applicable.

## Constraints and Assumptions

### Technical Constraints
- JVM tooling only; no ESLint or stylelint (AC-H).
- The Maven Enforcer rules from story 001 remain and are not duplicated here.

### Assumptions
- The pattern scan in REQ-009 is intentionally conservative; a false positive is fixed by
  moving logic server-side, not by weakening the pattern, unless the ADR log records why.
- Rules are added before much code exists, so they guide stories 002–013 as they land.

## Out of Scope
- CI wiring — story 019.
- Coverage gates — story 019.

## Success Metrics
- Every rule in REQ-002 to REQ-010 fails on its fixture and passes on the real code.
- Suite runtime under 10 seconds.
- No suppressions or frozen violations in the repository.
