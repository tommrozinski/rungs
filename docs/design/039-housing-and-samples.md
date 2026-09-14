# Design: 039 — Rent or mortgage, and sample households

<!-- Source: user request 2026-09-14; docs/scope.md (build order) -->
<!-- Ticket: none (backlog story 039) -->
<!-- Date: 2026-09-14 -->

The "We rent our home" checkbox becomes a two-option toggle: **Mortgage** (the default) or **Rent**. Each
option collects its own housing costs. Housing then feeds the stage logic. A "Load sample data" control
fills the form with one of five households.

The addendum's build order still applies. The static mock `docs/mockups/snapshot-form.html` changes first and is
checked in a browser. The template is updated only after that, by carrying the new markup forward.

## 1. Form

- **"About your household" card.**
  - Housing toggle: two radios named `housing`, values `MORTGAGE` (checked by default) and `RENT`,
    styled as a segmented pill switch.
  - The mortgage fields and the rent field sit in sibling blocks. CSS shows the block that matches the
    checked radio, using `:has()`. No JavaScript.
  - The college and retired checkboxes stay.
- **Mortgage fields:**
  - Remaining balance: `mortgageBalance`.
  - Interest rate %: `mortgageRate`.
  - Monthly payment: `mortgagePayment`, with the hint "Include property taxes (your annual taxes ÷ 12)."
  - Annual property taxes: `annualPropertyTaxes`.
  - Maturity: a month `<select>` (`maturityMonth`, 1–12) and a year number field (`maturityYear`).
  - Note under the block: "Paid off? Enter 0 for the remaining balance. Your payment is then just your
    property taxes."
- **Rent field:** Monthly rent (`monthlyRent`).
- **Essential expenses hint:** "Everything except housing and debt payments." This keeps rent or a
  mortgage from being counted twice.
- **Sample data:** next to "Check my stage", a `<select name="sample">` with the five households (§4) and
  a `Load sample data` button (`formaction="/sample"`, `formnovalidate`). The server re-renders the form
  filled with that household.

## 2. Domain

- `Housing` is a sealed interface with two records:
  - `Rent(Money monthlyRent)`
  - `Mortgage(Money balance, BigDecimal annualInterestRate, Money monthlyPayment, Money annualPropertyTaxes, YearMonth maturity)`

  The old `Mortgage` (with home value) and `HouseholdFlags.rents` are replaced. A null maturity is
  allowed and means unknown.
- `FinancialSnapshot`:
  - Gains `Housing housing` (never null) and `YearMonth asOf`, the assessment month.
  - `asOf` is set by the controller from an injected `Clock`, so the domain never reads the time.
  - `monthlyHousingCost()`:
    - Rent: the rent.
    - Mortgage: the payment, or if the payment is $0, `annualPropertyTaxes ÷ 12` rounded half-even to the cent.
  - `monthlySurplus()` = take-home − essentials − housing cost − debt minimums.
  - `monthlyEssentialCosts()` = essentials + housing cost.
- **Stage 3.** The target becomes 3 × `monthlyEssentialCosts()`. Copy is unchanged ("$F of $T.").
- **Stage 6 (`MortgageEvaluator`):**
  - Rent → Skipped: "You rent, so there's no mortgage to pay down."
  - Balance $0 → Done: "Your house is paid off."
  - Balance above $0:
    - `principalAndInterest` = payment − round(annualPropertyTaxes ÷ 12).
    - `firstMonthInterest` = balance × rate ÷ 1200, rounded half-even to the cent.
    - If P&I ≤ first-month interest → Not started. Blocker: "After property taxes, your $P payment
      doesn't cover the $I monthly interest."
    - Otherwise, amortize month by month in cents (interest first, then payment, capped at 600 months).
      That gives `monthsToPayoff` and `payoffMonth = asOf + monthsToPayoff`. Status is Not started, with
      `remaining = balance`.
  - **Payoff line.** "Paid off in {Month YYYY} at this payment, {Y years M months} from now." One of
    these follows:
    - Maturity known and payoff within one month of it: "That matches your loan's {Month YYYY} maturity."
    - Payoff at least 2 months before maturity: "That's {N months or Y years M months} ahead of your
      {Month YYYY} maturity."
    - Payoff at least 2 months after maturity: "That's later than your {Month YYYY} maturity, so check
      the balance, rate, or payment."
    - If the loan runs past 600 months: "This payment won't pay the loan off within 50 years."
  - `StageEvaluation` has no room for a structured date, so the evaluator exposes the payoff facts through
    a small `MortgagePayoff` result that `MortgageEvaluator` can compute on its own. The view uses it for
    copy. The implementer picks the cleanest route: a domain calculator class used by both the evaluator
    and the view model.
