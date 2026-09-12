---
status: proposed
---

# Core component library

Buttons, fields, toggles, checkboxes, stat cards, warning banners, and the
schedule table — the general-purpose components every screen composes from.

## Description

Design spec section 3, components C-01 through C-04 and C-08 through C-10. These
are the ordinary parts: the ladder and verdict components are story 026 because
they carry the design's whole argument and need their own attention.

Each component ships with all its states, because a state that is not built is a
state that gets improvised later at the call site — the spec enumerates
default, hover, focus, and disabled for buttons, and default, focused, error, and
read-only for fields.

Two details that are easy to skip and expensive to retrofit: the focus ring is
3px `blue-100` outside a `blue-600` border and appears on *every* interactive
element, and the schedule table's retired-debt row tint is the emotional payload
of the projection screen, not decoration.

## Acceptance Criteria

- C-01 Button in primary, secondary, and quiet variants, each with default,
  hover, focus, and disabled states. Primary is pill-shaped.
- C-02 Text field in default, focused, error, and read-only states, with the
  error state bound to a message slot.
- C-03 Toggle group with two options, one selected.
- C-04 Checkbox in on and off states.
- C-08 Stat card in neutral and positive variants.
- C-09 Warning banner in caution (gold) and problem (red) variants.
- C-10 Schedule table with a standard row and a debt-retired row tinted
  `green-100`.
- Every interactive element shows a visible 3px `blue-100` focus ring outside a
  `blue-600` border.
- Every component consumes tokens from story 024; no hardcoded values.
- Currency and duration cells use tabular numerals.
- Gold is never used as text on white (it fails contrast at body sizes); it
  appears as surface and border only.
- Each component is a reusable Thymeleaf fragment (`th:fragment`) styled by the
  token-based stylesheet from story 024 — no JavaScript component framework.
- Components render correctly with JavaScript disabled.
- A component gallery page, rendered by a Thymeleaf view in the same
  application, shows every component in every state for review.

## Size

M

## Open Questions

- ~~Thymeleaf fragments or an SPA component library?~~ **Resolved.** Design spec
  section 7 fixes server-rendered Thymeleaf with no SPA, so components are
  Thymeleaf fragments. Remaining detail: parameterised fragments per component,
  or a smaller set of fragments plus CSS modifier classes?
- Is the component gallery a permanent dev-only route, a test artifact, or
  something shipped? It is useful for review but is a second surface to maintain.
- C-09's "problem (red)" variant — which conditions use it? Story 012 raises the
  same question from the API side: the spec reserves red for input validation.
