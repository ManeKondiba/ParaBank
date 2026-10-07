# ParaBank automation

Java 17, Selenium WebDriver, REST Assured, TestNG, Apache POI and Maven. The UI framework uses the Page Object Model (POM); API tests use independent REST clients and fixtures. UI support includes Chrome, Edge, Firefox, headless execution and Selenium Grid.

## Run tests

Install JDK 17 or later and set `JAVA_HOME`. The Maven wrapper downloads Maven 3.9.16 on first use. Local UI tests require a supported browser; Selenium Manager resolves its driver. Initial dependency and driver downloads require internet access.

From PowerShell in the project directory:

```powershell
# Full UI regression (the default; requires a browser and ParaBank)
.\mvnw.cmd test

# Compile the application UI tests without launching a browser
.\mvnw.cmd -DskipTests verify

# Start a fresh, loopback-only ParaBank API test environment (first run downloads/builds pinned dependencies)
.\scripts\start-parabank.ps1

# API smoke or full core regression against that controlled environment
.\mvnw.cmd -Papi -Dgroups=ApiSmoke verify
.\mvnw.cmd -Papi -Dgroups=ApiRegression verify

# Stop the local server after API testing
.\scripts\stop-parabank.ps1

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

`mvn test` and `mvn verify` run the default UI suite: 93 UI cases using built-in login data plus 21 deterministic unit tests across the shared utilities and API infrastructure. The master suite groups reports by page. `-Pui` remains an alias for this default. `-Papi` selects the browser-free TestNG API suite; use `-Dgroups=ApiSmoke`, `ApiRegression`, or `ApiContract` to focus it. `-Dgroups=Unit verify` runs only deterministic unit tests; they do not launch a browser or require ParaBank. `-DskipTests verify` checks compilation without running tests. Choose one suite profile per invocation. Do not set a global `browser` override when you want the cross-browser suite to use all three browsers.

| Profile | Suite | Coverage |
| --- | --- | --- |
| Default / `ui` | `master.xml` | 93 UI cases across customer access, accounts, payments, transaction search, profile updates and loans; plus 21 deterministic unit tests |
| `smoke` | `groupingtest.xml` | 7 cases: registration, login, logout, account overview, two transfer amounts and bill payment (`Sanity` and `BankingSmoke`) |
| `cross-browser` | `CrossBrowserTesting.xml` | 7 smoke cases in each of Chrome, Edge and Firefox; 21 invocations with three parallel workers |
| `api` | `api.xml` | Core customer/account/money movement/transaction/loan tests, OpenAPI route inventory, and API infrastructure tests |

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
| `api.baseUrl` | `PARABANK_API_BASE_URL` | `http://localhost:8081/parabank/services/bank` |
| `api.environment` | `PARABANK_API_ENVIRONMENT` | `local` |
| `api.fixture.mode` | `PARABANK_API_FIXTURE_MODE` | `web` (controlled deployment registration) |
| `api.fixture.file` | `PARABANK_API_FIXTURE_FILE` | Blank; required only for `file` fixture mode |
| `api.connectTimeout.seconds` / `api.readTimeout.seconds` | `PARABANK_API_CONNECT_TIMEOUT_SECONDS` / `PARABANK_API_READ_TIMEOUT_SECONDS` | `10` / `30` |

Timeouts and window dimensions must be positive integers. Boolean values must be `true` or `false`. Invalid settings fail explicitly. Grid URLs use HTTP(S); configure any Grid authentication through your network/proxy setup rather than embedding credentials in a URL.

## Test data and isolation

Each test method and each data row gets a fresh browser session. Positive tests create an account with unique, bounded-length credentials and do not depend on execution order, `john/demo`, or accounts left by another run. The built-in data provider uses an internal marker to provision its positive case. Negative cases include both empty fields, each missing field, a nonexistent user, and a known user's wrong password.

UI tests create synthetic customers/accounts, move demo balances, pay test payees, update test customer profiles and request demo loans. Use an isolated ParaBank deployment for repeatable UI runs. The public demo is shared, can be reset or misconfigured by other users, and may block automated registration with a CAPTCHA. Set `appUrl` to your controlled test deployment for unattended runs. UI tests do not reset the shared database or change its administrative settings.

