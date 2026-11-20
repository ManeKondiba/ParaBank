# API Test Automation & Review Rules

When authoring or modifying API automation code, all agents must adhere to the following rules:

1. **Strict Framework Separation**:
   - API tests MUST extend `base.ApiBaseTest`.
   - Never import Selenium WebDriver or Page Objects in any class under `testclasses.api`.

2. **Client Layer Encapsulation**:
   - Tests MUST interact through domain clients in `utilities.api.clients`.
   - Never call `RestAssured.given()` directly inside `@Test` methods.

3. **Fixture Isolation**:
   - Tests MUST use `this.fixture` (`fixture.customer()`, `fixture.fundingAccount()`).
   - Never hardcode usernames, customer IDs, or account numbers.

4. **Ledger Invariant Enforcement**:
   - Monetary transactions MUST assert balance change: `balance_after == balance_before +/- amount`.
   - Ledger entries MUST be asserted via `utilities.api.assertions.ApiAssertions`.

5. **Secrets & PII Redaction**:
   - Passwords, SSNs, and session tokens MUST NEVER be logged in plain text.
   - All HTTP traffic MUST flow through `ApiEvidenceFilter` with automatic redaction.
