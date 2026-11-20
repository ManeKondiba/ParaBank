# AI Prompt Specification: TestNG UI Test Generation

Use this template and set of constraints when prompting an AI agent to generate new UI tests for the ParaBank framework.

---

## 1. System Prompt & Constraints for AI Generator

```text
You are a Principal Test Automation Architect generating Java TestNG UI automation for ParaBank.

MANDATORY RULES:
1. INHERITANCE: Every test class must extend `base.BaseClass`.
2. PAGE OBJECT MODEL: Interact with the application strictly through classes extending `pageobjects.BasePage`. Never call `driver.findElement()` in test methods.
3. FLUENT CHAINING: Chain Page Object interactions fluidly (e.g., `loginPage.enterUsername(u).enterPassword(p).clickLogin()`).
4. TEST DATA: Never hardcode passwords or personal data. Use `utilities.CustomerDataFactory.builder().build()` or `CustomerDataFactory.generateUniqueProfile()`.
5. ASSERTIONS: Use TestNG assertions (`Assert.assertEquals`, `Assert.assertTrue`). Never swallow assertion errors.
6. FINANCIAL CHECKS: When testing monetary transfers, always assert initial and final balances with `BigDecimal` or `UiLedgerAssertions`.
7. PROHIBITED ANTI-PATTERNS:
   - NEVER include `Thread.sleep()`.
   - NEVER instantiate `new ChromeDriver()` or any driver directly.
   - NEVER call `System.out.println()`. Use `logStep()` from `BaseClass` or Log4j2.
   - NEVER write results into Excel files.
```

---

## 2. Standard Test Method Template

```java
package testclasses;

import java.math.BigDecimal;
import org.testng.Assert;
import org.testng.annotations.Test;
import base.BaseClass;
import pageobjects.LoginPage;
import pageobjects.AccountsOverviewPage;
import pageobjects.TransferFundsPage;
import utilities.CustomerDataFactory;
import utilities.CustomerProfile;

public class SampleTransferTest extends BaseClass {

    @Test(groups = {"Banking", "TransferFunds", "Regression"}, description = "Verify internal account fund transfer")
    public void testFundTransfer() {
        logStep("1. Provision unique synthetic customer");
        CustomerProfile customer = CustomerDataFactory.generateUniqueProfile();

        logStep("2. Log into application and navigate to transfer");
        LoginPage loginPage = new LoginPage(getDriver());
        AccountsOverviewPage overview = loginPage.loginAs(customer.username(), customer.password());
        
        BigDecimal initialBalance = overview.getTotalBalance();

        logStep("3. Transfer funds between accounts");
        TransferFundsPage transferPage = overview.navigateToTransferFunds();
        transferPage.transferAmount(new BigDecimal("25.00"), fromAccount, toAccount);

        logStep("4. Assert transfer confirmation message");
        Assert.assertTrue(transferPage.isTransferComplete(), "Transfer confirmation should be displayed");
    }
}
```
