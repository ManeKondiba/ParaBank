---
name: pr-assistant
description: Reviews Git diffs, audits pull request readiness against repository checklists, and resolves review feedback.
---

# GitHub PR Review Assistant Skill

This skill automates pre-PR readiness checks, diff inspection, and review comment triage.

---

## 1. Pre-PR Readiness Audit Workflow

Before submitting a PR or merging code:
1. **Diff Inspection**: Run `git diff origin/master...HEAD` to isolate modified lines.
2. **Architecture Guardrail Gate**: Execute `./mvnw.cmd -B -ntp -Punit verify` to guarantee zero `Thread.sleep` or architectural regressions.
3. **PR Checklist Compliance**: Verify against `.github/pull_request_template.md`:
   - [ ] No raw `driver.findElement()` in test methods.
   - [ ] All new Page Objects extend `BasePage`.
   - [ ] Customer data generated via `CustomerDataFactory`.
   - [ ] Ledger assertions verify monetary math.
   - [ ] Sensitive fields masked in logs.

---

## 2. Resolving Review Comments

When reviewing PR feedback:
1. Locate the file and exact line number from the review comment.
2. Verify if the feedback aligns with framework rules (`rules/AGENTS.md`).
3. Apply targeted, minimal code modifications.
4. Re-run unit tests (`.\mvnw.cmd -Punit test`) to verify fix.
5. Provide a clear explanation of how the review thread was addressed.