- **Result page.**
  - Stage 6 line: Done, Skipped, or the payoff copy above.
  - Stage 6 verdict: line "Pay off the house.", sub "At this payment it's paid off in {Month YYYY}. Every
    extra dollar gets you there sooner."
  - The "Non-mortgage debt" stat is unchanged.

## 3. Mapping (lenient, as in 038)

- A blank or bad `housing` → MORTGAGE.
- Blank or bad money → $0. A bad rate → 0.
- Maturity month outside 1–12, or year outside 1900–2200 → null (unknown).
- In Rent mode the mortgage fields are ignored. In Mortgage mode the rent field is ignored.
- Nothing may return a 500.

## 4. Sample households

| id | Label | Summary | Lands on |
| --- | --- | --- | --- |
| `buried` | Buried in debt | Renter. Take-home 3,800, essentials 2,100, rent 1,250, starter fund $150, four debts (credit card 6,400 @ 24.9 min 190; car 14,200 @ 9.5 min 385; medical 2,300 @ 0 min 75; personal 4,800 @ 17 min 160). Surplus is negative. | 1 |
| `board` | Paying down debt | The board household as a renter. 6,200 / 2,100 + rent 1,400, starter 1,000, retirement 6%, the three board debts. Surplus $2,243 and stage 3 target $10,500 still match the board. | 2 |
| `debt-free` | Debt-free, building savings | Mortgage 285,000 @ 6.5%, annual taxes 4,800, payment ≈ P&I + 400, maturity about 27 years out. Take-home 7,800, essentials 2,600, starter 1,000, full fund 6,000, retirement 4%. | 3 |
| `homestretch` | Paying off the house | Mortgage 198,000 @ 3.25%, taxes 6,000/yr, maturity about 16 years out, with a payment a little above schedule so the "ahead of maturity" line shows. Take-home 11,500, essentials 3,400, full fund above target, retirement 15%, no college. | 6 |
| `paid-off` | Paid-off home, no debt | Mortgage balance 0, payment 0, taxes 6,300/yr ($525/mo). Take-home 9,200, essentials 3,100, full fund 18,000, retirement 15%, no college. | 7 |

The implementer computes each mortgage payment with the domain calculator so the maturity lines read as
intended, and tests pin each household's stage.

## 5. Tests

- **Domain (TDD):**
  - `monthlyHousingCost` for rent, a mortgage, and a paid-off home (taxes ÷ 12 rounding).
  - Surplus and stage 3 target include housing.
  - The payoff calculator: exact month count on a known loan, cross-checked with an amortization
    formula computed in the test (double is allowed in tests only); the payment-doesn't-cover-interest
    case; the 600-month cap; a paid-off home.
  - Each maturity comparison branch.
  - `MortgageEvaluator` for all statuses.
- **Web:**
  - Toggle markup: MORTGAGE checked by default, and state kept on re-render.
  - Mapping: the lenient cases, and the fields of the unselected mode ignored.
  - Stage 6 copy.
  - `/sample` for each id, plus an unknown id → the empty form.
  - Each sample household's stage through `POST /assess`.
- **Visual check:** headless Chrome screenshots of the updated mock and of the app in both modes, a
  sample loaded, and the `homestretch` result.

## 6. Phases

| Phase | Work |
| --- | --- |
| 1 | Static mock: `docs/mockups/snapshot-form.html` gets the toggle, the fields, the hints, and the sample control (CSS only, no JS); screenshot check |
| 2 | Domain: `Housing`, `asOf`, surplus and stage 3 changes, payoff calculator, `MortgageEvaluator`, tests |
| 3 | Web: carry the mock markup into `form.html`, mapping, `Clock`, samples, stage 6 copy and verdict, tests; screenshot check |

One Sonnet review after phase 3, then commit and push. After that, stretch item 1 (projection, smallest
balance first) is next.
