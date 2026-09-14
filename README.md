# Rungs

[![CI](https://github.com/tommrozinski/rungs/actions/workflows/ci.yml/badge.svg)](https://github.com/tommrozinski/rungs/actions/workflows/ci.yml)

Rungs assesses a household's finances against a seven-stage debt-elimination and
wealth-building framework, and reports which stage the household is on and what stands
between it and the next. It is an independent implementation of a widely published
personal-finance framework and is not affiliated with, or endorsed by, the framework's
author. It is built with Java 25 and Spring Boot 4.1.

This is a v0 build: the seven-stage domain evaluator and its tests exist today. A
one-page form and a matching result page, served with server-rendered Thymeleaf
templates, are coming next.

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

The build enforces JDK 25 or newer, and Maven 3.9.16 or newer (which the wrapper
provides). CI runs exactly this command on every push and pull request.

## Deliberately not included

Each of these is a legitimate concern for a production system. Leaving them out is a
conscious deferral, not an oversight.

- **Authentication.**
- **Rate limiting.**
- **Persistence.** Rungs stores nothing; see above.
- **Multi-tenancy.**
- **Internationalization.**
- **Saved households.** Rungs stores nothing, so a household lasts only as long as a request.

## Project documents

- [`prd/`](prd/): the product requirements
  ([`readiness-engine-prd.md`](prd/readiness-engine-prd.md)) and the web client design
  specification ([`design-spec.md`](prd/design-spec.md)).
- [`backlog/`](backlog/): the backlog of user stories drawn from the requirements.
- [`spec/`](spec/): per-story requirements, designs, and task lists.
