package TestClases;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import TestBases.AccountFixture;
import TestBases.BankingFixture;
import TestBases.BaseClass;
import PageObjects.AccountDetailsPage;
import PageObjects.AccountsOverviewPage;
import PageObjects.LogOut;
import PageObjects.ProtectedResourcePage;

public class BankingAccessTest extends BaseClass {
    @DataProvider
    public Object[][] protectedResources() { return new Object[][] {{"account"}, {"transaction"}}; }

    @Test(dataProvider = "protectedResources", groups = {"BankingAccess", "Banking", "Master", "Regression", "ProductionRules"},
            description = "VAL-002: customer B cannot read customer A's account or transaction")
    public void testOtherCustomerCannotReadProtectedResource(String resource) {
        var first = BankingFixture.registerWithTwoAccounts(getDriver());
        AccountDetailsPage details = new AccountDetailsPage(getDriver());
        details.open(first.sourceId());
        String transactionId = details.getTransactions().get(0).id();
        new LogOut(getDriver()).clickLogout();
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        Assert.assertFalse(overview.getAccountIds().contains(first.sourceId()));
        String route = resource.equals("account") ? "activity.htm?id=" + first.sourceId()
                : "transaction.htm?id=" + transactionId;
        ProtectedResourcePage page = new ProtectedResourcePage(getDriver());
        page.open(route);
        Assert.assertFalse(page.exposesResource(resource, resource.equals("account") ? first.sourceId() : transactionId),
                "A different customer must not see the requested customer's resource");
        Assert.assertTrue(page.isAccessErrorDisplayed(), "An inaccessible resource must show a controlled access error");
    }

    @Test(groups = {"BankingAccess", "Banking", "Master", "Regression", "ProductionRules"},
            description = "VAL-012: browser Back after logout must not expose cached account data")
    public void testBrowserBackAfterLogoutDoesNotExposeAccountData() {
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        String id = overview.getAccountIds().get(0);
        new LogOut(getDriver()).clickLogout();
        getDriver().navigate().back();
        ProtectedResourcePage page = new ProtectedResourcePage(getDriver());
        Assert.assertFalse(page.exposesOverviewAccount(id), "Browser Back must not expose cached authenticated account data");
    }
}