# Defect Register

This is the repository-local source of truth for automation findings until they are linked to the team's external issue tracker. IDs below are stable local references, not external ticket numbers. Status records the local lifecycle; open product-behavior candidates still require contract-owner confirmation, ownership, and an external tracker link.

Do not close or downgrade a record only to make a test suite pass. Preserve sanitized CI artifacts before cleaning generated output. Never add credentials, real customer data, or unredacted request/response bodies to this file.

## Closed Records

### PB-API-001: Customer update test used overlong credentials

| Field | Value |
| --- | --- |
| Status | Closed - automation test data corrected (2026-10-03) |
| Type | Test-data/database-limit mismatch, not a confirmed product defect |
| Severity / priority | Not applicable; no product defect filed |
| Owner | Framework test maintenance (resolved locally) |
| External tracker | Not filed; the verified cause was invalid test data |
| First observed | 2026-10-03 |
| Automation revision | `e4c5818` |
| Target | Local pinned ParaBank source `98c1c9ab4889eb92c7798da63bb417e27261d3d5`, API 3.0.0, OpenAPI 3.0.1 |
| Test | `TestClases.api.CustomerApiTest.profileAndCredentialUpdate` (`CUST-002`) |
| Frequency | Failed before correction; focused `CUST-002` passed after correction |

**Root cause:** The API fixture username is 20 characters and password is 17 characters. The test appended `-updated`, exceeding the pinned database's `username VARCHAR(20)` and `password VARCHAR(20)` columns. HSQLDB returned a string truncation error; the endpoint responded with HTTP 500. The profile fields were not the cause.

**Resolution:** The test now uses unique, customer-ID-based credentials below the column limits while retaining special-character URL-encoding coverage. No application schema or production behavior was changed.

**Verification:** `mvnw.cmd -Papi -Dgroups=CUST-002 verify` passed 1/1 against the pinned deployment after correction.

**Evidence:** The passing sanitized exchange is `target/api-evidence/CustomerApiTest-profileAndCredentialUpdate-d0094556-7b9c-49e2-875d-07bf6fa7dbc5.json`. Earlier failing runs and the truncation stack trace remain in generated local evidence/logs and can be removed by `mvn clean`.

## Open Records

### PB-API-002: Malformed bill-pay JSON returns HTTP 500

| Field | Value |
| --- | --- |
| Status | Open - triage / expected behavior confirmation |
| Type | API error-handling behavior candidate |
| Severity / priority | Moderate / P2 (provisional; product owner to confirm) |
| Owner | TBD |
| External tracker | Not filed; link required |
| First observed | 2026-10-03 |
| Automation revision | `e4c5818` |
| Target | Local pinned ParaBank source `98c1c9ab4889eb92c7798da63bb417e27261d3d5`, API 3.0.0, OpenAPI 3.0.1 |
| Test | `TestClases.api.BillPayApiTest.malformedPayeeDoesNotDebitAccount` (`BILL-002`) |
| Frequency | Failed in negative, full-regression, and focused reproduction runs |

**Observed:** `POST /billpay` with malformed JSON returned HTTP 500. The updated test then queried the account and ledger; both matched their pre-request state in the focused 2026-10-03 run. This confirms no balance or ledger mutation for that run only.

**Expected:** Malformed input must be rejected without balance or ledger mutation. The test asserts an error response and verifies both persisted-state invariants; it does not require a particular error status because the OpenAPI operation declares only a generic `default` response. HTTP 500 remains a product-behavior candidate for contract-owner review.

**Reproduction:** From the repository root, start the controlled instance with `scripts/start-parabank.ps1`, run `mvnw.cmd -Papi -Dgroups=BILL-002 verify`, then stop it with `scripts/stop-parabank.ps1`.

**Evidence:** `target/api-evidence/BillPayApiTest-malformedPayeeDoesNotDebitAccount-7dd678a2-14f5-4001-96b4-b98fe951866a.json` records the sanitized exchange and post-request state reads; both confirm unchanged balance and ledger in this run. Sanitized server logs are staged under `target/api-evidence/application-logs/`. These generated paths are local and may be removed by `mvn clean`; attach retained CI artifacts to the external issue.

**Next actions:** Confirm whether malformed JSON should return a client error rather than HTTP 500; inspect the server log; then assign ownership and link the external ticket.

### PB-UI-001: Accounts Overview after logout displays an internal error

