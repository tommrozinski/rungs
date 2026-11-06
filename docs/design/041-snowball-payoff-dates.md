# Design: 041 — Debt snowball, payoff dates, consistent result page

<!-- Source: user request 2026-09-14 -->
<!-- Ticket: none (backlog story 041) -->
<!-- Date: 2026-09-14 -->

## 1. Changes (user decisions)

1. **Remove the maturity month and year fields.** They are confusing. Removed from the form, `SnapshotForm`,
   the mapper, `Mortgage.maturity`, `MaturityComparison`, and the sample maturity calculations.
2. **True debt snowball.** The payoff schedule pays the **smallest starting balance first**. Ties keep the
   order the debts were entered. The mortgage always comes last.
   - Schedule title: "🏦 Payoff schedule — debt snowball (smallest balance first)".
   - Subtitle: "Your surplus goes to the smallest debt; each paid-off debt's payment rolls into the next. The
     mortgage comes last."
3. **Two mortgage payoff dates.** Stage 6 compares the payoff date at the current payment alone (no extra
   payments) with the early payoff under the debt snowball. The early date is the month the schedule's
   mortgage row closes.
4. **Result page agrees with the schedule.**
   - The stage 2 verdict comes from the schedule's debt-free month (the last non-mortgage loan closing),
     not from debt ÷ surplus.
   - The stage 6 line and verdict use the two dates above.
   - The all-done eyebrow becomes "Every stage that applies to you is done". It no longer contradicts
     "Stages complete K of 7", which counts Done stages only.

## 2. Copy

`{date}` is "Month YYYY". `{duration}` is "Y years M months", with singular forms and zero parts omitted.

- **Stage 2 verdict sub:**
  - Schedule finishes the debts: "With the debt snowball you're free of everything but the house by {date},
    about {N} months from now." Use "1 month" when N is 1.
  - Debts not finished within 600 months: "At these payments your debts won't be paid off within 50 years."
- **Stage 6 line when the balance is above $0:**
  - Current payment covers interest: "At your current payment it's paid off in {date}."
  - Current payment doesn't cover interest: "Your $P payment doesn't cover the $I monthly interest."
  - The snowball closes the mortgage within 600 months, earlier than the current-payment date or where
    that date doesn't exist: append " With the debt snowball it's paid off in {date}, {duration} sooner."
    Drop ", {duration} sooner" when there's no current-payment date.
  - Current payment not finished within 600 months: "At your current payment it won't be paid off within 50
    years."
- **Stage 6 verdict (current stage):**
  - Line: "Pay off the house."
  - Sub: "With the debt snowball it's paid off in {date}, {duration} sooner than your payment alone." If
    there's no comparison, use just "With the debt snowball it's paid off in {date}." If the snowball
    doesn't finish, use the stage 6 line.
- **Stage 6 Done and Skipped:** unchanged ("Your house is paid off." and the rent sentence).

## 3. Domain

- `PayoffScheduleCalculator` orders debts by starting balance ascending, ties in supplied order, then the
  mortgage.
- `PayoffSchedule` gains:
  - `Optional<YearMonth> debtFreeMonth()`: when the last non-mortgage loan closes. It is `asOf` minus
    nothing (empty/zero-case) when there are no debts.
  - `Optional<YearMonth> mortgagePayoffMonth()`: when the mortgage row closes, empty if there's no mortgage
    row or it never closes.
  - The implementer chooses the cleanest representation (e.g. a closing month per loan).
- `MortgagePayoffCalculator` stays as the "current payment alone" calculator, without maturity. Its result
  carries `principalAndInterest`, `firstMonthInterest`, `covered`, `monthsToPayoff` (null if not covered or
  capped), and `payoffMonth`.
- `MortgageEvaluator` stays pure. Stage 6 status is unchanged: Skipped for renters, Done for $0,
  otherwise Not started. The blocker holds the current-payment sentence. The snowball sentence needs the
  schedule, so `ResultViews` appends it. Everything else stays in the domain.

## 4. Build order

1. **Mocks** (phase 1):
   - `docs/mockups/snapshot-form.html`: remove the maturity fields and rebalance the mortgage block.
   - `docs/mockups/payoff-schedule.html`: re-run the rules with snowball order and update the
     figures, title, and subtitle.
   - `docs/mockups/assessment-result.html`: the board verdict sub becomes the schedule-based sentence (its months
     computed with snowball order for the board renter). Add a stage 6 example line in the build note.
   - Take screenshots.
2. **Domain** (phase 2, in parallel with 1): snowball ordering, the schedule helpers, maturity removal,
   and tests.
3. **Web** (phase 3): carry the mocks forward, remove the fields, ResultViews copy per §2 using the
   schedule, the samples, tests, and screenshots.

One Sonnet review, then commit and push.

## 5. Tests

- **Calculator:** smallest-balance-first order, including ties and the mortgage last. Rework the
  hand-verified cascade case for snowball order. `debtFreeMonth` and `mortgagePayoffMonth`, including
  empty cases and the 600-month cap.
- **Web:**
  - The form has no maturity fields.
  - The stage 2 verdict for the board household matches the schedule's debt-free month.
  - A homeowner with debts shows both stage 6 dates and "sooner".
  - A not-covered payment shows the snowball-only date.
  - A renter has no stage 6 dates.
  - The all-done eyebrow.
  - The schedule page title and snowball order.
