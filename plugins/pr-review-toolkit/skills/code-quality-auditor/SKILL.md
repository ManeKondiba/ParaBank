---
name: code-quality-auditor
description: Audits code quality beyond bugs, checking cyclomatic complexity, error handling, documentation, type design, and clean architecture.
---

# Code Quality Auditor Skill

This skill enforces high maintainability, readability, and structural cleanliness in test automation codebases.

---

## 1. Quality Dimensions

### A. Error Handling & Exception Hygiene
- No empty catch blocks: `catch (Exception e) {}` is strictly forbidden.
- Intentional ignores must be named `expected` or `ignored` with an explanatory comment.
- Re-thrown exceptions must preserve the original root cause: `throw new RuntimeException("Context", e);`.

### B. Method Length & Cyclomatic Complexity
- Page Object methods should focus on a single action (< 25 lines).
- Test methods should follow Arrange-Act-Assert structure (< 35 lines).
- Deep nesting (> 3 levels of `if`/`for`) must be refactored into helper methods or streams.

### C. Type Design & Modern Java Syntax
- Use immutable Java `record` types for data transfer objects (DTOs) and test fixtures.
- Avoid raw collections: always use typed generics (`List<Account>`, `Map<String, String>`).
- Use `Objects.requireNonNull()` in constructors to fail fast on null parameters.

### D. Documentation & Telemetry
- Every test method must include a `@Test(description = "...")` annotation.
- Sequential actions in tests should be logged via `logStep("Step description")`.
