---
status: proposed
---

# Container image

A hand-written multi-stage Dockerfile that packages the application as a small,
non-root container image, built and run with free tooling (Podman).

## Description

Not in the original PRD; added 2026-09-12 while specifying story 001. The
scaffold deliberately leaves containers out, because an image adds nothing until
there is an application worth running. Increment 3 is the natural home: the
Actuator probes from story 017 become the container's health check, and the CI
pipeline from story 019 can build the image.

A Dockerfile is chosen over Spring Boot's `build-image` (Cloud Native Buildpacks)
and Jib because it is the artifact a reviewer expects to see and can read at a
glance, and every container tool understands it. The image build is an optional
step and never part of `./mvnw verify`, which keeps needing only a JDK.

Tooling must be free. Podman is the reference engine: open source, daemonless,
rootless by default, and it reads a Dockerfile unchanged with the same `build` and
`run` commands. Docker Desktop is avoided because it requires a paid subscription
for larger organizations. On macOS, Podman runs containers in a small Linux VM
(`podman machine`).

The container must not weaken the statelessness claims. NFR-1 says uploaded CSV
files are never written to disk; inside a container that is provable by running
with a read-only root filesystem.

## Acceptance Criteria

- A multi-stage `Dockerfile` at the repository root: a build stage using a JDK 25
  image that runs `./mvnw package`, and a runtime stage using a Java 25 JRE image.
- Base images are pinned to a specific version, not `latest`.
- The runtime stage uses Spring Boot's layered jar so dependency layers are cached
  across rebuilds.
- The container runs as a non-root user.
- The image's health check uses the Actuator readiness probe (story 017).
- The container runs correctly with a read-only root filesystem, proving no disk
  writes (NFR-1, story 022).
- `podman build` and `podman run` work with no Docker-specific features; the same
  Dockerfile also works with the Docker CLI.
- A `.dockerignore` keeps `target/`, `.git/`, IDE files, and docs out of the build
  context.
- `./mvnw verify` does not build the image and does not require a container engine.
- The README documents building and running the image with Podman, including the
  one-time `podman machine init` on macOS.
- No secrets, credentials, or household data are baked into the image.

## Size

S

## Open Questions

- Which base images — Eclipse Temurin (`eclipse-temurin:25-jdk` / `25-jre`), or a
  distroless Java 25 runtime for a smaller attack surface?
- Should CI (story 019) build the image on every push, only on `main`, or not at
  all?
- Is the image pushed to a registry (e.g. GitHub Container Registry, free for
  private repos within limits), or built locally only?
- JVM memory settings for containers — rely on the JVM's container-aware defaults,
  or set `-XX:MaxRAMPercentage` explicitly?
