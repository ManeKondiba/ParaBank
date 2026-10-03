package TestClases.api;

import static utilities.api.assertions.ApiAssertions.account;
import static utilities.api.assertions.ApiAssertions.money;
import static utilities.api.assertions.ApiAssertions.singleTransaction;
import static utilities.api.assertions.ApiAssertions.success;
import static utilities.api.assertions.ApiAssertions.transactionList;

import java.math.BigDecimal;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import TestBases.ApiBaseTest;
import utilities.api.models.Account;

@Test(groups = "ApiRegression")
public class MoneyMovementApiTest extends ApiBaseTest {
    @DataProvider
    public Object[][] depositAmounts() {
        return new Object[][] {{"0.01"}, {"12.34"}, {"1000000.99"}};
    }

    @Test(dataProvider = "depositAmounts", groups = "ApiSmoke",
            description = "DEP-001/002: deposits including minimum-cent and large values credit the exact balance and ledger")
    public void depositCreditsBalanceAndLedger(String value) {
        BigDecimal amount = new BigDecimal(value);
        int id = fixture.fundingAccount().id();
        Account before = account(accounts.get(id));
        var ledgerBefore = transactionList(transactions.list(id));

        success(accounts.deposit(id, amount));

        money(account(accounts.get(id)).balance(), before.balance().add(amount));
        singleTransaction(ledgerBefore, transactionList(transactions.list(id)), id, "Credit", amount);
    }

    @DataProvider
    public Object[][] withdrawalAmounts() {
        return new Object[][] {{"0.01"}, {"12.34"}};
    }

    @Test(dataProvider = "withdrawalAmounts", groups = "ApiSmoke",
            description = "WDR-001/002: withdrawals including minimum-cent value debit the exact balance and ledger")
    public void withdrawalDebitsBalanceAndLedger(String value) {
        BigDecimal amount = new BigDecimal(value);
        int id = fixture.fundingAccount().id();
        Account before = account(accounts.get(id));
        Assert.assertTrue(before.balance().compareTo(amount) >= 0, "The isolated fixture must fund this withdrawal");
        var ledgerBefore = transactionList(transactions.list(id));

        success(accounts.withdraw(id, amount));

        money(account(accounts.get(id)).balance(), before.balance().subtract(amount));
        singleTransaction(ledgerBefore, transactionList(transactions.list(id)), id, "Debit", amount);
    }

    @Test(groups = "ApiSmoke", description = "XFER-001: transfer conserves the total balance and creates matching debit and credit")
    public void transferConservesFundsAndReconcilesBothLedgers() {
        Account destination = fixture.createAccount(1);
        Account source = account(accounts.get(fixture.fundingAccount().id()));
        var sourceLedger = transactionList(transactions.list(source.id()));
        var destinationLedger = transactionList(transactions.list(destination.id()));
        BigDecimal amount = new BigDecimal("15.27");
        Assert.assertTrue(source.balance().compareTo(amount) >= 0, "The isolated fixture must fund the transfer");

        success(accounts.transfer(source.id(), destination.id(), amount));

        Account sourceAfter = account(accounts.get(source.id()));
        Account destinationAfter = account(accounts.get(destination.id()));
        money(sourceAfter.balance(), source.balance().subtract(amount));
        money(destinationAfter.balance(), destination.balance().add(amount));
        money(sourceAfter.balance().add(destinationAfter.balance()), source.balance().add(destination.balance()));
        var debit = singleTransaction(sourceLedger, transactionList(transactions.list(source.id())), source.id(), "Debit", amount);
        var credit = singleTransaction(destinationLedger, transactionList(transactions.list(destination.id())), destination.id(), "Credit", amount);
        Assert.assertNotEquals(debit.id(), credit.id(), "The two ledger records must have distinct transaction IDs");
    }

    @DataProvider
    public Object[][] malformedAmountOperations() {
        return new Object[][] {{"deposit"}, {"withdraw"}, {"transfer"}};
    }

    @Test(dataProvider = "malformedAmountOperations", groups = "ApiNegative",
            description = "DEP-003/WDR-003/XFER-002: malformed amount fails before changing balances or ledger records")
    public void malformedAmountDoesNotChangeFinancialState(String operation) {
        Account destination = fixture.createAccount(1);
        Account source = account(accounts.get(fixture.fundingAccount().id()));
        var sourceLedger = transactionList(transactions.list(source.id()));
        var destinationLedger = transactionList(transactions.list(destination.id()));
        var request = requests.newRequest().queryParam("amount", "not-a-decimal");
        if (operation.equals("transfer")) {
            request.queryParam("fromAccountId", source.id()).queryParam("toAccountId", destination.id());
        } else {
            request.queryParam("accountId", source.id());
        }

        Assert.assertEquals(request.post("/" + operation).statusCode(), expectedStatus("api.status.invalidInput"));

        money(account(accounts.get(source.id())).balance(), source.balance());
        money(account(accounts.get(destination.id())).balance(), destination.balance());
        Assert.assertEquals(transactionList(transactions.list(source.id())), sourceLedger);
        Assert.assertEquals(transactionList(transactions.list(destination.id())), destinationLedger);
    }

    @Test(groups = {"ApiNegative", "ApiContract"},
            description = "XFER-003: an invalid destination cannot leave a partial debit or ledger entry")
    public void invalidTransferDestinationPreservesSourceState() {
        Account source = account(accounts.get(fixture.fundingAccount().id()));
        var sourceLedger = transactionList(transactions.list(source.id()));
        BigDecimal amount = new BigDecimal("5.00");

        var response = accounts.transfer(source.id(), -1, amount);

        Assert.assertEquals(response.statusCode(), expectedStatus("api.status.missingResource"));
        money(account(accounts.get(source.id())).balance(), source.balance());
        Assert.assertEquals(transactionList(transactions.list(source.id())), sourceLedger,
                "A failed transfer must not debit the source or append a debit record");
    }
}
