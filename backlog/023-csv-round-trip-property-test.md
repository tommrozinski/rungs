---
status: proposed
---

# CSV round-trip property test

Prove that export-then-import is the identity function on valid snapshots, by
property rather than by example.

## Description

AC-7.1 is specific about method: "For any valid snapshot, export then import
yields an equal snapshot. Verified by property test, not by example." PRD section
9 adds that the generator produces labels containing commas, quotes, and unicode.

This is its own story rather than a line in story 020 because it spans both
directions of the format, it reuses and extends the generator from story 014, and
because it is the test most likely to find the format's real defects — the
quoting bugs, the scale-normalization mismatches, the fields that silently do not
round trip at all.

It is also the story most likely to *fail* in an informative way: if a mortgage
or a debt id cannot round trip (story 020's open questions), this property is
where that surfaces as a fact rather than an opinion.

## Acceptance Criteria

- A jqwik property asserts `import(export(s))` equals `s` for arbitrary valid
  snapshots.
- The generator produces debt labels containing commas, double quotes, single
  quotes, newlines, leading and trailing whitespace, and non-ASCII unicode.
- The generator produces the full range of `DebtType` values, zero-debt
  snapshots, and 50-debt snapshots.
- The generator produces currency values at the cent boundary and at large
  magnitudes.
- "Valid snapshot" means a normalized one (PRD FR-4): the generator never
  produces a zero-balance debt, since normalization would remove it and break
  identity for a reason unrelated to the format. Debt balances start at one cent,
  and debt labels are never blank or whitespace-only, since normalization would
  replace them with "Debt N".
- A separate property asserts that a snapshot containing zero-balance debts
  imports as the same snapshot with those debts removed and reported as skipped.
- Equality is value equality across the whole snapshot graph, including debt
  ordering.
- Any field that cannot round trip is either fixed or explicitly excluded with a
  documented rationale in the test — not silently omitted from the assertion.
- Failures shrink to a minimal reproducing snapshot and print the intermediate
  CSV.
- The property runs in CI as part of `mvn verify`.

## Size

M

## Open Questions

- Does `Debt.id` participate in the equality assertion? If ids are generated on
  import (story 020), it cannot, and the exclusion needs justifying.
- Does the property cover snapshots with a mortgage? Only if the format can
  represent one.
- Is `import(export(s))` the required direction, or should
  `export(import(csv))` also hold for valid CSV input? The second is the stronger
  property and would catch writer-side normalization drift.
