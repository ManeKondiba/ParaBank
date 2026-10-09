# ParaBank page and validation audit

Implementation follow-up: see [VALIDATION_IMPLEMENTATION.md](VALIDATION_IMPLEMENTATION.md) for the current changes and verification. Contact Us, About, Services, News and Site Map were explicitly excluded by the user; the historical audit below retains their gap descriptions for context only.

Date: 2026-10-07. Reviewed framework revision: a20c4e0 plus the local defect-register changes. Application reference: pinned ParaBank source 98c1c9ab4889eb92c7798da63bb417e27261d3d5.

## Review result and scope

The framework has good coverage of normal customer banking workflows and empty-field errors. It does not yet provide complete validation coverage of the application. The highest-priority gaps are access control, financial boundary inputs, duplicate submissions, and complete ledger checks in the UI layer. Five public page areas have no dedicated tests.

This is a source and existing-evidence audit, not a new live browser run or penetration test. Reviewed every existing UI test class and page object, the API business tests and assertion helpers, utility/infrastructure scenario inventory, suite selection, GitHub workflow, and the pinned application's JSP page inventory, validators, database field limits and protected-route configuration. Missing tests are coverage gaps; they do not prove that the application fails. No test assertions, suites, application code or patches were changed during this audit.

Existing unpatched local UI evidence: 93 invocations, 92 passed, one failed, zero skips, in both one-worker and two-worker runs. The failure is PB-UI-001 (Overview after logout displays an internal error). See [defect register](DEFECT_REGISTER.md) and local target/regression-baseline/sequential/ and parallel-two/. Passing API tests do not resolve this UI defect.

## Validation basis

