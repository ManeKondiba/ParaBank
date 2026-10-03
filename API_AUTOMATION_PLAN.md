ParaBank API automation plan and implementation status, updated 2026-10-03.

The browser-free REST Assured suite is implemented in this Java 17/Maven/TestNG project. It covers the 17 core customer, account, money movement, loan and transaction operations; JSON response schemas; representative XML account parity; financial-state reconciliation; sanitized API evidence; and an OpenAPI core-operation manifest. The dedicated `api` Maven profile and CI smoke/regression jobs are in place.

This document is now a coverage and hardening roadmap, not a list of unimplemented foundation work. Remaining priorities are financial amount/funds boundaries, direct cross-customer resource authorization characterization, full contract/schema drift review, and repeatability across fresh starts. Those cases must distinguish documented requirements from behavior observed in the pinned ParaBank build. Position operations, JMS/admin APIs, load/concurrency, and accessibility remain separate scope.

Controlled-environment execution was performed on 2026-10-03. Results and the current open API behavior candidate are recorded in the validation notes and [DEFECT_REGISTER.md](DEFECT_REGISTER.md). The profile-update 500 was traced to test credentials exceeding the pinned database's 20-character columns and is closed as an automation test-data defect. The remaining malformed bill-pay behavior still needs contract-owner confirmation because the pinned OpenAPI declares only a generic `default` response.

