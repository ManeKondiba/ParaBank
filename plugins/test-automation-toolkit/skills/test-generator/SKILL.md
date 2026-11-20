---
name: test-generator
description: Generates robust, deterministic TestNG tests for unit, integration, and UI workflows following TDD and clean automation design.
---

# Test Automation Toolkit Skill

This skill provides templates, design rules, and data generation workflows for authoring TestNG tests.

---

## 1. Test Authoring Principles
- **Inheritance**: UI tests MUST extend `base.BaseClass`.
- **Data Provisioning**: Use `utilities.CustomerDataFactory.generateUniqueProfile()` for synthetic user creation.
- **Fluent Invocation**: Test actions must chain smoothly across Page Objects:
  ```java
  loginPage.loginAs(user, pass)
           .navigateToTransferFunds()
           .transferAmount(amount, fromId, toId);
  ```
- **Determinism**: Every test method must be self-contained; no test method may depend on the state left by another test.
- **Assertion Granularity**: Group assertions or assert specific domain states (e.g. balance math).

---

## 2. Test Scaffolding Template

```java
package testclasses;

import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseClass;
import pageobjects.LoginPage;
import pageobjects.OpenAccountPage;
import utilities.CustomerDataFactory;
import utilities.CustomerProfile;

public class SampleAccountOpeningTest extends BaseClass {

    @Test(groups = {"Banking", "Regression"}, description = "Verify opening a new savings account")
    public void testOpenSavingsAccount() {
        logStep("1. Provision synthetic test user");
        CustomerProfile profile = CustomerDataFactory.generateUniqueProfile();

        logStep("2. Log in and navigate to open account page");
        LoginPage loginPage = new LoginPage(getDriver());
        OpenAccountPage openAccountPage = loginPage.loginAs(profile.username(), profile.password())
                                                   .navigateToOpenAccount();

        logStep("3. Select savings account type and submit");
        openAccountPage.selectAccountType("SAVINGS")
                       .submitAccountOpening();

        logStep("4. Assert new account confirmation");
        Assert.assertTrue(openAccountPage.isAccountOpened(), "New account confirmation should appear");
    }
}
```
