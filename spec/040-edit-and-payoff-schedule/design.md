# Design: 040 — Change my numbers, payoff schedule, simpler mortgage fields

<!-- Source: user request 2026-09-14; prd/Flip_Snowball_Tracker_v6.xlsx sheet "Debt Avalanche" (format only);
     prd/v0-scope-addendum.md (build order) -->
<!-- Ticket: none (backlog story 040) -->
<!-- Date: 2026-09-14 -->

## 1. Changes

1. **Change my numbers.** The result page gets a button that returns to the form with every value kept.
2. **Show payoff schedule.** A second button shows the month-by-month paydown of every debt and the mortgage
   in an iframe below the seven stages. The layout follows the workbook's "Debt Avalanche" sheet.
3. **Simpler mortgage fields.** Remove "Annual property taxes". The monthly payment is the whole payment,
   taxes included.

   Note text: "Paid off? Enter 0 for the remaining balance, and enter your property taxes and insurance as
   the monthly payment, since those still come out of your take-home pay each month."

The workbook is personal. It is used only as a format reference and is git-ignored. No figures from it
enter the repo.

## 2. Build order (addendum)

Static mocks come first and are checked in a browser. Templates carry them forward.

- `prd/snapshot-form.html`: remove the taxes field and update the note and the payment hint (the hint
  becomes "Your full monthly payment, including taxes and insurance.").
- `prd/assessment-result.html`: an actions row under the ladder with `Change my numbers` (secondary) and
  `Show payoff schedule` (primary), and a `<section id="breakdown">` below it holding
  `<iframe title="Payoff schedule" class="breakdown-frame" src="payoff-schedule.html">`.
- `prd/payoff-schedule.html`: new standalone page with made-up figures (the board household plus a
  mortgage), laid out like the sheet.
  - Title in bold 14px: "🏦 Payoff schedule — highest rate first, with cascade".
  - Italic subtitle: "Your surplus goes to the highest-rate debt; each paid-off debt's minimum rolls to the
    next. The mortgage comes last."
  - Section "YOUR LOANS": a table of Loan, Balance, Rate, and Min Payment with a bold TOTAL row.
  - Section "PAYOFF SCHEDULE": Month, Extra cash, Cascade, Total avail, then for each loan in payoff order
    `{Name} open`, `{Name} int`, `{Name} pmt`, `{Name} end`, then Mo int, Total bal, and Cum int.
  - A bold totals row at the foot: total extra cash, total paid per loan, and total interest.
  - Styling: header cells bold 11–12px on `#F2F2F2`. Money as `$#,##0.00`, with a zero shown as `-`.
    Numbers right-aligned, tabular. The table sits in a horizontal scroll container, so the page body
    never scrolls sideways.

## 3. Domain

- **Mortgage.** `Mortgage` drops `annualPropertyTaxes`. P&I is the full payment. A paid-off home's housing
  cost is its payment. The stage 6 not-covered sentence becomes "Your $P payment doesn't cover the $I
  monthly interest." The taxes-rounding code and its tests go.
- **`PayoffScheduleCalculator.schedule(FinancialSnapshot)`** (new, in `domain.projection`) returns
  `PayoffSchedule`.
  - **Loans.**
    - Every debt, ordered by annual rate descending, with ties in supplied order.
    - Then the mortgage when its balance is above $0. Its minimum is the monthly payment and its name is
      "Mortgage".
    - A renter has no mortgage row.
  - **Each month m, starting from `asOf`.**
    1. `extraCash` = max(monthlySurplus, $0), which is the same every month.
    2. `cascade` = the sum of the minimums of loans paid off in earlier months.
    3. `totalAvail` = extraCash + cascade.
    4. For each loan still open, in order:
       - `int` = open × rate ÷ 1200, half-even to the cent.
       - `pmt` = min(open + int, minimum + pool), where pool starts at totalAvail and each loan reduces it
         by (pmt − minimum) when that difference is positive.
       - `end` = open + int − pmt.
       - Loans closed earlier show open, int, pmt, and end as $0.
    5. `moInt` = the sum of int. `totalBal` = the sum of end. `cumInt` is the running total of moInt.
  - Freed minimums are counted once (the sheet's `$E$6 − H` term double-counts them).
  - The schedule stops after the month the last loan closes, or after 600 months (flagged `truncated`).
  - If a loan never shrinks (its payment never covers its interest), the cap applies.
  - **Totals.** Total extra cash, total paid per loan, and total interest.
  - **Invariants.**
    - Per loan: paid = starting balance − ending balance + interest.
    - No negative balance.
    - Each month's payments total no more than the minimums of open loans plus totalAvail.
- **No loans.** An empty schedule, which the page renders as "No debts or mortgage to pay down."

## 4. Web

- **Hidden form.** The result page carries a hidden copy of every submitted form field (debt rows,
  housing, checkboxes), rendered from the bound form.
  - `Change my numbers` posts it with `formaction="/edit"`. The server re-renders `form.html` filled in.
  - `Show payoff schedule` posts it with `formaction="/assess#breakdown"`, `name="breakdown"`, and
    `value="true"`.
- **Breakdown.** When `breakdown=true`, the result page renders the iframe with `th:attr="srcdoc=…"`
  holding the server-rendered `payoff-schedule` HTML. The HTML comes from the same Thymeleaf template via
  `TemplateEngine.process`, and is escaped as an attribute. There's no extra request, no figures in any
  URL, and no JavaScript. Before the button is pressed, the iframe section is absent.
- `POST /breakdown` also serves the standalone schedule page, which is useful for checks and tests.
- **Iframe size.** Full content width, 600px tall, bordered like `.card`. The iframe scrolls internally.
- **Samples.** Drop taxes. Payments become whole payments: debt-free and homestretch keep computing their
  maturity from asOf. Paid-off becomes balance 0 with a $650 payment (taxes plus insurance).

## 5. Tests

- **Calculator (TDD).**
  - A hand-verified two-debt case over the first three months: interest, payment, cascade after a payoff,
    and the freed minimum counted once.
  - Ordering, including ties.
  - The mortgage last, even when its rate is higher than a debt's.
  - A renter.
  - No loans.
  - The 600 cap, including a loan that never shrinks.
  - A negative surplus becomes $0 extra.
  - The invariants asserted on every schedule.
  - Totals.
- **Web.**
  - The result page has both buttons and the hidden fields.
  - `/edit` restores every value, including the housing mode and debt rows.
  - `breakdown=true` renders an iframe srcdoc containing the schedule headers and the loan names.
  - The `/breakdown` page for the board household plus a mortgage has the expected first-row figures.
  - The taxes field and `annualPropertyTaxes` are gone.
- **Screenshots.** The three mocks, then the app result page with the schedule open.

## 6. Phases

| Phase | Work | Runs |
| --- | --- | --- |
| 1 | Static mocks (§2), screenshot check | parallel with 2 |
| 2 | Domain (§3) with tests | parallel with 1 |
| 3 | Web (§4): carry the mocks forward, hidden fields, /edit, breakdown, samples, tests, screenshots | after 1 and 2 |

One Sonnet review follows, then commit and push.
