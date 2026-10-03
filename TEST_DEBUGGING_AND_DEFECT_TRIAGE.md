# Framework Debugging, Maintenance, and Defect Triage

This runbook defines how to investigate failures from the ParaBank UI and API suites, keep the automation reliable over time, and report confirmed functional defects. The goal is to preserve useful evidence, identify the responsible layer, and keep product defects visible without weakening valid test expectations.

## 1. Triage Every Failure

1. **Preserve the run.** Before cleaning `target/`, save the CI artifacts or copy the relevant local reports. Record the commit, test suite, test name, run URL/build number, and timestamp.
2. **Classify the failure.** Choose one initial category: product-functional, automation/test defect, environment/setup, data/isolation, or intermittent/concurrency. Change the category only when evidence supports it.
3. **Check the test oracle.** Confirm expected behavior from an acceptance criterion, approved business rule, or versioned API contract. Label behavior seen only in the application as characterization, not as a requirement.
4. **Reproduce in isolation.** Use a controlled deployment and synthetic data. Re-run the smallest useful suite; use a fresh API environment when state may matter. Do not replay an ambiguous banking write automatically.
5. **Collect independent evidence.** Compare the user-visible/API result with persisted state, ledger records, and application logs. A matching HTTP status alone does not establish that a money operation succeeded correctly.
6. **Decide and track.** Fix the test if its setup/oracle is wrong. File a product defect if behavior violates an agreed requirement. File an environment incident if the target was unavailable or misconfigured. Do not silently change an assertion to match an unexplained result.

## 2. Reproduction and Evidence

For UI failures, capture the failing test/group, sanitized target URL, browser/version, viewport, screenshot, page title, browser console entries, and the application build. Failure diagnostics are written to `target/browser-diagnostics/` and linked from the Extent report. Chrome/Edge also record bounded network request/response metadata (method, sanitized URL, status and content type only); request/response bodies and headers are not captured. Firefox/Grid may report a log type as unavailable. Review artifacts before sharing because browser messages can contain application data.

For API failures, capture the operation and sanitized endpoint template, request parameters/body with secrets removed, status/content type, sanitized response, duration, environment/build/OpenAPI version, and before/after account balances and transaction IDs for financial actions. Check `target/api-evidence/`, `target/api-evidence/application-logs/`, `target/surefire-reports/`, and `target/reports/`. CI publishes sanitized application logs, not the full `target/api-environment/` directory. For failed writes, tests should collect read-only post-state even when the response status is unexpected; if a state read also fails, record that as a separate result.

Never attach passwords, login URLs containing credentials, session cookies, access tokens, real SSNs, or unredacted customer/payment data. Use synthetic identifiers in tickets. Do not use `clean` until reports and evidence are preserved.

For controlled API reproduction, start from the repository root:

```powershell
.\scripts\start-parabank.ps1
.\mvnw.cmd -Papi -Dgroups=ApiRegression verify
.\scripts\stop-parabank.ps1
```

The startup script resets only its owned loopback database on a fresh start and refuses to reuse a managed running process. If it reports an active server, stop that managed instance before starting a clean reproduction. UI tests should use a controlled `appUrl`; the shared public demo is unsuitable for repeatable defect confirmation.

Use the method-specific TestNG groups for the two tracked cases instead of rerunning all regression tests:

```powershell
# Customer update (PB-API-001)
.\mvnw.cmd -Papi '-Dgroups=CUST-002' verify

# Malformed bill-pay request and post-state checks (PB-API-002)
.\mvnw.cmd -Papi '-Dgroups=BILL-002' verify
```

## 3. Functional Defect Report

Use one report per independently actionable behavior. Keep the title searchable and state the observed failure, not a suspected root cause.

```text
Title: [Functional][API|UI][Area] Concise observed failure

Reporter / assignee:
Product component / affected release:
Application version and commit:
Automation commit and CI job/run URL:
Environment / browser or API version:
Test/case ID and requirement/acceptance-criteria link:
Frequency: reproducible x/y attempts / intermittent
Severity / priority (with impact rationale):
External tracker URL/ID:

Preconditions and synthetic data:
Steps to reproduce:
1.
2.
3.

Expected result:
Expected-result source: requirement / approved rule / API contract + version
Actual result:
Impact: affected user, workflow, data integrity, or downstream process
State/ledger check:
Evidence: sanitized report, screenshot, API evidence, relevant app-log excerpt
Workaround (if any):
Fix version / target release:
Notes / suspected cause (clearly marked as unconfirmed):
```

Severity describes impact; priority describes fix order. Suggested defaults: **Critical** for security exposure, data loss/corruption, or a blocked essential banking flow; **Major** for a core workflow returning an error or producing incorrect state; **Moderate** for a recoverable secondary-flow or field-validation problem; **Minor** for low-impact behavior. The product owner sets final severity and priority using release risk and affected users.

For a financial defect, include starting and ending balances, source/destination ownership, transaction IDs/types/amounts, and whether a failed operation partially committed. For a UI defect, include the page/workflow, visible state, browser/version, and whether the same action works through the API if that comparison is meaningful.

## 4. Current Findings to Triage

The profile-update finding `PB-API-001` in [DEFECT_REGISTER.md](DEFECT_REGISTER.md) is closed as an automation test-data defect: updated credentials exceeded database column limits. The remaining open candidate is `PB-API-002`, malformed bill-pay JSON returning HTTP 500. Its operation declares only a generic `default` response in OpenAPI, so confirm the expected status with the contract owner before calling it a contract violation. Assign an owner and link an external issue if confirmed.

Do not change the expected results to HTTP 500 just to make regression green. If the contract owner decides the expected behavior differs, update the contract/configuration and test with an explicit review note.

## 5. Defect Lifecycle and Fix Verification

1. Search the tracker for duplicates, then create/link the report with reproduction steps and sanitized evidence.
2. Triage severity, priority, affected release, owner, and target fix version. Keep product defects separate from automation and environment tickets.
3. Link the failing automated case to the defect. Do not disable or quarantine it without an issue link, owner, reason, and review/expiry date. A quarantine is not a passing result.
4. After the fix, run the focused reproducer first, then the relevant API/UI regression group and smoke gate. Verify persisted state and that neighboring workflows still work.
5. Review the diff for weakened assertions, added retries/sleeps, secret leakage, and order dependence. Update the contract, test, or operating documentation if behavior legitimately changed.
6. Close only after verification evidence is attached. If deferred, keep the issue open or explicitly accepted with owner, rationale, and review date.

## 6. Long-Run Maintenance Cadence

| Cadence | Maintenance activity |
| --- | --- |
| Every pull request | Compile; run deterministic unit tests and isolated API smoke; preserve results on failure. |
| Nightly | Run API regression against the pinned local deployment; review failures, setup failures, duration changes, and sanitized evidence. |
| Weekly | Review flaky tests and quarantines, stale/duplicate defects, fixture exhaustion, report retention, and failure trends by product/test/environment. |
| Monthly or on supported browser/runtime release | Update browser/Selenium/Maven dependencies in a dedicated change; run UI smoke and cross-browser verification before broad rollout. |
| On API/application upgrade | Review the OpenAPI version and operations, schemas, error behavior, fixture assumptions, loan configuration, and data-reset behavior. |

Track at minimum: pass/fail by suite and category, intermittent failure rate, setup/fixture failure rate, test duration, operation/scenario coverage, open functional defects by severity, and time to triage/resolve. Treat environment failures as blocked or failed runs, never as green runs with all application tests skipped. Keep each test independently diagnosable, deterministic, and tied to an expected-result source.
