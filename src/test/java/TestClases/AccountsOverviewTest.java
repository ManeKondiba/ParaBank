package TestClases;

import java.math.BigDecimal;
import java.util.List;
import PageObjects.AccountDetailsPage;
import PageObjects.AccountsOverviewPage;
import TestBases.AccountFixture;
import TestBases.BankingFixture;
import TestBases.BaseClass;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AccountsOverviewTest extends BaseClass {
    @Test(groups = {"AccountsOverview", "Banking", "BankingSmoke", "Master", "Regression"})
    public void testInitialAccountAndDetailsAgree() {
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        List<String> accountIds = overview.getAccountIds();
        Assert.assertEquals(accountIds.size(), 1);
        String id = accountIds.get(0);
        BigDecimal balance = overview.getBalance(id);
        BigDecimal available = overview.getAvailableBalance(id);
        Assert.assertEquals(overview.getTotalBalance().compareTo(balance), 0);
        Assert.assertEquals(available.compareTo(balance.max(BigDecimal.ZERO)), 0);
        overview.openAccount(id);
        AccountDetailsPage details = new AccountDetailsPage(getDriver());
        Assert.assertEquals(details.getAccountId(), id);
        Assert.assertEquals(details.getAccountType(), "CHECKING");
        Assert.assertEquals(details.getBalance().compareTo(balance), 0);
        Assert.assertEquals(details.getAvailableBalance().compareTo(available), 0);
    }

    @Test(groups = {"AccountsOverview", "Banking", "Master", "Regression"})
    public void testOverviewIncludesBothAccountsAndCorrectTotal() {
        BankingFixture.Accounts accounts = BankingFixture.registerWithTwoAccounts(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        List<String> ids = overview.getAccountIds();
        Assert.assertEquals(ids.size(), 2);
        Assert.assertTrue(ids.containsAll(List.of(accounts.sourceId(), accounts.destinationId())));
        BigDecimal sum = ids.stream().map(overview::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        Assert.assertEquals(overview.getTotalBalance().compareTo(sum), 0,
                "Overview total must equal the sum of the customer's account balances");
    }
}
