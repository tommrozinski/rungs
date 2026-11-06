---
status: approved
---

# Request validation and RFC 9457 problem details

Declare validation rules once in `application`, apply them in both inbound
adapters, and reject malformed snapshots with field-level errors in a standard
problem-detail response.

## Description

FR-4. Validation rules live in the `application` layer, declared once with
Jakarta Bean Validation on input models. Adapters trigger validation but define
no rules, and the domain stays framework-free (AC-A), so constraints are never on
domain records. Rejections return RFC 9457 problem detail
responses carrying field-level errors.

Those input models sit in `application` (PRD section 8), shared by both inbound
adapters: the REST controllers validate JSON requests against them, and the
Thymeleaf view controllers validate form posts against them. They cannot live on
the REST adapter's DTOs, because view controllers may not depend on the REST
adapter (AC-G). Sharing one set of constraints is what keeps the snapshot form's
messages identical to the API's (story 027).

Each rule is also either **field-only** or **whole-form**, because the form
checks field-only rules as the user leaves each field and whole-form rules only
on add-row and submit.

The design spec makes these responses load-bearing for the UI: "Field-level
messages map one-to-one to the RFC 9457 problem detail entries the API returns,
so the client never invents its own wording." So the `detail` strings are
user-facing copy and follow the voice rules — state what is wrong and what to do
("Enter zero or more"), never apologize, never be vague. Each entry needs a field
pointer the client can bind to an input.

Unknown JSON properties are rejected rather than silently ignored: a client
sending `retirementContributionPercentage` gets an error, not a default.

## Acceptance Criteria

- Negative money values are rejected with a field-level error naming the field.
- `retirementContributionPercent` outside 0–100 is rejected.
- A request with more than 50 debts is rejected, counting only debts with a
  balance above zero — zero-balance debts are removed first (PRD FR-4). A test
  covers 50 real debts plus several $0 debts being accepted.
- `horizonMonths` above 600 is rejected.
- Unknown JSON properties are rejected, not ignored.
- All rejections return an RFC 9457 `application/problem+json` body with `type`,
  `title`, `status`, `detail`, and a field-level error list.
- Each field-level entry carries a machine-usable field pointer plus
  user-presentable copy the client renders verbatim.
- Constraints are declared on input models in `application`, used by both the
  REST adapter and the view adapter; no constraint or message is duplicated.
- Each constraint is classified and documented as field-only (negative money
  values, `retirementContributionPercent` 0–100, `horizonMonths` ≤ 600) or
  whole-form (no more than 50 debts).
- A field-only constraint can be evaluated for a single property (e.g.
  `Validator.validateProperty`) and produces the same message as full validation.
- "Unknown JSON properties" is enforced only in the REST adapter's JSON binding;
  it has no form equivalent.
- A missing, null, or blank numeric value is treated as zero by the input models,
  identically for JSON, form posts, and CSV import. `horizonMonths` keeps its
  documented default of 360 instead. An absent `mortgage` object stays null.
- Debts with a zero balance are accepted and then removed by the input models
  before anything reaches the domain; they are never a validation error.
- Negative values are still rejected, including a negative balance on a debt
  that would otherwise be ignored.
- A blank or whitespace-only debt label is replaced with "Debt N", where N is
  the debt's 1-based position in the list as supplied (zero-balance debts
  included). It is never a validation error, and the domain never receives a
  blank label. Tests cover blank, whitespace-only, and a generated name that
  duplicates a user-typed label.
- Because missing numbers silently become zero, a misspelt numeric property must
  still fail as an unknown property rather than default to zero — covered by a
  contract test.
- Error copy follows the design spec voice rules: states the fix, sentence case,
  no apology.
- Multiple violations in one request are all reported, not just the first.
- Malformed JSON returns a problem detail rather than a stack trace or a bare 400.
- No snapshot field values appear in the problem-detail body or in logs (NFR-2).
- Contract tests assert the problem-detail JSON shape for each rejection class.

## Size

M

## Open Questions

- ~~Is `id` uniqueness a validation rule?~~ **Resolved** (spec 003). Ids are
  server-assigned; a client-supplied `id` is rejected as an unknown property.
- ~~Is FR-4's "at the adapter boundary" wording consistent?~~ **Resolved.** FR-4
  now states that rules live in `application` and adapters only trigger them.
- ~~Are zero-value debts valid?~~ **Resolved.** PRD FR-4: accepted and ignored,
  because a zero balance means the debt is paid off.
- ~~Is the 50-debt limit counted before or after removing $0 debts?~~
  **Resolved.** After. Raw request size is still bounded by the general body-size
  limit, not by this rule.
- ~~`MORTGAGE`-type debts: reject or warn?~~ **Resolved** (spec 006). Rejected;
  the warning was removed.
- ~~Problem `type` URI scheme?~~ **Resolved.** `urn:rungs:problem:<code>`, e.g.
  `urn:rungs:problem:validation`.
- ~~Field-pointer format?~~ **Resolved.** JSON Pointer (RFC 6901), e.g.
  `/debts/2/balance` (PRD FR-4).

Full requirements: [spec/011-request-validation-and-problem-details/requirements.md](../spec/011-request-validation-and-problem-details/requirements.md).
