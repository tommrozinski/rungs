---
status: approved
---

# Stage evaluators 1–3: starter fund, non-mortgage debt, full emergency fund

The three cash-and-debt stages, each with its own percent-complete arithmetic and
specific blocker message.

## Description

PRD section 5.2 defines the first three rungs:

| Stage | Satisfied when |
| --- | --- |
| 1 | `starterEmergencyFund >= $1,000` |
| 2 | no `Debt` of type other than `MORTGAGE` with `balance > 0` |
| 3 | `fullEmergencyFund >= 3 × monthlyEssentialExpenses` (configurable multiple, default 3, max 6) |

Blocker strings need real numbers in them, not generic text — the PRD's example
is "Emergency fund is $2,400 short of a 3-month target of $10,500." The design
spec raises the stakes: the blocker for the current stage is rendered as the
verdict block, 30px white on solid blue, and is the one frame a reviewer is meant
to remember. Its copy rules apply — second person, sentence case, name the
number, no encouragement.

Section 9 calls for boundary cases at exactly $1,000 and exactly 3 months of
expenses; the equality cases are satisfied, not in-progress.

## Acceptance Criteria

- Stage 1 is satisfied at exactly $1,000 and in progress below it;
  `percentComplete` is the fund over $1,000, and `remainingAmount` the shortfall.
- Stage 2 is satisfied when no non-`MORTGAGE` debt has a positive balance; a
  `MORTGAGE`-type debt in the list never blocks stage 2.
- Stage 2's blocker names the outstanding balance and the debt count.
- Stage 3 is satisfied at exactly `multiple × monthlyEssentialExpenses`, using
  the default multiple of 3.
- Stage 3 with `monthlyEssentialExpenses` of zero yields `percentComplete` of
  `0.0` rather than a division error (AC-1.4).
- A requested emergency-fund multiple above 6 is clamped to 6 (the
  `EMERGENCY_FUND_TARGET_CAPPED` warning is raised by story 012).
- Blockers state the shortfall and the target in dollars, in second person and
  sentence case, and are usable verbatim as the verdict line.
- AC-1.1 holds: $500 saved, $12k credit card debt, no mortgage reports current
  stage 1.
- One test class per evaluator, with the boundary cases named above.

## Size

M

## Open Questions

- ~~Where does the emergency-fund multiple come from?~~ **Resolved** (spec 003).
  `FinancialSnapshot.emergencyFundMonths`, 3–6, default 3, capped at 6.
- ~~Stage 2 percent-complete baseline?~~ **Resolved** (PRD 5.3). No baseline:
  stage 2 is `IN_PROGRESS` at 0.0% while non-mortgage debt remains.
- ~~Does a zero-balance debt need reporting anywhere?~~ **Resolved.** PRD FR-4:
  a zero-balance debt is paid off and is removed by the `application` input
  models before evaluation, so stage 2 never sees one. CSV import reports it as
  skipped (AC-7.7).
