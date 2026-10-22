package testclasses;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import base.AccountFixture;
import base.BankingFixture;
import base.BaseClass;
import pageobjects.AccountDetailsPage;
import pageobjects.AccountsOverviewPage;
import pageobjects.LogoutPage;
import pageobjects.ProtectedResourcePage;
import utilities.AppRoute;
import utilities.KnownDefect;

public class BankingAccessTest extends BaseClass {
    @DataProvider
    public Object[][] protectedResources() { return new Object[][] {{"account"}, {"transaction"}}; }

    @KnownDefect(id = "PB-SEC-001", description = "Customer B can read Customer A's account or transaction data")
    @Test(dataProvider = "protectedResources", groups = {"BankingAccess", "Banking", "Master", "Regression", "ProductionRules"},
            description = "VAL-002: customer B cannot read customer A's account or transaction")
    public void testOtherCustomerCannotReadProtectedResource(String resource) {
        var first = BankingFixture.registerWithTwoAccounts(getDriver());
        AccountDetailsPage details = new AccountDetailsPage(getDriver());
        details.open(first.sourceId());
        String transactionId = details.getTransactions().get(0).id();
        new LogoutPage(getDriver()).clickLogout();
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        Assert.assertFalse(overview.getAccountIds().contains(first.sourceId()));
        String route = resource.equals("account") ? AppRoute.activity(first.sourceId())
                : AppRoute.transaction(transactionId);
        ProtectedResourcePage page = new ProtectedResourcePage(getDriver());
        page.open(route);
        Assert.assertFalse(page.exposesResource(resource, resource.equals("account") ? first.sourceId() : transactionId),
                "A different customer must not see the requested customer's resource");
        Assert.assertTrue(page.isAccessErrorDisplayed(), "An inaccessible resource must show a controlled access error");
    }

    @KnownDefect(id = "PB-SEC-003", description = "Browser Back exposes cached Overview account data after logout")
    @Test(groups = {"BankingAccess", "Banking", "Master", "Regression", "ProductionRules"},
            description = "VAL-012: browser Back after logout must not expose cached account data")
    public void testBrowserBackAfterLogoutDoesNotExposeAccountData() {
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        String id = overview.getAccountIds().get(0);
        new LogoutPage(getDriver()).clickLogout();
        getDriver().navigate().back();
        ProtectedResourcePage page = new ProtectedResourcePage(getDriver());
        Assert.assertFalse(page.exposesOverviewAccount(id), "Browser Back must not expose cached authenticated account data");
    }

    @KnownDefect(id = "PB-SEC-003", description = "Browser Back exposes cached Overview account data after logout")
    @Test(groups = {"BankingAccess", "Banking", "Master", "Regression", "ProductionRules"},
            description = "VAL-012: session cookie invalidation requires re-authentication on protected routes")
    public void testSessionCookieInvalidationRequiresReAuthentication() {
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        String accountId = overview.getAccountIds().get(0);

        getDriver().manage().deleteAllCookies();

        getDriver().navigate().refresh();
        ProtectedResourcePage page = new ProtectedResourcePage(getDriver());
        Assert.assertFalse(page.exposesOverviewAccount(accountId),
                "Expired or invalidated session must not display sensitive account data");
        Assert.assertTrue(new LogoutPage(getDriver()).isLoggedOut(),
                "Session invalidation must force user back to logged-out state");
    }

    @KnownDefect(id = "PB-SEC-003", description = "Browser Back exposes cached Overview account data after logout")
    @Test(groups = {"BankingAccess", "Banking", "Master", "Regression", "ProductionRules"},
            description = "VAL-012: logout in a second tab must invalidate the session in the original tab")
    public void testSessionTerminationInNewTabReflectsAcrossWindows() {
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        String accountId = overview.getAccountIds().get(0);
        String originalWindow = getDriver().getWindowHandle();
        String currentUrl = getDriver().getCurrentUrl();

        getDriver().switchTo().newWindow(org.openqa.selenium.WindowType.TAB);
        getDriver().get(currentUrl);
        new LogoutPage(getDriver()).clickLogout();
        Assert.assertTrue(new LogoutPage(getDriver()).isLoggedOut(), "User must be logged out in new tab");
        getDriver().close();

        getDriver().switchTo().window(originalWindow);
        getDriver().navigate().refresh();
        ProtectedResourcePage page = new ProtectedResourcePage(getDriver());
        Assert.assertFalse(page.exposesOverviewAccount(accountId),
                "Original window must not retain access after server-side session logout");
        Assert.assertTrue(new LogoutPage(getDriver()).isLoggedOut(),
                "Original window must reflect the logged-out state");
    }
}
