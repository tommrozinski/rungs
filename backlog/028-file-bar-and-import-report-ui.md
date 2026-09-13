---
status: approved
---

# File bar and import report UI

The load/save bar and the three-state import report — the UI over CSV
import/export, and the thing standing between a user and losing their work.

> **Approved 2026-09-13.** Full requirements, which supersede the criteria and open
> questions below: [spec/028-file-bar-and-import-report-ui/requirements.md](../spec/028-file-bar-and-import-report-ui/requirements.md).
> Decided: multipart load re-renders the form; save requires a valid snapshot; a small
> script tracks edits and confirms before loading; a partial import counts as loaded.

## Description

Design spec section 4, components C-12 and C-13.

The file bar sits under the screen title with two actions and a state line. The
state model is the point: "The form is the working copy; the file is the saved
copy. That distinction has to be visible at a glance or people will lose work."

- Empty: "No file loaded. Fill in the form below, or load a saved one."
- Loaded: the filename and how many debts came in.
- Edited: the filename with "edited" appended, and "Save a file" becomes the
  primary button.

Loading replaces the entire form, so unsaved edits require a confirm that says
what will be lost. Saving is a browser download, since a web page cannot silently
overwrite a file on disk. Save-in-place via the File System Access API is out of
scope (design spec sections 4 and 8): Chromium-only, and the download covers
everyone.

Edited-state tracking and the unsaved-changes confirm are the two behaviours
here that need JavaScript. Per the design spec section 7 policy they live in a
small external script under `static/js`, handle page state only, and are
enhancements over a form that still works without them.

The import report (C-13) appears between the file bar and the first card after a
load, and only then. Its partial state is the one that earns its keep: accepted
rows are already in the form, and the user fixes the rest by typing rather than
editing the file and starting over.

## Acceptance Criteria

- C-12 File bar with "Load a file" and "Save a file" actions and a state line.
- Three states render per the spec: empty, loaded (filename plus debt count), and
  edited (filename plus "edited", with save promoted to primary).
- The edited state is entered by any form change after a load.
- Loading with unsaved edits prompts a confirm that names what will be lost.
- Save triggers a browser download of the exported CSV. No File System Access
  API save-in-place.
- Edited-state tracking and the unsaved-changes confirm are implemented in an
  external script under `static/js` that holds page state only — no arithmetic,
  formatting, or JSON fetching — and passes story 015's JavaScript check.
- C-13 Import report renders clean, partial, and failed states.
- Clean: one green line ("Loaded 3 debts and 8 values"), dismissible. Skipped
  paid-off debts keep the import clean and are mentioned in the same line
  ("… Skipped 1 paid-off debt."), using copy and counts from the server.
- Partial: gold banner stating what was accepted, plus a table of rejected rows
  with line number, raw row text, and reason.
- Failed: red banner carrying the server's problem detail; the form is left
  untouched.
- The report appears only after a load, never on first render.
- No CSV parsing, coercion, or currency conversion in client code — the client
  posts bytes and renders what comes back.

## Size

M

## Open Questions

- Does the export response's filename (story 022) drive the file bar's displayed
  name, or does the client track the loaded filename separately?
- After a partial import, the form contains accepted rows the user then edits —
  does the file bar show "loaded" or "edited"? Arguably it was never cleanly
  loaded at all.
- Does "Save a file" send the current form state to the export endpoint even when
  the form is invalid? Saving broken work-in-progress is usually right, but
  export takes a `FinancialSnapshot` that validation would reject.
- With JavaScript disabled, how does the download and the unsaved-changes confirm
  behave? The spec requires every screen to work without JS except the slider.
- ~~Is hand-written JavaScript acceptable for edited-state and the confirm?~~
  **Resolved.** Design spec section 7 permits it under the JavaScript policy;
  save-in-place is cut.
