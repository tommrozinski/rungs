# Planning history

This folder keeps the original plan for Rungs: the backlog of user stories and the
per-story specs (requirements, design, and tasks) that drove the build.

The project was built with a spec-driven, agentic workflow. AI agents drafted user
stories from the product requirements, a human reviewed and approved each one, and
each approved story then went through its own requirements, design, and task plan
before any code was written. The approval markers in each `spec/` folder record those
review gates.

- [`backlog/`](backlog/) holds the user stories, with an index in `backlog/README.md`.
- [`spec/`](spec/) holds the requirements, design, and task plan for each story.

## How this relates to the current code

The first plan was larger: JSON APIs, CSV import and export, a JavaScript-driven UI,
container packaging, and an architecture test suite. That scope was deliberately cut
back to the focused v0 described in the main [README](../../README.md), then grown
again one visually checked feature at a time (specs 038 to 041, with matching notes
in [`docs/design/`](../design/)). Many of these stories describe features that are not
in the current code.

The product requirements documents these stories reference are not included, so some
links in the backlog and specs point to files that are not in this repository.