Use the pinned application's messages and configuration for existing behavior, with equivalence classes, boundary values, decision tables and state transitions to design additional cases. OWASP's [Web Security Testing Guide](https://wstg.owasp.org/stable/4-Web_Application_Security_Testing/) supplies reference areas for authentication, authorization, sessions, input validation, error handling and business logic. The [OWASP authorization automation guidance](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Testing_Automation_Cheat_Sheet.html) supports a user/resource/action matrix. Accessibility checks should use the [W3C WCAG 2.2 quick reference](https://www.w3.org/WAI/WCAG22/quickref/), including keyboard access, visible focus, labels and error identification.

These references guide the proposed checks; this report is not a compliance certification. ParaBank is a demo. Do not assume it implements production banking rules. Confirm overdraft policy, currency precision, payment limits, password rules, recovery identity verification and duplicate-request policy before asserting a particular business decision.

Priorities: P1 = money integrity, customer-data isolation, authentication or a weak assertion of those outcomes; P2 = functional completeness, input boundaries and recovery; P3 = lower-risk content checks. Accessibility priority should be raised where users are blocked.

## Page-by-page coverage and required validations

Counts below are UI invocations with the built-in data providers, not the number of Java methods. Transaction Details is exercised indirectly; its count is included in Account Details and Find Transactions.

| Page / route | Current coverage | Required or missing validation | Priority / evidence |
| --- | --- | --- | --- |
| Home / index.htm; shared header, sidebar and footer | Navigation used by fixtures; no dedicated home test | Logged-in/logged-out navigation matrix; logo/home links; all internal destinations; page title and main content; unexpected error banners; news links; external-link target and URL checks without making third-party availability a blocking gate | P2; HomePage only exposes Register and Open New Account actions |
| Registration / register.htm and confirmation | 18: success through AccountFixture, all ten required fields together and individually, correction, masking, mismatch, duplicate username, optional phone | Stored profile values and initial account ownership after registration; rejected submissions create no customer; duplicate attempt preserves original credentials/profile; field length boundaries; spaces-only and trim policy; Unicode/apostrophes; markup displayed safely; success session and subsequent login | P1/P2; RegistrationTest, RegistrationPage, AccountFixture |
| Login / login.htm, loginForm.htm | 14: valid login, wrong password, masked password, Enter submission, corrected credentials, missing-field correction, logout/relogin, five DDT rows | Assert authenticated identity and owned account IDs rather than only Overview heading; credential case/space/Unicode policy; session expiry and rotation; failed login cannot access protected URLs; repeated-attempt policy if supported; secrets absent from URLs/artifacts | P1/P2; LoginTest, LoginDDTtest, LoginPage |
| Logout / logout.htm | 3: logout UI, refresh, protected Overview revisit; last case currently fails | Every protected route after logout; browser Back and new tab; server-side session invalidation; no cached sensitive data; fresh signed-out session; login again with correct customer identity | P1; LogoutTest; PB-UI-001 remains open |
| Open New Account / openaccount.htm and confirmation | 5: checking/savings, options/default, newly created funding source, refresh/relogin persistence, selected source debit in chained scenario | Both types add exactly one account; both opening-transfer ledgers and exact source/destination balances; other accounts unchanged; funding-account dropdown equals owned accounts; invalid/foreign funding source and type; insufficient-funds policy; double-click/refresh/back behavior | P1; OpenAccountTest; API checks cover several creation invariants but do not prove UI submission behavior |
| Accounts Overview / overview.htm | 2: initial account/details agree; two-account IDs and summed total | Identity/ownership in two-user sessions; duplicate IDs; all available balances for multiple accounts; refreshed totals after transfer/payment/loan; negative-balance display under approved overdraft policy; empty/error/loading states if supported | P1/P2; AccountsOverviewTest; getAccountIds waits for nonempty list, so an empty state needs another accessor |
| Account Details / activity.htm | 4: fresh empty history; opening credit/details; debit and credit type filters | Month and month/type combinations; exact result set and no duplicates; account switching; missing/malformed/foreign ID; persisted balances after login; multiple mixed transactions; service-error display without stale activity | P1/P2; AccountDetailsTest; current type-filter fixtures have only one direction per selected account |
| Transaction Details / transaction.htm | Indirect: opening-credit fields and transfer transaction ID/date during search | Debit, bill-payment and loan details; all fields agree with selected ledger record; timezone-aware date equality rather than parseability alone; missing/malformed/foreign ID; signed-out access; return navigation | P1/P2; TransactionDetailsPage, AccountDetailsTest, FindTransactionsTest |
| Transfer Funds / transfer.htm and result/error containers | 4: 1.00 and 12.34 confirmation, both balances, conserved total, debit/credit existence; blank/nonnumeric amounts preserve balances | Zero/negative/minimum-cent/excess precision/large/full-balance/over-balance cases; same-account policy; dropdown ownership; exactly one new entry per account, old entries preserved and no new entries on rejection; reverse transfer; duplicate submission and ambiguous timeout recovery | P1; TransferFundsTest; positive ledger checks use anyMatch; invalid cases do not compare ledgers |
| Bill Pay / billpay.htm and result/error containers | 6: confirmation and source debit, nine empty-field errors, mismatched account confirmation, three nonnumeric cases | Each required field independently; corrected submission; successful exact debit ledger/payee; rejection leaves ledger unchanged; multiple owned funding accounts; zero/negative/precision/over-balance; partial numeric strings, decimals and overflow in account number; leading-zero policy; repeated submit/error recovery | P1/P2; BillPayTest; API BILL-001 has exact ledger checks, UI does not |
| Find Transactions / findtrans.htm; transactionResults.jsp / transactions.jsp fragments | 13: own transfer found by ID/date/range/amount; eight invalid criteria; one empty amount search | All-and-only expected set and duplicates for date/range/amount; inclusive same-day endpoints; reversed range; leap day/impossible dates; no matches for every search type; repeated searches clear stale rows; foreign account/transaction IDs; amount boundaries; results links fully match details | P1/P2; FindTransactionsTest; UI date range currently spans yesterday through tomorrow, not endpoint equality |
| Update Contact Info / updateprofile.htm and confirmation | 11: initial seven values, required fields together/individually, correction, persistence after new login, optional phone removal | Individually rejected field leaves persisted profile unchanged; no-op/partial change preserves every untouched field; length/space/Unicode policy; source/API field equality; stored markup rendered safely; foreign customer mutation denied; credentials/account balances/ledger invariant on rejected update | P1/P2; UpdateContactInfoTest; combined-empty test already checks saved values, individual cases only check message |
| Request Loan / requestloan.htm and result/error containers | 3: funding list, decision/state consistency, excessive down payment denied without balance/account change | Deterministic UI approval and denial under pinned configuration; empty/nonnumeric/zero/negative/precision/overflow inputs for amount and down payment; down payment > amount and threshold boundaries; no ledger mutation on denial; exact approved debit ledger; other funding source; duplicates; meaningful service-error recovery | P1; RequestLoanTest accepts either approval or supported denial in normal scenario; API has deterministic approval but UI can miss an always-deny regression |
| Forgot Login Info / lookup.htm and lookupConfirm.jsp | 10: seven required fields individually/together, unknown customer, own recovered credentials and signed-in session | Incorrect identity field with otherwise valid customer SSN, one at a time; two-customer identity isolation; correction after error; lengths/spaces; privacy and enumeration behavior; secure recovery policy rather than plaintext password disclosure | P1/P2; CustomerLookupTest; current positive test intentionally verifies demo plaintext credential recovery, which does not validate production-safe recovery |
| Contact Us / contact.htm and contactConfirm.jsp | No dedicated page object or test | Correct form and fields; all required fields together and individually (name/email/phone/message); successful confirmation matching requester; correction; whitespace and limits; email/phone syntax policy; safe message rendering; service failure; keyboard and labels | P2; pinned ContactFormValidator checks empty values only; syntax rules need requirements |
| About Us / about.htm | No dedicated test | Title/heading and expected core content; navigation; links and images; logged-in and signed-out access | P3; view-controller and about.jsp |
| Services / services.htm | No dedicated test | Heading, service inventory, internal links and documented endpoint link targets; selected services load; no internal error; avoid claiming linked API availability from text alone | P2/P3; services.jsp |
| News / news.htm | No dedicated test | Home article link opens matching headline/date/story; available article IDs; missing/malformed ID handled; safe content and return navigation | P3; NewsController and news.jsp |
| Site Map / sitemap.htm | No dedicated test | Every listed internal link navigates correctly; protected links follow authentication rules; labels/destinations agree; content remains usable by keyboard | P2; sitemap.jsp includes public and account navigation |
| Generic errors / error.jsp, loginform.jsp | Auth/error checks incidentally exercised | Friendly controlled error, no stack trace or secrets, no stale financial data, usable recovery link; malformed/nonexistent pages and service failures | P2; known Overview internal error must remain tracked rather than accepted as success |
| Administration / admin.htm, database/reset/connection operations | Startup scripts configure an owned local environment; no UI admin suite | Explicit environment/admin access policy; unauthorized access and requests rejected according to agreed deployment model; configuration readback; admin functional changes only in a separate disposable instance | P1 for access; out of ordinary customer regression because reset changes shared state |
| Echo / echo.htm or echo.jsp; service proxy / services_proxy/* | No dedicated echo validation; proxy used implicitly by banking UI | Determine whether diagnostic echo is intended to be exposed; content/secret handling; proxy authenticated ownership and malformed request handling; admin/diagnostic exposure review | P1/P2; deployment-specific scope, not an assumed customer feature |
| Confirmation templates | Covered through parent workflow where noted; legacy *Confirm JSP existence alone does not prove an active route | Confirm actual route/template used before adding separate page tests; success messages plus persisted business outcome, not template presence | P2; avoid double-counting result containers as independently tested pages |

## API layer: current validation and gaps

| Area | Existing checks | Missing or incomplete validation |
| --- | --- | --- |
| Customer / login | Identity/profile equality, wrong password/unknown user, missing/malformed ID, credential/profile update and old credential rejection, account listing owner fields | Missing/space/long credentials, invalid profile fields, duplicate username update, failed-update atomicity; authorization against another customer's read/update. Listing two customers correctly is data-scoping coverage, not proof that customer B is forbidden to request customer A's ID |
| Accounts | Checking/savings creation, exact ownership/ID/list checks, fixture opening-transfer reconciliation, missing resource, noninteger type rejected without mutation, XML/JSON parity | Out-of-range integer type, missing/foreign source/customer, insufficient funding policy, precision limits, duplicate/replayed creation and atomicity |
| Deposit / withdrawal / transfer | Minimum-cent positive amounts, large deposit, exact balances, exact new ledger entry, transfer conservation, malformed amounts unchanged, nonexistent destination preserves source | Zero/negative/overprecision/overflow/missing amounts and IDs, full-balance and over-balance policy, same-account transfer, invalid source, real authorization model, concurrent same-account operations and uncertain-response recovery |
| Bill Pay | Result schema and exact debit/payee ledger; malformed JSON leaves balances/ledger unchanged; unsupported media type | Missing/invalid payee fields and valid JSON with wrong types; amount boundaries, missing/foreign funding account, duplicate policy; actionable error contract. PB-API-002 remains open because malformed JSON returns HTTP 500, although the no-mutation test passes |
| Loans | Deterministic approval with created account/down-payment ledger, denial with unchanged accounts/ledger, malformed amount no mutation | Approval threshold just below/at/above, missing/nonnumeric down payment, zero/negative/overflow, invalid/foreign customer/source, duplicate policy; loan account's credit ledger if required |
| Transaction queries | Full DTO equality, all-and-only results with duplicate detection for amount/date/range/month/type, same-day inclusive range, reversed range empty, nonexistent transaction | Missing/malformed IDs/date/type/month, more than one distinct transaction date/year, impossible date/leap day route behavior, direct customer authorization |
| OpenAPI / schemas | Core 17-path method inventory, API/OpenAPI versions, schema validation through shared response helpers, representative XML parity | Required parameter names/locations/types, request bodies, documented response codes/content/schema refs, security requirements; extra unreviewed operations. Inventory only checks expected paths exist; it does not validate the full contract or exclude admin/other APIs |
| Financial failure atomicity | Several malformed-input and invalid-destination tests check persisted balances/ledger | Error bodies and safe diagnostics; behavior when the first step succeeds and later processing fails; duplicate network delivery. Transport unit tests prove the client sends once, not server-side idempotency |
| API parallel execution | api.xml explicitly uses parallel=none | No evidence that API class methods are safe in parallel: ApiBaseTest uses mutable per-instance fixture/client fields. Retain serial mode until lifecycle isolation and concurrent shared-account tests are designed |

The REST demo's access model must be characterized separately from the browser session model. Do not invent a 401/403 expectation for endpoints documented without authentication; record the security gap against an agreed deployment requirement. Core API coverage excludes administration, stocks/positions and other routes outside the reviewed manifest; these need a scope decision before claiming whole-API coverage.

## Concrete assertion weaknesses

1. Login positives prove that the Overview heading is visible; they do not independently prove the correct customer identity and account ownership.
2. Transfer UI uses existence checks for new matching debit/credit rows. Extra new transactions, modified old rows or unexpected entries are not checked directly. Reuse the API-style before/after ledger comparison through an appropriate UI assertion helper.
3. UI bill payment checks confirmation and balance but never verifies the corresponding ledger entry. Negative UI payment/transfer cases check balances, not ledger immutability.
4. The normal UI loan test accepts either decision. That is useful for configurable deployments but cannot prove the pinned expected approval branch ran. Add a separate pinned-config approval test without weakening the flexible test.
5. UI loan denial checks account IDs and source balance, not all ledger entries. A harmless-looking balance could hide erroneous records.
6. Registration fixture success is asserted inside AccountFixture; testRegistration is not assertion-free. However, welcome text alone does not prove complete persisted profile/ownership.
7. Find Transactions verifies presence plus per-row criteria; it does not establish the complete expected set for non-ID searches. API tests already use set equality and duplicate detection; do not classify those API checks as missing.
8. Transaction date parseability is weaker than agreement with the ledger's transaction instant/day under a defined timezone. Avoid raw equality between browser-local and server-rendered dates.
9. Registration's negative cases and single-field profile failures do not all verify persisted state. Visible errors alone do not prove writes were prevented.
10. Helpers validate numeric account/transaction IDs before navigation. For malformed-ID UI tests, use a deliberate direct-navigation helper; otherwise the framework rejects the test data before the application is exercised. Loan apply accepts BigDecimal, so empty/nonnumeric loan inputs require a raw-input page action.

## Prioritized validation backlog

These are proposed case families, not confirmed application defects or an invented coverage percentage. Requirements-dependent rows need a documented expected result first.

| ID | Priority | Add validation | Expected evidence / decision needed |
| --- | --- | --- | --- |
| VAL-001 | P1 | Anonymous and logged-out access to all nine protected UI routes | Login required; no account/transaction data; no financial mutation. Keep PB-UI-001 open until fixed |
| VAL-002 | P1 | Two-user account/transaction reads and profile/money writes | Explicit actor-resource-action matrix; test customer A/B sessions and foreign IDs on owned local deployment |
| VAL-003 | P1 | UI transfer exact ledger delta, old-row preservation and rejection ledger equality | Exactly one debit and one credit for one accepted submission; unchanged on rejected input |
| VAL-004 | P1 | UI bill-payment success/rejection ledger reconciliation | Correct account, amount, direction, payee and exact count; unchanged rejected state |
| VAL-005 | P1 | Transfer/payment/withdraw/deposit amount boundaries | Table: blank, malformed, 0, -0.01, 0.01, configured precision, excess precision, full balance, +0.01 over balance, huge value; agree overdraft/rounding policy |
| VAL-006 | P1 | Same-account transfer and invalid/foreign source/destination | Agree business rule; preserve total and ledger consistency even if supported |
| VAL-007 | P1 | Double click, Enter+click, refresh/back and duplicate API delivery | Define duplicate-operation policy; one user action must not accidentally produce multiple unintended effects |
| VAL-008 | P1 | Deterministic UI loan approval and denied-loan ledger invariants | Pin processor/settings; exact account delta and source ledger; no side effects on denial |
| VAL-009 | P1 | Loan amount/down-payment boundaries and threshold decision table | Invalid input cannot create accounts or debit; threshold decision independently computed from approved policy |
| VAL-010 | P1 | Opening-account exact account and ledger delta for both types and alternate source | New owned ID; opening funds conserved; unrelated accounts/ledgers unchanged |
| VAL-011 | P1 | Customer recovery mismatch fields and two-user identity verification | Correct SSN with wrong name/address must follow agreed identity policy; characterize demo behavior and track product gaps |
| VAL-012 | P1 | Session expiry, logout invalidation, back/cache/new-tab | No sensitive data/action after invalidation; bounded session lifecycle per configured policy |
| VAL-013 | P1 | Concurrent money operations on the same owned account | Final balance equals accepted operations; no lost updates/duplicate IDs/partial transfer; run separately from independent-fixture parallel regression |
| VAL-014 | P2 | Registration field limits and rejected-create persistence | Boundary fixtures; no customer on rejection, friendly errors and no truncated values; verified limits below |
| VAL-015 | P2 | Duplicate registration preserves original customer and login | No replacement profile/password/account, no misleading success |
| VAL-016 | P2 | Profile single-field rejection, partial/no-op updates | Persisted unchanged/untouched fields, credentials and accounts remain consistent |
| VAL-017 | P2 | Whitespace, Unicode/apostrophe, safe markup across text fields | Agreed trimming/allowed-character rules; stored values round-trip; no executable rendering |
| VAL-018 | P2 | Bill Pay each missing field and correction after each error | Specific message; form remains usable; no prior debit; corrected submission has exactly one debit |
| VAL-019 | P2 | Partial numeric and overflow account/amount strings | E.g. 12abc, 1.5, 1e3, Infinity, huge integer; server rejects unsupported representations without mutation. Page parseFloat accepts numeric prefixes, so plain alphabetic cases are insufficient |
| VAL-020 | P2 | UI search exact result set, duplicates, endpoint dates and repeated searches | All-and-only expected records; no stale previous results; invalid/impossible and reversed date behavior defined |
| VAL-021 | P2 | Activity month/type combinations and debit/bill/loan transaction details | Known mixed ledger fixtures; exact filtered set and detail equality |
| VAL-022 | P2 | Contact Us complete form validations | Required fields, correction and confirmation; email syntax/phone format policy confirmed |
| VAL-023 | P2 | Navigation inventory and Site Map; session-specific menu | Every internal customer/public destination mapped; protected navigation verified |
| VAL-024 | P2 | Friendly service errors and recoverable forms | No misleading success/stale balances, safe error and usable retry; ambiguous write resolved by querying state before retry |
| VAL-025 | P2 | Accessibility across all forms/pages | Keyboard-only workflows, focus order/visibility, accessible names/labels, errors programmatically tied to inputs, status announcements, zoom/reflow and contrast; manual checks supplement automation |
| VAL-026 | P2 | Browser regression beyond seven smoke cases | Critical negative fields, async search/profile/loan pages in Chrome/Edge/Firefox; specify supported viewport matrix |
| VAL-027 | P2 | Full UI CI and expected scenario inventory | Add explicit full-regression job with retained artifacts and count/skip checks after defect disposition; manual smoke cannot substitute for all pages |
| VAL-028 | P2 | OpenAPI parameter/body/response/security contract | Compare contract structure, not only path presence; define excluded routes |
| VAL-029 | P2 | Error response schemas and safe data handling | Error text/media/status contract; no passwords/SSNs in screenshots/report parameters/console/history under agreed policy |
| VAL-030 | P2 | Repeated performance measurements | Same deployment/fixture/browser/resources; at least several repeated baseline and candidate runs; include provisioning/build time separately; no SLA threshold without agreement |
| VAL-031 | P3 | About/Services/News static content checks | Correct expected heading/content, article identity and internal links/images |
| VAL-032 | P1/P2 | Administration/diagnostic exposure review | Deployment owner defines intended access; isolated admin suite for allowed changes; diagnostic route must not leak secrets |

### Verified field-boundary reference

The pinned Customer table has first/last name 30 characters, street 45, city/state/zip/phone/username/password 20, SSN 15. Test valid maximum and one above maximum, plus empty and whitespace cases. These database limits are observed implementation constraints, not a password-strength or international-address requirement. Current CustomerValidator uses rejectIfEmpty, not whitespace-only rejection; the Contact validator likewise checks presence rather than email syntax. Test and report the actual outcome before filing a defect against an agreed stronger requirement. Money storage is DECIMAL(19,4), while UI commonly displays two decimals; rounding/precision requirements must be specified rather than assumed.

## Framework, suites and reporting gaps

- Current deterministic unit inventory is 22 methods: ten in UtilityUnitTest and twelve in ApiInfrastructureTest. README still says 21 in several places; its statement that this update's hosted workflow has not run also conflicts with prior successful hosted evidence. Update documentation against retained run artifacts separately.
- API infrastructure units cover unsafe URL/timeout/fixture guards, wire parameter encoding, redaction, decimal precision, send-once transport failure, no redirect replay, timezone/leap-day conversion, ledger delta correctness and safe assertion messages. These are framework checks, not page behavior or security certification.
- CrossBrowserTesting.xml selects Sanity and BankingSmoke only: seven per browser, 21 invocations. Open Account, search, update, loan and recovery regressions are not included there.
- GitHub workflow runs unit + API smoke on push/PR and API regression on schedule. UI smoke and nine-case hybrid subset require manual dispatch; no full 93-case UI job or hosted cross-browser job is defined.
- Fresh browser per invocation, unique credentials, ThreadLocal WebDriver, explicit waits and no automatic retry are good isolation mechanisms. ProvisioningLock serializes creation in one JVM only; it does not protect across jobs/processes or prove database concurrency correctness.
- No scenario-to-requirement IDs for most UI methods, no accepted full-page denominator, no approved financial decision tables, no skip/count gate, and no automated accessibility/load suite were found. Avoid claiming a percentage of requirements covered from the 93 count.
- Existing screenshots/diagnostics and API evidence are useful; test-only credentials can still appear in TestNG data-provider parameters and demo recovery pages. Audit artifact handling with synthetic data and validate redaction on negative cases.

## Recommended implementation order

1. Agree money, identity, access and session requirements; document expected behavior for each decision table. Link the known logout failure to the application owner.
2. Strengthen existing assertions first: VAL-003/004/008/010/015/016. These improve detection without adding many browser launches.
3. Add money and access boundaries at the API layer with persisted-state checks; keep representative UI checks for input mapping, messages and prevention of accidental duplicate clicks. API provisioning should not bypass the UI business action under test.
4. Add missing Contact/navigation pages, search/activity boundaries and input correction cases; use data providers with independent fixtures.
5. Add broader cross-browser coverage, accessibility/manual verification and full UI CI. Track known defects explicitly; do not change assertions to match an internal error simply to obtain a green build.
6. Establish repeated execution-time measurements after the new scope is stable. Keep browser/UI and API coverage separately traceable.

Completion for each new case: requirement/reference and priority, reproducible synthetic data, asserted UI result plus relevant persisted state, exact affected/unaffected account/ledger invariants, no silent skip/retry, isolated execution, source-linked report evidence, and a defect record for any requirement-backed failure.

## Existing scenario inventory

Every existing @Test method is listed below with its source location. UI invocation counts/statuses come from the retained two-worker unpatched run; API and unit methods were source-reviewed in this audit and were not rerun. Data-provider rows are represented by their method plus the number of UI invocations. This inventory is intended to make the page/gap review traceable, not to claim every proposed boundary has already been tested.
| Class / source | Scenario method | Existing UI invocations / outcome |
| --- | --- | --- |
| [AccountDetailsTest](src/test/java/TestClases/AccountDetailsTest.java#L22) | `testFreshAccountHasNoTransactionHistory` | 1: 1 pass, 0 fail, 0 skip |
| [AccountDetailsTest](src/test/java/TestClases/AccountDetailsTest.java#L35) | `testOpeningDepositLinksToMatchingTransactionDetails` | 1: 1 pass, 0 fail, 0 skip |
| [AccountDetailsTest](src/test/java/TestClases/AccountDetailsTest.java#L62) | `testActivityTypeFilter` | 2: 2 pass, 0 fail, 0 skip |
| [AccountsOverviewTest](src/test/java/TestClases/AccountsOverviewTest.java#L15) | `testInitialAccountAndDetailsAgree` | 1: 1 pass, 0 fail, 0 skip |
| [AccountsOverviewTest](src/test/java/TestClases/AccountsOverviewTest.java#L35) | `testOverviewIncludesBothAccountsAndCorrectTotal` | 1: 1 pass, 0 fail, 0 skip |
| [AccountsApiTest](src/test/java/TestClases/api/AccountsApiTest.java#L26) | `createAccountAndReadBack` | Source reviewed; not rerun |
| [AccountsApiTest](src/test/java/TestClases/api/AccountsApiTest.java#L48) | `accountDetailsMatchList` | Source reviewed; not rerun |
| [AccountsApiTest](src/test/java/TestClases/api/AccountsApiTest.java#L59) | `missingAccount` | Source reviewed; not rerun |
| [AccountsApiTest](src/test/java/TestClases/api/AccountsApiTest.java#L64) | `malformedAccountTypePreservesFinancialState` | Source reviewed; not rerun |
| [AccountsApiTest](src/test/java/TestClases/api/AccountsApiTest.java#L82) | `xmlAccountRepresentationMatchesJson` | Source reviewed; not rerun |
| [ApiInfrastructureTest](src/test/java/TestClases/api/ApiInfrastructureTest.java#L51) | `configurationRejectsUnsafeUrlsWithoutEchoingSecrets` | Source reviewed; not rerun |
| [ApiInfrastructureTest](src/test/java/TestClases/api/ApiInfrastructureTest.java#L63) | `configurationRejectsTimeoutOverflowAndIncompleteFileFixtures` | Source reviewed; not rerun |
| [ApiInfrastructureTest](src/test/java/TestClases/api/ApiInfrastructureTest.java#L80) | `fixtureGuardRejectsSharedDemoHostnameVariants` | Source reviewed; not rerun |
| [ApiInfrastructureTest](src/test/java/TestClases/api/ApiInfrastructureTest.java#L90) | `redactionKeepsDiagnosticMoneyWhileRemovingNestedProfileAndTextSecrets` | Source reviewed; not rerun |
| [ApiInfrastructureTest](src/test/java/TestClases/api/ApiInfrastructureTest.java#L112) | `clientsEncodeCredentialsAndPutWriteParametersInTheirContractLocations` | Source reviewed; not rerun |
| [ApiInfrastructureTest](src/test/java/TestClases/api/ApiInfrastructureTest.java#L168) | `jsonPathAndDtoRetainDecimalPrecision` | Source reviewed; not rerun |
| [ApiInfrastructureTest](src/test/java/TestClases/api/ApiInfrastructureTest.java#L183) | `ambiguousPostIsSentExactlyOnceAndHasTransportEvidence` | Source reviewed; not rerun |
| [ApiInfrastructureTest](src/test/java/TestClases/api/ApiInfrastructureTest.java#L203) | `credentialTransportFailureCannotLeakTheRawLoginUri` | Source reviewed; not rerun |
| [ApiInfrastructureTest](src/test/java/TestClases/api/ApiInfrastructureTest.java#L222) | `redirectsAreReturnedWithoutReplayingAWrite` | Source reviewed; not rerun |
| [ApiInfrastructureTest](src/test/java/TestClases/api/ApiInfrastructureTest.java#L239) | `datesUseTheServerTimezoneAcrossMidnightAndLeapDay` | Source reviewed; not rerun |
| [ApiInfrastructureTest](src/test/java/TestClases/api/ApiInfrastructureTest.java#L250) | `ledgerComparisonIgnoresOrderButRejectsMissingDuplicateAndExtraEntries` | Source reviewed; not rerun |
| [ApiInfrastructureTest](src/test/java/TestClases/api/ApiInfrastructureTest.java#L278) | `assertionFailuresCannotPrintPayeeNamesFromTransactionOrBillPaymentModels` | Source reviewed; not rerun |
| [ApiOpenApiContractTest](src/test/java/TestClases/api/ApiOpenApiContractTest.java#L18) | `deployedOpenApiMatchesCoreOperationManifest` | Source reviewed; not rerun |
| [BankingJourneyApiTest](src/test/java/TestClases/api/BankingJourneyApiTest.java#L24) | `completeBankingJourney` | Source reviewed; not rerun |
| [BillPayApiTest](src/test/java/TestClases/api/BillPayApiTest.java#L20) | `billPaymentReconcilesBalanceAndLedger` | Source reviewed; not rerun |
| [BillPayApiTest](src/test/java/TestClases/api/BillPayApiTest.java#L39) | `malformedPayeeDoesNotDebitAccount` | Source reviewed; not rerun |
| [BillPayApiTest](src/test/java/TestClases/api/BillPayApiTest.java#L67) | `unsupportedMediaTypeDoesNotDebitAccount` | Source reviewed; not rerun |
| [CustomerApiTest](src/test/java/TestClases/api/CustomerApiTest.java#L19) | `validLoginAndCustomerRead` | Source reviewed; not rerun |
| [CustomerApiTest](src/test/java/TestClases/api/CustomerApiTest.java#L34) | `invalidPassword` | Source reviewed; not rerun |
| [CustomerApiTest](src/test/java/TestClases/api/CustomerApiTest.java#L41) | `unknownUsername` | Source reviewed; not rerun |
| [CustomerApiTest](src/test/java/TestClases/api/CustomerApiTest.java#L47) | `missingCustomer` | Source reviewed; not rerun |
| [CustomerApiTest](src/test/java/TestClases/api/CustomerApiTest.java#L52) | `accountListingsRemainScopedToTheirOwners` | Source reviewed; not rerun |
| [CustomerApiTest](src/test/java/TestClases/api/CustomerApiTest.java#L65) | `profileAndCredentialUpdate` | Source reviewed; not rerun |
| [CustomerApiTest](src/test/java/TestClases/api/CustomerApiTest.java#L102) | `malformedCustomerIdPreservesProfile` | Source reviewed; not rerun |
| [LoansApiTest](src/test/java/TestClases/api/LoansApiTest.java#L23) | `approvedLoanCreatesAccountAndDebitsDownPayment` | Source reviewed; not rerun |
| [LoansApiTest](src/test/java/TestClases/api/LoansApiTest.java#L52) | `deniedLoanPreservesAccountsAndLedger` | Source reviewed; not rerun |
| [LoansApiTest](src/test/java/TestClases/api/LoansApiTest.java#L76) | `malformedLoanAmountPreservesFinancialState` | Source reviewed; not rerun |
| [MoneyMovementApiTest](src/test/java/TestClases/api/MoneyMovementApiTest.java#L25) | `depositCreditsBalanceAndLedger` | Source reviewed; not rerun |
| [MoneyMovementApiTest](src/test/java/TestClases/api/MoneyMovementApiTest.java#L44) | `withdrawalDebitsBalanceAndLedger` | Source reviewed; not rerun |
| [MoneyMovementApiTest](src/test/java/TestClases/api/MoneyMovementApiTest.java#L64) | `transferConservesFundsAndReconcilesBothLedgers` | Source reviewed; not rerun |
| [MoneyMovementApiTest](src/test/java/TestClases/api/MoneyMovementApiTest.java#L91) | `malformedAmountDoesNotChangeFinancialState` | Source reviewed; not rerun |
| [MoneyMovementApiTest](src/test/java/TestClases/api/MoneyMovementApiTest.java#L113) | `invalidTransferDestinationPreservesSourceState` | Source reviewed; not rerun |
| [TransactionsApiTest](src/test/java/TestClases/api/TransactionsApiTest.java#L31) | `transactionDetailsMatchLedger` | Source reviewed; not rerun |
| [TransactionsApiTest](src/test/java/TestClases/api/TransactionsApiTest.java#L40) | `searchByAmount` | Source reviewed; not rerun |
| [TransactionsApiTest](src/test/java/TestClases/api/TransactionsApiTest.java#L53) | `searchOnServerDate` | Source reviewed; not rerun |
| [TransactionsApiTest](src/test/java/TestClases/api/TransactionsApiTest.java#L66) | `searchInclusiveDateRange` | Source reviewed; not rerun |
| [TransactionsApiTest](src/test/java/TestClases/api/TransactionsApiTest.java#L82) | `searchByMonthAndType` | Source reviewed; not rerun |
| [TransactionsApiTest](src/test/java/TestClases/api/TransactionsApiTest.java#L102) | `missingTransaction` | Source reviewed; not rerun |
| [BillPayTest](src/test/java/TestClases/BillPayTest.java#L19) | `testPaymentConfirmationAndFundingAccountDebit` | 1: 1 pass, 0 fail, 0 skip |
| [BillPayTest](src/test/java/TestClases/BillPayTest.java#L47) | `testEmptyPaymentShowsRequiredFieldsWithoutDebitingAccount` | 1: 1 pass, 0 fail, 0 skip |
| [BillPayTest](src/test/java/TestClases/BillPayTest.java#L77) | `testMismatchedAccountConfirmationDoesNotDebitAccount` | 1: 1 pass, 0 fail, 0 skip |
| [BillPayTest](src/test/java/TestClases/BillPayTest.java#L93) | `testNonnumericPaymentFieldsDoNotDebitAccount` | 3: 3 pass, 0 fail, 0 skip |
| [CustomerLookupTest](src/test/java/TestClases/CustomerLookupTest.java#L30) | `testAllRequiredLookupFields` | 1: 1 pass, 0 fail, 0 skip |
| [CustomerLookupTest](src/test/java/TestClases/CustomerLookupTest.java#L42) | `testEachRequiredLookupField` | 7: 7 pass, 0 fail, 0 skip |
| [CustomerLookupTest](src/test/java/TestClases/CustomerLookupTest.java#L54) | `testUnknownCustomerIsRejected` | 1: 1 pass, 0 fail, 0 skip |
| [CustomerLookupTest](src/test/java/TestClases/CustomerLookupTest.java#L66) | `testCustomerCanRecoverOwnLoginInformation` | 1: 1 pass, 0 fail, 0 skip |
| [FindTransactionsTest](src/test/java/TestClases/FindTransactionsTest.java#L32) | `testFindOwnTransfer` | 4: 4 pass, 0 fail, 0 skip |
| [FindTransactionsTest](src/test/java/TestClases/FindTransactionsTest.java#L102) | `testInvalidSearchCriteria` | 8: 8 pass, 0 fail, 0 skip |
| [FindTransactionsTest](src/test/java/TestClases/FindTransactionsTest.java#L115) | `testSearchWithNoMatchesReturnsEmptyResults` | 1: 1 pass, 0 fail, 0 skip |
| [LoginDDTtest](src/test/java/TestClases/LoginDDTtest.java#L15) | `loginDDTest` | 5: 5 pass, 0 fail, 0 skip |
| [LoginTest](src/test/java/TestClases/LoginTest.java#L15) | `testValidLogin` | 1: 1 pass, 0 fail, 0 skip |
| [LoginTest](src/test/java/TestClases/LoginTest.java#L25) | `testWrongPassword` | 1: 1 pass, 0 fail, 0 skip |
| [LoginTest](src/test/java/TestClases/LoginTest.java#L36) | `testPasswordIsMasked` | 1: 1 pass, 0 fail, 0 skip |
| [LoginTest](src/test/java/TestClases/LoginTest.java#L45) | `testLoginWithEnterKey` | 1: 1 pass, 0 fail, 0 skip |
| [LoginTest](src/test/java/TestClases/LoginTest.java#L57) | `testLoginAfterCorrectingWrongPassword` | 1: 1 pass, 0 fail, 0 skip |
| [LoginTest](src/test/java/TestClases/LoginTest.java#L81) | `testLoginAfterProvidingMissingCredentials` | 3: 3 pass, 0 fail, 0 skip |
| [LoginTest](src/test/java/TestClases/LoginTest.java#L99) | `testLoginAgainAfterLogout` | 1: 1 pass, 0 fail, 0 skip |
| [LogoutTest](src/test/java/TestClases/LogoutTest.java#L14) | `testLogout` | 1: 1 pass, 0 fail, 0 skip |
| [LogoutTest](src/test/java/TestClases/LogoutTest.java#L24) | `testLogoutRemainsEffectiveAfterRefresh` | 1: 1 pass, 0 fail, 0 skip |
| [LogoutTest](src/test/java/TestClases/LogoutTest.java#L35) | `testLoggedOutUserCannotRevisitAccountsOverview` | 1: 0 pass, 1 fail, 0 skip |
| [OpenAccountTest](src/test/java/TestClases/OpenAccountTest.java#L28) | `testOpenAccount` | 2: 2 pass, 0 fail, 0 skip |
| [OpenAccountTest](src/test/java/TestClases/OpenAccountTest.java#L46) | `testAccountTypeOptionsAndDefaultFundingAccount` | 1: 1 pass, 0 fail, 0 skip |
| [OpenAccountTest](src/test/java/TestClases/OpenAccountTest.java#L60) | `testNewAccountCanFundAnotherAccount` | 1: 1 pass, 0 fail, 0 skip |
| [OpenAccountTest](src/test/java/TestClases/OpenAccountTest.java#L93) | `testNewAccountPersistsAfterLoginAgain` | 1: 1 pass, 0 fail, 0 skip |
| [RegistrationTest](src/test/java/TestClases/RegistrationTest.java#L31) | `testRegistration` | 1: 1 pass, 0 fail, 0 skip |
| [RegistrationTest](src/test/java/TestClases/RegistrationTest.java#L36) | `testRequiredFields` | 1: 1 pass, 0 fail, 0 skip |
| [RegistrationTest](src/test/java/TestClases/RegistrationTest.java#L49) | `testEachRequiredField` | 10: 10 pass, 0 fail, 0 skip |
| [RegistrationTest](src/test/java/TestClases/RegistrationTest.java#L61) | `testRegistrationAfterCompletingRequiredFields` | 1: 1 pass, 0 fail, 0 skip |
| [RegistrationTest](src/test/java/TestClases/RegistrationTest.java#L76) | `testPasswordFieldsAreMasked` | 1: 1 pass, 0 fail, 0 skip |
| [RegistrationTest](src/test/java/TestClases/RegistrationTest.java#L89) | `testPasswordMismatch` | 1: 1 pass, 0 fail, 0 skip |
| [RegistrationTest](src/test/java/TestClases/RegistrationTest.java#L100) | `testDuplicateUsername` | 1: 1 pass, 0 fail, 0 skip |
| [RegistrationTest](src/test/java/TestClases/RegistrationTest.java#L112) | `testRegistrationWithoutPhoneNumber` | 1: 1 pass, 0 fail, 0 skip |
| [RegistrationTest](src/test/java/TestClases/RegistrationTest.java#L125) | `testRegistrationAfterCorrectingPasswordMismatch` | 1: 1 pass, 0 fail, 0 skip |
| [RequestLoanTest](src/test/java/TestClases/RequestLoanTest.java#L29) | `testLoanFundingAccountsMatchCustomerAccounts` | 1: 1 pass, 0 fail, 0 skip |
| [RequestLoanTest](src/test/java/TestClases/RequestLoanTest.java#L42) | `testLoanDecisionMatchesAccountState` | 1: 1 pass, 0 fail, 0 skip |
| [RequestLoanTest](src/test/java/TestClases/RequestLoanTest.java#L95) | `testDownPaymentAboveAvailableFundsIsDeniedWithoutDebit` | 1: 1 pass, 0 fail, 0 skip |
| [TransferFundsTest](src/test/java/TestClases/TransferFundsTest.java#L22) | `testTransferUpdatesBothBalancesAndActivity` | 2: 2 pass, 0 fail, 0 skip |
| [TransferFundsTest](src/test/java/TestClases/TransferFundsTest.java#L67) | `testInvalidAmountDoesNotMoveMoney` | 2: 2 pass, 0 fail, 0 skip |
| [UpdateContactInfoTest](src/test/java/TestClases/UpdateContactInfoTest.java#L45) | `testProfilePrefillsRegisteredContactDetails` | 1: 1 pass, 0 fail, 0 skip |
| [UpdateContactInfoTest](src/test/java/TestClases/UpdateContactInfoTest.java#L54) | `testAllRequiredContactFieldsAndUnchangedSavedProfile` | 1: 1 pass, 0 fail, 0 skip |
| [UpdateContactInfoTest](src/test/java/TestClases/UpdateContactInfoTest.java#L72) | `testEachRequiredContactField` | 6: 6 pass, 0 fail, 0 skip |
| [UpdateContactInfoTest](src/test/java/TestClases/UpdateContactInfoTest.java#L84) | `testProfileUpdateAfterCorrectingRequiredField` | 1: 1 pass, 0 fail, 0 skip |
| [UpdateContactInfoTest](src/test/java/TestClases/UpdateContactInfoTest.java#L100) | `testUpdatedProfilePersistsAfterNewLogin` | 1: 1 pass, 0 fail, 0 skip |
| [UpdateContactInfoTest](src/test/java/TestClases/UpdateContactInfoTest.java#L118) | `testOptionalPhoneNumberCanBeRemoved` | 1: 1 pass, 0 fail, 0 skip |
| [UtilityUnitTest](src/test/java/TestClases/UtilityUnitTest.java#L24) | `resolvesConfigurationOverridesInOrder` | Source reviewed; not rerun |
| [UtilityUnitTest](src/test/java/TestClases/UtilityUnitTest.java#L40) | `validatesBooleanIntegerAndDurationSettings` | Source reviewed; not rerun |
| [UtilityUnitTest](src/test/java/TestClases/UtilityUnitTest.java#L54) | `validatesLoginRowsAndSupportsLegacyResultHeader` | Source reviewed; not rerun |
| [UtilityUnitTest](src/test/java/TestClases/UtilityUnitTest.java#L67) | `rejectsMalformedLoginRows` | Source reviewed; not rerun |
| [UtilityUnitTest](src/test/java/TestClases/UtilityUnitTest.java#L82) | `recognizesFreshAccountMarkerOnlyForValidPlaceholder` | Source reviewed; not rerun |
| [UtilityUnitTest](src/test/java/TestClases/UtilityUnitTest.java#L91) | `createsUniqueRegistrationDataAndRedactsPassword` | Source reviewed; not rerun |
| [UtilityUnitTest](src/test/java/TestClases/UtilityUnitTest.java#L100) | `acceptsOnlyAbsoluteHttpUrlsWithoutCredentials` | Source reviewed; not rerun |
| [UtilityUnitTest](src/test/java/TestClases/UtilityUnitTest.java#L110) | `sanitizesBrowserDiagnosticUrlsAndPersonalIdentifiers` | Source reviewed; not rerun |
| [UtilityUnitTest](src/test/java/TestClases/UtilityUnitTest.java#L124) | `writesBrowserDiagnosticContextWhenLogTypesAreUnavailable` | Source reviewed; not rerun |
| [UtilityUnitTest](src/test/java/TestClases/UtilityUnitTest.java#L154) | `apiBackedUiFixturesRejectMismatchedDeploymentsBeforeProvisioning` | Source reviewed; not rerun |
