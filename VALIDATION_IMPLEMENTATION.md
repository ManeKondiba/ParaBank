# Banking validation implementation

Date: 2026-10-07. The initial validation below used the unpatched pinned ParaBank deployment. Subsequent application source repairs for its 42 UI failures are described in UI_FAILURE_ANALYSIS.md and application/ui-fixes/README.md. `start-parabank.ps1 -UiFixes` selects the repaired variant; default startup remains upstream.

## Scope and acceptance rules

Implemented the banking validation expansion from VALIDATION_GAP_REPORT.md. Contact Us, About, Services, News and Site Map are excluded by explicit user instruction. No tests or page objects for those five areas were added.

The user selected production-style rules and visible application failures. The acceptance tests therefore require positive amounts with at most two decimal places, no overdrafts, rejection of same-account transfers, authenticated banking reads/writes, matching recovery identity, safe rejected-input state, and meaningful validation errors. These are project acceptance rules, not claims that every production bank uses identical policies. The duplicate-transfer scenario defines a proposed `Idempotency-Key` header contract: identical keys with identical payloads must apply once. The pinned demo does not document that contract; its failure records a missing production capability, not a violation of its published OpenAPI document. Zero down payment is not automatically treated as syntactically invalid; loan decisions remain tied to the pinned processor's documented 20-percent rule.

ProductionRules cases remain in full regression; they are not quarantined, skipped, retried or changed to accept the demo's unsafe behavior. Passing quick smoke demonstrates only its selected scenarios. The unpatched UI and unresolved API production cases are expected to fail until the application findings are resolved. Local UI repair results are recorded below.

## Implemented automated checks

| Validation IDs | Implementation |
| --- | --- |
| VAL-001, 012 | Anonymous and logged-out checks across all nine protected routes; browser Back cannot expose cached account data |
| VAL-002 | Correct customer account IDs after login; browser customer B cannot read A's account/transaction; anonymous API read and write checks with post-write financial-state comparisons |
| VAL-003, 004 | Shared UI ledger assertions require exact new-entry count, direction/amount, unique IDs and unchanged old rows; transfer and bill-pay success and rejection use them |
| VAL-005, 006, 019 | API zero/negative/excess precision/overflow/partial numeric/Infinity inputs across deposit, withdrawal, transfer and bill-pay; overdraft cases; same-account transfer rejection; existing minimum-cent positive API coverage and UI reverse-transfer minimum-cent scenario |
| VAL-007 | Repeated transfer using an identical idempotency key must apply once; balances and both ledgers checked |
| VAL-008, 009 | Deterministic pinned UI loan approval; exact approved down-payment ledger; denied-loan ledger unchanged; malformed UI loan fields; production numeric API boundaries and below/at/above threshold decision table |
| VAL-010 | Both checking/savings UI creation now checks exact account delta, source debit, destination credit and conserved funds |
| VAL-011 | Six mismatched recovery identity fields with a valid synthetic SSN; sensitive recovery result is left before assertions/screenshots |
| VAL-013 | Eight concurrent withdrawals against one isolated account, bounded worker shutdown, per-thread evidence and final balance/ledger reconciliation |
| VAL-014, 017 | UI maximum-length profile/credential round trip; API fields one above verified storage limits and whitespace-only required profile fields; partial Unicode/apostrophe profile update |
| VAL-015, 016 | Duplicate registration preserves original credentials/name and rejects replacement password; individual profile-field rejection rechecks saved values; partial/no-op updates preserve untouched fields |
| VAL-018 | Nine independently missing bill-pay fields; no balance/ledger mutation on rejection and exactly one debit after a corrected submission |
| VAL-020, 021 | UI transaction searches compare all-and-only expected records with duplicate detection; same-day inclusive range and reversed range; mixed debit/credit filters and complete debit detail fields |
| VAL-024 | Loan input errors must be actionable, while balances/accounts/ledger remain unchanged; broader simulated failures remain manual below |
| VAL-025 | Nine core banking forms check programmatic accessible names for visible enabled controls; this is a structural subset of WCAG checks |
| VAL-026 | The Chrome/Edge/Firefox suite covers 21 invocations per browser, including open account, search, profile, loan, recovery, negative money input, mixed activity and browser Back after logout |
| VAL-027 | Manual full UI and expanded cross-browser GitHub jobs; report gate rejects failures, skips, unknown statuses and wrong scenario counts |
| VAL-028 | Reviewed pinned operation shapes now compare parameter names/locations/required/type/schema, request bodies, response definitions and operation security as well as existing route inventory |
| VAL-029 | Existing sanitized API/browser evidence retained; log capture now reads with writer-compatible file sharing; negative recovery avoids credential-result screenshots and invalid-profile checks avoid printing customer DTOs |
| VAL-030 | Repeatable isolated full-suite benchmark script records startup/build wall time, test outcomes and fresh TestNG timing artifacts; it refuses to report failed runs as passing validation |

