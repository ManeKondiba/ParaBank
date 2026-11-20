# ParaBank Automation Framework: Semantic Code & Architecture Map

This registry provides a bidirectional mapping across the ParaBank Automation Framework for SDETs, AI agents, and code reviewers. Use it to trace application routes, Page Objects, API endpoints, DTO models, and test classes.

---

## 1. Web UI: Route &rarr; PageObject &rarr; Test Mapping

All UI Page Objects inherit from [`pageobjects.BasePage`](file:///f:/pravin%20customer/D/Backup2-6-26/Workspaces2025/SeleniumWebdriver/ParaBank/src/test/java/pageobjects/BasePage.java) and use fluent method chaining.
All UI test classes inherit from [`base.BaseClass`](file:///f:/pravin%20customer/D/Backup2-6-26/Workspaces2025/SeleniumWebdriver/ParaBank/src/test/java/base/BaseClass.java).

| Application Route | Page Object Class | Key Actions & Invariants | Test Class | TestNG Group |
| :--- | :--- | :--- | :--- | :--- |
| `/parabank/index.htm` | `LoginPage`, `HomePage` | `login(username, pass)`, `isErrorDisplayed()` | `LoginTest`, `LoginDDTtest` | `Login`, `Datadriven`, `Sanity` |
| `/parabank/register.htm` | `RegistrationPage` | `fill(CustomerProfile)`, `register()` | `RegistrationTest` | `Registration`, `Sanity` |
| `/parabank/overview.htm` | `AccountsOverviewPage` | `getAccountBalances()`, `clickAccount(num)` | `AccountsOverviewTest` | `Banking`, `AccountsOverview` |
| `/parabank/openaccount.htm` | `OpenAccountPage` | `openAccount(type, fromAccountId)` | `OpenAccountTest` | `Banking`, `OpenAccount` |
| `/parabank/transfer.htm` | `TransferFundsPage` | `transfer(amount, fromId, toId)` | `TransferFundsTest` | `Banking`, `TransferFunds` |
| `/parabank/billpay.htm` | `BillPayPage` | `sendPayment(payee, amount, fromId)` | `BillPayTest` | `Banking`, `BillPay` |
| `/parabank/findtrans.htm` | `FindTransactionsPage` | `findByAmount()`, `findByDate()`, `findById()` | `FindTransactionsTest` | `Banking`, `FindTransactions` |
| `/parabank/updateprofile.htm`| `UpdateContactInfoPage` | `updateProfile(profile)` | `UpdateContactInfoTest` | `Banking`, `UpdateContactInfo` |
| `/parabank/requestloan.htm` | `RequestLoanPage` | `applyForLoan(amount, downPayment, fromId)` | `RequestLoanTest` | `Banking`, `RequestLoan` |
| `/parabank/lookup.htm` | `CustomerLookupPage` | `findLoginInfo(firstName, ssn, ...)` | `CustomerLookupTest` | `CustomerLookup` |
| `/parabank/logout.htm` | `LogoutPage` | `logout()`, `verifyLoggedOut()` | `LogoutTest` | `Logout`, `Sanity` |
| *Site-wide accessibility* | `BankingAccessibilityPage`| Semantic headings, form labels, ARIA landmarks | `BankingAccessibilityTest` | `Accessibility` |

---

## 2. REST API: Endpoint &rarr; Client &rarr; Model &rarr; Test Mapping

All API test classes inherit from [`base.ApiBaseTest`](file:///f:/pravin%20customer/D/Backup2-6-26/Workspaces2025/SeleniumWebdriver/ParaBank/src/test/java/base/ApiBaseTest.java) and use typed domain clients.

| HTTP Method & Path | Client Method | Request/Response DTO | Test Class | TestNG Group |
| :--- | :--- | :--- | :--- | :--- |
| `GET /customers/{id}` | `CustomerClient.get()` | `Customer` | `CustomerApiTest` | `ApiSmoke`, `ApiRegression` |
| `POST /customers/update/{id}` | `CustomerClient.update()` | `Customer` | `CustomerApiTest` | `ApiRegression` |
| `GET /customers/{id}/accounts` | `AccountClient.getAccounts()`| `List<Account>` | `AccountsApiTest` | `ApiSmoke`, `ApiRegression` |
| `POST /accounts/create` | `AccountClient.create()` | `Account` | `AccountsApiTest` | `ApiRegression` |
| `POST /transfer` | `AccountClient.transfer()` | `TransferResult` | `MoneyMovementApiTest` | `ApiSmoke`, `ApiRegression` |
| `POST /billpay` | `AccountClient.billPay()` | `BillPayment` | `BillPayApiTest` | `ApiSmoke`, `ApiRegression` |
| `GET /accounts/{id}/transactions` | `TransactionClient.get()` | `List<Transaction>` | `TransactionsApiTest` | `ApiSmoke`, `ApiRegression` |
| `POST /requestLoan` | `LoanClient.request()` | `LoanResponse` | `LoansApiTest` | `ApiRegression` |
| `GET /openapi.json` | `ApiRequestFactory` | OpenAPI Specification | `ApiOpenApiContractTest` | `ApiContract` |

---

## 3. Data & Utility Registry

| Utility | Type | Responsibility | Thread-Safety Guarantee |
| :--- | :--- | :--- | :--- |
| `CustomerDataFactory` | Builder / Generator | Generates distinct synthetic customer records with unique SSN & username. | Thread-safe, stateless. |
| `CustomerProfile` | Immutable Record | Masks `password` and `ssn` in `toString()`. | Immutable. |
| `ExcelUtility` | Read-only Reader | Stream-based reads of `.xlsx` without disk writes or locks. | Read-only per operation. |
| `ProvisioningLock` | Monitor | Prevents concurrent collision during account provisioning on single JVM. | Reentrant monitor. |
| `SmartRetryAnalyzer` | TestNG Analyzer | Retries transient connection/driver errors (max 1x); strictly rejects `AssertionError`. | Thread-safe. |
| `ExecutionTimingListener`| TestNG Listener | Records nanosecond invocation metrics and dumps to CSV. | `ConcurrentLinkedQueue`. |
| `ExtentReportManager` | TestNG Reporter | Thread-safe ExtentReports logging with MDC contextual breadcrumbs. | `ThreadLocal`. |

---

## 4. Architectural Rules for AI Agents

When authoring or updating tests in this repository, AI agents must abide by the following:
1. **Never use `Thread.sleep`**: Use `WebDriverWait` with `ExpectedConditions` in Page Objects.
2. **Never instantiate raw drivers**: Driver lifecycle is managed exclusively by `BaseClass`.
3. **Never write test output into Excel files**: All test telemetry belongs in ExtentReports and TestNG.
4. **Never accept unredacted credentials**: Always use `CustomerDataFactory` and log masked strings.
5. **Always enforce ledger balance invariants**: In monetary tests, assert `balance_after == balance_before +/- delta`.
