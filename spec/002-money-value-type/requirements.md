# Requirements Specification

<!-- Feature name: 002-money-value-type -->
<!-- Created by: tommrozinski -->
<!-- Date: 2026-09-12 -->
<!-- Ticket: none (backlog story 002) -->
<!-- Link: backlog/002-money-value-type.md -->
<!-- Branch: main -->

## Feature Overview

`Money` is the one type the whole engine uses for dollar amounts: an immutable value
backed by a `long` count of whole cents, living in `domain.model`. It exists because
floating-point money is a defect (PRD §5) and "correct to the cent, and provably so" is
the project's headline goal (G5). Every later calculation — stage shortfalls, the
monthly surplus, interest accrual, payoff schedules — builds on its arithmetic and its
half-even rounding. It also owns how money is written and read: the display format shown
on every screen and API response, the bare format written to CSV, and parsing that
accepts `$` and thousands separators. Formatting and parsing use fixed US rules and never
depend on the machine's locale, so output is identical everywhere (NFR-4). `Money` allows
negative amounts, because differences such as the monthly surplus can legitimately be
negative; rules like "a balance cannot be negative" belong to validation (story 011). It
serves every later domain story, and the reviewer checking that no `double` ever touches
a dollar.

## User Stories

### Story 1: Exact arithmetic for domain code
**As** a developer writing stage and payoff logic
**I want** a money type that does exact whole-cent math
**So that** every figure the engine reports is correct to the cent

**Acceptance Criteria:**
- [ ] `Money` is an immutable record backed by a `long` count of cents, with a `ZERO`
  constant and an `ofCents(long)` factory.
- [ ] It supports add, subtract, negate, multiply by `BigDecimal` (rounded half-even to the
  cent), comparison, `min`/`max`, and `isZero`/`isNegative`/`isPositive`.
- [ ] Negative amounts are allowed.
- [ ] Arithmetic that would overflow a `long` throws `ArithmeticException` instead of
  wrapping.
- [ ] No `double` or `float` appears in the type or its tests.
- [ ] Tests cover zero, one cent, negatives, and rounding exactly at a half cent in both
  directions.

### Story 2: One display format everywhere
**As** someone reading a screen or API response
**I want** every dollar amount written the same way
**So that** figures are easy to read and never differ between machines

**Acceptance Criteria:**
- [ ] Display format is `$1,234.56`: dollar sign, comma thousands separators, always two
  decimals.
- [ ] Zero displays as `$0.00`; negatives display as `-$1,234.56`.
- [ ] Output is identical regardless of the machine's default locale, proven by a test
  that runs under a non-US default locale.

### Story 3: Reading and writing CSV amounts
**As** a user loading or saving a snapshot file
**I want** amounts to accept common typing styles and save in one plain form
**So that** hand-edited files load and saved files are clean (AC-7.4)

**Acceptance Criteria:**
- [ ] Bare format is `1234.56`: no `$`, no separators, always two decimals; negatives are
  `-1234.56`.
- [ ] Parsing accepts an optional leading `-`, an optional `$`, optional comma thousands
  separators, surrounding whitespace, and zero, one, or two decimal places (`1234`,
  `1234.5`, `$1,234.56`, `-$50`).
- [ ] More than two decimal places (`12.345`) is rejected as malformed, not rounded.
- [ ] Blank or whitespace-only input is rejected by `Money` itself; turning blanks into
  zero is the job of the `application` input models (PRD FR-4).
- [ ] Malformed input fails with a specific exception that says why, never a silent zero.
- [ ] Parsing never converts through `double`.
- [ ] Round trip: `parse(toBare(m))` equals `m` for any amount.

## Functional Requirements

<!-- P0 = Must have for launch
     P1 = Should have, workarounds exist
     P2 = Nice to have
     Use REQ-001, REQ-002 etc. as stable identifiers -->

### Must Have (P0)
- **REQ-001**: `Money` is a record in `com.rungs.readiness.domain.model` with a single
  `long cents` component, a `ZERO` constant, and an `ofCents(long)` factory.
- **REQ-002**: `plus`, `minus`, and `negate` use exact `long` arithmetic
  (`Math.addExact` and friends) and throw `ArithmeticException` on overflow.
- **REQ-003**: `times(BigDecimal)` multiplies and rounds `HALF_EVEN` to the nearest cent,
  throwing `ArithmeticException` if the result does not fit in a `long`.
- **REQ-004**: `Money` implements `Comparable<Money>` and provides `min`, `max`, `isZero`,
  `isNegative`, and `isPositive`.
