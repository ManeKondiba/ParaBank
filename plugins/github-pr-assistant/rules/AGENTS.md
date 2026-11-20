# PR Assistant Rules

1. Never submit or approve a PR without a green unit test verification pass (`-Punit verify`).
2. Every PR description must populate the checklist defined in `.github/pull_request_template.md`.
3. Code suggestions must never weaken test assertions to artificially make tests pass.
