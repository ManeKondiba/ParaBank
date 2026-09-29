# ParaBank automation

Java 17, Selenium WebDriver, TestNG, Apache POI and Maven. The framework uses the Page Object Model (POM): page objects contain browser interactions; tests assert business outcomes. The framework supports Chrome, Edge, Firefox, headless execution and Selenium Grid.

## Run tests

Install JDK 17 or later and set `JAVA_HOME`. The Maven wrapper downloads Maven 3.9.16 on first use. Local UI tests require a supported browser; Selenium Manager resolves its driver. Initial dependency and driver downloads require internet access.

From PowerShell in the project directory:

```powershell
# Full UI regression (the default; requires a browser and ParaBank)
.\mvnw.cmd test

# Compile the application UI tests without launching a browser
.\mvnw.cmd -DskipTests verify

# Customer access and core banking smoke tests
.\mvnw.cmd -Psmoke -Dheadless=true test

# Run one page, with HTML reports and failure screenshots
.\mvnw.cmd -Dgroups=Registration -Dheadless=true verify
.\mvnw.cmd -Dgroups=Login -Dheadless=true verify
.\mvnw.cmd -Dgroups=Logout -Dheadless=true verify
.\mvnw.cmd -Dgroups=OpenAccount -Dheadless=true verify

# Added banking workflows, or one banking page
.\mvnw.cmd -Dgroups=Banking -Dheadless=true verify
.\mvnw.cmd -Dgroups=TransferFunds -Dheadless=true verify

# Explicit alias for the default full UI regression suite
.\mvnw.cmd -Pui -Dheadless=true test

# All three browsers in parallel
.\mvnw.cmd -Pcross-browser -Dheadless=true test

# One browser or another deployment
.\mvnw.cmd -Psmoke -Dbrowser=edge -Dheadless=true test
.\mvnw.cmd -Pui '-DappUrl=http://localhost:8080/parabank/index.htm' test

# Selenium Grid, with the optional platform matched by Grid nodes
.\mvnw.cmd -Psmoke '-Dremote.url=http://localhost:4444' -Dbrowser=chrome -Dos=linux -Dheadless=true test
```

On Linux/macOS, replace `.\mvnw.cmd` with `bash mvnw`. An installed Maven 3.9+ can also run these commands as `mvn ...`. Use `verify` instead of `test` for the full Maven verification lifecycle.

`mvn test` and `mvn verify` run the full suite: 93 UI cases using built-in login data plus seven utility unit tests. The master suite groups reports by page. `-Pui` remains an alias for this default. `-Dgroups=Unit verify` runs only the deterministic utility tests; they do not launch a browser. `-DskipTests verify` checks compilation without running tests. Choose one suite profile per invocation. Do not set a global `browser` override when you want the cross-browser suite to use all three browsers.

| Profile | Suite | Coverage |
| --- | --- | --- |
| Default / `ui` | `master.xml` | 93 UI cases across customer access, accounts, payments, transaction search, profile updates and loans; plus 7 utility unit tests |
| `smoke` | `groupingtest.xml` | 7 cases: registration, login, logout, account overview, two transfer amounts and bill payment (`Sanity` and `BankingSmoke`) |
| `cross-browser` | `CrossBrowserTesting.xml` | 7 smoke cases in each of Chrome, Edge and Firefox; 21 invocations with three parallel workers |

Page-wise runs use the existing `master.xml`. Groups and case counts are: `Registration` 18, `Login` 14, `Logout` 3, `OpenAccount` 5, `AccountsOverview` 2, `AccountDetails` 4, `TransferFunds` 4, `BillPay` 6, `FindTransactions` 13, `UpdateContactInfo` 11, `RequestLoan` 3 and `CustomerLookup` 10. `Banking` selects the 53 newly added banking cases. Keep suite changes in the existing `master.xml`, `groupingtest.xml` and `CrossBrowserTesting.xml`; separate page XML files are unnecessary. These suites register `utilities.ExtentReportManager` as a TestNG listener. Tests retain the existing `Master`, `Sanity`, `Regression` and `Datadriven` groups. For example, `-Pui -Dgroups=Datadriven` selects the login data cases.

## Configuration

Defaults are in `src/test/resources/config.properties`. Override precedence is JVM `-D` property, environment variable, TestNG XML parameter (browser/OS only), optional external properties file, then bundled defaults.

Load an untracked local file with `'-Dconfig.file=config.local.properties'` or `PARABANK_CONFIG_FILE`. Environment names start with `PARABANK_`, with camel-case and dots converted to underscores:

