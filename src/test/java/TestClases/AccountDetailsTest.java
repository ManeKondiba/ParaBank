package TestClases;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import PageObjects.AccountDetailsPage;
import PageObjects.AccountsOverviewPage;
import PageObjects.TransactionDetailsPage;
import PageObjects.TransactionTable;
import TestBases.AccountFixture;
import TestBases.BankingFixture;
import TestBases.BaseClass;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utilities.TransactionData;

public class AccountDetailsTest extends BaseClass {
    @Test(groups = {"AccountDetails", "Banking", "Master", "Regression"})
    public void testFreshAccountHasNoTransactionHistory() {
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        String id = overview.getAccountIds().get(0);
        AccountDetailsPage details = new AccountDetailsPage(getDriver());
        details.open(id);
        Assert.assertEquals(details.getAccountId(), id);
        Assert.assertTrue(details.getTransactions().isEmpty());
        Assert.assertTrue(details.isNoTransactionsDisplayed(), "An unused account must show the empty activity state");
    }

    @Test(groups = {"AccountDetails", "Banking", "Master", "Regression"})
    public void testOpeningDepositLinksToMatchingTransactionDetails() {
        BankingFixture.Accounts accounts = BankingFixture.registerWithTwoAccounts(getDriver());
        AccountDetailsPage details = new AccountDetailsPage(getDriver());
        details.open(accounts.destinationId());
        BigDecimal deposit = details.getBalance();
        List<TransactionData> transactions = details.getTransactions();
        Assert.assertEquals(transactions.size(), 1, "A new savings account must have one opening-deposit transaction");
        TransactionData transaction = transactions.get(0);
        Assert.assertEquals(transaction.credit().compareTo(deposit), 0);
        Assert.assertEquals(transaction.debit().compareTo(BigDecimal.ZERO), 0);
        new TransactionTable(getDriver()).openTransaction(transaction.id());
        TransactionDetailsPage transactionDetails = new TransactionDetailsPage(getDriver());
        Assert.assertEquals(transactionDetails.getId(), transaction.id());
        Assert.assertEquals(transactionDetails.getDescription(), transaction.description());
        // Details use the server timezone; activity formats dates in the browser timezone.
        LocalDate.parse(transactionDetails.getDate(), DateTimeFormatter.ofPattern("MM-dd-uuuu")
                .withResolverStyle(ResolverStyle.STRICT));
        Assert.assertEquals(transactionDetails.getType(), "Credit");
        Assert.assertEquals(transactionDetails.getAmount().compareTo(deposit), 0);
    }

    @DataProvider
    public Object[][] activityTypes() {
        return new Object[][] {{"Debit"}, {"Credit"}};
    }

    @Test(dataProvider = "activityTypes", groups = {"AccountDetails", "Banking", "Master", "Regression"})
    public void testActivityTypeFilter(String type) {
        BankingFixture.Accounts accounts = BankingFixture.registerWithTwoAccounts(getDriver());
        AccountDetailsPage details = new AccountDetailsPage(getDriver());
        // The source has the opening-transfer debit; the destination has the corresponding credit.
        String id = type.equals("Debit") ? accounts.sourceId() : accounts.destinationId();
        details.open(id);
        List<TransactionData> openingTransactions = details.getTransactions();
        Assert.assertFalse(openingTransactions.isEmpty(), "Opening the savings account must create account activity");
        TransactionData expected = openingTransactions.get(0);
        details.filterActivity("All", type);
        List<TransactionData> selected = details.getTransactions();
        Assert.assertFalse(selected.isEmpty());
        Assert.assertTrue(selected.stream().anyMatch(transaction -> transaction.id().equals(expected.id())));
        for (TransactionData transaction : selected) {
            BigDecimal oppositeColumn = type.equals("Debit") ? transaction.credit() : transaction.debit();
            Assert.assertEquals(oppositeColumn.compareTo(BigDecimal.ZERO), 0,
                    "Filtered activity must not contain the opposite transaction type");
        }
        details.filterActivity("All", type.equals("Debit") ? "Credit" : "Debit");
        Assert.assertTrue(details.getTransactions().isEmpty(), "The opposite filter must not show the opening transfer");
        Assert.assertTrue(details.isNoTransactionsDisplayed());
    }
}
