# Design: 037 — Container image

<!-- Source: spec/037-container-image/requirements.md -->
<!-- Ticket: none (backlog story 037) -->
<!-- Date: 2026-09-13 -->

## Layout

A single `Dockerfile` at the repository root, three stages:

1. **`build`** (`eclipse-temurin:25.0.4_7-jdk-noble`): installs `unzip` (the Maven Wrapper
   needs it — without it `mvnw` silently falls back to the `.tar.gz` distribution, whose
   checksum does not match the pinned `.zip` checksum in
   `.mvn/wrapper/maven-wrapper.properties`, and the build fails), copies `.mvn/`, `mvnw`,
   `pom.xml`, and `src/`, then runs `./mvnw -B -ntp package -DskipTests`.
2. **`extract`** (`FROM build`): runs `java -Djarmode=tools -jar target/rungs-*.jar extract
   --layers --launcher --destination extracted`, producing `dependencies/`,
   `spring-boot-loader/`, `snapshot-dependencies/`, and `application/` under `extracted/`.
3. **`runtime`** (`eclipse-temurin:25.0.4_7-jre-noble`): installs `curl` (the only tool
   `HEALTHCHECK` can use — the base image has neither curl nor wget), creates a system
   group and user `rungs` (uid/gid 999, no login shell, no home directory), copies the four
   layers in dependency-likelihood order (least to most likely to change) with
   `--chown=rungs:rungs`, switches to `USER rungs`, declares `HEALTHCHECK` against
   `/actuator/health/readiness`, and starts the app with
   `ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]`.

`.dockerignore` excludes `target/`, `.git/`, IDE folders, `prd/`, `backlog/`, `spec/`, and
`docs/` (REQ-006).

## Decisions

- **Base images**: pinned to `25.0.4_7` (the current Temurin 25 release as of 2026-09-13),
  Ubuntu Noble variants, never `latest`.
- **Actuator readiness/liveness**: story 017 isn't done, so
  `management.endpoint.health.probes.enabled` was `false`. Flipped to `true` so
  `/actuator/health/{readiness,liveness}` exist; `show-details` stays `never` and exposure
  stays `health` only. Enabling probes makes Boot list the configured group names
  (`liveness`, `readiness`) alongside the status on the plain `/actuator/health` response —
  that's independent of `show-details` (which only gates component detail), so
  `ActuatorExposureTests` was updated (TDD) to assert on `$.status` and `$.groups`
  separately instead of an exact-body string match; the readiness/liveness endpoints still
  return exactly `{"status":"UP"}`, and `/actuator/env` still 404s.
- **`--format docker`**: Podman's default build format is OCI, which silently drops
  `HEALTHCHECK`. The README's build command passes `--format docker`; the Docker CLI
  defaults to that format already, so the same `Dockerfile` needs no changes there.
- **No heap flags** (REQ-008): the JVM's container-aware ergonomics size the heap from the
  container's memory limit; nothing is hard-coded.
- **Read-only root**: the app runs fine with `--read-only`, except the JVM wants a writable
  `/tmp` for small runtime files; `--tmpfs /tmp` supplies that without touching disk, and
  the README explains why.

## Verification

- `./mvnw -B -ntp clean verify`: 1386 tests, all green, coverage gates met — no container
  engine touched.
- `podman build --format docker -t rungs .`: succeeds; image `370 MB`.
- `podman run -d --read-only --tmpfs /tmp -p 18080:8080 --name rungs-smoke rungs`: starts.
- `podman exec rungs-smoke id` → `uid=999(rungs) gid=999(rungs)`.
- `podman healthcheck run rungs-smoke` → exit 0; `podman inspect` health status → `healthy`.
- `curl localhost:18080/actuator/health/readiness` → `{"status":"UP"}`.
- `POST /v1/assessments` with a minimal snapshot → HTTP 200 with a full stage assessment.
- Rebuild after a one-line comment change in `RungsApplication.java`: the `dependencies`,
  `spring-boot-loader`, and `snapshot-dependencies` `COPY` layers report `Using cache`; only
  the `application` layer (and everything after it) rebuilds.
- Container removed after the smoke check; the image was kept.
