---
status: proposed
---

# Stage evaluators 4–7: retirement, college, mortgage, wealth building

The four lifecycle stages, including the skip rules that let a renter or a
household with no college intent advance past stages that do not apply to them.

## Description

PRD section 5.2 defines the upper rungs:

| Stage | Satisfied when |
| --- | --- |
| 4 | `retirementContributionPercent >= 15` |
| 5 | `intendsToFundCollege == false` (skipped) or a college contribution is present |
| 6 | `rents == true` (skipped) or `mortgage == null` or `mortgage.balance == 0` |
| 7 | stages 1–6 all satisfied or skipped |

This is where `SKIPPED` earns its place in the status enum. AC-1.2 is the
headline case: a renter satisfying stages 1–4 with no college intent reports
current stage 7, with 5 and 6 both skipped and explained.

Skip reasons are user-facing copy, not diagnostics — the design spec renders them
on the ladder and gives the pattern: "You rent, so there's no mortgage to pay
down." Explanations, not shrugs.

Section 9 calls for a boundary case at exactly 15%.

## Acceptance Criteria

- Stage 4 is satisfied at exactly `15`, in progress below it, with
  `percentComplete` as the contribution over 15.
- Stage 5 is skipped with a populated `skipReason` when `intendsToFundCollege`
  is false.
- Stage 6 is skipped with a populated `skipReason` when `rents` is true, and
  satisfied when `mortgage` is null or its balance is zero.
- Stage 7 is satisfied only when stages 1–6 are each satisfied or skipped.
- AC-1.2 holds end to end: stages 1–4 satisfied, renting, no college intent →
  current stage 7, stages 5 and 6 `SKIPPED` with non-null `skipReason`.
- Skip reasons are second-person sentence-case explanations, not enum names, and
  render directly on the ladder without client rewording.
- One test class per evaluator, with the 15% boundary case.

## Size

M

## Open Questions

- PRD open question, unresolved: does stage 5 accept a dollar target for college
  funding, or stay a boolean intent flag in v1? As written the rule says "a
  college contribution is present", but no such field exists on
  `FinancialSnapshot` and none exists in the CSV format — so stage 5 can
  currently only be skipped or blocked, never satisfied. This is now blocking two
  layers: the evaluator and the ladder UI.
- `HouseholdFlags.retired` is defined, is carried in the CSV format as
  `FLAG,retired`, but no stage rule uses it. Should a retired household skip
  stage 4 or stage 5? The PRD's problem statement mentions "a retiree has no
  college fund to build" but section 5.2 encodes no such rule.
- What is stage 7's `percentComplete` — binary, or graded?
- Does `dependentsInCollegePlan == 0` imply a skip for stage 5 independently of
  the `intendsToFundCollege` flag?
