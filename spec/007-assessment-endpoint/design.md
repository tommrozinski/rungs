# Design: 007 + 011 — Assessment endpoint, validation, and problem details

<!-- Source: requirements.md in spec/007 and spec/011 -->
<!-- Ticket: none (backlog stories 007, 011) -->
<!-- Date: 2026-09-13 -->

Combined design (lean process). The requirements files hold the exact codes, messages, JSON shapes and
acceptance criteria, and are not repeated here.

## 1. Layout

```
application/
  SnapshotInput, DebtInput, MortgageInput, HouseholdFlagsInput   records of raw strings; constraints (011)
  SnapshotInput.toSnapshot()     FR-4 normalization → domain FinancialSnapshot
  AssessmentService, ReadinessAssessment, Warning
  response/   AssessmentResponse, StageResponse, MoneyResponse, WarningResponse + from(ReadinessAssessment)
  validation/ ValidationCode (enum: code + message), constraint annotations + validators, WholeForm group
adapter/in/web/api/
  AssessmentController           POST /v1/assessments; returns application response records
  ProblemDetailsAdvice           the single @RestControllerAdvice (011)
  JsonPointers                   property path / Jackson path → RFC 6901 pointer
config/
  ApplicationConfiguration       @Configuration: builds AssessmentService with the seven evaluators
domain/model/FinancialSnapshot   + monthlySurplus()
```

## 2. Request binding (007 REQ-002/003, 011 REQ-004)

- Jackson binds the request body directly to the `application` input records; a separate request DTO
  would only duplicate them. They carry no Jackson annotations.
- Money and percentage fields are `String`, so that blank values can become zero and a value that won't
  parse gets its own code. The other fields:
  - `emergencyFundMonths` is `Integer`;
  - `type` is `String`, so `MORTGAGE` and unknown types get their own codes;
  - the flags are `Boolean`, with null meaning false;
  - `debts` is a `List`, with null meaning empty;
  - `mortgage` is nullable.
- The Jackson configuration, as a Boot customizer in the adapter:
  - `FAIL_ON_UNKNOWN_PROPERTIES` on, so unknown fields (including a client-sent debt `id`) are rejected;
  - JSON numbers and booleans are not coerced into `String` fields, so they give `INVALID_TYPE`;
  - no float coercion into `Integer`.

## 3. Normalization (007 REQ-004, FR-4)

- `toSnapshot()` is called only after validation passes. It applies these steps in order:
  1. Blank or null money and percentages become 0, and `emergencyFundMonths` defaults to 3.
  2. Each debt gets id `debt-N`, and a blank label becomes `Debt N`, by position in the list as supplied.
  3. Debts with a zero balance are removed.
  4. The domain records are built.
- "Blank" means `isBlank` or every character being whitespace or a space character (the same test
  `Money.parse` uses for trimming), so a label of only non-breaking spaces is blank. This is the shared
  predicate noted in 011, and it lives in `application`.

## 4. Service and response (007 REQ-005–008)

- `FinancialSnapshot.monthlySurplus()` is income − essential expenses − `Money.sum` of the debts' minimum
  payments. The mortgage is not included, and the result can be negative.
- `AssessmentService(StageLadder)` has `assess(SnapshotInput)`, which does `toSnapshot`, then
  `ladder.assess`, and returns `ReadinessAssessment(currentStage, stages, monthlySurplus, warnings = List.of())`.
  - The ladder is built once in `ApplicationConfiguration`, with constructor injection only.
  - `application` stays free of Spring stereotypes.
- The response records live in `application.response`, not in the API adapter. Design spec §7 has
  view pages (029) build their models from the same response objects, and ArchUnit AC-C forbids the
  view adapter from depending on the API adapter (changed 2026-09-13, during phase 1). Their component
  order is the wire order:
  - `MoneyResponse(amount = toBareString, display = toDisplayString)`;
  - `percentComplete` is `toPlainString()`, e.g. `"50.0"`;
  - null values are always written.
  - Identical input therefore produces byte-identical output.
- `Warning(code, message)` is a placeholder record; story 012 finalizes it.

## 5. Validation (011)

