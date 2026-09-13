# Rungs

[![CI](https://github.com/tommrozinski/rungs/actions/workflows/ci.yml/badge.svg)](https://github.com/tommrozinski/rungs/actions/workflows/ci.yml)

Rungs is a stateless engine that assesses a household's finances against a seven-stage
debt-elimination and wealth-building framework, reports which stage the household is on and
what stands between it and the next, and projects the debt payoff path. It is an independent
implementation of a widely published personal-finance framework and is not affiliated with,
or endorsed by, the framework's author. It is built with Java 25 and Spring Boot 4.1, with a
server-rendered Thymeleaf web client in the same application.

## Nothing is stored

Statelessness is a design commitment of the project, not an afterthought. Rungs has no
database, sets no cookies, and keeps no sessions. Household figures travel in the request
and nowhere else: requests are processed in memory and discarded, and money values and
snapshot contents are never written to logs.

## Prerequisites

- JDK 25 or newer, from any vendor.

Nothing else. The Maven Wrapper downloads the correct Maven version on first use, so the
first build needs an internet connection. If `java` is not on your `PATH`, set `JAVA_HOME`
to the JDK's installation directory.

## Build and run

Build and run the tests:

```sh
./mvnw verify
```

On Windows, use `.\mvnw.cmd verify` instead (works in PowerShell and Command Prompt).

The build enforces JDK 25 or newer, Maven 3.9.16 or newer (which the wrapper provides), no
`SNAPSHOT` dependencies, a repository-wide check that the codebase uses only neutral
vocabulary, and line coverage of at least 80% for the module and 85% for each `domain`
package. The coverage report is written to `target/site/jacoco/index.html`. CI runs exactly
this command on every push and pull request.

Start the application:

```sh
./mvnw spring-boot:run
```

Then, from another terminal, check that it is healthy:

```sh
curl localhost:8080/actuator/health
```

It should respond with:

```json
{"status":"UP"}
```

## Run with Podman

A hand-written multi-stage `Dockerfile` builds a small, non-root container image; the same
file also works with the Docker CLI. Building or running the image is optional and never
part of `./mvnw verify`, which only needs a JDK.

Install Podman and start its VM (once):

```sh
brew install podman
podman machine init
podman machine start
```

Build the image. `--format docker` is required so the `HEALTHCHECK` instruction below is
honored; Podman's default OCI format silently ignores it:

```sh
podman build --format docker -t rungs .
```

Run it read-only and as its non-root user, on port 18080 so it does not clash with a
locally running `./mvnw spring-boot:run`:

```sh
podman run -d --read-only --tmpfs /tmp -p 18080:8080 --name rungs rungs
```

`--tmpfs /tmp` gives the JVM a writable `/tmp` (it uses it for a handful of small runtime
files); nothing else on disk is writable, and nothing durable is ever written there — the
tmpfs disappears with the container.

Confirm it is non-root and becomes healthy:

```sh
podman exec rungs id
podman healthcheck run rungs
```

Smoke-check it end to end: the readiness probe, then one assessment request.

```sh
curl localhost:18080/actuator/health/readiness

curl -X POST localhost:18080/v1/assessments -H 'Content-Type: application/json' -d '
{"monthlyTakeHomeIncome": "5000", "monthlyEssentialExpenses": "3000", "starterEmergencyFund": "1000",
 "fullEmergencyFund": "9000", "retirementContributionPercent": "15", "emergencyFundMonths": 3,
 "debts": [], "mortgage": null,
 "householdFlags": {"rents": true, "retired": false, "intendsToFundCollege": false}}'
```

The first should respond `{"status":"UP"}`; the second, a full stage-by-stage assessment
with HTTP 200.

Stop and remove the container when done:

```sh
podman rm -f rungs
```

The image is built and run locally only: it is not published to a registry, and no CI
workflow builds it.

## Deliberately not included

Each of these is a legitimate concern for a production system. Leaving them out is a
conscious deferral, not an oversight.

- **Authentication.**
- **Rate limiting.**
- **Persistence.** Rungs stores nothing; see above.
- **Multi-tenancy.**
- **Internationalization.**
- **Saved households.** Rungs stores nothing, so a household lasts only as long as a request.
- **Printing and PDF export.**
- **Dark mode.** It would prove nothing about the domain logic, which is the subject of the
  project.
- **Charting.**
- **Save-in-place.** Overwriting a file on disk through the File System Access API works
  only in Chromium-based browsers, and a plain download already covers everyone.

## Project documents

- [`prd/`](prd/): the product requirements
  ([`readiness-engine-prd.md`](prd/readiness-engine-prd.md)) and the web client design
  specification ([`design-spec.md`](prd/design-spec.md)).
- [`backlog/`](backlog/): the backlog of user stories drawn from the requirements.
- [`spec/`](spec/): per-story requirements, designs, and task lists.
