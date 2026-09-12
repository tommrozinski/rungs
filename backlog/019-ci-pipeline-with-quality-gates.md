---
status: proposed
---

# CI pipeline with coverage and architecture gates

A pipeline that builds, tests, and fails on the coverage and architecture
thresholds the PRD sets.

## Description

Increment 3 is done when "the pipeline enforces coverage and architecture gates".
The gates come from section 9: JaCoCo with a build failure below 85% line
coverage on `domain`, plus the ArchUnit suite from story 015.

Golden-file fixtures also want pipeline treatment — section 9 says a diff in a
fixture is a deliberate act, reviewed like code, which means CI surfaces fixture
changes rather than regenerating them.

Design spec section 7's single-module decision is the design constraint on this
story: one Maven module, no separate frontend build, no second toolchain, and no
`package.json` anywhere. So the pipeline needs only a JDK and is a thin wrapper
around `mvn verify` — not a parallel definition of what "green" means, and never
a Node setup step. Any web-client checks (accessibility, no-JS, the client-side
arithmetic ban) have to run inside that same Maven build using JVM tooling.

Note: the user's standing PR guidance sets a different bar from the PRD — 80%
minimum with 100% desired, and a green suite before review. Worth reconciling
before this story is spec'd.

## Acceptance Criteria

- CI runs on every push and pull request: build, unit tests, property tests,
  architecture tests, contract tests.
- The pipeline's test step is `mvn verify` — no CI-only test invocation that
  cannot be reproduced locally.
- JaCoCo is wired and the build fails below the agreed line-coverage threshold on
  the `domain` package.
- The ArchUnit suite failing fails the pipeline.
- Golden-file drift fails the build rather than silently regenerating fixtures.
- The coverage report is published as a build artifact.
- The pipeline runs on Java 21 and installs no Node.js or other frontend
  toolchain.
- A failing pipeline blocks merge.
- Build is reproducible — no network-dependent or wall-clock-dependent tests.

## Size

M

## Open Questions

- Which CI platform — GitHub Actions, Azure Pipelines? The standing workflow
  references ADO tickets, but the repo host is not stated and this project is not
  currently a git repository at all.
- Is the coverage gate 85% (PRD section 9) or the standing 80%-minimum /
  100%-desired guidance? They conflict.
- Should coverage be gated on `domain` only, or the whole module? With the
  Thymeleaf view controllers now in the same module as everything else, a
  module-wide gate would include them.
- Is accessibility testing (story 033) and the no-JS pass (story 034) part of the
  pipeline or a manual gate? If automated, it runs in `mvn verify` with a JVM
  browser driver, not a Node-based tool.
- Is a release or publish step in scope, or build-and-verify only?
