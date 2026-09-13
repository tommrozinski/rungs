---
status: approved
---

# Observability: health probes and metrics

Actuator health, readiness and liveness endpoints, plus Micrometer metrics
including a counter per stage verdict and a timer per endpoint.

## Description

NFR-5. The stage-verdict counter is the interesting one: it turns the service's
own output into an operational signal, so a shift in the distribution of verdicts
is visible without logging any household's figures — which matters because NFR-2
forbids logging the figures themselves.

Timers per endpoint are what make the NFR-3 targets measurable rather than
aspirational, and the projection timer now has a user-facing consequence: the
design spec's slider re-queries the API on every change.

## Acceptance Criteria

- Actuator exposes health, readiness, and liveness probes.
- A Micrometer counter increments per stage verdict, tagged by stage and status.
- A Micrometer timer records latency per endpoint, including the snapshot
  import and export endpoints.
- No metric tag carries a money value, a household figure, a debt label, a
  filename, or anything caller-identifying (NFR-2).
- Metric cardinality is bounded — tags are enum-valued, never free text such as
  a blocker message or an import failure reason.
- A test asserts the stage-verdict counter increments as expected for a known
  assessment.
- The metrics endpoint is documented in the README.

## Size

S

## Open Questions

- ~~Metrics backend?~~ **Resolved.** Prometheus scrape endpoint.
- ~~Separate management port?~~ **Resolved.** No, same port.
- ~~Import counter?~~ **Resolved.** Yes, tagged by `outcome` (`CLEAN`, `PARTIAL`,
  `FAILED`) only.

Full requirements: [spec/017-observability-actuator-and-metrics/requirements.md](../spec/017-observability-actuator-and-metrics/requirements.md).
