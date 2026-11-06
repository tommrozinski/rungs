---
status: approved
---

# ArchUnit architecture constraint suite

Encode section 8's eight constraints as tests so the layering is enforced by the
build rather than by reviewer vigilance.

## Description

The PRD frames these as testable constraints, not preferences, and expects
ArchUnit to enforce them in CI:

- **AC-A** `domain` imports nothing from Spring, Jakarta, or Jackson.
- **AC-B** `domain` has no dependency on `application` or `adapter`.
- **AC-C** Adapters depend on ports (domain-owned interfaces), never on other adapters.
- **AC-D** No field injection. Constructor injection only.
- **AC-E** No public mutable state. Domain types are records or immutable classes.
- **AC-F** `double` and `float` are banned in `domain`.

Section 8 now also carries two constraints for the web client:

- **AC-G** No business logic in the web client — no arithmetic, number or
  currency parsing or formatting, or JSON fetching in templates or `static/js`;
  view controllers depend on `application` only.
- **AC-H** One Maven module — no second module, `package.json`, Node toolchain,
  or frontend build tool.

Both are enforced by the build, not by review; the design spec has been amended
to say so.

Design spec section 7 now fixes the client technology: server-rendered Thymeleaf
in the same single Maven module, no SPA, no `package.json`, no frontend build
tool — optionally htmx as a single script tag. That makes the ban much more
checkable than it was while the SPA door was open: the "client code" is a known,
small set of template files and hand-written static scripts, all inside the
same build. Template expressions and `<script>` blocks are greppable; a
convincing-looking `parseFloat` is exactly the shortcut the constraint exists to
prevent. The no-SPA decision itself is also a structural rule worth enforcing,
since it is stated as final.

## Acceptance Criteria

- One ArchUnit rule per constraint AC-A through AC-F, each named for the
  constraint it encodes. AC-G and AC-H are enforced by the ArchUnit rules and
  build-time checks below, also named for their constraints.
- Each rule is proven to actually fire: a deliberate violation fails the suite
  (verified during development, not committed).
- The suite runs as part of `mvn verify`, not a separate opt-in profile.
- AC-A bans both Jackson 3 (`tools.jackson`, the default in Spring Boot 4) and
  Jackson 2 (`com.fasterxml.jackson`) packages in `domain`.
- Rule failures name the offending class and the constraint in the message.
- AC-F covers `double`, `float`, and their boxed types in fields, parameters,
  return types, and local variables where detectable.
- AC-E accepts records and final classes with final fields; flags public mutable
  collections.
- A build-time check over `src/main/resources/templates` and
  `src/main/resources/static` enforces the client-side arithmetic and parsing
  ban (e.g. no `parseFloat`, `parseInt`, `Number(`, arithmetic in Thymeleaf
  expressions, or `#numbers` formatting of money), runs in `mvn verify`, and
  documents its limits rather than assuming them away.
- A build-time check fails if a `package.json`, lockfile, `node_modules`, or a
  second Maven module appears, enforcing design spec section 7's single-module,
  no-frontend-build decision.
- `adapter.in.web.view` and `adapter.in.web.api` do not depend on each other
  (AC-C applied inside `adapter.in.web`).
- `adapter.in.web.view` depends on `application` only — never directly on
  `domain.stage` or `domain.projection` — so pages cannot compute figures the
  API does not expose.
- The JavaScript policy in design spec section 7 is checked in `mvn verify`:
  no inline `<script>` blocks or inline event handlers in templates; hand-written
  files in `static/js` contain no arithmetic, number/currency parsing or
  formatting, or `fetch` calls; vendored htmx is excluded from the scan;
  hand-written JavaScript stays within the ~200-line budget.

## Size

M

## Open Questions

- ~~Does AC-A ban `jakarta.validation`?~~ **Resolved.** Yes, all of `jakarta..`;
  constraints live in `application`.
- ~~Are tests exempt from AC-F?~~ **Resolved.** Rules apply to main classes only.
- ~~Ban nondeterminism in `domain`?~~ **Resolved.** Yes: no `now()`, system clocks,
  random sources, or `UUID.randomUUID`.
- ~~Review or automation for the client-side ban?~~ **Resolved.** Automated
  (PRD AC-G; design spec section 0 amended), via JUnit scans of template and
  static files, with committed violation fixtures proving each rule fires.

Full requirements: [spec/015-archunit-architecture-suite/requirements.md](../spec/015-archunit-architecture-suite/requirements.md).
- ~~Is hand-written JavaScript allowed beyond htmx?~~ **Resolved.** Design spec
  section 7: yes, in external `static/js` files, page state only, no arithmetic,
  formatting, or JSON fetching, roughly 200 lines total.
- ~~PRD FR-7 cites the wrong constraint (AC-F).~~ **Resolved.** FR-7 now cites
  the new AC-G.
