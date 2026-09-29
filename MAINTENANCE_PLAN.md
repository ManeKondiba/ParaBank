# ParaBank framework maintenance plan

Prepared: 2026-09-24. Coverage baseline updated: 2026-09-28. Review monthly and after a major application or tooling change.

The objective is to keep test results trustworthy while adopting supported, compatible tooling. Every upgrade must preserve test discovery, assertions, browser isolation, failure evidence and a failing exit code when something breaks.

This document is an implementation plan. Verification is recorded in [FRAMEWORK_REVIEW.md](FRAMEWORK_REVIEW.md); its framework-check results are historical because those test classes have been removed. Proposed schedules, update bots and migrations are not yet enabled.

## 1. Starting point and priorities

The current baseline is Java 17, Maven wrapper 3.9.16, Selenium 4.49.0, TestNG 7.12.0 and Surefire 3.5.5. The suite contains 93 UI invocations with built-in login data plus seven deterministic utility unit tests. The cross-browser smoke suite contains seven invocations per browser. `test` and `verify` run the full master suite by default; `-Pui` remains an alias. The workflow compiles UI tests and runs the `Unit` group on pushes/PRs, and offers manual Chrome smoke. UI runs remain manual until a controlled deployment is available.

| Priority | Work | Completion evidence |
| --- | --- | --- |
| P0 | Establish a dedicated ParaBank deployment and repeatable data reset | Record application version and seed settings; a fresh environment can run all 93 UI invocations; cleanup affects only that environment |
| P0 | Exercise the GitHub workflow and require its build job before merging; add required UI smoke after the dedicated deployment is available | A real PR shows a passing compilation check; the future UI job also rejects an intentional test failure |
| P0 | Make cross-browser results complete | Each invocation has browser, class, method and case identity; 21 smoke invocations across three browsers appear in the chosen CI result format |
| P1 | Validate Firefox and Grid | Firefox smoke passes; one remote smoke run verifies session creation, failure attachments and cleanup |
| P1 | Replace the sunset reporter | A maintained candidate passes the report checks in section 4 before ExtentReports is removed |
| P1 | Introduce controlled dependency monitoring | Weekly Maven/GitHub Actions update proposals, reviewed by the framework owner; no blanket automatic merge |
| P2 | Expand business coverage | Prioritized transfer, transaction-history and bill-payment cases have independent data and real business assertions |

For complete results immediately, evaluate separate CI jobs using `-Psmoke -Dbrowser=chrome`, `edge` and `firefox`, with unique artifact names. Check that this resolves the observed JUnit XML undercount before treating it as fixed. Keep the existing combined suite as a separate concurrency check. Until then, use Spark or TestNG's per-method results to count cross-browser invocations.

## 2. Ownership and routine

Assign a framework owner and a backup. In a one-person project, you can own all roles; keep the same checklist so the work stays repeatable. The application owner supplies expected behavior, version changes and test-environment resets. The CI owner maintains runners, artifact retention and required checks.

| When | Action | Owner | Expected output |
| --- | --- | --- | --- |
| Every PR | Compile with `-DskipTests verify`. For page, fixture, driver, configuration, suite or dependency changes, also run affected UI coverage on the dedicated environment when available | Change author | Compilation results, UI scope and evidence or the environment blocker attached to the PR |
| After each application deployment; daily when deployments are frequent | Run Chrome smoke and classify failures | Automation owner | Current application health and assigned failures |
| Weekly | Run full UI regression plus three-browser smoke; review flaky cases, dependency proposals and deprecated API warnings | Framework owner | Maintenance backlog with owners and target dates |
| Monthly | Review library/plugin releases, JDK patch level, Maven wrapper, browser/driver versions, CI actions, runner image and vulnerability alerts | Framework + CI owners | One small upgrade batch, compatibility evidence and an updated baseline |
| Quarterly | Review coverage against business risks, reporting support, Java baseline and one useful automation advancement | Framework + application owners | Adopt/defer decision with measured benefit and migration cost |
| On an applicable security advisory or broken browser release | Triage immediately; do not wait for the monthly window | Framework owner | Tested fix, temporary containment or a documented time-limited exception |

Suggested routine effort: 30-60 minutes weekly and one half-day monthly, adjusted to failure and release volume. Do not introduce frequent scheduled registration/account-creation runs on the public demo. Keep public-demo runs manual until the dedicated environment exists.

## 3. Dependency and runtime upgrade process

