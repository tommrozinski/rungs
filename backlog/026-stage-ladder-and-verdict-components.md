---
status: proposed
---

# Stage ladder and verdict components

The status chip, stage row, progress bar, and verdict block — the components the
whole design hangs on.

## Description

Design spec section 3, components C-05, C-06, C-07, and C-11. The spec says of
the stage row: "This is the component the whole design hangs on." Its rules are
specific and each one is there for a reason:

- **Every row shows a reason.** In-progress rows state the shortfall in dollars;
  skipped rows state why the stage does not apply. No row is ever left with a
  bare status chip. This is what makes the API's `blocker` and `skipReason`
  contract (story 004) load-bearing rather than decorative.
- **Exactly one row is expanded at a time** — the current stage, with a gold
  border, the blocker sentence, a progress bar, and the single call to action.
- **The connector line runs behind the discs** and stops at the first and last
  row, so the ladder reads as a sequence rather than a list.
- Numbering is legitimate here; the content genuinely is an ordered sequence, and
  the markup is an ordered list with `aria-current="step"` on the current stage.

The verdict block (C-11) is where the spec says to spend its one allowance of
boldness: a single sentence in 30px display type, white on solid blue, naming the
dollar amount standing between the household and the next stage. "If a reviewer
remembers one frame, it should be that one."

## Acceptance Criteria

- C-05 Status chip in done, in-progress, not-started, and skipped variants, each
  with a text label — status is never carried by color alone.
- C-06 Stage row in done, in-progress, not-started, skipped, and expanded
  current-stage variants.
- Every stage row renders a reason: the blocker for in-progress, the skip reason
  for skipped.
- Exactly one row is expanded at any time, and it is the current stage.
- The expanded row carries a gold border, the blocker sentence, a progress bar,
  and one call to action.
- The connector line runs behind the discs and terminates at the first and last
  row.
- Every stage disc shows a number, a check, or a dash — never color alone.
- C-07 Progress bar is gold in progress and green at 100%.
- C-11 Verdict block renders the current stage's blocker as display type, white
  on `blue-600`.
- The ladder is an ordered list in markup with `aria-current="step"` on the
  current stage.
- All seven stages always render; skipped stages are visible and explained.
- Blocker and skip-reason text comes from the API verbatim — no client rewording,
  no client-side number formatting.

## Size

M

## Open Questions

- The verdict block names "the dollar amount standing between the household and
  the next stage" — is that the API's `remainingAmount` for the current stage, or
  a different figure? For stage 2 there may be no meaningful single number (story
  005's percent-complete question).
- What does the verdict block show when the current stage is 7 and there is no
  blocker at all?
- Display type is specced at 34px in section 2.2 but described as 30px in section
  1. Which is right?
- What is the single call to action on the expanded row, per stage? The spec
  requires one but does not enumerate them.
