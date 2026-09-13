# Requirements Specification

<!-- Feature name: 037-container-image -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 037) -->
<!-- Link: backlog/037-container-image.md -->
<!-- Branch: main -->

## Feature Overview

A hand-written multi-stage Dockerfile that packages the application as a small, non-root
container image, built and run with free tooling (Podman). The container proves the
statelessness claims hold in a runtime environment — it runs with a read-only root
filesystem — and uses the Actuator readiness probe as its health check. Building the
image is optional and never part of `./mvnw verify`, which keeps needing only a JDK.

## User Stories

### Story 1: Run it anywhere with one command
**As** a reviewer
**I want** to build and run the app as a container with free tools
**So that** I can try it without installing Java

**Acceptance Criteria:**
- [ ] `podman build -t rungs .` and `podman run -p 8080:8080 rungs` start the app.
- [ ] The same Dockerfile works with the Docker CLI.

### Story 2: A container that proves "stores nothing"
**As** the engineering reviewer
**I want** the container to run read-only and as non-root
**So that** the no-disk and least-privilege claims are demonstrable

**Acceptance Criteria:**
- [ ] The app runs with `--read-only` and a non-root user.
- [ ] The health check reports healthy via the readiness probe.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: A multi-stage `Dockerfile` at the repository root: build stage
  `eclipse-temurin:25-jdk` (pinned to a specific version tag) running `./mvnw -B package
  -DskipTests`; runtime stage `eclipse-temurin:25-jre` (pinned).
- **REQ-002**: The runtime stage extracts Spring Boot's layered jar (dependencies,
  spring-boot-loader, snapshot-dependencies, application) into separate layers.
- **REQ-003**: A dedicated non-root user and group own and run the application.
- **REQ-004**: `HEALTHCHECK` calls `/actuator/health/readiness` (story 017) using a tool
  present in the image, with a sensible interval and start period.
- **REQ-005**: The app starts and serves requests with `--read-only`; no writable path is
  needed (a tmpfs is mounted only if the JVM requires one, and the README documents it).
- **REQ-006**: `.dockerignore` excludes `target/`, `.git/`, IDE folders, `prd/`, `backlog/`,
  `spec/`, and `docs/`.
- **REQ-007**: README section "Run with Podman": `brew install podman`, `podman machine
  init`, `podman machine start`, build, and run commands.
- **REQ-008**: JVM memory uses the JVM's container-aware defaults; no hard-coded heap
  flags.

### Should Have (P1)
- **REQ-009**: A script-free smoke check documented in the README: after `podman run`,
  `curl` the readiness probe and one assessment request.

### Nice to Have (P2)
- None.

## Non-Functional Requirements

### Performance
- Rebuilds after an application-only change reuse the dependency layers.

### Security
- Non-root user, read-only root filesystem, no secrets or household data baked in, base
  images pinned (not `latest`).

### Accessibility / Browser Support
- Not applicable.

## Constraints and Assumptions

### Technical Constraints
- Free tooling only; Podman is the reference engine, Docker CLI compatible.
- `./mvnw verify` neither builds the image nor requires a container engine.

### Assumptions
- The image is built locally only: not pushed to a registry and not built in CI
  (decided 2026-09-13). Both can be added later.
- Podman and its VM are installed at implementation time with the user approving each
  prompt, as with the JDK in story 001.

## Out of Scope
- Registry publishing, CI image builds, Kubernetes manifests, and distroless images.

## Success Metrics
- The image builds and runs under Podman with `--read-only` as a non-root user, and the
  container reports healthy.
- An assessment request against the running container returns 200.
