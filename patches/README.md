# PB-UI-001 application patch candidate

This patch belongs to the ParaBank application, not the Selenium framework. It targets pinned application revision 98c1c9ab4889eb92c7798da63bb417e27261d3d5.

The Overview controller is annotation-mapped, but the login interceptor is attached only to the legacy SimpleUrlHandlerMapping. Registering that interceptor for /overview.htm through mvc:interceptors ensures authentication is checked before the required userSession argument is resolved. The existing login view contains the message expected by the test.

Apply from a separate ParaBank application checkout:

```text
git apply --check PB-UI-001-overview-authentication.patch
git apply PB-UI-001-overview-authentication.patch
```

Rebuild and deploy the application using its own build instructions. Validate logged-out Overview access and normal authenticated Overview access, then run the framework's Logout group and full UI suite against that deployment. Preserve the original failing baseline for comparison.

Validation performed: git apply --check succeeded against the downloaded pinned source. Runtime validation on 2026-10-07 passed all five Logout/AccountsOverview cases and all 93 UI regression cases (zero failures, errors or skips). Maven full-regression time was 4:31; TestNG wall time was 266.758 seconds. The candidate was applied to the downloaded source, rebuilt as a WAR, and its rebuilt configuration installed temporarily for local testing. After testing, the server was stopped and the original source XML, WAR and deployed XML restored with matching hashes. Selenium assertions were unchanged. Keep PB-UI-001 open until the application patch is verified. The focused patch covers Overview only; review other protected annotation-mapped routes separately.

The patched WAR and focused/full XML reports are preserved under target/patch-validation/PB-UI-001/. The original failing baseline remains under target/regression-baseline/sequential/. Default local startup and API CI jobs use the unpatched pinned application. The manual full UI and cross-browser jobs explicitly select the broader source overlay with -UiFixes; see application/ui-fixes/README.md. Upstream acceptance or an explicitly configured patched deployment is required for a permanent fix.