1. **Discover.** Add weekly Dependabot proposals for the root Maven project and GitHub Actions. Check the wrapper, installed JDK and browser/Grid images explicitly as well. Monitor security advisories separately from routine version proposals. GitHub documents the available [scheduling and grouping controls](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-options-reference).
2. **Assess.** Read upstream release and migration notes. Check Java requirements, removed APIs, TestNG listeners/groups/parameters, browser capabilities, driver resolution and transitive dependency changes. Review actual advisory applicability; test-scoped libraries still execute on developer and CI machines.
3. **Isolate.** Use a small branch/PR for one dependency family. Keep the Log4j modules aligned. Handle runner upgrades, Java baseline changes and reporting migrations separately so failures have an identifiable cause.
4. **Pin and test.** Use explicit stable versions and the checked-in wrapper. Record the tested application, Java, browser, driver and Grid versions. Run the upgrade checks below against the existing baseline and the candidate when a failure needs comparison.
5. **Accept.** Merge only when the required cases were discovered and passed, report counts reconcile, and the controlled failure checks still fail correctly. A successful Maven exit with missing cases is not acceptable.
6. **Observe.** Review the next three scheduled runs for an ordinary upgrade. For a browser/runtime/reporting migration, use a five-run trial on the dedicated environment before replacing the baseline. These are initial confidence checks, not proof that a test can never be flaky.
7. **Record or roll back.** Update the dependency decisions, review date and evidence. If the candidate breaks the framework, revert its PR and restore the previous known-good tool/image versions; do not weaken assertions or increase timeouts to make an upgrade pass. A security-driven rollback also needs an explicit mitigation and fix deadline.

