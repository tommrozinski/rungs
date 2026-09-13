---
status: approved
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

- ~~Stage 5 has no contribution field.~~ **Resolved** (spec 003).
  `monthlyCollegeContribution`; satisfied above $0.
- ~~What does `retired` do?~~ **Resolved.** A retired household skips stages 4
  and 5 (PRD 5.2).
- ~~Stage 7 `percentComplete`?~~ **Resolved.** Graded: the share of stages 1–6
  satisfied or skipped.
- ~~`dependentsInCollegePlan`?~~ **Resolved.** Removed (spec 003).
- **Also decided:** `MORTGAGE`-type debts are rejected by validation, so stage 6
  reads only the `mortgage` object.

Full requirements: [spec/006-stage-evaluators-4-to-7/requirements.md](../spec/006-stage-evaluators-4-to-7/requirements.md).