## Suite inventory and commands

The full UI inventory is 147 cases plus 23 deterministic units. Initial upstream UI evidence was collected in two executions: 135 page/regression cases and the additional 12 access/accessibility cases. Those initial results are not presented as one 147-case execution. The full API inventory is 107 business/contract/production cases; API infrastructure units are excluded by the ApiRegression group. Cross-browser inventory is 63 UI invocations, including one browser Back after logout case in each browser. Built-in data is used for these counts; an external login workbook changes the UI count and needs a separately reviewed gate value.

```powershell
# Start the original upstream deployment; production failures remain expected
powershell -ExecutionPolicy Bypass -File scripts/start-parabank.ps1

# Full customer banking UI, including production failures
.\mvnw.cmd -Pregression '-Dui.threads=2' '-Dheadless=true' '-DappUrl=http://127.0.0.1:8081/parabank/index.htm' verify

# Full API regression and production acceptance rules
.\mvnw.cmd -Papi-regression verify

# Expanded three-browser banking compatibility, including logout history
.\mvnw.cmd -Pcross-browser '-Dheadless=true' '-DappUrl=http://127.0.0.1:8081/parabank/index.htm' verify

powershell -ExecutionPolicy Bypass -File scripts/stop-parabank.ps1

# Repeated measurements start/stop their own managed environment
powershell -ExecutionPolicy Bypass -File scripts/benchmark-banking.ps1 -Runs 3 -Workers 2
```

GitHub workflow inputs run_full_ui and run_cross_browser are manual and false by default. Full UI count gate is 147; cross-browser gate is 63. The existing run_ui seven-case smoke remains unchanged. Original API smoke remains focused; nightly ApiRegression now includes production acceptance cases and will fail on unresolved application gaps. The new hosted full UI and cross-browser jobs have not yet been executed.

## Initial upstream verification

| Execution | Cases | Passed | Failed | Errors / skips |
| --- | ---: | ---: | ---: | --- |
| Main UI regression (two workers) | 135 | 105 | 30 | 0 / 0 |
| Additional customer access/accessibility | 12 | 0 | 12 | 0 / 0 |
| Final API regression | 107 | 54 | 53 | 0 / 0 |
| Expanded Chrome/Edge/Firefox compatibility | 60 | 60 | 0 | 0 / 0 |
| Framework unit tests | 23 | 23 | 0 | 0 / 0 |

The two UI executions cover all 147 distinct UI invocations; 105 pass and 42 fail in that combined coverage, not one combined timing measurement. All 40 existing/new characterized API contract cases passed. The 53 API failures are in the production acceptance class; all three below/at/above pinned loan threshold cases passed. Same-account transfer fails the project's rejection rule and appends two entries. Concurrent withdrawals failed in both API runs; the final run captured post-request ledger state as well as the lost balance update. The initial 60-case three-browser subset covered working banking workflows and excluded the known failing production/security/accessibility cases.

