# Tasks: 012 + 034 + 030 — Warnings, htmx and JavaScript policy, projection screen

<!-- Source: spec/030-projection-screen/design.md -->
<!-- Ticket: none (backlog stories 012, 030, 034) -->
<!-- Date: 2026-09-13 -->

## Phase 1: Warnings and policy (parallel)

- [x] Add domain WarningCode, Severity, Warning, MonthlyInterest and WarningDetector; refactor PayoffSimulator to MonthlyInterest; wire warnings into assessment and projection responses with severity and debtIds; tests per condition, order, and contract (012)
  - ID 1.1 · Depends on: none
- [x] Add the security headers filter (CSP, nosniff, Referrer-Policy), htmx-config meta, new noscript copy, vendor VERSIONS.md, and header, cookie, and 405 tests (034)
  - ID 1.2 · Depends on: none

## Phase 2: Projection screen (030)

- [x] Add ProjectionPage and projectPage, ProjectionPageController (/projection, /projection/results, /projection/schedule), projection page template with control card, OOB value label, stats, schedule with 24-row cap and show-all, truncated foot, warnings, and view plus figures-match tests
  - ID 2.1 · Depends on: 1.1, 1.2