The review used the official [service listing](https://parabank.parasoft.com/parabank/services.htm), [Swagger UI](https://parabank.parasoft.com/parabank/api-docs/index.html), [OpenAPI definition](https://parabank.parasoft.com/parabank/services/bank/openapi.json), and [ParaBank source repository](https://github.com/parasoft/parabank). The target reviewed is `/parabank`, with REST base URL `https://parabank.parasoft.com/parabank/services/bank`. Keep `/parabankv2` and `/services_proxy/bank` separate unless explicitly selected as targets.

The reviewed definition declares OpenAPI 3.0.1 and API version 3.0.0. Its 27 operations divide into 17 core banking operations, five position operations and five environment administration operations. The following inventory records the actual HTTP methods and paths; priorities are proposed test priorities, not API metadata. P0 means initial smoke coverage, P1 means core regression, and P2 means subsequent expansion. Paths are relative to the REST base URL. [API contract](https://parabank.parasoft.com/parabank/services/bank/openapi.json)

| Area | Method and path | Planned coverage |
| --- | --- | --- |
| Login | `GET /login/{username}/{password}` | P0: valid fixture; P1: invalid credentials and URL encoding |
| Customer | `GET /customers/{customerId}` | P0: correct customer and response structure |
| Customer update | `POST /customers/update/{customerId}` | P1: persistence, field boundaries and credential changes |
| Account list | `GET /customers/{customerId}/accounts` | P0: customer ownership and expected account membership |
| Account details | `GET /accounts/{accountId}` | P0: ID, owner, type and balance |
| Account creation | `POST /createAccount` | P0: checking; P1: savings, funding and invalid parameters |
| Deposit | `POST /deposit` | P0: credit and balance delta; P1: boundaries |
| Withdrawal | `POST /withdraw` | P0: debit and balance delta; P1: boundaries and funds policy |
| Transfer | `POST /transfer` | P0: both balances and transactions; P1: failure atomicity |
| Bill payment | `POST /billpay` | P0: payee result, debit and transaction; P1: invalid payloads |
| Loan | `POST /requestLoan` | P1: deterministic approval and denial with pinned provider/settings |
| Transaction list | `GET /accounts/{accountId}/transactions` | P0: discover new transactions created by test actions |
| Transaction details | `GET /transactions/{transactionId}` | P0: lookup matches the discovered transaction |
| Amount search | `GET /accounts/{accountId}/transactions/amount/{amount}` | P1: matching and nonmatching records |
| Date search | `GET /accounts/{accountId}/transactions/onDate/{onDate}` | P1: server date and date boundaries |
| Date range | `GET /accounts/{accountId}/transactions/fromDate/{fromDate}/toDate/{toDate}` | P1: inclusive/exclusive behavior, empty and reversed ranges |
| Month/type search | `GET /accounts/{accountId}/transactions/month/{month}/type/{type}` | P1: month format, debit/credit and case handling |
| Buy position | `POST /customers/{customerId}/buyPosition` | P2: holding and cash changes |
| Sell position | `POST /customers/{customerId}/sellPosition` | P2: partial/full sale, cash and holding changes |
| Position list | `GET /customers/{customerId}/positions` | P2: ownership and created holdings |
| Position details | `GET /positions/{positionId}` | P2: consistency with list |
| Position history | `GET /positions/{positionId}/{startDate}/{endDate}` | P2: dates and provider availability |
| Database cleanup | `POST /cleanDB` | Isolated environment administration only |
| Database initialization | `POST /initializeDB` | Isolated environment administration only |
| Configuration | `POST /setParameter/{name}/{value}` | Isolated environment administration only |
| JMS stop | `POST /shutdownJmsListener` | Optional integration suite with broker available |
| JMS start | `POST /startupJmsListener` | Optional integration suite with broker available |

Several contract details must shape the implementation. Most writes use query parameters: `createAccount` takes `customerId`, integer `newAccountType` and `fromAccountId`; deposit/withdraw take `accountId` and `amount`; transfer takes `fromAccountId`, `toAccountId` and `amount`; loan requests take `customerId`, `amount`, `downPayment` and `fromAccountId`. Customer update uses query fields for the profile and credentials. Do not send these operations a generic JSON request body. Bill payment combines query `accountId`/`amount` with a JSON or XML Payee body. [Service interface](https://github.com/parasoft/parabank/blob/master/src/main/java/com/parasoft/parabank/service/ParaBankService.java), [bill payment interface](https://github.com/parasoft/parabank/blob/master/src/main/java/com/parasoft/parabank/service/IBillPayService.java)

| Finding from documentation/source | Consequence for the plan |
| --- | --- |
| JSON and XML representations are advertised. | Use explicit `Accept: application/json` for initial coverage; add representative XML parity checks later. Set request `Content-Type` when sending a body. |
| Responses largely use `default`, without explicit success/error status contracts. | Record exact deployed statuses, content types and error structures before making assertions. Do not assume every POST returns 201 or every absent ID returns 404. |
| Login returns customer data; no bearer-token flow is described. | Do not invent JWT/OAuth setup. Determine access/session behavior independently; missing security documentation is not proof of runtime access control. |
| Login credentials occur in the path; update credentials occur in the query. | Redact URLs as well as request/response bodies, cookies, passwords and SSNs in all reports. |
| No customer registration or per-record deletion operation appears in the reviewed bank API. | Provision customers through environment-owned seed data. Keep any verified web registration adapter separate from the REST client. |
| Date parameters are strings, and month/type input descriptions differ from response enum casing. | Characterize accepted formats, timezone, case and range boundaries; build requests from server transaction timestamps. |
| Account type request is numeric while response type is an enum string. | Verify the deployed numeric mapping before using named constants. |

These observations come from the [published contract](https://parabank.parasoft.com/parabank/services/bank/openapi.json). Upstream implementation is useful supporting evidence, but its current branch is not proof of the version running on the public demo. Pin the target application build and save that deployment's OpenAPI snapshot/checksum when implementation begins.

The existing repository provides most of the test infrastructure. Reuse the build and configuration conventions, while separating HTTP setup from browser setup.

| Existing component | Planned change or reuse |
| --- | --- |
| `pom.xml`: Java 17, TestNG, Maven Surefire and suite selection | Implemented: test-scoped REST Assured, Jackson, JSON schema validation and the `api` profile. |
| `src/test/java/utilities/FrameworkConfig.java` and `utilities/api/ApiConfig.java` | Implemented: JVM/environment/file configuration, API URL/timeout/fixture validation, and rejection of the shared public demo for writes. |
| `src/test/java/TestBases/BaseClass.java` and `ApiBaseTest.java` | Implemented: independent HTTP lifecycle; API tests do not start Selenium. |
| Existing `AccountFixture` / `BankingFixture` | Implemented separately as `ApiFixture`, using controlled web registration or an exclusive fixture-file lease. |
| `RegistrationData`, existing monetary assertions | Reuse synthetic-data conventions and `BigDecimal` balance checks where appropriate. Read credentials without the generic configuration getter trimming intentional whitespace. |
| `ExtentReportManager`, Log4j, Surefire output | Implemented: per-test API evidence is sanitized and archived separately from browser screenshots. |
| `.github/workflows/tests.yml`, local `Jenkinsfile` | Implemented: API smoke runs on push/PR, scheduled regression and manual selection use the controlled local deployment, and artifacts are archived. |

REST Assured fits this repository because tests can stay in Java/TestNG with reusable request specifications and JSON assertions. Use its [official setup documentation](https://github.com/rest-assured/rest-assured/wiki/GettingStarted) when selecting dependencies. A reporting migration, Cucumber layer or separate build module is unnecessary for the initial scope.

The implemented API slice follows the existing package conventions:

```text
src/test/java/
  TestBases/
    ApiBaseTest.java
    ApiFixture.java
  TestClases/api/
    CustomerApiTest.java
    AccountsApiTest.java
    MoneyMovementApiTest.java
    BillPayApiTest.java
    TransactionsApiTest.java
    LoansApiTest.java
    BankingJourneyApiTest.java
  utilities/api/
    ApiRequestFactory.java
    ApiEvidenceFilter.java
    ApiDates.java
    clients/     CustomerClient, AccountClient, TransactionClient, LoanClient
    models/      Customer, Account, Transaction, Payee, BillPayResult, LoanResponse
    assertions/  ApiAssertions
src/test/resources/api/
  contracts/     Versioned core operation inventory
  schemas/       Reviewed response schemas
api.xml          One dedicated TestNG API suite
```

The operation inventory pins the reviewed OpenAPI versions and core method/path set. It is not a full OpenAPI snapshot: request/response schema changes still need review and corresponding schema/test updates.

Clients should encode endpoint paths, parameters and serialization, and return responses for tests to inspect. Keep business expectations in tests/assertion helpers. Use a request specification per test and avoid mutable REST Assured global state. Create money values from decimal strings and use `BigDecimal` consistently, including JSON number parsing. Keep the raw response available for contract failures rather than hiding mismatches through permissive deserialization.

Use `api.baseUrl` explicitly instead of guessing it from the UI `appUrl`. `api.connectTimeout.seconds`, `api.readTimeout.seconds`, `api.environment`, `api.fixture.mode` and `api.fixture.file` are implemented with the shared JVM/environment/external-file precedence. Keep credentials and fixture secrets in CI credentials or the existing ignored local configuration file. Required settings and controlled-host requirements are validated before tests run.

Test data is a prerequisite for reliable automation. Prefer a dedicated ParaBank instance per CI job, seeded with synthetic customers and known provider/settings. Assign each worker its own customer and funding account, then create fresh scenario accounts. Capture returned IDs in a per-test fixture context instead of hard-coding customer/account IDs or depending on `john/demo`.

Because customer creation is absent from this REST surface, API setup uses the separate HTTP web-registration adapter by default or an exclusive JSON fixture-file lease. CAPTCHA-dependent UI registration is not used by the API suite. The local startup script pins the application revision and resets its owned loopback database on each fresh start; a managed running instance must be stopped before another fresh start.

Serialize tests that mutate balances until fixture isolation is proven. Give each customer-update and loan scenario a fresh or restored customer baseline, exclusively assigned to that scenario. Fresh accounts alone are insufficient: credential changes persist, and loan decisions can depend on balances across the customer's accounts. Two seeded customers suffice for initial characterization; the full suite needs scenario-specific seeds or verified restoration between cases. Snapshot balances and transaction IDs after setup and before the action under test. Avoid compensating transfers as cleanup: they add ledger records and can hide failures. Rebuild/reset only the dedicated environment between runs. Never use database reset, configuration or JMS controls against the shared public demo as test teardown. Public-demo observations are informational because other users can change its data and settings.

Each test should have a stable ID, operation mapping, priority, fixture prerequisites, request variant, expected response and expected persisted state. Record the source of the expectation: API contract, agreed business rule, or observed implementation behavior. The initial scenario matrix is:

| Scenario family | Positive and business assertions | Negative/boundary coverage |
| --- | --- | --- |
| `AUTH` | Login resolves the seeded customer's identity; subsequent reads return the expected data. | Wrong password, unknown user, encoded characters; distinguish missing path segments from invalid credentials. |
| `CUST` | Read profile, update all intended fields, read back exact values; changed credentials behave consistently. | Empty/omitted fields, length limits, Unicode, unknown ID; preserve unaffected fields and unrelated customers. |
| `ACCT` | Create checking/savings, assert owner/type and list membership; fetch persisted balances and reconcile configured opening funding. | Unsupported type, malformed/missing IDs, invalid funding account and mismatched customer/account. |
| `DEP` / `WDR` | Verify exact balance delta and one corresponding credit/debit record. | Zero, negative, minimum positive, extra decimal places, large values, malformed amount, insufficient funds. |
| `XFER` | Verify source debit, destination credit, equal amounts, new transaction IDs and conserved total. | Same account, invalid destination, missing parameters, insufficient funds and failure without partial debit. |
| `BILL` | Verify payee/amount/account result, source balance decrement and new debit transaction. | Missing/malformed Payee body, address fields, account number, unsupported media type and amount boundaries. |
| `TXN` | Compare discovered records with detail lookup; verify amount/date/range/month/type filters include all expected fixture records and exclude nonmatches. | Unknown transaction/account IDs, no matches, invalid dates, reversed ranges, leap day/month boundaries and invalid type. |
| `LOAN` | Under fixed provider/settings, assert expected approval or denial; reconcile resulting loan account and funding-account effects. | Invalid amount/down payment, excessive down payment, invalid funding account and configured rejection threshold. |
| `FLOW` | Customer lookup -> create account -> fund -> transfer -> bill pay -> reconcile account and transaction queries in one isolated journey. | Stop on the first failed prerequisite; preserve evidence for every completed step. |

Money-related tests must assert post-operation state with fresh GET requests. For a valid transfer of amount `A`, source balance must change from `S` to `S - A`, destination from `D` to `D + A`, and their total remain constant under the configured no-fee behavior. Assert exactly the expected new debit and credit records by comparing transaction-ID sets, not by assuming list order. For rejected writes, assert the agreed error and that relevant balances, records and profiles remain unchanged.

Boundary cases above are candidates for validating requirements, not claims that ParaBank rejects them today. The upstream [bank implementation](https://github.com/parasoft/parabank/blob/master/src/main/java/com/parasoft/parabank/domain/logic/impl/BankManagerImpl.java) contains direct balance arithmetic, so a blanket assumption that negative amounts or overdrafts are rejected would be unsafe. Agree the required rule, record any mismatch as a defect, and keep characterization cases distinct from requirements-based gates. Do not turn any observed 500 response or invalid accepted transaction into an unquestioned expected success.

For every core response, validate exact agreed status, content type, schema/shape, essential business fields, identity relationships and side effects. The published schemas alone may not require every field needed by the business; add explicit assertions for those fields. Validate the checked-in contract separately from supplemental requirements, so stronger local checks are not misrepresented as upstream contract guarantees. Add representative XML checks after JSON regression is stable. Do not parse a plain-text error as JSON merely because the request asked for JSON.

Access-control checks should use two synthetic customers on the isolated deployment: attempt reads and writes across their resources and compare behavior with the intended access model. Determine whether login establishes any session at all; do not assume calling login authorizes later requests. Test unsupported methods/media types and accidental credential leakage. Load, race-condition and destructive security tests belong to a later isolated-environment suite. Concurrent transfer tests should verify no lost updates or duplicate processing; repeated identical POSTs should be characterized without assuming undocumented idempotency support.

The implemented execution groups are `ApiSmoke`, `ApiRegression`, `ApiNegative`, `ApiContract` and `ApiWorkflow`. Smoke covers valid login/reads, account creation, monetary writes and transaction reconciliation. Keep environment administration outside the normal API suite. Positions remain deferred.

The following commands run against the controlled local deployment. A fresh start resets its owned database and configuration; stop an existing managed instance before restarting it.

```powershell
# Start or reset the dedicated loopback deployment
.\scripts\start-parabank.ps1

# Core API smoke
.\mvnw.cmd -Papi -Dgroups=ApiSmoke verify

# Full core regression, including negative and contract checks
.\mvnw.cmd -Papi -Dgroups=ApiRegression verify

# Focused contract run
.\mvnw.cmd -Papi -Dgroups=ApiContract verify

# Stop the local server
.\scripts\stop-parabank.ps1
```

GitHub Actions currently runs API smoke on pushes/pull requests and full regression on its schedule; workflow dispatch can select smoke, regression or none. Jenkins exposes manual API smoke/regression parameters. Keep the reports and sanitized request/response evidence as separate artifacts, as the current jobs do.

Reports should show case ID, endpoint template, environment/build, status, duration, assertion failure and relevant before/after state. UI failures attach screenshots and `target/browser-diagnostics/` with sanitized page URL/title, bounded console entries, and Chromium network metadata only (no bodies or headers). API artifacts include sanitized request/response evidence and bounded sanitized server logs under `target/api-evidence/application-logs/`; never archive the full `target/api-environment/` directory. Redact before data reaches logs, TestNG parameter names or attachments. Unit-test the redaction filter and monetary/transaction comparison helpers with meaningful cases. Do not automatically retry writes after timeouts: the server may already have committed them. Treat timeouts as ambiguous outcomes, collect account/transaction evidence and fail for investigation. Use bounded readiness polling where needed, without fixed sleeps or automatic reruns that conceal failures.

Measure response durations initially and establish a baseline on the controlled environment. Define latency gates only after agreeing a workload and threshold; a public-demo timing threshold is not a service-level agreement. Track operation coverage, risk/scenario coverage, assertion failures, setup failures and intermittent failures separately. An environment outage should produce a visible failed/blocked run, not a green build with every application case skipped.

## Remaining Work

| Priority | Work | Completion condition |
| --- | --- | --- |
| P0 | Complete write-failure atomicity matrix | Invalid-destination transfer returned the expected missing-resource status and preserved the source balance/ledger in the observed run. Still cover insufficient funds, same-account transfer, and other agreed rejected writes; preserve after-state checks even when status assertions fail. |
| P0 | Financial amount/funds boundaries | Cover zero, negative, over-precision and insufficient-funds amounts plus same-account transfer; first agree business behavior and distinguish requirement failures from characterization. |
| P0 | Direct cross-customer resource access | Account-list ownership isolation passes for two customers. Still characterize direct account/transaction reads and writes across owners; confirm intended security model before making rejection behavior a gate. |
| P1 | OpenAPI/schema drift | Version and 17 core operation inventory check passes. Review full request/response schemas and error contracts against a pinned target contract; current OpenAPI declares only generic `default` responses for the two open findings. |
| P1 | Repeatability evidence | Pass `ApiSmoke` three times from separate fresh local starts and verify no order dependence, stale database state, hidden retries, or secret leakage. Only one smoke execution is currently recorded. |
| P1 | Resolve tracked API candidate | Triage `PB-API-002` in [DEFECT_REGISTER.md](DEFECT_REGISTER.md): confirm malformed-input behavior, assign an owner, and link an external issue if confirmed as a product defect. `PB-API-001` is closed as a test-data limit mismatch. |
| P2 | Positions and non-functional suites | Add position coverage, JMS only where provisioned, plus separately scoped accessibility, concurrency and performance tests. |

## Validation Notes (2026-10-03)

Java test compilation succeeded. `ApiSmoke` passed 12/12; deterministic `Unit` tests passed 21/21, including sanitized browser-diagnostic coverage. Focused `CUST-002` passes after correcting overlong test credentials to fit the pinned database's 20-character username/password columns. Focused `BILL-002` still receives HTTP 500 but verifies unchanged balance and ledger state. The live core OpenAPI version/operation inventory check passed. The transfer invalid-destination atomicity and two-customer account-list isolation checks passed. The controlled server was stopped after validation.

Core completion requires positive coverage for all 17 core operations, applicable negative/boundary cases mapped to each operation, balance/ledger checks for every monetary write, and deterministic loan approval/denial on the configured target. Every P0 test must pass in three consecutive runs on fresh isolated environments, with no ordering dependency or hidden retries. Known application defects and contract gaps must be reported explicitly rather than encoded as passing expectations. Reports must be sufficient to diagnose a failure without rerunning the test and must not expose fixture secrets.

Next, confirm the expected malformed bill-pay behavior and resolve `PB-API-002`, then agree and implement the financial-boundary and direct cross-customer access scenarios. Finish by running smoke from three fresh starts and reviewing full schema drift before considering the API regression gate stable.
