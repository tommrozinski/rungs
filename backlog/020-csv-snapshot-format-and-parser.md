---
status: proposed
---

# CSV snapshot format and parser

The versioned CSV format and its domain-side reader and writer — the file is the
save mechanism for a service that stores nothing.

## Description

FR-7. The service has no persistence, so a CSV file the user keeps *is* the save
mechanism. The format is a single CSV whose first line is a bare version marker
(`rungs_snapshot_v1`), with every subsequent row beginning with a type
discriminator:

```
rungs_snapshot_v1
FIELD,monthly_take_home,6200.00
FLAG,rents,true
DEBT,Store card,CREDIT_CARD,840.00,26.99,35.00
```

Row order is not significant. Unknown `FIELD` and `FLAG` keys are reported as
warnings rather than failures, so a file written by a later version degrades
instead of breaking.

Parsing lives in the domain, not the adapter and emphatically not the client
(stories 015 and 035). This story owns the format, the reader, and the writer as
pure domain logic; the HTTP surfaces are stories 021 and 022.

The hard part is quoting. AC-7.5 requires a debt label containing a comma, a
quote, or a newline to survive a round trip intact, which means RFC 4180 quoting
done properly in both directions rather than a `split(",")`.

## Acceptance Criteria

- A reader parses the format into a `FinancialSnapshot` plus a structured report
  of accepted rows, rejected rows, and unknown keys.
- A writer serializes a `FinancialSnapshot` back to the format.
- The version marker is written as the first line and required on read.
- Row order is insignificant on read.
- Unknown `FIELD` and `FLAG` keys are collected as warnings, not failures.
- A malformed row is rejected individually with its line number and a reason
  code; parsing continues (AC-7.2). No row is ever silently discarded.
- AC-7.4: currency values parse with or without a leading `$` and with or without
  thousands separators, and are written without either.
- AC-7.5: labels containing commas, double quotes, and newlines round trip
  intact, with RFC 4180 quoting and escaping in both directions.
- Unicode labels round trip intact; encoding is UTF-8 and is explicit.
- Reason codes for rejection are a bounded enum, so story 017 can tag metrics and
  story 018 can log without echoing content.
- The reader is pure domain code with no Spring, Jakarta, or Jackson imports.
- No `double` or `float` is used in parsing currency (AC-F).
- AC-7.7: a blank value in a `FIELD` or `DEBT` row reads as zero and is not a
  malformed row.
- The reader emits every `DEBT` row it parses, including zero-balance ones. Zero-
  balance debts are then removed by the `application` input models (PRD FR-4),
  which record each as a skipped row — line number and a "paid off" reason code —
  so nothing is silently discarded.
- The writer never writes a zero-balance debt, because a normalized snapshot
  never contains one.
- A `DEBT` row with a blank label is not malformed. The reader passes the blank
  through, and the `application` input models name it "Debt N" by its order
  among `DEBT` rows in the file (PRD FR-4). The writer always writes a label,
  since a normalized snapshot never has a blank one.

## Size

L

## Open Questions

- **The version marker contains the project name** (`rungs_snapshot_v1`). The
  name is settled (story 001) and nothing has shipped, so no historical-marker
  support is needed now. Worth deciding whether the reader should tolerate a list
  of accepted markers anyway, so a future rename is not a breaking change for
  every file a user has saved.
- The `DEBT` row has no id column, but `Debt.id` exists in the domain model. Are
  ids generated on import, and how does that interact with the AC-7.1 identity
  property?
- The sample shows no `MORTGAGE` row and no `dependentsInCollegePlan` field. Can
  a mortgage round trip at all? If not, export is lossy and AC-7.1 cannot hold for
  snapshots that have one.
- Is the emergency-fund multiple (stories 005, 012) a `FIELD` key?
- Are `FIELD` keys case-sensitive? Is whitespace around values trimmed?
- What happens on a duplicate `FIELD` key — last wins, or a rejection?
