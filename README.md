# Rungs

[![CI](https://github.com/tommrozinski/rungs/actions/workflows/ci.yml/badge.svg)](https://github.com/tommrozinski/rungs/actions/workflows/ci.yml)

A household enters take-home pay, expenses, emergency funds, a retirement savings
percentage, debts, and mortgage or rent. Rungs reports which of seven stages the
household is on, what's blocking the next one, and a month-by-month debt snowball
payoff schedule that includes the mortgage. It is an independent implementation of a
widely published personal-finance framework and is not affiliated with, or endorsed by,
the framework's author. It is built with Java 25 and Spring Boot 4.1.

## Nothing is stored

Statelessness is a design commitment of the project, not an afterthought. Rungs has no
database, sets no cookies, and keeps no sessions. Household figures travel in the request
and nowhere else: requests are processed in memory and discarded, and money values and
snapshot contents are never written to logs.

## Run it

Prerequisite: JDK 25 or newer, from any vendor.

```sh
./mvnw spring-boot:run
```

Then open <http://localhost:8080>.

Run the tests:

```sh
./mvnw verify
```

On Windows, use `.\mvnw.cmd` instead of `./mvnw` (works in PowerShell and Command
Prompt). The Maven Wrapper downloads the correct Maven version on first use, so the
first run needs an internet connection.

## How it works

**Package map:**

- `domain/model` — `Money` (exact cents), `FinancialSnapshot`, and the sealed `Housing`
  type (`Rent` or `Mortgage`).
- `domain/stage` — the seven stage evaluators and `StageLadder`, which runs them in
  order and reports the current stage.
- `domain/projection` — the debt snowball payoff schedule and mortgage payoff
  calculations.
- `web` — the controller, lenient form-to-domain mapping, view models, and the
  Thymeleaf templates.

**Request flow:** `GET /` renders the form. `POST /sample` fills it with a chosen sample
household; `POST /debts/add` and `POST /debts/remove/{index}` add and remove a debt row.
`POST /assess` builds a snapshot from the submitted values and renders the result;
`breakdown=true` also renders the "Your loans" and "Payoff schedule" iframes below the
seven stages. `POST /edit` ("Change my numbers") re-renders the form from the result
page's hidden copy. The result page carries every submitted value in hidden fields and
posts them back to the server, so nothing here needs a session, cookies, or JavaScript.

**Security hardening:** a strict Content-Security-Policy and other browser security
headers on every response, `Cache-Control: no-store` outside static assets, request-size
and parameter-count limits with multipart disabled, a bind allow-list plus length caps on
every form field, and a custom error page instead of stack traces.

## Key design decisions

- Money is exact cents throughout, with half-even rounding at every calculation
  boundary — no floating point.
- The domain is pure: the controller reads an injected `Clock` and passes the current
  month into the snapshot, so nothing in `domain` reads the system clock.
- Form input parsing is lenient: a blank or invalid field becomes 0 rather than a
  validation error, amounts are capped at $1B, and a household can enter up to 20
  debts.
- The app is stateless and ships no JavaScript.
- Templates are built from static HTML mockups in `docs/mockups/` first, then carried
  forward with Thymeleaf attributes, so template and controller are never written from
  two different descriptions of the same screen.

## Testing

JUnit 5 and MockMvc cover the web layer; domain rules are tested at their boundaries
(the exact dollar and percentage thresholds each stage evaluates). CI runs the full
suite on every push to `main` and on every pull request.

## Project history

An earlier, larger version of this project scoped out JSON APIs, CSV import/export, a
JavaScript-driven UI, and an architecture test suite; that scope was deliberately cut
back to the focused version described above, then grown again one visually checked
feature at a time. [`docs/design/`](docs/design/) and [`docs/scope.md`](docs/scope.md)
are design history — decisions as they were made at the time, not a description of the
current app; where a later change supersedes one, the affected document says so at the
top. The rest of the history is in git.

`docs/mockups/` holds the static, pre-build HTML/CSS design artifacts each template was
carried forward from (see "Key design decisions" above); they use a placeholder product
name ("Gazelle") and throwaway `sessionStorage`/JavaScript for their own demo
interactivity, and are not served by the app.

The original plan, including the user stories and the per-story requirements, design, and
task specs from the spec-driven workflow, is in [`docs/planning/`](docs/planning/).