| Property | Environment variable | Default |
| --- | --- | --- |
| `appUrl` | `PARABANK_APP_URL` | Public ParaBank demo |
| `browser` | `PARABANK_BROWSER` | `chrome` |
| `headless` | `PARABANK_HEADLESS` | `false` |
| `browser.binary` | `PARABANK_BROWSER_BINARY` | Auto-detect |
| `remote.url` | `PARABANK_REMOTE_URL` | Blank: local browser |
| `os` | `PARABANK_OS` | Blank: Grid chooses platform |
| `timeout.explicit.seconds` | `PARABANK_TIMEOUT_EXPLICIT_SECONDS` | `15` |
| `timeout.pageLoad.seconds` | `PARABANK_TIMEOUT_PAGE_LOAD_SECONDS` | `45` |
| `timeout.script.seconds` | `PARABANK_TIMEOUT_SCRIPT_SECONDS` | `30` |
| `window.width` / `window.height` | `PARABANK_WINDOW_WIDTH` / `PARABANK_WINDOW_HEIGHT` | `1440` / `1000` |
| `testdata.login.path` | `PARABANK_TESTDATA_LOGIN_PATH` | Blank: built-in cases |
| `testdata.login.sheet` | `PARABANK_TESTDATA_LOGIN_SHEET` | `Sheet1` |

Timeouts and window dimensions must be positive integers. Boolean values must be `true` or `false`. Invalid settings fail explicitly. Grid URLs use HTTP(S); configure any Grid authentication through your network/proxy setup rather than embedding credentials in a URL.

## Test data and isolation

Each test method and each data row gets a fresh browser session. Positive tests create an account with unique, bounded-length credentials and do not depend on execution order, `john/demo`, or accounts left by another run. The built-in data provider uses an internal marker to provision its positive case. Negative cases include both empty fields, each missing field, a nonexistent user, and a known user's wrong password.

UI tests create synthetic customers/accounts, move demo balances, pay test payees, update test customer profiles and request demo loans. Use an isolated ParaBank deployment for repeatable CI. The public demo is shared, can be reset or misconfigured by other users, and may block automated registration with a CAPTCHA. Set `appUrl` to your controlled test deployment for unattended runs. Tests do not reset the shared database or change its administrative settings; test records remain until that environment is reset.

Payment/transfer scenarios require enough initial funds for the opening deposit and up to $12.34 in transactions. Monetary assertions use `BigDecimal` and compare actual before/after balances; deposit and initial-balance settings are not hard-coded. Loan approval depends on the configured provider and thresholds: tests verify decision/state consistency, while the excessive-down-payment denial assumes ParaBank's built-in processors. Transaction date searches use server-rendered transaction dates. Positive customer lookup uses a unique synthetic SSN to isolate the recovered account.

The original `TestData/ParaBank_LoginData.xlsx` is retained as a legacy example. To use a maintained workbook explicitly:

```powershell
.\mvnw.cmd -Pui -Dgroups=Datadriven '-Dtestdata.login.path=TestData/ParaBank_LoginData.xlsx' test
```

The first row must contain exactly `username`, `password`, `expected` (case-insensitive). The original workbook's `res` header is also accepted in place of `expected`. Subsequent rows must have three cells with an expectation of `Valid` or `Invalid`. A blank credential cell is allowed for an invalid case. Entirely blank rows are skipped; malformed rows, missing sheets/files and empty datasets fail clearly. Format credentials as Excel text to preserve leading zeros. Formula cells are evaluated when reading.

For workbook `Valid` rows, provision the supplied account in the target environment first. Workbook values never trigger automatic registration. Credentials are preserved verbatim, including spaces. Use test-only credentials: TestNG's standard data-provider reports can include parameter values. `ExcelUtility` supports read/write/color operations with closed streams; concurrent writes to the same workbook require caller coordination.

## Results and troubleshooting

Failed-test screenshots are saved in the project-level `screenshots/` folder. Build output, logs and reports are saved under `target/`. To remove build output, logs and reports from a previous run, use:

```powershell
.\mvnw.cmd clean
```

Run `.\mvnw.cmd verify` afterward to rebuild and run the full UI suite against the configured ParaBank deployment. Use `.\mvnw.cmd -DskipTests verify` for compilation only. Save any reports you need before cleaning; Maven clean preserves `screenshots/`. Remove old screenshot files manually when they are no longer needed. A local `.maven-cache/` directory, if used, contains reusable Maven downloads and is ignored by Git; it does not need routine deletion. Formatting defaults are defined in `.editorconfig`.

