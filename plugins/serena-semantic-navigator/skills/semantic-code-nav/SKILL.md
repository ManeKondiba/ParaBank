---
name: semantic-code-nav
description: Navigates codebase symbols, traces routes to Page Objects and API clients, and guides safe automated refactoring.
---

# Serena Semantic Code Navigator Skill

This skill provides semantic code understanding, symbol search, and safe refactoring workflows using `SEMANTIC_CODE_MAP.md` and Java AST structure.

---

## 1. Symbol & Route Resolution Protocol

```mermaid
flowchart LR
    Query["Target Route or Symbol (e.g. /transfer.htm)"] --> MapLookup["1. Lookup in SEMANTIC_CODE_MAP.md"]
    MapLookup --> PageClass["2. Locate Target PageObject / API Client"]
    PageClass --> CallerTests["3. Trace all caller Test Classes & TestNG Groups"]
    CallerTests --> Impact["4. Generate Blast-Radius / Impact Analysis"]
```

---

## 2. Refactoring Safety Workflow

When refactoring a Page Object method, locator, or API client method:
1. **Find Usages**: Scan all test classes in `src/test/java/testclasses/` for call sites.
2. **Atomic Update**: Rename the definition and all invocation points.
3. **Verify Compilation**: Execute `./mvnw.cmd -DskipTests test-compile` to detect any broken references.
4. **Run Unit & Guardrail Gate**: Execute `./mvnw.cmd -B -ntp -Punit test` to confirm `ArchitectureGuardTest` passes without violations.
