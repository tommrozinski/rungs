# Tasks: 001 — Project scaffold and package layout

<!-- Source: spec/001-project-scaffold-and-package-layout/design.md -->
<!-- Ticket: none (backlog story 001) -->
<!-- Date: 2026-09-13 -->

## Overview

Single Maven module, Spring Boot 4.1.1 on Java 25. Install the toolchain first, then the
build (pom + wrapper), then the empty ports-and-adapters package tree and runtime config,
then the three tests, then README and supporting files. Finish with manual checks against
the success metrics. Commits go straight to `main`.

## Time estimates

| Phase | Estimate |
| --- | --- |
| 1. Toolchain | 15 min |
| 2. Build | 45 min |
| 3. Application skeleton | 20 min |
| 4. Tests | 60 min |
| 5. README and supporting files | 30 min |
| 6. Verification | 30 min |
| **Total** | **~3 h 20 min** |

## Phase 1: Toolchain

- [x] Install Maven (brew install maven, which brings OpenJDK 26) with user approval, and confirm mvn -v reports Maven 3.9.16 on Java 26
  - ID 1.1 · Depends on: none
  - Note: Temurin 25 was dropped; its installer needs sudo, and JDK 26 satisfies the Enforcer's [25,) with --release 25.

## Phase 2: Build

- [x] Write pom.xml: parent spring-boot-starter-parent 4.1.1, com.rungs:rungs:0.1.0-SNAPSHOT, java.version 25, the five starters, spring-boot-maven-plugin, maven-enforcer-plugin (requireJavaVersion [25,), requireMavenVersion [3.9.16,), requireReleaseDeps with default settings, requirePluginVersions) with the design's messages, and a pluginManagement pin for maven-site-plugin 3.22.0
  - ID 2.1 · Depends on: 1.1
  - ⚠️ Risk: without the site plugin pin, `requirePluginVersions` fails the first build, because Boot's BOM does not manage that plugin.
- [x] Generate the Maven wrapper (mvn -N wrapper:wrapper -Dmaven=3.9.16 -Dtype=only-script) and confirm ./mvnw -v reports Maven 3.9.16 on Java 26 with JAVA_HOME set to Homebrew OpenJDK
  - ID 2.2 · Depends on: 2.1

## Phase 3: Application skeleton

- [ ] Add RungsApplication main class, application.properties (application name, health-only exposure, show-details never, probes disabled), and templates/.gitkeep and static/.gitkeep
  - ID 3.1 · Depends on: 2.2
- [ ] Add package-info.java with the design's one-line Javadoc to each of the eight architecture packages
  - ID 3.2 · Depends on: 2.2

## Phase 4: Tests

- [ ] Add RungsApplicationTests with a @SpringBootTest contextLoads test
  - ID 4.1 · Depends on: 3.1
- [ ] Add ActuatorExposureTests (@SpringBootTest, @AutoConfigureMockMvc, MockMvcTester) asserting /actuator/health returns 200 with body exactly {"status":"UP"}, /actuator/env returns 404, and /actuator _links keys are exactly self, health, and health-path
  - ID 4.2 · Depends on: 3.1
  - Note: import `AutoConfigureMockMvc` from `org.springframework.boot.webmvc.test.autoconfigure`. The exact body holds only while probes stay disabled (task 3.1).
- [ ] Add NeutralVocabularyTests, a plain JUnit test that scans every repo file except .git/, target/, IDE folders, prd/, backlog/, and its own source for the PRD §1 trademark terms and owner name (any case; space, hyphen, underscore, or no separator) and fails listing each path:line
  - ID 4.3 · Depends on: 2.2
  - ⚠️ Risk: local untracked folders can cause false failures; keep the skip list tight and add to it only for generated or untracked folders.
- [ ] Run ./mvnw verify and fix any failures, including Enforcer rule misconfiguration and Thymeleaf template-location warnings
  - ID 4.4 · Depends on: 3.2, 4.1, 4.2, 4.3

## Phase 5: README and supporting files

- [ ] Write README.md with the six sections from design §7 (summary, nothing is stored, prerequisites, build and run, deliberately not included, project documents) using neutral vocabulary only
  - ID 5.1 · Depends on: 2.2
- [ ] Add .editorconfig per design §8 (including a *.{cmd,bat} CRLF section), add .gitattributes (text=auto eol=lf, /mvnw eol=lf, *.cmd and *.bat eol=crlf), append .jqwik-database to the existing .gitignore, then run git add --renormalize . and confirm git ls-files --eol shows mvnw i/lf, mvnw.cmd i/crlf, and mvnw mode 100755
  - ID 5.2 · Depends on: 2.2
  - ⚠️ Risk: without .gitattributes a Windows clone (core.autocrlf=true) checks mvnw out as CRLF and ./mvnw fails with "bad interpreter" under WSL or Git Bash. Renormalizing stages files; stage only these supporting files and the wrapper, not unrelated work.

## Phase 6: Verification

- [ ] Start the app with ./mvnw spring-boot:run and confirm curl localhost:8080/actuator/health returns {"status":"UP"}
  - ID 6.1 · Depends on: 4.4
- [ ] Confirm the Enforcer fails with the "requires Java 25" message on an older JDK, or by temporarily raising the required version, then revert
  - ID 6.2 · Depends on: 4.4
- [ ] Rerun ./mvnw verify so NeutralVocabularyTests covers the README and supporting files, and search the repo for package.json, lockfiles, node_modules, and second modules, confirming none exist
  - ID 6.3 · Depends on: 4.4, 5.1, 5.2
