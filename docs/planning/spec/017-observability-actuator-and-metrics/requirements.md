# Requirements Specification

<!-- Feature name: 017-observability-actuator-and-metrics -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-13 -->
<!-- Ticket: none (backlog story 017) -->
<!-- Link: backlog/017-observability-actuator-and-metrics.md -->
<!-- Branch: main -->

## Feature Overview

Operational visibility without household data (PRD NFR-5): Actuator liveness and
readiness probes, a Prometheus scrape endpoint, per-endpoint latency timers, and a counter
of stage verdicts. The verdict counter turns the service's own output into a signal — a
shift in which stages households are at becomes visible without logging a single figure,
which NFR-2 forbids. Every metric tag is bounded and enum-valued.

## User Stories

### Story 1: Know the service is up and ready
**As** an operator or container runtime
**I want** liveness and readiness probes
**So that** unhealthy instances are restarted and not sent traffic

**Acceptance Criteria:**
- [ ] `/actuator/health/liveness` and `/actuator/health/readiness` report `UP`.
- [ ] Health responses show status only, no details.

### Story 2: See latency and verdicts
**As** the engineering reviewer
**I want** endpoint timers and a stage-verdict counter
**So that** NFR-3 targets are measurable and verdict distribution is observable

**Acceptance Criteria:**
- [ ] `/actuator/prometheus` exposes request timers per endpoint and the verdict counter.
- [ ] A test proves the counter increments by stage and status for a known assessment.
- [ ] No tag carries a figure, label, filename, or free text.

## Functional Requirements

### Must Have (P0)
- **REQ-001**: Add `micrometer-registry-prometheus`. Actuator exposes exactly `health` and
  `prometheus` on the application port; `show-details` stays `never`; probe groups are
  enabled. Story 001's test that other endpoints (e.g. `/actuator/env`) return 404 still
  holds.
- **REQ-002**: Endpoint latency uses Spring's built-in `http.server.requests` timer, whose
  `uri` tag is the route template (bounded); no custom timers duplicate it.
- **REQ-003**: Counter `rungs.stage.verdicts`, tags `stage` (`1`–`7`) and `status`
  (`SATISFIED`, `IN_PROGRESS`, `NOT_STARTED`, `SKIPPED`), incremented once per stage per
  successful `/v1/assessments` call (not by projections, to avoid double counting).
- **REQ-004**: Counter `rungs.snapshot.imports`, tag `outcome` (`CLEAN`, `PARTIAL`,
  `FAILED`), incremented by the import endpoint when story 021 lands.
- **REQ-005**: Counter `rungs.validation.rejections`, tag `code` (the bounded validation
  code enum from story 011), incremented once per error entry.
- **REQ-006**: Metrics recording lives in adapters or `application`, never in `domain`
  (AC-A).

### Should Have (P1)
- **REQ-007**: README documents the probe and metrics endpoints and lists each custom
  metric with its tags.

### Nice to Have (P2)
- **REQ-008**: Percentile histograms enabled for `http.server.requests` so p95 is directly
  readable.

## Non-Functional Requirements

### Performance
- Metric recording adds negligible overhead (in-memory counters).

### Security
- The Prometheus endpoint is unauthenticated but exposes no household data; acceptable for
  this demo and noted in the README.
- No tag value is derived from request content except bounded enums.

### Accessibility / Browser Support
- Not applicable.

## Constraints and Assumptions

### Technical Constraints
- Same port as the application; no separate management port.
- Health details remain hidden (story 001).

### Assumptions
- Story 037's container health check uses `/actuator/health/readiness`.

## Out of Scope
- Dashboards, alerting, and a metrics backend deployment.
- Logging — story 018.
- Tracing.

## Success Metrics
- Tests assert probe responses, the exposed endpoint set, and counter increments for a
  known assessment and a validation failure.
- A scrape of `/actuator/prometheus` after a test run contains no dollar amount or label.
