# ParaBank UI application repairs

These are modified source files from ParaSoft ParaBank revision `98c1c9ab4889eb92c7798da63bb417e27261d3d5`, maintained as an explicit local application variant. Upstream project: https://github.com/parasoft/parabank. Original license is preserved in LICENSE. Modifications made 2026-10-07: authenticated MVC route interception and resource ownership, full recovery identity comparison without SSN logging, browser history/cache protection, associated visible form labels, and loan input validation before the AJAX request.

This is application source, not Selenium assertion changes or a `.patch` artifact. Only the fifteen replaced/added source files are included. The overlay also includes AccessModeController.java to keep authentication results scoped to each request and remove credential logging. Excluded Contact Us, About, Services, News and Site Map content is not changed.

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-parabank.ps1 -UiFixes
.\mvnw.cmd -Pregression '-Dui.threads=2' '-Dheadless=true' '-DappUrl=http://127.0.0.1:8081/parabank/index.htm' verify
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/stop-parabank.ps1
```

The start script verifies the pinned source archive, extracts it into an isolated directory, copies this overlay, builds a separate WAR, and starts it on loopback. Source and deployment directories use a content fingerprint, so changing an overlay creates a fresh build; the full fingerprint is recorded in server.json. Default startup without `-UiFixes` preserves the original upstream application for comparison. Stop the managed server before switching variants. No changes are made to the public ParaBank demo.

The ownership gate protects the server-rendered activity/transaction pages. It does not secure the separate demo REST endpoints. Recovery now verifies all six identity fields, but the original successful recovery still discloses credentials; production password-reset tokens and removal of plaintext recovery remain separate work. Loan input validation improves the browser form; backend financial validation/concurrency/idempotency defects remain open. Passing UI acceptance is not a production security certification.