API tests require a controlled deployment and reject the shared public ParaBank host. `scripts/start-parabank.ps1` runs the pinned local deployment on loopback and resets its owned database/configuration on each fresh start. It refuses to reuse a managed running instance; stop it with `scripts/stop-parabank.ps1` before restarting. Most API cases provision one fresh customer; the ownership-isolation case needs two unique customers in file-fixture mode. Tests verify persisted balance/ledger state. Never point the API suite at the public demo or use administrative reset/configuration endpoints as teardown against a shared environment.

Payment/transfer scenarios require enough initial funds for the opening deposit and up to $12.34 in transactions. Monetary assertions use `BigDecimal` and compare actual before/after balances; deposit and initial-balance settings are not hard-coded. Loan approval depends on the configured provider and thresholds: tests verify decision/state consistency, while the excessive-down-payment denial assumes ParaBank's built-in processors. Transaction date searches use server-rendered transaction dates. Positive customer lookup uses a unique synthetic SSN to isolate the recovered account.

The original `TestData/ParaBank_LoginData.xlsx` is retained as a legacy example. To use a maintained workbook explicitly:

```powershell
.\mvnw.cmd -Pui -Dgroups=Datadriven '-Dtestdata.login.path=TestData/ParaBank_LoginData.xlsx' test
```

The first row must contain exactly `username`, `password`, `expected` (case-insensitive). The original workbook's `res` header is also accepted in place of `expected`. Subsequent rows must have three cells with an expectation of `Valid` or `Invalid`. A blank credential cell is allowed for an invalid case. Entirely blank rows are skipped; malformed rows, missing sheets/files and empty datasets fail clearly. Format credentials as Excel text to preserve leading zeros. Formula cells are evaluated when reading.

For workbook `Valid` rows, provision the supplied account in the target environment first. Workbook values never trigger automatic registration. Credentials are preserved verbatim, including spaces. Use test-only credentials: TestNG's standard data-provider reports can include parameter values. `ExcelUtility` supports read/write/color operations with closed streams; concurrent writes to the same workbook require caller coordination.

## Results and troubleshooting

For failure classification and the maintenance process, see [TEST_DEBUGGING_AND_DEFECT_TRIAGE.md](TEST_DEBUGGING_AND_DEFECT_TRIAGE.md). Current local findings and their triage status are tracked in [DEFECT_REGISTER.md](DEFECT_REGISTER.md).

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
| Sanitized UI browser diagnostics | `target/browser-diagnostics/` |
| Sanitized ParaBank API server logs | `target/api-evidence/application-logs/` |
| Rolling logs | `target/logs/automation.log` |

When sharing a UI report, preserve the project layout: `target/reports/` for the HTML report, `screenshots/` at the project root, and `target/browser-diagnostics/` for failure context. Chromium runs capture bounded console entries and network request/response metadata without bodies or headers; unsupported browser/Grid log types are marked unavailable. Review diagnostic artifacts for application data before sharing. Screenshot/diagnostic collection failures are logged without replacing the original test failure. Configuration failures are recorded even when browser creation fails. No automatic retry hides an intermittent failure.

For cross-browser runs, use Spark or the per-method entries in `testng-results.xml` as the complete record. The tested Surefire/TestNG combination's `TEST-TestSuite.xml` did not retain every repeated class invocation across browser contexts.

If startup fails, check Java, browser installation, Grid availability and access to Maven Central/browser driver downloads. For an offline network, pre-provision dependencies and matching drivers using Selenium's standard driver settings. If registration reports an existing user despite a newly generated username, inspect the screenshot and application/database logs; the application can use that message for backend insertion failures too.

## Project structure

