package TestClases;

import java.math.BigDecimal;
import java.util.List;
import PageObjects.AccountDetailsPage;
import PageObjects.AccountsOverviewPage;
import PageObjects.TransferFundsPage;
import TestBases.BankingFixture;
import TestBases.BaseClass;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utilities.TransactionData;
import utilities.UiLedgerAssertions;

public class TransferFundsTest extends BaseClass {
    @DataProvider
    public Object[][] transferAmounts() {
        return new Object[][] {{"1.00"}, {"12.34"}};
    }

    @Test(dataProvider = "transferAmounts", groups = {"TransferFunds", "Banking", "BankingSmoke", "Master", "Regression"})
    public void testTransferUpdatesBothBalancesAndActivity(String value) {
        BankingFixture.Accounts accounts = BankingFixture.prepareTransferAccounts(getDriver());
        BigDecimal amount = new BigDecimal(value);
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        BigDecimal sourceBefore = overview.getBalance(accounts.sourceId());
        BigDecimal destinationBefore = overview.getBalance(accounts.destinationId());
        BigDecimal totalBefore = overview.getTotalBalance();
        Assert.assertTrue(sourceBefore.compareTo(amount) >= 0, "Test environment must provide enough initial funds");

        AccountDetailsPage details = new AccountDetailsPage(getDriver());
        details.open(accounts.sourceId());
        List<TransactionData> sourceTransactionsBefore = details.getTransactions();
        details.open(accounts.destinationId());
        List<TransactionData> destinationTransactionsBefore = details.getTransactions();

        TransferFundsPage transfer = new TransferFundsPage(getDriver());
        transfer.open();
        transfer.transfer(value, accounts.sourceId(), accounts.destinationId());
        Assert.assertEquals(transfer.getConfirmationHeading(), "Transfer Complete!");
        Assert.assertEquals(transfer.getConfirmedAmount().compareTo(amount), 0);
        Assert.assertEquals(transfer.getFromAccountId(), accounts.sourceId());
        Assert.assertEquals(transfer.getToAccountId(), accounts.destinationId());

        overview.open();
        Assert.assertEquals(overview.getBalance(accounts.sourceId()).compareTo(sourceBefore.subtract(amount)), 0);
        Assert.assertEquals(overview.getBalance(accounts.destinationId()).compareTo(destinationBefore.add(amount)), 0);
        Assert.assertEquals(overview.getTotalBalance().compareTo(totalBefore), 0,
                "An internal transfer must preserve the customer's total balance");
        details.open(accounts.sourceId());
        TransactionData debit = UiLedgerAssertions.singleEntry(sourceTransactionsBefore, details.getTransactions(), "Debit", amount);
        Assert.assertEquals(debit.description(), "Funds Transfer Sent");
        details.open(accounts.destinationId());
        TransactionData credit = UiLedgerAssertions.singleEntry(destinationTransactionsBefore, details.getTransactions(), "Credit", amount);
        Assert.assertEquals(credit.description(), "Funds Transfer Received");
        Assert.assertNotEquals(debit.id(), credit.id());
    }

    @DataProvider
    public Object[][] invalidAmounts() {
        return new Object[][] {{""}, {"not-a-number"}};
    }

    @Test(dataProvider = "invalidAmounts", groups = {"TransferFunds", "Banking", "Master", "Regression", "BankingCompatibility"})
    public void testInvalidAmountDoesNotMoveMoney(String amount) {
        BankingFixture.Accounts accounts = BankingFixture.prepareTransferAccounts(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        BigDecimal sourceBefore = overview.getBalance(accounts.sourceId());
        BigDecimal destinationBefore = overview.getBalance(accounts.destinationId());
        AccountDetailsPage details = new AccountDetailsPage(getDriver());
        details.open(accounts.sourceId());
        List<TransactionData> sourceLedger = details.getTransactions();
        details.open(accounts.destinationId());
        List<TransactionData> destinationLedger = details.getTransactions();
        TransferFundsPage transfer = new TransferFundsPage(getDriver());
        transfer.open();
        transfer.transfer(amount, accounts.sourceId(), accounts.destinationId());
        Assert.assertFalse(transfer.getError().isBlank(), "The invalid transfer must be rejected");
        overview.open();
        Assert.assertEquals(overview.getBalance(accounts.sourceId()).compareTo(sourceBefore), 0);
        Assert.assertEquals(overview.getBalance(accounts.destinationId()).compareTo(destinationBefore), 0);
        details.open(accounts.sourceId());
        UiLedgerAssertions.sameRecords(details.getTransactions(), sourceLedger);
        details.open(accounts.destinationId());
        UiLedgerAssertions.sameRecords(details.getTransactions(), destinationLedger);
    }
}
