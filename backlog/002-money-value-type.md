---
status: approved
---

# Money value type

An immutable `Money` type backed by whole cents in a `long`, with the arithmetic,
rounding, and presentation formatting the rest of the system depends on.

## Description

The PRD is explicit: all money is whole cents in a `long`-backed value type, and
floating-point money is a defect. `double` and `float` are banned outright in the
`domain` package (AC-F). Interest arithmetic elsewhere rounds half-even to the
cent, so `Money` owns that rounding rather than leaving it to call sites.

The design spec adds a requirement the PRD alone did not imply: "Formatted
currency strings come from the API, not from client formatting logic. The server
already owns money representation and should own its presentation too." So
`Money` owns display formatting as well, and API responses carry a formatted
string alongside the machine-readable value. That is what keeps `parseFloat` out
of the browser.

CSV import and export (stories 020–022) also need parsing tolerant of `$` and
thousands separators, writing back a bare canonical form.

## Acceptance Criteria

- `Money` is immutable, backed by a `long` count of cents, and carries no
  currency field (USD only per NG5).
- Supports add, subtract, multiply-by-`BigDecimal`, comparison, `min`/`max`, and
  a zero constant.
- Multiplication and any division round half-even to the nearest cent.
- Exposes a canonical display format (`$1,234.56`) owned by the domain.
- Exposes a canonical bare format for CSV output (`1234.56` — no symbol, no
  separators) per AC-7.4.
- Parses input with or without a leading `$` and with or without thousands
  separators (AC-7.4).
- No `double` or `float` appears anywhere in the type or its tests.
- JSON serialization carries both the machine value and the formatted string, and
  round-trips without precision loss.
- Boundary tests cover zero, one cent, and rounding exactly at the half-cent.

## Size

M

## Open Questions

- ~~Wire format?~~ **Resolved.** `{"amount": "1234.56", "display": "$1,234.56"}` —
  decimal string, never a JSON number (PRD §5).
- ~~Negative values?~~ **Resolved.** `Money` allows negatives; input rules such as
  non-negative balances belong to validation (story 011).
- ~~Negative display format?~~ **Resolved.** `-$50.00`.

Full requirements: [spec/002-money-value-type/requirements.md](../spec/002-money-value-type/requirements.md).