- `ValidationCode` enum: each code carries its message (REQ-009, REQ-013). `TOO_PRECISE` fills in `{n}`.
  - New codes not listed in the requirements, recorded here:
    - `AMOUNT_TOO_LARGE`: "Enter an amount of $1,000,000,000.00 or less." At most $1B per amount keeps
      6 × expenses and the total of 50 debts well inside a `long` (the overflow note carried from 005/006).
    - `INVALID_PERCENT`: "Enter a percentage like 7.25."
- Field constraints are custom annotations, one per field kind. Each reports only the first failing rule
  for its field, and the violation's message template is the code name:
  - `@MoneyAmount`: INVALID_AMOUNT → NEGATIVE → AMOUNT_TOO_LARGE. Blank is valid.
  - `@Percentage(scale)`: INVALID_PERCENT → PERCENT_RANGE → TOO_PRECISE. Blank is valid.
  - `@EmergencyFundMonths`: MONTHS_MIN. Null is valid.
  - `@DebtTypeName`: MORTGAGE_AS_DEBT or INVALID_CHOICE. Blank → INVALID_CHOICE, since a debt type is
    required.
- Whole-form rule: a class-level `@AtMostFiftyDebts` in group `WholeForm`, with pointer `/debts`.
  `@GroupSequence({SnapshotInput.class, WholeForm.class})` means it runs only when every field rule passes
  (REQ-007). It counts debts whose parsed balance is above zero.
- REQ-008 (one field validated alone) uses `Validator.validateProperty` with the same annotation, and is
  pinned by a test.
- `ProblemDetailsAdvice` handles every error response:
  - **Validation failures** (`MethodArgumentNotValidException`) → 400 `urn:rungs:problem:validation`.
    The title and detail follow REQ-006, and `errors[]` is sorted by pointer in numeric order, so
    `/debts/2` comes before `/debts/10`.
  - **Unknown fields** (`HttpMessageNotReadableException` caused by Jackson `UnrecognizedPropertyException`)
    → a validation problem with `UNKNOWN_FIELD` at that property's pointer.
  - **Wrong JSON types** (`MismatchedInputException` / `InvalidFormatException`) → `INVALID_TYPE` at the
    field's pointer.
  - **Any other unreadable body** → `urn:rungs:problem:malformed-request`, with no `errors` array.
  - Messages come only from `ValidationCode`, so submitted values never appear (REQ-010). No stack traces.
- `/snapshot` pointer prefixing (REQ-011) arrives with the projection endpoints (010, 013). The pointer
  builder takes the prefix as a parameter.

## 6. Tests

- **007:**
  - input-model normalization unit tests;
  - `monthlySurplus`;
  - service tests;
  - MockMvc contract tests asserting the exact JSON for AC-1.1–1.4, blank-as-zero for each numeric
    field, zero-balance removal, and positional ids and labels;
  - two identical requests give byte-identical bodies;
  - golden file `src/test/resources/golden/assessment-ac-1-2.json` (REQ-010).
- **011:**
  - one contract test per code (status, `type`, pointer, code, message);
  - multiple violations in pointer order;
  - a misspelt numeric property is rejected, not zeroed;
  - no submitted value appears in the body;
  - `validateProperty` parity;
  - a negative balance on a debt that would be removed is still rejected;
  - 51 debts with positive balances → `TOO_MANY_DEBTS`, but 60 debts of which 10 are zero passes.
- 007 REQ-011 (latency smoke test) is skipped: a wall-clock assertion breaks 019 REQ-006 (reproducible
  tests).

## 7. Phases

| Phase | Stories | Notes |
| --- | --- | --- |
| 1 | 007 | Endpoint, normalization, response, golden file; no constraints yet |
| 2 | 011 | Adds `spring-boot-starter-validation`, constraints, and the advice onto phase 1's models |

The phases run in sequence, since both touch the same input models. There is one correctness review and
one security review for the group.

## 8. Risks

| Risk | Mitigation |
| --- | --- |
| Jackson 3 class and feature names differ from Jackson 2 | Implementer checks `tools.jackson` APIs on the classpath |
| `@GroupSequence` on a record with `@Valid` nested lists | Contract test with a field violation plus 51 debts proves the whole-form rule is skipped |
| Byte-identical output depends on Boot's Jackson defaults | Explicit response records plus a golden-file test |
| ArchUnit AC-C/AC-D on the new adapter and config | The existing suite runs in `verify` |
