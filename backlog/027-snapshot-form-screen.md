---
status: proposed
---

# S-01 Snapshot form screen

The only input screen: three cards covering money in and out, debts, and
household, with inline validation bound to the API's problem details.

## Description

Design spec section 4, S-01, excluding the file bar and import report which are
story 028.

Three cards. Debts are a table with an add-row action, "not a repeating stack of
form cards" — the spec is explicit, and the distinction matters at 50 debts.

Two requirements here are product claims rather than layout:

- **The subhead states that nothing is stored**, above the fold rather than in a
  footer, because it is a privacy claim and a feature (NFR-1).
- **"Load an example" fills the form with a demo household** so anyone reviewing
  the repo sees output in one click without typing twelve numbers. The spec
  routes it through the same import endpoint as a user file — one code path,
  exercised on every demo.

Validation is inline and on blur, and field-level messages map one-to-one to the
RFC 9457 entries the API returns (story 011). The client never invents its own
wording, which also means it never validates independently — it submits and
renders what comes back.

## Acceptance Criteria

- Three cards: money in and out, debts, household.
- Debts render as a table with an add-row action and a remove action per row.
- The subhead states that nothing is stored, above the fold.
- "Load an example" populates the form by posting a preset CSV through
  `/v1/snapshots/import` — the same path a user file takes.
- Validation is inline and on blur via htmx: leaving a field posts it and swaps
  in the server-rendered field fragment with any error message. With JavaScript
  disabled, validation runs on submit.
- **Field-only rules (checked on blur, that field alone):** each money field is
  not negative — take-home income, essential expenses, starter fund, full fund,
  and each debt row's balance and minimum payment (FR-4); retirement contribution
  percent is between 0 and 100 (FR-4). Leaving a debt-row field checks that field
  only, not the rest of the row.
- **Whole-form rules (checked on add-row and on submit, never on blur):** no more
  than 50 debts (FR-4). The message renders on the debts table, and adding a 51st
  row is refused by the server.
- Submit checks every rule and renders every error at once.
- A blank number field (money values, retirement percent, and each debt row's
  balance, rate, and minimum payment) is treated as zero. Leaving one empty shows
  no message, on blur or on submit. The conversion happens server-side in the
  shared `application` input models (PRD FR-4), exactly as for the API — never in
  the browser (AC-G).
- A debt row with a zero balance, including a fully blank row, is ignored: it is
  paid off and contributes nothing to the assessment or the payoff plan. It is
  never an error.
- A debt row with a blank label is named "Debt N" by the server, where N is its
  row position (PRD FR-4). After submit the generated name appears in the row's
  label field. It is never an error.
- After submit, an ignored row stays visible in the debts table, with its blank
  number fields shown as $0 (rendered server-side). The user sees what they
  entered, and the $0 balance makes clear why it did not count.
- Blur and submit produce identical messages for the same field, because both
  use the same constraints from story 011.
- FR-4's "unknown JSON properties" rule does not apply to the form, which cannot
  send unknown fields.
- The four FR-5 warnings span several fields but are non-fatal, so they are never
  shown on this screen; they appear on the assessment screen (story 029).
- Field-level error messages are rendered verbatim from the API's problem-detail
  entries, bound by field pointer; the client contains no validation copy.
- No stage rules, payoff math, currency arithmetic, or parsing exist in client
  code.
- All currency figures displayed come from server-formatted strings.
- The screen renders and submits correctly with JavaScript disabled.
- Copy follows the voice rules: second person, sentence case, buttons say what
  happens ("Check my stage", never "Submit").

## Size

M

## Open Questions

- With JavaScript disabled, how do add-row and remove-row work? Server-rendered
  Thymeleaf (design spec section 7) makes a server round trip per row the
  natural baseline, with htmx swapping only the table when JavaScript is on — or
  does the form ship with a fixed number of blank rows?
- How many preset households ship? Design spec open question leans toward three
  (deep in debt, mid-journey, nearly done) to make skip logic and warnings
  visible without data entry.
- Does the form collect a mortgage? The domain model has one and stage 6 depends
  on it, but S-01's three cards do not obviously include it and the CSV format
  has no mortgage row (story 020).
- Where does `dependentsInCollegePlan` appear, if anywhere?
- ~~How does on-blur validation work without client-side rules?~~ **Resolved.**
  Design spec section 7: an htmx post on blur re-renders the field fragment from
  the server's validation, falling back to validation on submit without
  JavaScript.
- ~~On blur, validate one field or the whole form?~~ **Resolved.** One field
  for field-only rules; the 50-debt limit is whole-form and runs on add-row and
  submit (design spec S-01).
- ~~Is a blank field an error or treated as zero?~~ **Resolved.** A blank number
  field is zero.
- ~~Does blank-as-zero apply to the API too?~~ **Resolved.** Yes — PRD FR-4.
- ~~Is a fully blank or $0 debt row ignored?~~ **Resolved.** Ignored; a zero
  balance means paid off.
- ~~Is a blank debt label an error?~~ **Resolved.** No — the server names it
  "Debt N" by row position.
- ~~Does an ignored $0 row stay visible after submit?~~ **Resolved.** Yes, shown
  with $0 values.
- Any new rule added in story 011 needs the same classification. Candidates
  already raised there: debt `id` uniqueness (whole-form, if adopted) and
  zero-value debts (field or row level).
