# Tasks: 019 — CI pipeline with quality gates

<!-- Source: spec/019-ci-pipeline-with-quality-gates/design.md -->
<!-- Ticket: none (backlog story 019) -->
<!-- Date: 2026-09-13 -->

## Phase 1: Pipeline

- [x] Add jacoco-maven-plugin 0.8.15 to pom.xml with prepare-agent, report, and check bound to verify (BUNDLE line ≥ 80%, each domain package line ≥ 85%), and prove a raised threshold fails verify
  - ID 1.1 · Depends on: none
- [x] Add .github/workflows/ci.yml (push and pull_request, contents: read, concurrency, Temurin 25 with Maven cache, ./mvnw -B verify, JaCoCo artifact always, test reports on failure)
  - ID 1.2 · Depends on: 1.1
- [ ] Add the CI badge and coverage-gate note to README.md, push, and confirm a green run with the jacoco-report artifact
  - ID 1.3 · Depends on: 1.2