**Current exceptions:** retain the existing Surefire pin during ordinary updates. [Surefire 3.6 removes `suiteXmlFiles` support](https://maven.apache.org/surefire-archives/surefire-LATEST/maven-surefire-plugin/whats-new-3-6-0.html), so its upgrade needs a separate execution migration that preserves all profile and browser behavior. Review that exception monthly, including applicable security fixes. [ExtentReports is sunset upstream](https://github.com/extent-framework/extentreports-java); upgrading its version is not a long-term maintenance strategy.

Update the installed JDK within the supported Java 17 line as part of patch maintenance. Evaluate a later supported Java baseline separately, first running the existing framework on both runtimes. Update `pom.xml`, CI and IDE settings together only after compatibility is demonstrated. Do not freeze old browsers indefinitely; use a controlled baseline for reproducibility and a separate current-browser check for early warning.

## 4. Upgrade acceptance checklist

Run commands from the project root. On Linux/macOS, replace `.\mvnw.cmd` with `bash mvnw`. Set the dedicated deployment before running UI tests:

```powershell
$env:PARABANK_APP_URL = 'http://localhost:8080/parabank/index.htm' # Replace with your dedicated URL
.\mvnw.cmd --batch-mode --no-transfer-progress -DskipTests verify
.\mvnw.cmd --batch-mode --no-transfer-progress -Dheadless=true verify
.\mvnw.cmd --batch-mode --no-transfer-progress -Psmoke -Dheadless=true verify
.\mvnw.cmd --batch-mode --no-transfer-progress -Pcross-browser -Dheadless=true verify
```

The first Maven command compiles without executing tests; it is not evidence that UI cases passed. The default `verify` command runs the full UI suite. The last command requires all three browsers locally or matching Grid nodes. Do not set `PARABANK_BROWSER` or a global `-Dbrowser` for the combined cross-browser suite, because that would override its browser parameters. Use a clean checkout/workspace when validating a dependency migration so stale compiled classes cannot affect discovery.

| Area | Acceptance check |
| --- | --- |
| Discovery | Current baseline: seven smoke invocations per browser; 93 UI invocations across 13 page classes; 21 combined smoke invocations when all three browsers run. Seven utility unit tests are also in the master suite. Workbook-driven counts depend on workbook rows. Update expected counts with intentional coverage changes |
| Correctness | No unexpected failures or skipped cases; no swallowed exceptions, order dependencies, sleeps or retries used to disguise problems |
| Lifecycle | A session per invocation; independent accounts; cleanup after pass, assertion failure and setup failure |
| Reporting | Correct status and unique identity for every browser/data case; no overwritten results; screenshot links work after artifacts are downloaded |
| Negative controls | In an isolated probe suite, an invalid browser setting and a deliberate assertion failure produce nonzero exits and the original error; screenshot failure does not replace the test failure |
| Configuration | JVM/environment/XML/file precedence remains intact; headless and local modes work; test Grid when changing remote execution |
| Data | Excel blanks/formulas/header aliases still work; credentials are not introduced into logs or attachment labels; workbook success cases use provisioned accounts |
| Timing | Investigate a runtime increase above 20% against the last five comparable successful runs; do not trade correctness for an arbitrary time target |

Preserve old and new reports until the upgrade is accepted. Keep normal artifacts for a proposed 30 days and migration evidence for 90 days, subject to the repository's storage and data policy. Record browser/session capabilities in future reports and give data-driven cases non-secret case IDs.

## 5. Keep failures actionable

For each failure, first check environment reachability and setup errors, then inspect the screenshot, original stack trace and application behavior. Classify it as an application defect, test defect, environment/data issue or tooling change. Assign an owner and include the test ID, browser, application version and reproduction command.

A diagnostic rerun must retain the original result. A fail-then-pass outcome is evidence to investigate, not grounds to overwrite the failure as green. If temporary quarantine is necessary, use a tracked issue, an owner and a target expiry within seven days; keep its exclusion visible in reports. Do not quarantine core smoke coverage just to enable merging.

Track first-attempt results, unexpected skips, report-count mismatches, median runtime, time to diagnose and quarantined cases. Initial goals: zero missing report entries, zero unexplained skips, and no reproducible framework-caused failures. After at least 100 comparable case invocations, aim for an observed flake rate below 1%, with a named issue for every known flaky test. Show application failures separately so the metrics do not reward hiding defects.

## 6. First 90 days

| Period | Deliverable | Exit condition |
| --- | --- | --- |
| Days 1-7 | Name owners, create a dedicated deployment, validate GitHub checks, record runtime versions and start failure triage | Fresh checkout builds; existing Chrome coverage passes on the controlled deployment; CI demonstrably rejects an intentional failure |
| Days 8-30 | Enable reviewed update proposals; introduce per-browser CI artifacts; validate Firefox and Grid; add lifecycle/report probes | 21 smoke invocations are visible across three browsers; Grid evidence exists; no unresolved report-count mismatch in the chosen CI path |
| Days 31-60 | Trial a maintained reporting replacement and move reports only after parity checks; define the Surefire migration approach | Candidate handles parallel/data cases, setup failure, skip, screenshot and CI artifacts; Surefire has a tested migration prototype or a dated reason to defer |
| Days 61-90 | Add transfer/transaction-history cases, then bill payment; trial faster API-based fixture setup; evaluate one diagnostic improvement | New cases assert persisted business outcomes, remain independent, and pass repeated runs on the controlled environment; trial has a measured adopt/defer decision |

Keep registration UI coverage when adding API-based fixtures. Use API setup only where supported and only for test-owned data, with cleanup constrained to the dedicated environment. Evaluate effort after each phase; expand scope only when the existing baseline remains reliable.

## 7. Adopt advancements through small trials

| Candidate | When it helps | Trial and adoption condition |
| --- | --- | --- |
| Maintained TestNG reporting integration, such as [Allure TestNG](https://allurereport.org/docs/testng/) | Replacing the sunset reporter; improving case history and attachments | Run it alongside current output in a branch; verify dependency support and all reporting gates; measure CI/storage overhead before switching |
| [Selenium WebDriver BiDi](https://www.selenium.dev/documentation/webdriver/bidi/) | Console/network evidence would explain recurring UI failures | Capture diagnostic events for one flow; test the exact browser/Grid versions; filter credentials and tokens; retain normal assertions |
| API-assisted setup and API contract tests | Registration setup becomes a runtime bottleneck or UI-only checks miss backend behavior | Compare setup runtime and failure rate for one flow; preserve end-to-end registration tests and verify created state |
| Additional browser infrastructure | Required OS/browser coverage exceeds available runners or runtime limits | Compare an additional CI job with Grid/cloud capacity using the actual suite; adopt only if coverage, reliability and cost justify it |
| A different automation engine or AI-assisted authoring | There is a measured gap that the current framework cannot address economically | Limit the trial to one representative flow; review generated code and assertions; require parity and a migration-cost estimate before wider use |

Selenium's BiDi APIs expose browser events, but validate the particular features needed on each supported browser. A new tool should demonstrate a concrete improvement in coverage, diagnosis, reliability or maintenance effort. Keep experimental changes separate from routine patch updates.

For every adoption decision, record: problem, alternatives, tested versions, before/after results, owner, cost, rollback and next review date. For recurring releases, use this maintenance record:

```text
Date / owner:
Application and environment:
Dependency, Java, Maven, browser, driver and Grid versions:
Change and upstream release/advisory link:
Expected / discovered / passed / failed / skipped cases:
Failure-report and cleanup checks:
Evidence / PR link:
Decision / rollback / exceptions and expiry:
Next review date:
```