The first 103-case API run is retained in api-regression/ and the final 107-case run in api-final/. Main UI test wall time is in its timing artifact, additional access/accessibility took 40.852 seconds, final API took 29.936 seconds and cross-browser took 335.104 seconds. Browser startup/platform differences make this one cross-browser run unsuitable as a performance claim.

Scenario-gate self-checks accept a complete passing inventory and reject skips, failures and count mismatches; the real 60-case browser report also passed the gate. XML suites and workflow YAML parse successfully. A live-writer log-sharing check reproduced the capture condition and verified the improved FileShare.ReadWrite reader. Sanitized logs were preserved after the isolated server stopped. Retained artifacts are in target/validation-implementation/ and can be removed by Maven clean. Preserve needed XML, screenshots, browser diagnostics, sanitized API evidence and logs before cleaning.

## Remaining manual or deployment-dependent checks

These are explicit remaining coverage limits, not silently implemented or certified:

| Area | Procedure / acceptance evidence still required |
| --- | --- |
| Session expiry/rotation and new tabs | Define inactivity/absolute timeout and cookie policy; record old/new session IDs without publishing values; verify expired/old IDs cannot restore banking access. Browser Back and logged-out route checks already run; timed expiry was not simulated |
| Accessibility beyond accessible names | Run keyboard-only create/transfer/payment/search/update/loan workflows; inspect visible focus/order, associated error messages, live announcements with a screen reader, zoom/reflow and contrast. Structural name checks cannot establish full WCAG conformance |
| Duplicate UI click/Enter/refresh/back | Use bounded controlled browser interactions and correlate server request IDs with ledger deltas; distinguish a deliberate second payment from an accidental repeat. The API repeated-key acceptance test establishes the required backend capability first |
| Network failures and ambiguous writes | Inject an isolated delayed/disconnected response after a committed operation; confirm the UI reads persisted state before offering retry. Existing transport units prove the client does not replay, not the server/UI's complete recovery behavior |
| Registration above-limit rejection | Profile API tests and maximum-length registration are implemented; a full browser create-rejection matrix still needs a read-only customer existence oracle or database test hook to prove no customer was inserted |
| Account month/date diversity | Mixed debit/credit and exact search sets are automated; year/month boundary and distinct historical-date fixtures need controlled transaction-date seeding. Do not add admin reset or direct database edits to normal customer tests |
| Stored markup and output encoding | Unicode/apostrophe round trip is automated. Review each rendered destination with controlled inert markup and verify escaping; do not equate field-value round trip with proof against all injection attacks |
| Administration/diagnostic exposure (VAL-032) | Deployment owner defines admin/diagnostic authorization model; review read-only access separately. Destructive database reset/connection changes stay outside the customer suite |
| Full customer navigation matrix (VAL-023) | Banking links exercised by existing page actions; excluded Site Map/public-content navigation is outside scope. Dedicated header/footer/keyboard navigation remains a follow-up |
| Performance stability | Benchmark script is ready; repeated measurements were not run for the expanded failing workload. Do not compare the old 93-case timing with the new 147-case scope as if they were equivalent |
| Error contracts and production authentication adapter | Demo REST contract has no production auth or structured error model. Anonymous denial tests expose that gap; an authenticated customer A/B REST matrix needs the future server-supported auth mechanism |

## Source and scope notes

The audit file preserves its original source inventory for historical traceability. Current scenario counts and implementation are described here and in README. Application behavior failures are tracked in DEFECT_REGISTER.md. Customer banking tests remain independent; API business suite remains serial. The concurrency case controls only its eight requests on one owned fixture and does not change api.xml to unsafe class/method parallelism.
## UI repair verification follow-up

The final repaired application variant passed the complete 147-case UI suite with three browser workers: zero failures/errors/skips, Maven BUILD SUCCESS, reviewed scenario-count gate passed. The original 42 failures were resolved through application source changes, preserving the acceptance tests. See UI_FAILURE_ANALYSIS.md for before/after evidence, the intermediate Bill Pay ID correction, deployment selection and remaining backend/security limitations. The initial failing tables above remain historical evidence for the original upstream application.
