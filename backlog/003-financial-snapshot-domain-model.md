---
status: proposed
---

# Financial snapshot domain model

The immutable request-root types — `FinancialSnapshot`, `Debt`, `Mortgage`,
`HouseholdFlags` — that describe a household's position at a point in time.

## Description

PRD section 5.1 defines the core domain types. These are pure data with no
framework coupling: the `domain` package imports nothing from Spring, Jakarta, or
Jackson (AC-A), so DTO-to-domain mapping lives in the web adapter and belongs to
the endpoint stories, not this one.

Percentages are `BigDecimal` with explicit scale. Domain types are records or
immutable classes with no public mutable state (AC-E).

CSV import and export (stories 020–022) round-trip these types, and AC-7.1
requires export-then-import to be the identity function — which makes value
equality on every type here a hard requirement, not an incidental.

## Acceptance Criteria

- `FinancialSnapshot` carries `monthlyTakeHomeIncome`, `monthlyEssentialExpenses`,
  `starterEmergencyFund`, `fullEmergencyFund`, `debts`, `retirementContributionPercent`,
  `mortgage` (nullable), `dependentsInCollegePlan`, and `householdFlags`.
- `Debt` carries `id`, `label`, `balance`, `annualInterestRate`, `minimumPayment`,
  and a `DebtType` enum of `MORTGAGE | STUDENT_LOAN | AUTO | CREDIT_CARD |
  MEDICAL | PERSONAL | OTHER`.
- `Mortgage` carries `balance`, `annualInterestRate`, `monthlyPayment`, `homeValue`.
- `HouseholdFlags` carries `rents`, `retired`, `intendsToFundCollege`.
- All types are records or immutable classes; collections are defensively copied
  and returned unmodifiable.
- Value equality is well-defined on every type, including nested collections —
  the CSV round-trip property depends on it.
- `annualInterestRate` and `retirementContributionPercent` are `BigDecimal` with
  a documented, enforced scale, and equality is scale-insensitive or scale is
  normalized on construction.
- No Spring, Jakarta, or Jackson import appears in any of these types.
- Debt list ordering is preserved exactly as supplied — story 009 depends on it
  for tie-breaking.

## Size

S

## Open Questions

- Is `annualInterestRate` a percentage (`7.25`) or a fraction (`0.0725`)? The PRD
  uses `15` for `retirementContributionPercent` and the CSV sample in FR-7 shows
  `26.99` and `6.40` for rates, which implies percentage-style — worth confirming.
- What scale is required for `annualInterestRate`? The CSV sample uses 2 decimals.
- Is `Debt.id` caller-supplied and required to be unique? The CSV `DEBT` row
  format in FR-7 has no id column, so imported debts need ids generated — which
  makes id round-trip-unstable unless it is derived or excluded from equality.
- `dependentsInCollegePlan` is on the snapshot but no stage rule references it,
  and the CSV format has no field for it. What is it for, and should it round
  trip?
