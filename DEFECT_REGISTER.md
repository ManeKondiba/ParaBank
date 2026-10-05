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

## Resolution Policy

When an external ticket is created, add its URL/ID to the matching record. Update status and owner as triage proceeds. Close a record only after the defect is fixed or explicitly accepted, its expected behavior is documented, and the focused test plus relevant regression checks pass. If a test is quarantined, record the linked ticket, owner, reason, and expiry/review date here.