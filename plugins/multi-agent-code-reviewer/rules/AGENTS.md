# Multi-Perspective Review Rules

1. Every pull request review MUST evaluate all four perspectives: SDET, QA Architect, Security, and Performance.
2. An arbitrary `Thread.sleep()` or raw driver instantiation is an automatic BLOCKER (score deduction of 25+ points).
3. Plain-text credential logging in test outputs or reports is an automatic BLOCKER.
4. All reviews must provide exact, actionable replacement snippets for identified defects.
