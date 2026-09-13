---
status: approved
---

# S-05 Responsive mobile layout

The 375pt layout: stacked content, compressed stage rows, and a sticky primary
action.

> **Approved 2026-09-13.** Full requirements, which supersede the criteria and open
> questions below: [spec/032-responsive-mobile-layout/requirements.md](../spec/032-responsive-mobile-layout/requirements.md).
> Decided: breakpoints at 480px and 960px; non-current reasons behind a "Why?"
> disclosure rather than removed; tables scroll in their own container; file bar
> actions stack.

## Description

Design spec section 4, S-05. The rules are specific enough to build against:

- Stack everything. Stats go from three across to two.
- Stage rows compress: 30px discs, 13px titles, and blocker text is dropped
  except on the current stage.
- The primary action becomes full width and sticks to the bottom of the ladder.

Dropping blocker text on non-current rows is the one judgement call worth
noticing, because it sits in tension with the stage-row rule that "every row
shows a reason" (story 026). At 375pt the compromise is deliberate — the current
stage keeps its reason, the rest keep their status chips — but it means the
mobile ladder is not merely a narrower desktop ladder and needs its own review.

The schedule table (story 030) is the other hard case: a month-by-month table
with per-debt columns does not stack gracefully.

## Acceptance Criteria

- At 375pt everything stacks to a single column.
- Stat card rows go from three across to two.
- Stage discs compress to 30px and titles to 13px.
- Blocker text is hidden on all rows except the current stage; skip reasons
  follow the same rule.
- The primary action is full width and sticks to the bottom of the ladder.
- The verdict block remains legible at 375pt without truncating its figure.
- The schedule table remains usable — horizontal scroll within its own container,
  never a horizontally scrolling page.
- No layout breaks between 320pt and 375pt.
- Touch targets meet the minimum size story 034 requires.
- Tested at 375pt and at least one larger breakpoint.

## Size

M

## Open Questions

- Are there defined breakpoints, or is 375pt the only specified target? The spec
  names one width.
- How does the schedule table behave on mobile — horizontal scroll, a card per
  month, or fewer columns?
- Does the file bar (story 028) compress, and do both actions stay visible?
- Hiding blocker text on non-current rows removes information from the screen.
  Should it be behind a disclosure rather than absent, so the reason is still
  reachable?
