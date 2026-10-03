package TestClases.api;

import static utilities.api.assertions.ApiAssertions.account;
import static utilities.api.assertions.ApiAssertions.accountList;
import static utilities.api.assertions.ApiAssertions.customer;
import static utilities.api.assertions.ApiAssertions.decode;
import static utilities.api.assertions.ApiAssertions.json;
import static utilities.api.assertions.ApiAssertions.money;
import static utilities.api.assertions.ApiAssertions.newTransactions;
import static utilities.api.assertions.ApiAssertions.singleTransaction;
import static utilities.api.assertions.ApiAssertions.success;
import static utilities.api.assertions.ApiAssertions.transaction;
import static utilities.api.assertions.ApiAssertions.transactionList;

import java.math.BigDecimal;
import org.testng.Assert;
import org.testng.annotations.Test;
import TestBases.ApiBaseTest;
import utilities.api.models.BillPayResult;

@Test(groups = "ApiRegression")
public class BankingJourneyApiTest extends ApiBaseTest {
    @Test(groups = "ApiWorkflow", description = "FLOW-001: login, create, deposit, transfer, and bill pay reconcile persisted accounts and ledger")
    public void completeBankingJourney() {
        int customerId = customer(customers.login(fixture.username(), fixture.password())).id();
        Assert.assertEquals(customerId, fixture.customer().id());
        var destination = fixture.createAccount(1);
        var source = account(accounts.get(fixture.fundingAccount().id()));
        var sourceStartLedger = transactionList(transactions.list(source.id()));
        var destinationStartLedger = transactionList(transactions.list(destination.id()));
        BigDecimal deposit = new BigDecimal("35.67");
        BigDecimal transfer = new BigDecimal("20.12");
        BigDecimal bill = new BigDecimal("7.89");

        success(accounts.deposit(source.id(), deposit));
        money(account(accounts.get(source.id())).balance(), source.balance().add(deposit));
        var sourceAfterDeposit = transactionList(transactions.list(source.id()));
        singleTransaction(sourceStartLedger, sourceAfterDeposit, source.id(), "Credit", deposit);

        success(accounts.transfer(source.id(), destination.id(), transfer));
        money(account(accounts.get(source.id())).balance(), source.balance().add(deposit).subtract(transfer));
        money(account(accounts.get(destination.id())).balance(), destination.balance().add(transfer));
        var sourceAfterTransfer = transactionList(transactions.list(source.id()));
        var destinationAfterTransfer = transactionList(transactions.list(destination.id()));
        singleTransaction(sourceAfterDeposit, sourceAfterTransfer, source.id(), "Debit", transfer);
        singleTransaction(destinationStartLedger, destinationAfterTransfer, destination.id(), "Credit", transfer);

        var payee = fixture.payee();
        var paid = decode(json(accounts.billPay(destination.id(), bill, payee), 200, "billpay-result.json"), BillPayResult.class);
        Assert.assertEquals(paid.accountId(), destination.id());
        Assert.assertTrue(paid.payeeName().equals(payee.name()), "The workflow must pay its intended payee");
        money(paid.amount(), bill);
        var destinationAfterBill = transactionList(transactions.list(destination.id()));
        singleTransaction(destinationAfterTransfer, destinationAfterBill, destination.id(), "Debit", bill);

        var sourceFinal = account(accounts.get(source.id()));
        var destinationFinal = account(accounts.get(destination.id()));
        money(sourceFinal.balance(), source.balance().add(deposit).subtract(transfer));
        money(destinationFinal.balance(), destination.balance().add(transfer).subtract(bill));
        money(sourceFinal.balance().add(destinationFinal.balance()), source.balance().add(destination.balance()).add(deposit).subtract(bill));
        var listed = accountList(customers.accounts(customerId));
        Assert.assertTrue(listed.contains(sourceFinal) && listed.contains(destinationFinal), "Account listing must agree with final detail reads");
        Assert.assertEquals(transactionList(transactions.list(source.id())), sourceAfterTransfer,
                "Paying from the destination must not change the source ledger");
        for (var entry : newTransactions(sourceStartLedger, sourceAfterTransfer)) {
            Assert.assertEquals(transaction(transactions.get(entry.id())), entry);
            Assert.assertTrue(transactionList(transactions.byAmount(source.id(), entry.amount())).contains(entry));
        }
        for (var entry : newTransactions(destinationStartLedger, destinationAfterBill)) {
            Assert.assertEquals(transaction(transactions.get(entry.id())), entry);
            Assert.assertTrue(transactionList(transactions.byAmount(destination.id(), entry.amount())).contains(entry));
        }
    }
}