```text
src/test/java/
  PageObjects/       Explicit waits, locators and page actions
  TestBases/         Browser/API lifecycles and isolated test fixtures
  TestClases/        UI/API tests and business assertions
  utilities/         Configuration, drivers, API clients, Excel, data and reporting
src/test/resources/  Configuration, API schemas and core contract manifest
.github/workflows/  Compile/unit gates, API smoke/regression and manual UI smoke
screenshots/        Failure screenshots (folder kept; generated images ignored)
target/             Generated build output, reports and logs
  browser-diagnostics/ Sanitized UI failure context
  api-evidence/       Sanitized API exchanges and application logs
```

Keep new code in these existing packages. Place locators and browser actions in `PageObjects`, shared browser/API setup in `TestBases`, test cases in `TestClases`, and reusable helpers/data in `utilities`. `master.xml` and the UI suite files list UI classes; `api.xml` selects the API suite. Maven-generated output stays under `target/`; failed UI screenshots use `screenshots/` and sanitized API evidence uses `target/api-evidence/`.

To add a UI test, extend `BaseClass`, use `getDriver()`, place page actions in a page object, and register the class in the appropriate suite. Add real outcome assertions and independent data. Use explicit waits for the state being asserted; do not add implicit waits or sleeps.

GitHub Actions compiles the framework and runs utility tests on pushes and pull requests. Its isolated API job runs `ApiSmoke` on pushes and pull requests and `ApiRegression` on the nightly schedule; workflow dispatch can select smoke, regression, or none. The API job starts/stops the pinned local deployment and uploads sanitized evidence and server logs. UI smoke remains a manual workflow option against the configured UI URL and uploads browser diagnostics. The workflow has not been executed as part of this framework update.

## Jenkins on Windows

Use the root `Jenkinsfile` for the `ParaBank-Automation` Pipeline job. Jenkins and its Windows agent require a supported Java runtime (Java 21 for this setup); configure a separate **Manage Jenkins > Tools > JDK** installation named `jdk17` pointing to the installed JDK 17 for the Maven build. Install the **Pipeline**, **Git** and **JUnit** plugins with their dependencies. The build node must have the `windows` label, Git and Windows PowerShell available, and a writable workspace separate from this developer checkout. Leave the job's custom workspace unset: each build clears its Jenkins workspace before checkout. The Maven wrapper handles Maven installation; the Jenkins build account needs access to dependency downloads and its own writable Maven cache.

Configure **Pipeline script from SCM > Git** with `https://github.com/ManeKondiba/ParaBank.git`, branch `*/master`, and script path `Jenkinsfile`. A public repository needs no checkout credential; private access uses a Jenkins credential. Run **Build Now** once to load the parameters and polling schedule. Jenkins then checks GitHub every five minutes and builds only when changes exist. This PC must be awake with Jenkins and the build agent running.

Every build compiles the framework and runs the 21 deterministic unit tests using `mvnw.cmd -B -ntp -Dgroups=Unit clean verify`. For a manual UI run, select **Build with Parameters**, choose `UI_SUITE` (`smoke`, `regression` or `cross-browser`), and set `APP_URL` to the test deployment. `BROWSER` selects Chrome, Edge or Firefox for smoke/regression; cross-browser uses all three. Install the selected browsers for the Jenkins build account; the pipeline runs them headlessly. `UI_SUITE=none` is the default, and automatically triggered builds always skip UI tests. The shared public demo may show CAPTCHA or reset data; a controlled ParaBank deployment gives repeatable UI runs.

Test results appear on the Jenkins build page. Artifacts under `.jenkins-results/unit/`, `.jenkins-results/ui/`, and `.jenkins-results/api/` preserve Surefire/TestNG output, Spark HTML, sanitized logs, browser diagnostics, API evidence, and failure screenshots, including after test failures. Download and extract the artifacts together to retain the `target/reports/`, `target/browser-diagnostics/`, and `screenshots/` paths used by report links. Unit results are saved before a UI run cleans Maven output. Cross-browser details remain available in Spark and `testng-results.xml` because Surefire's JUnit summary can omit repeated browser contexts. Builds run one at a time, stop after 90 minutes, retain 20 build records and keep artifacts for the latest 10 builds. This pipeline provides build/test automation; it has no application deployment stage.

