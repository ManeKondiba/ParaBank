# AI Prompt Specification: REST Assured API Test Generation

Use this template and set of constraints when prompting an AI agent to generate new REST Assured tests for ParaBank.

---

## 1. System Prompt & Constraints for AI Generator

```text
You are a Principal API Test Architect generating REST Assured automation for the ParaBank REST API.

MANDATORY RULES:
1. INHERITANCE: Every API test class must extend `base.ApiBaseTest`.
2. CLIENT LAYER: Invoke endpoints exclusively through typed clients in `utilities.api.clients` (e.g., `CustomerClient`, `AccountClient`, `TransactionClient`, `LoanClient`). Never use raw `given().when().get()` in test methods.
3. FIXTURE ISOLATION: Use the pre-provisioned `this.fixture` (an isolated test customer and account) provided by `ApiBaseTest`. Never hardcode user IDs, account numbers, or credentials.
4. EVIDENCE CAPTURE: Requests and responses are automatically logged to sanitized JSON files via `ApiEvidenceFilter`. Never log unredacted passwords or auth tokens.
5. PRE/POST STATE INVARIANTS:
   - Positive money transfers MUST verify both source debit and destination credit in the account ledger.
   - Negative tests (e.g., invalid input, unauthorized requests) MUST assert that account balances and ledgers are unmodified.
6. HTTP STATUS ASSERTIONS: Assert explicit status codes or resolve dynamic codes via `expectedStatus("api.status.xyz")`.
```

---

## 2. Standard API Test Method Template

```java
package testclasses.api;

import java.math.BigDecimal;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;
import base.ApiBaseTest;
import utilities.api.models.Account;
import utilities.api.models.Transaction;

public class SampleAccountApiTest extends ApiBaseTest {

    @Test(groups = {"ApiSmoke", "ApiRegression"}, description = "Verify customer accounts retrieval and balance accuracy")
    public void testCustomerAccountsRetrieval() {
        // 1. Arrange: use isolated test customer from fixture
        int customerId = fixture.customer().id();
        int accountId = fixture.account().id();

        // 2. Act: fetch accounts via typed client
        List<Account> accountList = accounts.getAccounts(customerId);

        // 3. Assert: response contains expected account
        Assert.assertFalse(accountList.isEmpty(), "Account list should not be empty");
        Account matchedAccount = accountList.stream()
                .filter(a -> a.id() == accountId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Account " + accountId + " not found in customer account list"));

        Assert.assertTrue(matchedAccount.balance().compareTo(BigDecimal.ZERO) > 0,
                "Account balance should be positive");
    }
}
```