- **REQ-005**: `toDisplayString()` returns `$1,234.56` form: `$`, comma thousands
  separators, exactly two decimals, `$0.00` for zero, `-$1,234.56` for negatives. It uses
  fixed US rules and never reads the default locale.
- **REQ-006**: `toBareString()` returns `1234.56` form: no `$`, no separators, exactly two
  decimals, `-1234.56` for negatives.
- **REQ-007**: `parse(String)` accepts, after trimming whitespace: an optional leading `-`,
  an optional `$`, digits with optional comma separators in valid groups of three, and an
  optional `.` followed by one or two digits. It rejects blank input, more than two
  decimals, misplaced commas, and any other character. A negative zero (`-0`, `-$0.00`)
  parses to `ZERO`, and zero never displays with a minus sign.
- **REQ-008**: Parse failures throw an unchecked `MoneyFormatException` (in
  `domain.model`) carrying a reason code; the message names the reason, not the raw input
  (NFR-2).
- **REQ-009**: **Wire format contract.** In JSON responses, a `Money` value is an object
  `{"amount": "<bare string>", "display": "<display string>"}`, e.g.
  `{"amount": "-45.00", "display": "-$45.00"}`. In JSON requests, it is a plain decimal
  string (`"1234.56"`) read with `parse`. The amount is a string, never a JSON number
  (request format amended 2026-09-13 in story 007). `Money` supplies both strings; the Jackson mapping is implemented in the web
  adapter (story 007), and the OpenAPI schema documents it (story 016).
- **REQ-010**: No `double`, `float`, `Double`, or `Float` appears in the type, its
  exception, or its tests.

### Should Have (P1)
- **REQ-011**: `Money.sum(Collection<Money>)` totals a collection with exact arithmetic,
  returning `ZERO` for an empty collection.
- **REQ-012**: Parsing rejects input longer than 32 characters before inspecting it, so
  pathological input cannot cause expensive work.

### Nice to Have (P2)
- **REQ-013**: `ofDollars(long)` convenience factory for readable tests and fixtures.

## Non-Functional Requirements

### Performance
- `plus`, `minus`, `negate`, and comparisons use primitive `long` math with no
  `BigDecimal` allocation, since a 600-month, 50-debt projection calls them hundreds of
  thousands of times.
- Only `times(BigDecimal)`, formatting, and parsing may allocate.

### Security
- `MoneyFormatException` messages never include the raw input, so a logged exception
  cannot leak a household figure (PRD NFR-2).
- Parsing bounds input length (REQ-012).

### Accessibility / Browser Support
- Not applicable: `Money` has no UI. Screens render its display string with tabular
  numerals (story 024).

## Constraints and Assumptions

### Technical Constraints
- Pure domain code: no Spring, Jakarta, or Jackson imports (PRD AC-A); immutable record
  (AC-E); no floating-point types (AC-F).
- USD only; no currency field (PRD NG5).
- Rounding is `HALF_EVEN` to the cent (PRD FR-2).
- Output must be byte-identical for identical input on any machine (PRD NFR-4).

### Assumptions
- Amounts fit in a `long` count of cents (about ±$92 quadrillion), which is far beyond
  any household figure.
- Interest arithmetic (story 008) computes a monthly rate as `BigDecimal` and calls
  `times`; `Money` does not model interest rates itself.
- Blank values become zero in the `application` input models before `parse` is called
  (PRD FR-4).
- jqwik is not yet a dependency; this story uses example-based boundary tests, and story
  014 adds property-based coverage.

## Out of Scope
- The Jackson serializer and deserializer — story 007 (contract fixed by REQ-009).
- Rejecting negative balances and other input rules — story 011.
- Percent-complete ratios between two amounts — stories 004 and 005.
- Interest accrual and payoff arithmetic — story 008.
- CSV row parsing and quoting — story 020.
- Log redaction policy, including whether the record's `toString` must hide the amount —
  story 018.
- Any currency other than USD.

## Success Metrics
- 100% line and branch coverage on `Money` and `MoneyFormatException`.
- A search of the type and its tests finds no floating-point types.
- The display and bare format tests pass with the default locale set to `de-DE` and
  `fr-FR`.
- `parse(toBare(m))` equals `m` across the boundary set: zero, ±1 cent, ±half-cent
  rounding results, and `Long.MIN_VALUE + 1` / `Long.MAX_VALUE`.
- Half-even rounding matches hand-computed expectations for `.005`, `.015`, `.025`, and
  their negatives.
