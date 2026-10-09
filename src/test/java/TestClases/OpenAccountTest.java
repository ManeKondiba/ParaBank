package TestClases;

import PageObjects.HomePage;
import PageObjects.AccountDetailsPage;
import PageObjects.AccountsOverviewPage;
import utilities.TransactionData;
import utilities.UiLedgerAssertions;
import PageObjects.LoginPage;
import PageObjects.LogOut;
import PageObjects.OpenAccountPage;
import TestBases.AccountFixture;
import TestBases.BaseClass;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utilities.RegistrationData;

import java.math.BigDecimal;
import java.util.List;

public class OpenAccountTest extends BaseClass {

    @DataProvider
    public Object[][] accountTypes() {
        return new Object[][] {
            {"CHECKING"},
            {"SAVINGS"}
        };
    }

    @Test(dataProvider = "accountTypes", groups = {"OpenAccount", "Master", "Regression", "BankingCompatibility"})
    public void testOpenAccount(String accountType) {
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        List<String> beforeIds = overview.getAccountIds();
        String sourceId = beforeIds.get(0);
        BigDecimal sourceBefore = overview.getBalance(sourceId);
        AccountDetailsPage details = new AccountDetailsPage(getDriver());
        details.open(sourceId);
        List<TransactionData> ledgerBefore = details.getTransactions();
        new HomePage(getDriver()).clickOpenNewAccount();
        OpenAccountPage openAccountPage = new OpenAccountPage(getDriver());

        String fundingAccountId = openAccountPage.openAccount(accountType);
        String newAccountId = openAccountPage.getNewAccountId();

        Assert.assertEquals(openAccountPage.getConfirmationHeading(), "Account Opened!");
        Assert.assertNotEquals(newAccountId, fundingAccountId,
                "Opening an account must create a different account ID");

        openAccountPage.openAccountDetails();
        Assert.assertEquals(openAccountPage.getDetailAccountId(), newAccountId);
        Assert.assertEquals(openAccountPage.getAccountType(), accountType);
        BigDecimal deposit = openAccountPage.getAccountBalance();
        details.open(newAccountId);
        UiLedgerAssertions.singleEntry(List.of(), details.getTransactions(), "Credit", deposit);
        details.open(sourceId);
        UiLedgerAssertions.singleEntry(ledgerBefore, details.getTransactions(), "Debit", deposit);
        overview.open();
        Assert.assertEquals(overview.getAccountIds().size(), beforeIds.size() + 1);
        Assert.assertTrue(overview.getAccountIds().containsAll(beforeIds));
        Assert.assertTrue(overview.getAccountIds().contains(newAccountId));
        Assert.assertEquals(overview.getBalance(sourceId).compareTo(sourceBefore.subtract(deposit)), 0);
        Assert.assertEquals(overview.getTotalBalance().compareTo(sourceBefore), 0);
    }

    @Test(groups = {"OpenAccount", "Master", "Regression"})
    public void testAccountTypeOptionsAndDefaultFundingAccount() {
        AccountFixture.register(getDriver());
        new HomePage(getDriver()).clickOpenNewAccount();
        OpenAccountPage openAccountPage = new OpenAccountPage(getDriver());

        Assert.assertEquals(openAccountPage.getAccountTypes(), List.of("CHECKING", "SAVINGS"),
                "The customer must be able to choose either supported account type");
        Assert.assertEquals(openAccountPage.getSelectedAccountType(), "CHECKING");
        List<String> fundingAccounts = openAccountPage.getFundingAccountIds();
        Assert.assertEquals(fundingAccounts.size(), 1,
                "A newly registered customer must have one initial funding account");
    }

    @Test(groups = {"OpenAccount", "Master", "Regression"})
    public void testNewAccountCanFundAnotherAccount() {
        AccountFixture.register(getDriver());
        HomePage homePage = new HomePage(getDriver());
        homePage.clickOpenNewAccount();
        OpenAccountPage openAccountPage = new OpenAccountPage(getDriver());
        String originalAccountId = openAccountPage.openAccount("SAVINGS");
        String firstAccountId = openAccountPage.getNewAccountId();
        openAccountPage.openAccountDetails();
        Assert.assertEquals(openAccountPage.getDetailAccountId(), firstAccountId);
        String fundingDetailsUrl = openAccountPage.getAccountDetailsUrl();
        BigDecimal fundingBalanceBefore = openAccountPage.getAccountBalance();

        homePage.clickOpenNewAccount();
        Assert.assertTrue(openAccountPage.getFundingAccountIds().contains(firstAccountId),
                "A newly opened account must be available as a funding source");
        openAccountPage.openAccount("CHECKING", firstAccountId);
        String secondAccountId = openAccountPage.getNewAccountId();

        Assert.assertEquals(openAccountPage.getConfirmationHeading(), "Account Opened!");
        Assert.assertNotEquals(secondAccountId, originalAccountId);
        Assert.assertNotEquals(secondAccountId, firstAccountId);
        openAccountPage.openAccountDetails();
        Assert.assertEquals(openAccountPage.getDetailAccountId(), secondAccountId);
        Assert.assertEquals(openAccountPage.getAccountType(), "CHECKING");
        BigDecimal openingDeposit = openAccountPage.getAccountBalance();

        openAccountPage.revisitAccountDetails(fundingDetailsUrl);
        Assert.assertEquals(openAccountPage.getDetailAccountId(), firstAccountId);
        Assert.assertEquals(openAccountPage.getAccountBalance().compareTo(fundingBalanceBefore.subtract(openingDeposit)),
                0, "The selected funding account must be debited by the new account's opening deposit");
    }

    @Test(groups = {"OpenAccount", "Master", "Regression"})
    public void testNewAccountPersistsAfterLoginAgain() {
        RegistrationData customer = AccountFixture.register(getDriver());
        HomePage homePage = new HomePage(getDriver());
        homePage.clickOpenNewAccount();
        OpenAccountPage openAccountPage = new OpenAccountPage(getDriver());
        openAccountPage.openAccount("SAVINGS");
        String newAccountId = openAccountPage.getNewAccountId();
        openAccountPage.openAccountDetails();
        Assert.assertEquals(openAccountPage.getDetailAccountId(), newAccountId);
        openAccountPage.refreshAccountDetails();
        Assert.assertEquals(openAccountPage.getDetailAccountId(), newAccountId,
                "Refreshing account details must retain the created account");
        Assert.assertEquals(openAccountPage.getAccountType(), "SAVINGS");

        new LogOut(getDriver()).clickLogout();
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(customer.username(), customer.password());
        Assert.assertTrue(loginPage.isLoginSuccessDisplayed());
        homePage.clickOpenNewAccount();
        Assert.assertTrue(openAccountPage.getFundingAccountIds().contains(newAccountId),
                "The new account must remain linked to the customer after a new login");
    }
}