See the official [Windows installation guide](https://www.jenkins.io/doc/book/installing/windows/), [Java support policy](https://www.jenkins.io/doc/book/platform-information/support-policy-java/), and [Pipeline syntax reference](https://www.jenkins.io/doc/book/pipeline/syntax/) for Jenkins setup details.

## Dependency decisions

Versions were checked against Maven Central on 2026-10-03: Selenium 4.50.0, TestNG 7.12.0, POI 5.5.1, Log4j 2.26.1, ExtentReports 5.1.2, compiler plugin 3.16.0 and Maven 3.9.16. Selenium 4.50.0 includes CDP v154 support for the installed Chrome and Edge versions. Dependencies are test-scoped. Redundant WebDriverManager, email and unused direct Commons dependencies were removed.

Surefire is deliberately pinned to **3.5.5**: [Surefire 3.6 removes TestNG XML suite support](https://maven.apache.org/surefire-archives/surefire-LATEST/maven-surefire-plugin/whats-new-3-6-0.html). A migration to 3.6+ needs a separate suite/execution redesign, not a version-only change.

[Selenium Manager](https://www.selenium.dev/documentation/selenium_manager/) supplies driver management. Explicit waits follow [Selenium's waiting guidance](https://www.selenium.dev/documentation/webdriver/waits/). Validation assertions follow [ParaBank's own messages](https://github.com/parasoft/parabank/blob/master/src/main/resources/messages.properties).

[ExtentReports is sunset upstream](https://github.com/extent-framework/extentreports-java). Its final release is retained to keep Spark report compatibility. Plan a separately verified migration to a maintained reporter; standard TestNG/Surefire results remain available.

The API suite includes a versioned core-operation manifest checked against the deployed OpenAPI document, response-schema validation, representative XML parity, balance/ledger reconciliation, and customer-list ownership isolation. Direct cross-customer resource authorization, further negative financial boundaries, positions/JMS administration, accessibility, and load/concurrency testing remain follow-up scope. API contract coverage does not imply that the ParaBank application itself enforces an authorization model.

## Execution time comparison

UI regression and smoke accept `-Dui.threads=N` (positive integer; default `1`). Regression runs XML tests in parallel; smoke runs classes in parallel because it has one XML test. Methods and data rows within each class remain sequential. Cross-browser retains its existing three browser workers; API execution is unchanged. Keep fresh browsers and independent test data. UI registration and account-opening submissions share a JVM lock because ParaBank uses a read/update ID allocator that can collide under concurrent provisioning. Form filling and other browser actions remain parallel. The lock does not coordinate separate JVMs, CI jobs, external users or API writes. Use a dedicated deployment for each run; this is functional test orchestration, not a concurrency test of ParaBank. Registration locking relies on the current NORMAL page-load strategy completing the submitted response; revalidate it before adopting EAGER or NONE.

Compare the same suite and deployment with one, two, then three workers:

```powershell
.\mvnw.cmd -Pui -Dheadless=true -Dui.threads=1 test
.\mvnw.cmd -Pui -Dheadless=true -Dui.threads=2 test
.\mvnw.cmd -Pui -Dheadless=true -Dui.threads=3 test

# Shorter pilot
.\mvnw.cmd -Psmoke -Dheadless=true -Dui.threads=2 test
```

Use a controlled deployment via `-DappUrl=...`. Repeat each setting three times after dependency and driver downloads are warm; compare median wall time, test counts, failures/skips, CPU and memory. Choose the fastest setting with stable results. Compare headed and headless separately so only one factor changes at a time.

Every suite writes `target/reports/execution-timing-<run-id>.csv` and a matching `.txt` summary. CSV rows include test and configuration invocations sorted by duration, without parameter values. `BaseClass.setup` measures browser startup plus initial navigation; fixture creation inside test methods is included in test time. Parallel duration totals overlap. The summary reports TestNG wall time; Maven compilation, downloads and startup are excluded. Use Maven's total time or PowerShell `Measure-Command` for end-to-end comparisons. Status codes are 1=pass, 2=fail and 3=skip. Tests skipped before invocation may have no timing row; use TestNG results for complete outcome counts.

After measuring, prioritize repeated UI fixture creation and slow page conditions. API provisioning and EAGER navigation are follow-up experiments that need validation. Keep current timeouts until measurements identify a specific wait problem.

Validation on 2026-10-07: 21 unit tests, 7 smoke cases with two workers, and 23 Registration/OpenAccount cases with two workers passed against the pinned local deployment. The full UI regression and three-worker runs were not executed. These checks validate the provisioning workaround; they do not establish a speed improvement. Keep the default one worker until repeated timings justify increasing it.

## API-backed transfer UI setup

The transfer-funds pilot supports `ui.fixture.mode=api` (environment: `PARABANK_UI_FIXTURE_MODE`). The default `ui` keeps browser provisioning. Only `TransferFundsTest` switches fixture routes; registration, login, account-opening and other UI coverage continue using their existing setup.

API mode provisions a unique customer with the existing HTTP web-registration adapter (ParaBank has no REST registration endpoint), verifies the customer through the REST login endpoint, and creates a savings account through REST. Selenium then logs in and executes the transfer, including the existing UI balance and transaction assertions. Every invocation/data row has independent data and a fresh browser. Provisioning shares the JVM lock with UI registration/account opening; other browser actions remain parallel. Separate jobs need independent deployments/databases.

Both URLs must have the same scheme, host, effective port and application context. `localhost` and `127.0.0.1` are treated as different configured hosts; use one consistently. Invalid modes, the shared public demo, and mismatched URLs fail before browser startup or fixture writes. There is no fallback that hides an API setup failure. Sanitized provisioning evidence links appear in Spark reports and files under `target/api-evidence/`; the report also shows HTTP provisioning duration, including lock wait.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-parabank.ps1

# Transfer pilot plus existing smoke cases, with two workers
.\mvnw.cmd '-Dgroups=TransferFunds,BankingSmoke,Sanity' -Dheadless=true -Dui.threads=2 -Dui.fixture.mode=api '-DappUrl=http://127.0.0.1:8081/parabank/index.htm' '-Dapi.baseUrl=http://127.0.0.1:8081/parabank/services/bank' verify

powershell -NoProfile -ExecutionPolicy Bypass -File scripts/stop-parabank.ps1
```

Compare the same command with `-Dui.fixture.mode=ui`, without running Maven clean while the local server is active. Use TestNG wall time and per-method timing CSVs; repeat measurements before drawing performance conclusions.

GitHub Actions has a manual `run_hybrid_ui` option. Its separate Windows job starts an isolated deployment, runs these nine UI cases with two workers and API-backed transfer setup, stops the server even on failure, and uploads UI reports, sanitized API evidence and server logs. Hosted GitHub Actions verification passed on 2026-10-07; see the run linked below. Server startup and Maven tests run in the same PowerShell step, with shutdown in finally, so the background server remains alive during tests. The separate UI smoke job also uses its own isolated local deployment and browser-based provisioning.

Local validation on 2026-10-07: 22 unit tests passed, including deployment-mismatch checks. All nine selected UI cases passed with two workers in both modes. The final HTTP setup run took 51.1 seconds of TestNG wall time versus 58.7 seconds for browser setup; this is one comparison, not a repeatable benchmark or a promise of full-suite savings. Four transfer invocations had provisioning evidence and duration entries in Spark. The workflow YAML parsed successfully; the hosted Actions job subsequently passed as recorded below.

Hosted validation on 2026-10-07: [GitHub Actions run 37582721761](https://github.com/ManeKondiba/ParaBank/actions/runs/37582721761) passed 22 unit tests, 39 API regression cases and all nine API-backed UI pilot/smoke cases with two workers. No failures, errors or skips were reported in those selected tests. Server cleanup and artifact upload succeeded. This verifies the configured hosted jobs; it does not establish full UI regression coverage or repeatable performance gains.

The manual run_ui option runs the seven Chrome smoke cases on an isolated Windows runner deployment. Server startup and tests share one PowerShell step, with shutdown in finally and sanitized server logs uploaded alongside UI artifacts. This avoids Cloudflare challenges on the shared public demo.
