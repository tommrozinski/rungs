---
status: approved
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
  `emergencyFundMonths`, `monthlyCollegeContribution`, `mortgage` (nullable), and
  `householdFlags`. `dependentsInCollegePlan` is removed.
- `Debt` carries `id` (server-assigned `debt-N`), `label`, `balance`,
  `annualInterestRate` (a percentage), `minimumPayment`,
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

- ~~Percentage or fraction?~~ **Resolved.** Percentage, as a decimal string.
- ~~Rate scale?~~ **Resolved.** Up to 3 decimals, normalized on construction.
- ~~Is `Debt.id` caller-supplied?~~ **Resolved.** Server-assigned `debt-N` by
  position; clients never send it.
- ~~What is `dependentsInCollegePlan` for?~~ **Resolved.** Removed; stage 5 uses the
  new `monthlyCollegeContribution`.

Full requirements: [spec/003-financial-snapshot-domain-model/requirements.md](../spec/003-financial-snapshot-domain-model/requirements.md).
