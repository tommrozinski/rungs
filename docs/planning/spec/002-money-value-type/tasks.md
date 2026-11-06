# Tasks: 002 — Money value type

<!-- Source: spec/002-money-value-type/design.md -->
<!-- Ticket: none (backlog story 002) -->
<!-- Date: 2026-09-13 -->

## Overview

Pure domain code in `com.rungs.readiness.domain.model`: a `Money` record over `long cents`
and `MoneyFormatException`. First make coverage measurable (Surefire `@{argLine}`), then build
`Money` test-first in slices: exact arithmetic, comparison and `sum`, `times`, formatting,
the exception, parsing, round trips. Finish with the floating-point search and a 100% JaCoCo
check. Commits go straight to `main`.

## Time estimates

| Phase | Estimate |
| --- | --- |
| 1. Build | 30 min |
| 2. Arithmetic | 75 min |
| 3. Formatting | 30 min |
| 4. Parsing | 90 min |
| 5. Verification | 30 min |
| **Total** | **~4 h 15 min** |

## Phase 1: Build

- [x] Change the Surefire argLine in pom.xml to @{argLine} -javaagent:${org.mockito:mockito-core:jar} with an empty argLine property default, and confirm ./mvnw verify passes with no dynamic-agent warning
  - ID 1.1 · Depends on: none
  - ⚠️ Risk: if `@{argLine}` is left unreplaced (no property default), the forked JVM fails to start.
- [x] Run JaCoCo 0.8.15 (officially supports Java 26) prepare-agent, test, and report from the command line, confirm a report is produced for the existing classes, and record the command in claude-progress.txt
  - ID 1.2 · Depends on: 1.1
  - Note: checked 2026-09-13; 0.8.15 is the latest release and officially supports Java 26.

## Phase 2: Arithmetic

- [x] Add the Money record (long cents) with ZERO, ofCents, ofDollars (multiplyExact), and plus, minus, negate using Math.*Exact, with MoneyArithmeticTests for zero, ±1 cent, mixed signs, and overflow including negate of Long.MIN_VALUE
  - ID 2.1 · Depends on: 1.1
- [x] Add Comparable compareTo, instance min and max returning this on ties, isZero/isNegative/isPositive, and static sum(Collection) returning ZERO when empty, with tests for ties, signs, and empty, single, mixed, and overflowing collections
  - ID 2.2 · Depends on: 2.1
- [x] Add times(BigDecimal) per design §6.2 (ArithmeticException for a factor over 64 significant digits, exact multiply, ZERO when the leading-digit exponent is below -1, ArithmeticException above 18, HALF_EVEN setScale, longValueExact), with tests for 100 cents × .005/.015/.025 and negatives (0, 2, 2 and 0, -2, -2 cents), .0051, zero and negative factors, extreme exponents, guard edges (products of 0.1 and 0.09 cents, 1E18 and 1E19 cents, 64- and 65-digit factors), and overflow
  - ID 2.3 · Depends on: 2.1
  - ⚠️ Risk: an off-by-one in the guards either rounds a real half cent to zero or lets an expensive `setScale` run; the guard-edge tests pin each boundary.

## Phase 3: Formatting

- [x] Add toBareString and toDisplayString built from Long.toString(cents) with manual comma grouping, with MoneyFormattingTests for 0, 1, 10, 99, 100, 123456, 100000000, -1, -123456, Long.MAX_VALUE, and Long.MIN_VALUE under the default, de-DE, fr-FR, and ar-EG-u-nu-arab locales
  - ID 3.1 · Depends on: 2.1
  - ⚠️ Risk: a locale-aware helper (`String.format`, `NumberFormat`) slipping in; de-DE and fr-FR catch separator changes and ar-EG-u-nu-arab catches digit substitution. Restore the default locale in `finally`.

## Phase 4: Parsing

- [x] Add MoneyFormatException extending IllegalArgumentException with a Reason enum (BLANK, TOO_LONG, INVALID_CHARACTER, MISSING_DIGITS, MISPLACED_SEPARATOR, TOO_MANY_DECIMALS, OUT_OF_RANGE), a reason() accessor, and one fixed message per reason that never includes the input, with tests in MoneyParsingTests
  - ID 4.1 · Depends on: 1.1
- [x] Add Money.parse as the single left-to-right scan in design §6.4 (null or blank, 32-character bound before strip, optional - then $, ASCII digits only, comma groups, one or two decimals, negative-side accumulation), with MoneyParsingTests for every accepted example and at least one rejected input per Reason asserting the message omits the input, plus pinned precedence cases: "1,23.456" MISPLACED_SEPARATOR, "$-5" INVALID_CHARACTER, 40 spaces TOO_LONG, "-" MISSING_DIGITS, "1.2,3" MISPLACED_SEPARATOR
  - ID 4.2 · Depends on: 2.1, 4.1
  - ⚠️ Risk: reason precedence follows the design §6.4 step order; the pinned precedence cases and the design §4 examples guard it.
- [x] Add round-trip tests that parse(m.toBareString()) and parse(m.toDisplayString()) equal m for 0, ±1, ±50, ±123456, Long.MAX_VALUE, Long.MIN_VALUE, and Long.MIN_VALUE + 1
  - ID 4.3 · Depends on: 3.1, 4.2

## Phase 5: Verification

- [x] Run ./mvnw verify and search Money.java, MoneyFormatException.java, and the three test classes for double, float, Double, and Float, confirming none appear
  - ID 5.1 · Depends on: 2.2, 2.3, 4.3
- [x] Measure JaCoCo line and branch coverage for Money and MoneyFormatException and add tests for any uncovered line or branch until both reach 100%, using JaCoCo 0.8.15 as in task 1.2
  - ID 5.2 · Depends on: 1.2, 5.1
