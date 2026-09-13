---
status: approved
---

# S-03 Projection screen

The extra-payment slider, the strategy toggle, three outcome stats, and the
month-by-month schedule.

> **Approved 2026-09-13.** Full requirements, which supersede the criteria and open
> questions below: [spec/030-projection-screen/requirements.md](../spec/030-projection-screen/requirements.md).
> Decided: slider $0–$2,000 in $25 steps; outcome stats are debt-free in, total
> interest, first debt paid off; first 24 months shown with "show all"; live region
> for results. No submit-button fallback (JavaScript required).

## Description

Design spec section 4, S-03. The control card sits at the top, and changing
either control "re-renders the results from a fresh server-side projection" —
the client never interpolates, never recalculates, never guesses at an
intermediate value. That is
the constraint that makes the slider honest and also the one that makes latency a
product concern (story 010, NFR-3).

The schedule's retired-debt rows are tinted green, and the spec is direct about
why: "That tint is the emotional payload of the screen and the reason people will
scroll it."

Truncated schedules say so explicitly at the foot of the table, rather than
trailing off and letting the reader assume the debt was cleared.

Design spec section 7 fixes how the re-render happens: the page is
server-rendered Thymeleaf, and live updates use htmx, which is part of this
increment: a request debounced by 250ms that swaps in a server-rendered results
fragment. No SPA and no frontend build. With JavaScript disabled, a submit
button re-renders the whole page from the same fragment.

## Acceptance Criteria

- A control card at the top holds the extra-payment slider and the strategy
  toggle (C-03).
- Changing either control re-renders the screen from a fresh server-side
  projection; no client-side interpolation or recalculation.
- With JavaScript on, slider and toggle changes trigger an htmx request debounced
  by 250ms that swaps in a server-rendered results fragment — never JSON.
- The view controller calls the projection application service in-process, not
  `/v1/projections` over HTTP.
- With JavaScript off, a submit button re-renders the page from the same
  fragment.
- If a newer control change fires while a request is in flight, the older
  request is cancelled and its response discarded (`hx-sync="this:replace"`); the
  results always reflect the latest control values. Covered by a test that
  delays a response and asserts it never replaces a newer one.
- Three outcome stat cards, then the month-by-month schedule table (C-10).
- Rows where a debt is retired are tinted `green-100`.
- A truncated schedule states so explicitly at the foot of the table.
- The slider responds to arrow keys with a $25 step.
- With JavaScript disabled the slider degrades to a number input and the screen
  still renders on submit.
- All currency and duration figures use tabular numerals and server-formatted
  strings.
- No payoff math of any kind in client code.

## Size

M

## Open Questions

- ~~Debounce and call the server, or fetch a range and interpolate?~~
  **Resolved.** Design spec section 7: debounce 250ms and re-render server-side
  via htmx.
- What is the slider's range and maximum? The $25 keyboard step is given; the
  bounds are not.
- A 360-month schedule is a long table to re-render on every slider change. Is it
  paginated or capped in display? (Client-side virtualization is effectively
  ruled out by the no-SPA decision.)
- ~~Is live re-render in scope for 3.5b?~~ **Resolved.** Yes — design spec
  section 7 makes htmx part of the increment.
- ~~Is a stale response discarded?~~ **Resolved.** Yes — design spec section 7.
  No reason found to keep it: showing numbers for a slider position the user has
  already left would be wrong. The only cost is that the server may finish a
  projection nobody sees, which is cheap within the 250ms budget.
- Which three outcome stats — months to debt freedom, total interest, months to
  next stage?
