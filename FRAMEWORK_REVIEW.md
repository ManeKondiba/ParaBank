# Framework review and verification

## Page-wise scenario expansion: 2026-09-28

Expanded the existing application pages from 20 to 40 invocations: registration 18, login 14, logout 3 and open account 5. Added independent required-field registration checks, recovery after validation errors, password masking, logout persistence, protected-page access after logout, account selection and persistence, and verification that the selected funding account supplies the new account's opening deposit.

Only the existing suite files are retained. `master.xml` now groups tests by page; use `-Dgroups=Registration`, `Login`, `Logout` or `OpenAccount` for a page run. `groupingtest.xml` and `CrossBrowserTesting.xml` retain their existing three-case smoke selection. No separate page XML files are needed.

Maven compilation verification passed. The full headless Chrome run against the public demo executed **40 cases: 37 passed, 3 failed, 0 skipped**:

| Page | Passed | Failed | Finding |
| --- | --- | --- | --- |
| Registration | 18 | 0 | All registration scenarios passed |
| Login | 13 | 1 | The positive DDT fixture encountered CAPTCHA during registration |
| Logout | 2 | 1 | Revisiting Accounts Overview after logout returned an internal-error message instead of the expected login-required message |
| Open account | 4 | 1 | The alternate-funding-account fixture encountered CAPTCHA during registration |

