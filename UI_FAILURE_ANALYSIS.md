# Analysis and repair of 42 UI failures

Date: 2026-10-07. The original two executions covered 147 UI invocations: 105 passed, 42 failed, zero errors/skips. The failures were real application behavior, rather than missing browser drivers or an incorrect Maven suite selection.

| Failure family | Cases | Root cause | Application repair |
| --- | ---: | --- | --- |
| Anonymous and logged-out protected routes | 18 | Login interceptor was only attached to legacy handler mappings; annotated controllers bypassed it | Register authentication on all nine protected MVC routes before controller execution |
| Mismatched recovery identity | 6 | Recovery queried SSN alone and ignored supplied name/address | Compare first name, last name, street, city, state and ZIP before creating a session or revealing results; avoid logging SSN |
| Malformed loan fields | 6 | Raw values were sent to the service and its generic internal error shown | Validate complete finite numeric values before submission; keep form visible and identify invalid fields with actionable messages |
| Other customer's account/transaction | 2 | Controllers loaded resource IDs without comparing customer ownership | Resolve resource ownership against the authenticated session; deny missing/non-owned IDs with the same safe error |
| Browser Back after logout | 1 | Prior authenticated DOM was restored from browser history | Send no-store headers, clear customer panels on pagehide, and reload restored pages to recheck server session |
| Unnamed banking form controls | 9 | Visual labels were bold text without programmatic control associations | Associate existing visible labels with control IDs, including the dynamically generated bill-pay phone ID |
| **Total** | **42** | | |

The source repairs are in `application/ui-fixes/src/main/`. Startup explicitly selects `scripts/start-parabank.ps1 -UiFixes`. The original upstream deployment and all prior failing evidence are retained. No test assertions, production groups, scenario gates or retries were weakened to pass these checks. The two manually dispatched GitHub full UI/cross-browser jobs select this application variant; other jobs retain their original targets. Hosted jobs have not been executed for these local changes.

## Verification

First focused pass: 42 production UI scenarios executed, 40 passed, 2 failed, zero errors/skips. Remaining failures identified missing Open Account and Transfer Funds labels; those were corrected before the final build. Evidence: `target/ui-fix-verification/focused-first/testng-results.xml`.

First full regression (two workers): 147 cases executed, 146 passed, 1 failed, zero errors/skips. All 42 original failures passed; the run exposed a Bill Pay label-ID collision with existing confirmation spans. Input IDs were renamed to `billpay-amount` / `billpay-fromAccountId`, retaining the confirmation IDs and existing selectors. Evidence: `target/ui-fix-verification/full-first/testng-results.xml`.

Final full regression (three workers, final source fingerprint `9fe29a73934dcbca6416018e5a4beca765e8f7e3dcc381dcdb0aafe2983f74e9`): **147 executed, 147 passed, zero failures/errors/skips**. Maven BUILD SUCCESS; scenario-count gate passed. TestNG wall time 377.556 seconds; Maven total 6:23. This is functional verification, not a performance improvement claim. Evidence: `target/ui-fix-verification/full-final/`; deployment metadata: `target/ui-fix-verification/deployment.json`.

The latest attempted cross-browser run on 2026-10-08 did not pass: `target/ui-fix-browsers-final.log` records test failures and skipped cases. Acceptance for all 63 browser invocations remains unverified. The 2026-10-09 suite dry run confirmed selection of 63 cases only; it did not launch browsers or establish an application pass.

The current overlay also includes a later AccessModeController.java change that keeps login results local to each call instead of shared across requests. The 147-pass result above applies to its recorded earlier fingerprint; UI regression and cross-browser execution have not been repeated against the current fifteen-file overlay.

## Limits

This repair addresses the 42 UI failures, not the separately recorded 53 API production failures. Direct REST authentication, financial validation, atomicity and idempotency require backend work. Matching recovery identity prevents the six observed mismatches; the demo's plaintext successful recovery still needs replacement before production use. Accessible names do not establish full WCAG compliance. The five public pages excluded by the user remain outside the test expansion.