| Output | Location |
| --- | --- |
| TestNG / Surefire XML and HTML | `target/surefire-reports/` |
| Extent Spark HTML for UI suites | `target/reports/ParaBank-*.html` |
| Failure screenshots | `screenshots/` |
| Rolling logs | `target/logs/automation.log` |

When sharing a UI report, preserve the project layout: `target/reports/` for the HTML report and `screenshots/` at the project root. Image links are relative to the report location. Screenshots are captured for failed tests and configuration failures when a browser session is available; generated image files are ignored by Git. Reports do not open a desktop browser automatically. Screenshot collection failures are logged without replacing the original test failure. Configuration failures are recorded even when browser creation fails. No automatic retry hides an intermittent failure.

For cross-browser runs, use Spark or the per-method entries in `testng-results.xml` as the complete record. The tested Surefire/TestNG combination's `TEST-TestSuite.xml` did not retain every repeated class invocation across browser contexts.

If startup fails, check Java, browser installation, Grid availability and access to Maven Central/browser driver downloads. For an offline network, pre-provision dependencies and matching drivers using Selenium's standard driver settings. If registration reports an existing user despite a newly generated username, inspect the screenshot and application/database logs; the application can use that message for backend insertion failures too.

## Project structure

```text
src/test/java/
  PageObjects/       Explicit waits, locators and page actions
  TestBases/         Browser lifecycle and shared test account setup
  TestClases/        Application UI tests and business assertions
  utilities/        Configuration, drivers, Excel, test data and reporting
src/test/resources/ Configuration and logging
.github/workflows/  Compilation checks plus manually requested Chrome smoke run
screenshots/        Created on demand for failure screenshots; ignored by Git
target/             Generated build output, reports and logs
```

Keep new code in these existing packages. Place locators and browser actions in `PageObjects`, shared test setup in `TestBases`, test cases in `TestClases`, and reusable helpers/data in `utilities`. The suites list only application UI test classes. Maven-generated output stays under `target/`; failed-test screenshots use `screenshots/`.

To add a UI test, extend `BaseClass`, use `getDriver()`, place page actions in a page object, and register the class in the appropriate suite. Add real outcome assertions and independent data. Use explicit waits for the state being asserted; do not add implicit waits or sleeps.

GitHub Actions compiles the UI tests with `-DskipTests verify` on pushes and pull requests. UI smoke runs only when manually selected in **Actions → ParaBank tests → Run workflow**; keep application runs manual until a controlled deployment is available. UI reports are uploaded even on failure. The workflow has not been published or executed by the local update.

## Dependency decisions

Versions were checked against Maven Central on 2026-09-24: Selenium 4.49.0, TestNG 7.12.0, POI 5.5.1, Log4j 2.26.1, ExtentReports 5.1.2, compiler plugin 3.16.0 and Maven 3.9.16. Dependencies are test-scoped. Redundant WebDriverManager, email and unused direct Commons dependencies were removed.

Surefire is deliberately pinned to **3.5.5**: [Surefire 3.6 removes TestNG XML suite support](https://maven.apache.org/surefire-archives/surefire-LATEST/maven-surefire-plugin/whats-new-3-6-0.html). A migration to 3.6+ needs a separate suite/execution redesign, not a version-only change.

[Selenium Manager](https://www.selenium.dev/documentation/selenium_manager/) supplies driver management. Explicit waits follow [Selenium's waiting guidance](https://www.selenium.dev/documentation/webdriver/waits/). Validation assertions follow [ParaBank's own messages](https://github.com/parasoft/parabank/blob/master/src/main/resources/messages.properties).

[ExtentReports is sunset upstream](https://github.com/extent-framework/extentreports-java). Its final release is retained to keep Spark report compatibility. Plan a separately verified migration to a maintained reporter; standard TestNG/Surefire results remain available.

The banking expansion adds account overview/activity, transfer balance conservation and ledger entries, bill-payment debits, transaction search/details, profile persistence, customer lookup and loan decision/state consistency. These assertions follow [ParaBank's page templates](https://github.com/parasoft/parabank/tree/master/src/main/webapp/WEB-INF/jsp/content) and [banking operations](https://github.com/parasoft/parabank/blob/master/src/main/java/com/parasoft/parabank/domain/logic/impl/BankManagerImpl.java). Compilation and suite inventory have been checked; live verification of the new workflows is pending. API contracts, accessibility, load testing and additional authorization/boundary rules require separately agreed scope and a controlled environment.
