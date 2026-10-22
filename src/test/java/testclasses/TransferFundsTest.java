package testclasses;

import java.math.BigDecimal;
import java.util.List;
import pageobjects.AccountDetailsPage;
import pageobjects.AccountsOverviewPage;
import pageobjects.TransferFundsPage;
import base.BankingFixture;
import base.BaseClass;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utilities.TransactionData;
import utilities.UiLedgerAssertions;
import utilities.KnownDefect;

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

    @KnownDefect(id = "PB-FIN-003", description = "Rapid double transfer submission applies transfer multiple times due to missing idempotency")
    @Test(groups = {"TransferFunds", "Banking", "Master", "Regression", "ProductionRules"},
            description = "VAL-007: rapid double-click submission must apply transfer exactly once")
    public void testRapidDoubleSubmissionAppliesTransferOnce() {
        BankingFixture.Accounts accounts = BankingFixture.prepareTransferAccounts(getDriver());
        BigDecimal amount = new BigDecimal("5.00");
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        BigDecimal sourceBefore = overview.getBalance(accounts.sourceId());
        BigDecimal destinationBefore = overview.getBalance(accounts.destinationId());
        BigDecimal totalBefore = overview.getTotalBalance();

        AccountDetailsPage details = new AccountDetailsPage(getDriver());
        details.open(accounts.sourceId());
        List<TransactionData> sourceTransactionsBefore = details.getTransactions();
        details.open(accounts.destinationId());
        List<TransactionData> destinationTransactionsBefore = details.getTransactions();

        TransferFundsPage transfer = new TransferFundsPage(getDriver());
        transfer.open();
        transfer.doubleClickTransfer(amount.toPlainString(), accounts.sourceId(), accounts.destinationId());
        Assert.assertEquals(transfer.getConfirmationHeading(), "Transfer Complete!");

        overview.open();
        BigDecimal sourceAfter = overview.getBalance(accounts.sourceId());
        BigDecimal destinationAfter = overview.getBalance(accounts.destinationId());
        BigDecimal totalAfter = overview.getTotalBalance();

        Assert.assertEquals(totalAfter.compareTo(totalBefore), 0,
                "Double submission must conserve total customer balance");
        Assert.assertEquals(sourceAfter.compareTo(sourceBefore.subtract(amount)), 0,
                "Double submission must debit source account only once");
        Assert.assertEquals(destinationAfter.compareTo(destinationBefore.add(amount)), 0,
                "Double submission must credit destination account only once");

        details.open(accounts.sourceId());
        TransactionData debit = UiLedgerAssertions.singleEntry(
                sourceTransactionsBefore, details.getTransactions(), "Debit", amount);
        Assert.assertEquals(debit.description(), "Funds Transfer Sent");

        details.open(accounts.destinationId());
        TransactionData credit = UiLedgerAssertions.singleEntry(
                destinationTransactionsBefore, details.getTransactions(), "Credit", amount);
        Assert.assertEquals(credit.description(), "Funds Transfer Received");
    }
}
