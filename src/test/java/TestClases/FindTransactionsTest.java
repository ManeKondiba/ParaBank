package TestClases;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import PageObjects.AccountDetailsPage;
import PageObjects.AccountsOverviewPage;
import PageObjects.FindTransactionsPage;
import PageObjects.FindTransactionsPage.SearchType;
import PageObjects.TransferFundsPage;
import PageObjects.TransactionDetailsPage;
import PageObjects.TransactionTable;
import TestBases.AccountFixture;
import TestBases.BankingFixture;
import TestBases.BaseClass;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utilities.TransactionData;

public class FindTransactionsTest extends BaseClass {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final BigDecimal TRANSFER_AMOUNT = new BigDecimal("12.34");

    @DataProvider
    public Object[][] searchTypes() {
        return new Object[][] {{SearchType.ID}, {SearchType.DATE}, {SearchType.DATE_RANGE}, {SearchType.AMOUNT}};
    }

    @Test(dataProvider = "searchTypes", groups = {"FindTransactions", "Banking", "Master", "Regression"})
    public void testFindOwnTransfer(SearchType searchType) {
        BankingFixture.Accounts accounts = BankingFixture.registerWithTwoAccounts(getDriver());
        AccountDetailsPage details = new AccountDetailsPage(getDriver());
        details.open(accounts.sourceId());
        Assert.assertTrue(details.getBalance().compareTo(TRANSFER_AMOUNT) >= 0,
                "The test environment must supply enough funds for a transfer");
        List<String> existingIds = details.getTransactions().stream().map(TransactionData::id).toList();
        TransferFundsPage transfer = new TransferFundsPage(getDriver());
        transfer.open();
        transfer.transfer(TRANSFER_AMOUNT.toPlainString(), accounts.sourceId(), accounts.destinationId());
        Assert.assertEquals(transfer.getConfirmationHeading(), "Transfer Complete!");
        details.open(accounts.sourceId());
        TransactionData expected = details.getTransactions().stream()
                .filter(row -> !existingIds.contains(row.id()) && row.debit().compareTo(TRANSFER_AMOUNT) == 0)
                .findFirst().orElseThrow(() -> new AssertionError("The new transfer debit must exist before searching"));
        new TransactionTable(getDriver()).openTransaction(expected.id());
        TransactionDetailsPage transactionDetails = new TransactionDetailsPage(getDriver());
        Assert.assertEquals(transactionDetails.getId(), expected.id());
        // Searches use server dates, as displayed in transaction details, rather than browser-local activity dates.
        String serverDate = transactionDetails.getDate();
        LocalDate transactionDate = LocalDate.parse(serverDate, DATE_FORMAT);
        String value = switch (searchType) {
            case ID -> expected.id();
            case DATE -> serverDate;
            case DATE_RANGE -> transactionDate.minusDays(1).format(DATE_FORMAT);
            case AMOUNT -> TRANSFER_AMOUNT.toPlainString();
        };
        FindTransactionsPage search = new FindTransactionsPage(getDriver());
        search.open();
        search.search(searchType, accounts.sourceId(), value, transactionDate.plusDays(1).format(DATE_FORMAT));
        List<TransactionData> results = search.getResults();
        Assert.assertTrue(results.contains(expected), "Search results must contain the exact transaction from account activity");
        if (searchType == SearchType.ID) {
            Assert.assertEquals(results.size(), 1, "An ID search must return only the requested transaction");
        }
        for (TransactionData row : results) {
            switch (searchType) {
                case ID -> Assert.assertEquals(row.id(), expected.id());
                case DATE -> {
                    transactionDetails.open(row.id());
                    Assert.assertEquals(transactionDetails.getId(), row.id());
                    Assert.assertEquals(transactionDetails.getDate(), serverDate);
                }
                case DATE_RANGE -> {
                    transactionDetails.open(row.id());
                    Assert.assertEquals(transactionDetails.getId(), row.id());
                    LocalDate found = LocalDate.parse(transactionDetails.getDate(), DATE_FORMAT);
                    Assert.assertFalse(found.isBefore(transactionDate.minusDays(1)) || found.isAfter(transactionDate.plusDays(1)));
                }
                case AMOUNT -> Assert.assertTrue(row.debit().compareTo(TRANSFER_AMOUNT) == 0
                        || row.credit().compareTo(TRANSFER_AMOUNT) == 0, "Every result must match the requested amount");
            }
        }
    }

    @DataProvider
    public Object[][] invalidCriteria() {
        return new Object[][] {
            {SearchType.ID, "", "", "Invalid transaction ID"},
            {SearchType.ID, "unknown", "", "Invalid transaction ID"},
            {SearchType.DATE, "", "", "Invalid date format"},
            {SearchType.DATE, "2026/09/28", "", "Invalid date format"},
            {SearchType.DATE_RANGE, "", "", "Invalid date format"},
            {SearchType.DATE_RANGE, "01-01-2026", "invalid", "Invalid date format"},
            {SearchType.AMOUNT, "", "", "Invalid amount"},
            {SearchType.AMOUNT, "invalid", "", "Invalid amount"}
        };
    }

    @Test(dataProvider = "invalidCriteria", groups = {"FindTransactions", "Banking", "Master", "Regression"})
    public void testInvalidSearchCriteria(SearchType type, String value, String endDate, String error) {
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        String id = overview.getAccountIds().get(0);
        FindTransactionsPage search = new FindTransactionsPage(getDriver());
        search.open();
        search.search(type, id, value, endDate);
        Assert.assertEquals(search.getValidationError(type), error);
        Assert.assertTrue(search.isSearchFormDisplayed(), "Invalid criteria must leave the search form available");
    }

    @Test(groups = {"FindTransactions", "Banking", "Master", "Regression"})
    public void testSearchWithNoMatchesReturnsEmptyResults() {
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        String id = overview.getAccountIds().get(0);
        FindTransactionsPage search = new FindTransactionsPage(getDriver());
        search.open();
        search.search(SearchType.AMOUNT, id, "42.37", "");
        Assert.assertTrue(search.getResults().isEmpty(), "An unused account must not return fabricated transactions");
    }
}