| Field | Value |
| --- | --- |
| Status | Open - local application patch verified; upstream adoption pending |
| First observed | 2026-10-07 |
| Automation revision | `a20c4e0` |
| Target | Pinned local ParaBank source `98c1c9ab4889eb92c7798da63bb417e27261d3d5` |
| Test | `LogoutTest.testLoggedOutUserCannotRevisitAccountsOverview` |
| Owner / external tracker | TBD / not filed |
| Frequency | Reproduced in full sequential regression, two-worker regression, and focused Logout group |

**Observed:** Register and log in, save the Accounts Overview URL, log out, then revisit the saved URL. The page displays `An internal error has occurred and has been logged.` The test expects `You must be logged in to use this feature.` Login controls remain visible in the screenshot; this observation does not establish unauthorized account-data exposure.

**Cause evidence:** Server logs report `IllegalStateException: Missing parameter 'userSession' of type [com.parasoft.parabank.web.UserSession]`. The Overview controller requires this session parameter. Source inspection shows the login interceptor attached to the legacy SimpleUrlHandlerMapping, while Overview also has an annotation-based mapping. An interceptor/mapping mismatch is a likely upstream cause; the precise routing fix needs application-owner verification.

**Validation:** Full UI baseline with browser provisioning and one worker: 93 run, 92 passed, 1 failed, no skips; TestNG wall time 243.884 seconds, Maven total 4:17. Focused `-Dgroups=Logout` run: 3 run, 2 passed, the same case failed. Two-worker full UI regression on the same unpatched deployment also ran 93 cases: 92 passed, the same case failed, zero errors/skips; TestNG wall time 222.717 seconds, Maven total 3:47. This single comparison reduced TestNG wall time by 21.167 seconds (8.7%); repeated runs are needed to establish a stable performance gain. Assertions and test selection were not relaxed.

**Evidence:** Full-run XML and timing CSV are preserved under `target/regression-baseline/sequential/` and `target/regression-baseline/parallel-two/`. Original screenshots are under `screenshots/`, browser diagnostics under `target/browser-diagnostics/`, and sanitized server logs under `target/api-evidence/application-logs/`. Generated evidence is local and can be removed by Maven clean; preserve it before cleaning.

**Patch validation (2026-10-07):** The candidate in patches/PB-UI-001-overview-authentication.patch registers the existing login interceptor for the annotated Overview route. A rebuilt local candidate passed five Logout/AccountsOverview cases and all 93 UI cases with one worker, zero failures/errors/skips (Maven 4:31; TestNG 266.758 seconds). Reports and the patched WAR are preserved under target/patch-validation/PB-UI-001/. Original source XML, WAR and deployed XML were restored and hashes verified afterward. The default deployment remains unpatched, so this record stays open. No Selenium assertions were changed.

**Next action:** Have the application owner adopt the patch or explicitly configure a patched validation deployment. The unpatched two-worker comparison reproduced the same defect without additional failures. Repeat full regression against the adopted application build before presenting it as fixed.

## Production validation findings (2026-10-07)

The user explicitly selected production-style acceptance rules. The following findings are open on the unpatched pinned local deployment. Owner and external tracker are TBD; no external issues were filed. These include production capability gaps beyond the demo's published contract. Local XML/evidence is retained under target/validation-implementation/; generated files are removed by Maven clean. That initial validation implementation did not include application changes or patches; subsequent local UI repairs are recorded below.

