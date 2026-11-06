---
status: approved
---

# Project scaffold and package layout

Stand up the Java 25 / Spring Boot 4.1 application skeleton with the
ports-and-adapters package structure the rest of the backlog builds on.

## Description

The PRD fixes the target stack (Java 25, Spring Boot 4.1, ports-and-adapters)
and prescribes a package layout in section 8. Every subsequent story lands code
into that structure, so it needs to exist first.

Design spec section 7 settles the build shape explicitly: server-rendered
Thymeleaf templates in the same Spring Boot application, **one Maven module**.
No separate frontend build, no second toolchain, no `readiness-web` module, no
`package.json`, and no frontend build tool anywhere in the repository. The spec
marks this final for the scope of the demo, not a default pending
reconsideration. htmx may be added later as a single script tag with no build
step; it does not change the module structure.

So the scaffold includes the Thymeleaf starter and the standard
`src/main/resources/templates` and `static` locations from day one, rather than
leaving the client's home to be decided when increment 3.5 arrives.

This story creates the skeleton only. The architecture *tests* that enforce the
layout are story 015, and the CI gates that run them are story 019.

Naming constraint from the PRD: the repo, packages, and public README use
neutral vocabulary (`Stage`, `StageEvaluator`, `ReadinessAssessment`), and the
trademarked names of the published framework must not appear.

## Acceptance Criteria

- A Maven build produces a runnable Spring Boot 4.1.1 application on Java 25.
  (Originally Java 21 / Boot 3.x; changed 2026-09-12 because Boot 3.5's open-source
  support ended 2026-06-30.)
- The Maven Wrapper is committed, so a JDK is the only prerequisite.
- The package tree `domain/{model,stage,projection,port}`, `application`, and
  `adapter/{in.web.api,in.web.view,out.ai}` exists under the root package
  `com.rungs.readiness`.
  `in.web.api` holds REST controllers; `in.web.view` holds Thymeleaf view
  controllers. Both call application services in-process, and neither depends on
  the other (design spec section 7).
- The build is a single Maven module; there is no parent/aggregator POM and no
  second module.
- `spring-boot-starter-thymeleaf` is a dependency, and
  `src/main/resources/templates` and `src/main/resources/static` exist.
- No `package.json`, `node_modules`, lockfile, or frontend build plugin (e.g.
  `frontend-maven-plugin`) exists anywhere in the repository.
- `mvn verify` runs clean on a fresh clone with no manual setup and no toolchain
  other than a JDK.
- The application starts and responds to an Actuator health check.
- The test source set is wired and an empty-but-green test run succeeds.
- No trademarked framework names appear in the repo name, package names, or
  public README.
- README names the conscious deferrals from PRD section 12 and design spec
  section 8: auth, rate limiting, persistence, multi-tenancy, i18n, saved
  households, printing/PDF, dark mode, charting, and save-in-place.

## Size

S

## Open Questions

- ~~What is the root package namespace?~~ **Resolved.** `com.rungs.readiness`
  (Maven `groupId` `com.rungs`). The reverse-domain convention does not require
  owning the domain for an application that is never published as a library.
- ~~Trademark risk on the project name.~~ **Resolved.** The project was
  originally named `Gazelle`, which borrowed vocabulary ("gazelle intensity")
  from the same published framework the PRD is careful not to name. It is now
  **Rungs**, after the seven-stage ladder that is the product's central
  metaphor, and the CSV version marker is `rungs_snapshot_v1`. Settled before
  any code, remote, or saved file existed, so no migration path is needed.
- ~~Does the web client get its own Maven module?~~ **Resolved.** Design spec
  section 7 now decides it: one Maven module, Thymeleaf in the same application,
  no SPA and no frontend build tool, final for this demo.
- ~~Where do view controllers live, and do they call the API over HTTP?~~
  **Resolved.** Design spec section 7: `adapter.in.web.api` and
  `adapter.in.web.view` are sibling inbound adapters, and view controllers call
  application services in-process. PRD section 8 has been updated to match, and
  adds constraints AC-G (no client logic) and AC-H (one Maven module).