Screenshots confirm both CAPTCHA interruptions and the separate logout behavior mismatch. The latter remains a failing test: [ParaBank's login interceptor](https://github.com/parasoft/parabank/blob/master/src/main/java/com/parasoft/parabank/web/LoginInterceptor.java) routes unauthenticated access to its login form, while this deployment displayed `An internal error has occurred and has been logged.` The application/deployment needs investigation; the expected authentication behavior was not relaxed to accept that error.

Full-run XML/text results are preserved in `target/validation/pagewise-full/`, the console log in `target/validation/pagewise-run.log`, Spark HTML under `target/reports/`, and failure images under `screenshots/`. These results do not represent a passing full regression. The full run preceded the added funding-balance assertion and final page-group tags; focused verification of that final change is recorded below.

Final focused verification: `-Dgroups=OpenAccount -Dheadless=true verify` used the existing `master.xml` and ran **5 cases, all passed, 0 skipped**. This confirms the page-group filter, account-opening scenarios and the selected funding account's balance decrease, without assuming a fixed opening-deposit amount. Maven completed with `BUILD SUCCESS`. The console log is `target/validation/open-account-group-run.log`; current Surefire results are in `target/surefire-reports/`. The 40-case results above remain separately preserved; the focused pass does not erase the full-run failures.

## Registration verification: 2026-09-28

This earlier check covered the six registration scenarios present before the page-wise expansion. Review found no concrete defect requiring changes to those tests or their helpers. Registration now runs through the existing `master.xml` using `-Dgroups=Registration`; the separate page suite was removed as requested.

Environment: Java 17.0.11, Maven 3.9.16, headless Chrome 154.0.8037.57 and matching ChromeDriver 154.0.8037.57, against the public ParaBank demo. Selenium Manager resolved the matching driver when execution had network access; the earlier driver-download and browser-startup failures did not recur.

| Scenario | Initial class-only run | Registration suite with reporting |
| --- | --- | --- |
| Valid registration | Timed out waiting for success | Passed |
| Required fields | Passed | Passed |
| Password mismatch | Passed | Passed |
| Duplicate username | Passed | Failed during account setup: CAPTCHA |
| Optional phone number | Timed out waiting for success | Passed |
| Corrected password mismatch | Passed | Passed |

The latest run had **5 passed, 1 failed, 0 skipped** and correctly returned a failing Maven exit code. The duplicate-username failure screenshot shows Cloudflare's human-verification page during registration of the prerequisite account, before the duplicate check could execute. The first run's two timeouts had no screenshots, so their cause is unconfirmed. These separate runs do not constitute one passing six-test run; complete verification still needs a controlled deployment without CAPTCHA.

This check's results are preserved under `target/validation/registration-with-report/`, the Spark report in `target/reports/`, and `screenshots/testDuplicateUsername_ddb67736-372d-4c94-8c59-7ac65f976ea7.png`. The initial run is preserved under `target/validation/registration-initial/`. Generated reports are ignored by Git and removed by Maven clean; screenshots are preserved.

## Failure screenshot folder: 2026-09-25

Failure screenshots now use the project-level `screenshots/` folder. Spark reports remain under `target/reports/` and link to the screenshots using relative paths. Preserve both locations in the same project layout when sharing a report. Generated screenshots are ignored by Git, and Maven clean preserves this folder while removing reports, logs and other build output under `target/`.

## Application UI tests only: 2026-09-25

Removed the five browser-free test classes (`DataProvidersTest`, `DriverFactoryTest`, `ExcelUtilityTest`, `FrameworkConfigTest` and `RegistrationDataTest`) and `framework-tests.xml` as requested. Their utility implementations remain available to the UI tests. The retained application test classes cover registration, login, data-driven login, logout and account opening.

The default Maven suite is now `master.xml`; `-Pui` remains an alias for that suite. CI compiles the application UI tests on pushes and pull requests with `-DskipTests verify`; Chrome smoke execution remains manually requested. The earlier 19 framework-check results below are historical and no longer describe the current test suite.

Validation: Maven `-DskipTests verify` successfully compiled all 19 remaining Java files. All three UI suite files resolve to the retained test classes, with no duplicate class entries within a test context. Live UI tests were not rerun; the public-demo CAPTCHA limitation remains. Generated `target/` output was removed after validation.

## POM folder organization: 2026-09-25

Consolidated Java code into the existing `PageObjects`, `TestBases`, `TestClases` and `utilities` packages. Shared account setup now belongs to `TestBases`, registration data to `utilities`, and browser-free checks to `TestClases`. Removed the empty `fixtures` and `frameworktests` folders and updated imports, documentation and the explicit framework suite class list.

Fresh compilation covered all 24 Java files; Maven `verify` passed all 19 framework checks with no failures or skips. The initial sandboxed compilation could not close the JDK's `ct.sym` resource; the same clean build passed with access to the installed JDK. UI flows were not rerun for this package-only change. Generated `target/` output was removed after verification.

## Current verification: 2026-09-25

Removed unused configuration state and an unused, misleading logout method. Account selection now uses the value returned by its existing wait, without repeating the option filtering. No new dependencies, features or test cases were added.

- Existing framework checks: 19 passed, 0 failed, 0 skipped.
- Full Chrome UI suite: 11 passed, 2 failed, 0 skipped. Failure screenshots show Cloudflare CAPTCHA pages during registration for `testWrongPassword` and the savings-account case. These flows require verification on a controlled test deployment.
- The initial restricted-environment UI attempt failed during browser setup. Chrome sessions ran successfully outside that restriction; the two CAPTCHA failures above remained visible in the final results.
- Generated evidence was removed with `target/` during folder cleanup; screenshots were stored under `target/screenshots/` at that time. Current runs save screenshots under `screenshots/`, with Spark reports and test results under `target/reports/` and `target/surefire-reports/`. Firefox, Grid and GitHub Actions were not exercised in this review.

The evidence below records the earlier 2026-09-24 verification.

## Defects corrected

| Original problem | Result |
| --- | --- |
| Valid login printed a message without asserting | Login must reach Accounts Overview or the test fails |
| Invalid DDT branch was nested inside the valid branch; exceptions were swallowed | Every data row asserts its expected result; exceptions fail the test |
| Static WebDriver was shared between parallel runs | Per-thread sessions, created and closed for every test invocation |
| Setup/teardown only ran for selected groups and entire classes | `alwaysRun` method lifecycle covers every group and data row |
| Default/stale credentials made tests order-dependent | Unique bounded-length accounts are provisioned by positive tests |
| Page actions had no reliable synchronization | Explicit conditions for visibility, interaction and dynamic account data; zero implicit wait |
| Screenshot rename could silently fail; filenames could collide | Directory creation, byte writes, unique names and relative report links |
| Report state was overwritten across contexts; setup failures were omitted | One execution report with independent nodes, configuration failures and null-safe skipped results |
| Report opened a desktop browser at completion | Report files are generated without a desktop dependency |
| Excel streams leaked and color operations used an uninitialized output stream | Resource-safe reads/writes, preserved styles and tested reopening of workbooks |
| Workbook was reopened for every cell; malformed data was accepted | Bulk reading, formula formatting and header/outcome/empty-dataset validation |
| Account data provider referenced a nonexistent workbook | Removed unused broken provider; added executable checking/savings account coverage |
| No documented setup or CI checks | Maven wrapper, configuration guide, framework checks and GitHub Actions workflow |

## Verification evidence

Environment: Windows, Temurin Java 17.0.11, wrapper-managed Maven 3.9.16, Chrome 153.0.8010.53 and Edge 153.0.4234.48.

| Check | Result |
| --- | --- |
| Default framework checks / Maven `verify` | 19 passed, 0 failed, 0 skipped |
| Complete UI suite, Chrome headless | 13 passed, 0 failed, 0 skipped |
| Parallel smoke, Chrome and Edge headless | 6 passed, 0 failed, 0 skipped |
| Controlled unsupported-browser configuration | Expected build failure; HTML contains the setup failure and skipped test, including the original error |
| Failure screenshots | Captured during the live fixture validation failure, before browser teardown |
| XML configuration and suites | Parsed successfully |

These results record the original modernization runs. Generated evidence was stored under `target/validation/`, including a controlled configuration-failure probe, and was removed during framework cleanup. The results above are historical; rerun the commands in README.md to create fresh evidence. Generated reports are ignored by Git and are removed by cleaning the build.

The parallel validation used a temporary copy of the cross-browser suite with Chrome and Edge. Firefox options are covered by framework checks, but live Firefox and Selenium Grid were not exercised. GitHub Actions was authored and inspected locally; it has not been run on GitHub.

For the parallel run, TestNG's per-method results and the Spark HTML both contain all six successful invocations. The generated `TEST-TestSuite.xml` contains only three entries for the repeated classes across browser contexts. Use `testng-results.xml` or Spark for the complete cross-browser result; JUnit XML aggregation for this execution pattern remains a reporting limitation.

## Compatibility and remaining coverage

Surefire 3.6 was evaluated and replaced with 3.5.5 because [3.6 removes TestNG XML suite support](https://maven.apache.org/surefire-archives/surefire-LATEST/maven-surefire-plugin/whats-new-3-6-0.html). [ExtentReports is sunset](https://github.com/extent-framework/extentreports-java); 5.1.2 is retained for existing Spark report compatibility. All other dependency decisions and commands are in [README.md](README.md).

The legacy workbook's `res` header remains supported. Its existing accounts were not assumed to be valid on the shared demo, and the workbook was not modified.

The public demo can change independently of these tests. Use a dedicated deployment for continuous UI execution and environment-controlled test-data cleanup. Additional business coverage such as transfers, bill payments, loans and transaction history, plus API/accessibility checks, remains outside the implemented suites.