| ID | Open finding | Evidence / observed behavior | Required resolution |
| --- | --- | --- | --- |
| PB-UI-002 | All protected-page entry checks fail the expected login-required behavior | Nine anonymous and nine logged-out route cases fail. Most display the generic internal error; transaction.htm with a missing ID reports a missing transaction instead of requiring login. See ui-regression/testng-results.xml. This observation alone does not establish data exposure | Require authentication before protected-route handling and show a controlled login response |
| PB-SEC-001 | Banking data/resources accessible without appropriate authorization | Browser customer B can read A's account and transaction in both BankingAccess cases. Four anonymous REST resource reads return HTTP 200; four anonymous REST writes return success and change balances/ledger. See access-and-accessibility/ and api-regression/ XML | Implement session/REST authentication and ownership authorization for every banking resource/action |
| PB-SEC-002 | Recovery accepts mismatched identity with a valid SSN | Six cases vary first name, last name, street, city, state or ZIP; the recovery result still reveals credentials. The test navigates away before failure screenshots. This extends the known demo plaintext recovery behavior | Adopt a secure verified recovery flow; do not disclose passwords |
| PB-SEC-003 | Browser Back exposes cached Overview account data after logout | BankingAccessTest.testBrowserBackAfterLogoutDoesNotExposeAccountData sees the prior account link. This is cached browser disclosure evidence, not proof that the old server session remains valid | Review sensitive response caching and logout/history behavior; verify server invalidation separately |
| PB-UI-003 | Malformed loan fields display a generic internal error | All six malformed amount/down-payment UI cases preserve financial state but show the internal-error message instead of actionable input validation | Validate inputs and retain a usable form with specific safe errors |
| PB-A11Y-001 | Visible banking controls lack accessible names | Nine core form-route cases report unnamed visible enabled fields, including login, registration, recovery and banking forms. No claim of full WCAG audit is made | Add programmatic labels/names and verify keyboard/error/status accessibility |
| PB-FIN-001 | Unsupported amount values are accepted or return internal errors | 13 of 24 deposit/withdraw/transfer/bill-pay boundary cases fail the positive/two-decimal/client-error/no-mutation acceptance rules; some append records or change balances. Inspect per-case XML/evidence before assigning individual behavior | Reject zero/negative/excess precision and unsupported numeric values before financial writes; safe client error for overflow |
| PB-FIN-002 | Debit operations permit overdrafts | All three withdraw/transfer/bill-pay cases fail the no-overdraft acceptance rule; requests above available funds succeed and mutate state | Enforce the project's no-overdraft rule atomically |
| PB-FIN-003 | Repeated-key transfer applies more than once | Duplicate request scenario sends the same transfer and Idempotency-Key twice; the observed source debit exceeds one operation. The demo does not publish idempotency support | Define and implement the proposed production idempotency contract; preserve exact balance/ledger effects |
| PB-FIN-004 | Concurrent withdrawals lose balance updates | Original eight-request run expected source balance 992.00 and observed 994.00. All eight responses had succeeded before reconciliation. See original api-regression XML and sanitized per-thread request evidence | Make balance/ledger updates atomic and concurrency-safe; reproduce under controlled load |
| PB-API-003 | Invalid profile fields are stored or yield HTTP 500 | All 19 over-limit/whitespace profile cases fail production acceptance: over-limit values return internal errors, whitespace values can change saved fields or invalidate original credentials | Validate field boundaries/presence before mutation and return client errors with unchanged rejected state |
| PB-API-004 | Invalid numeric loan fields do not return safe input rejection | Seven invalid amount/down-payment numeric cases fail production acceptance; decisions or internal errors are returned instead of the required safe rejection. Per-case state comparisons determine actual side effects | Validate loan values before decision/account creation; retain no-mutation guarantees |

Additional final API observation: **PB-FIN-005 (Open)** - same-account transfer returns success and appends two ledger entries rather than rejecting the operation; the source balance remains unchanged in the observed case. The three pinned below/at/above loan-threshold cases pass. Final API run: 107 cases, 54 passed, 53 failed, no errors/skips; concurrent lost-update failure reproduced with post-request ledger capture. Final evidence is target/validation-implementation/api-final/.

See VALIDATION_IMPLEMENTATION.md for explicit rules, suite counts, test execution results and the remaining manual/deployment-dependent coverage. PB-UI-001 and PB-API-002 remain open; these new findings do not close them.

## Resolution Policy

When an external ticket is created, add its URL/ID to the matching record. Update status and owner as triage proceeds. Close a record only after the defect is fixed or explicitly accepted, its expected behavior is documented, and the focused test plus relevant regression checks pass. If a test is quarantined, record the linked ticket, owner, reason, and expiry/review date here.
## Local UI application repair follow-up (2026-10-07)

The 42 failing UI invocations were traced to PB-UI-002 (including the earlier PB-UI-001 Overview case), PB-SEC-001/002/003, PB-UI-003 and PB-A11Y-001. The explicit `-UiFixes` deployment applies reviewed source replacements under application/ui-fixes; details and fresh evidence are in UI_FAILURE_ANALYSIS.md. The original upstream deployment remains available and upstream/external adoption is pending. Recovery identity comparison addresses the six mismatch scenarios, while plaintext successful recovery remains an open production limitation. UI page ownership checks do not close REST authentication findings. API financial/profile/concurrency findings remain open. No assertions or scenarios were removed.
