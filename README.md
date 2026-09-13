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
