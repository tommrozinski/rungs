# PRD Addendum: v0 Scope (the few-days build)

**Supersedes:** delivery increments in `readiness-engine-prd.md` section 10, for the
current build window only. That document is not wrong, it's sized for a month. This one
is sized for a few days.

**Rule for this pass: if it's not listed under "In," it does not get built, no matter how
easy an agent makes it look.**

---

## In scope

1. **One domain concept.** The seven-stage evaluator. `FinancialSnapshot` in,
   `ReadinessAssessment` out. PRD sections 5.1–5.3 and FR-1 only.
2. **One form, one result page.** S-01 (snapshot) and S-02 (assessment) from the design
   board. Nothing else — no projection, no comparison, no CSV, no file bar.
3. **Plain server-rendered HTML.** One Maven module. A form that POSTs to a controller
   that builds the domain object, runs the evaluator, and renders the result template.
   No JavaScript required for this to work end to end.
4. **Basic tests only.** Plain JUnit 5 against `StageEvaluator`, one test class per
   stage, covering the boundary cases already listed in the PRD (exactly $1,000, exactly
   3 months of expenses, exactly 15%). No ArchUnit, no jqwik, no coverage gate. Correctness
   of the seven rules is what matters; proving it with three testing frameworks does not.
5. **A handful of hardcoded debts**, entered as free-form rows in the form — no dynamic
   validation, no problem-detail responses. A blank or bad value can be handled with a
   plain default rather than a rejection.

## Explicitly out, for now

Projection and payoff simulation (FR-2), strategy comparison (FR-3), CSV import/export
(FR-7), categorization (FR-6), RFC 9457 problem details, ArchUnit, jqwik, coverage gates,
Actuator/Micrometer, and the mobile layout. Every one of these is real, and every one of
them is documented already in the full PRD. None of them earns its place if the seven-
stage evaluator isn't done, tested, and matching the design board first.

## Stretch, only if time remains after v0 is solid

In this order, stop whenever you run out of days:

1. Projection (FR-2) — smallest-balance-first payoff simulation only, skip avalanche.
2. Strategy comparison (FR-3).
3. Basic input validation with a friendly error message (not full problem-detail).
4. CSV import/export (FR-7).
5. ArchUnit and the architecture constraints in PRD section 8.

Do not start item N+1 before item N is working and visually correct. A half-built
projection screen is worse than no projection screen — it invites the exact question you
don't want in a demo: "what happens if I click this?"

## Build order for v0

This is the part that actually prevents a repeat of what just happened.

1. Copy the literal markup and CSS from `design-board.html` frames S-01 and S-02 into two
   static HTML files. No Spring, no templates, no backend. Confirm in a browser that they
   look right.
2. Add real `name` attributes to every input and a real `<form method="post" action="/assess">`.
   Still no backend — this file is the contract the controller will match.
3. Stand up the Spring Boot app with exactly one `GET /` (returns the form) and one
   `POST /assess` that does nothing but echo the submitted values back onto the result
   page, unmodified. Confirm the round trip works before any domain logic exists.
4. Only now write `FinancialSnapshot`, `Debt`, and the seven `StageEvaluator`s, with their
   JUnit tests, and swap the echo for a real assessment.
5. Convert the two static HTML files to Thymeleaf templates last, by adding `th:` attributes
   to the file that's already known to render correctly — not by regenerating them from a
   prose description of what the screen should look like.

Step 5 is the specific fix for what went wrong: templates were generated from the PRD's
description of the screens, in parallel with a controller, without a visual check against
the actual board markup at any point. This order removes that failure mode by construction
— there is never a moment where template and controller are written from two different
descriptions of the same thing.

## Definition of done for v0

You can open a browser, fill in the form exactly as pictured in board frame S-01, submit
it, and see the seven-stage ladder from S-02 rendered correctly with the right stage
marked current and the right blockers shown — using real Java domain logic, not an echo.
That's it. That's the whole demo if the clock runs out.
