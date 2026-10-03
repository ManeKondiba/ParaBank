package TestClases.api;

import static utilities.api.assertions.ApiAssertions.account;
import static utilities.api.assertions.ApiAssertions.accountList;
import static utilities.api.assertions.ApiAssertions.decode;
import static utilities.api.assertions.ApiAssertions.json;
import static utilities.api.assertions.ApiAssertions.money;
import static utilities.api.assertions.ApiAssertions.singleTransaction;
import static utilities.api.assertions.ApiAssertions.transactionList;

import java.math.BigDecimal;
import java.util.HashSet;
import org.testng.Assert;
import org.testng.annotations.Test;
import TestBases.ApiBaseTest;
import utilities.api.ApiDates;
import utilities.api.models.Account;
import utilities.api.models.LoanResponse;

@Test(groups = "ApiRegression")
public class LoansApiTest extends ApiBaseTest {
    @Test(groups = "ApiContract", description = "LOAN-001: configured local provider approves a 20% down payment and reconciles account creation and debit")
    public void approvedLoanCreatesAccountAndDebitsDownPayment() {
        int customerId = fixture.customer().id();
        var beforeAccounts = accountList(customers.accounts(customerId));
        var funding = account(accounts.get(fixture.fundingAccount().id()));
        var ledgerBefore = transactionList(transactions.list(funding.id()));
        BigDecimal amount = new BigDecimal("10.00");
        BigDecimal downPayment = new BigDecimal("2.00");
        Assert.assertTrue(funding.balance().compareTo(downPayment) >= 0, "The fixture must fund the configured approval scenario");

        var result = decode(json(loans.request(customerId, amount, downPayment, funding.id()), 200, "loan-response.json"), LoanResponse.class);

        assertDecisionMetadata(result);
        Assert.assertTrue(result.approved(), "The pinned local down-payment processor at 20% must approve this loan");
        Assert.assertNotNull(result.accountId(), "Approval must identify the newly created loan account");
        Assert.assertFalse(beforeAccounts.stream().anyMatch(a -> a.id() == result.accountId()));
        Account loanAccount = account(accounts.get(result.accountId()));
        Assert.assertEquals(loanAccount.customerId(), customerId);
        Assert.assertEquals(loanAccount.type(), "LOAN");
        money(loanAccount.balance(), amount);
        money(account(accounts.get(funding.id())).balance(), funding.balance().subtract(downPayment));
        singleTransaction(ledgerBefore, transactionList(transactions.list(funding.id())), funding.id(), "Debit", downPayment);
        var expectedIds = new HashSet<>(beforeAccounts.stream().map(Account::id).toList());
        expectedIds.add(loanAccount.id());
        var afterAccounts = accountList(customers.accounts(customerId));
        Assert.assertEquals(new HashSet<>(afterAccounts.stream().map(Account::id).toList()), expectedIds);
        Assert.assertEquals(afterAccounts.size(), beforeAccounts.size() + 1);
    }

    @Test(groups = {"ApiNegative", "ApiContract"}, description = "LOAN-002: a down payment beyond available funds is denied with no financial side effects")
    public void deniedLoanPreservesAccountsAndLedger() {
        int customerId = fixture.customer().id();
        var beforeAccounts = accountList(customers.accounts(customerId));
        var funding = account(accounts.get(fixture.fundingAccount().id()));
        var ledgerBefore = transactionList(transactions.list(funding.id()));
        BigDecimal available = beforeAccounts.stream().filter(a -> !a.type().equals("LOAN"))
                .map(Account::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal downPayment = available.max(BigDecimal.ZERO).add(BigDecimal.ONE);
        BigDecimal amount = downPayment.add(new BigDecimal("100.00"));

        var result = decode(json(loans.request(customerId, amount, downPayment, funding.id()), 200, "loan-response.json"), LoanResponse.class);

        assertDecisionMetadata(result);
        Assert.assertFalse(result.approved(), "A down payment above available funds must be denied by the local processor");
        Assert.assertNull(result.accountId(), "A denied request must not create a loan account");
        Assert.assertNotNull(result.message(), "A denied loan should explain the decision");
        Assert.assertFalse(result.message().isBlank(), "A denied loan should explain the decision");
        Assert.assertEquals(accountList(customers.accounts(customerId)), beforeAccounts,
                "Denial must preserve all customer accounts and balances");
        money(account(accounts.get(funding.id())).balance(), funding.balance());
        Assert.assertEquals(transactionList(transactions.list(funding.id())), ledgerBefore);
    }

    @Test(groups = "ApiNegative", description = "LOAN-003: malformed loan amount fails before creating an account or debiting funds")
    public void malformedLoanAmountPreservesFinancialState() {
        int customerId = fixture.customer().id();
        int sourceId = fixture.fundingAccount().id();
        var beforeAccounts = accountList(customers.accounts(customerId));
        var funding = account(accounts.get(sourceId));
        var ledgerBefore = transactionList(transactions.list(sourceId));

        var response = requests.newRequest().queryParam("customerId", customerId)
                .queryParam("fromAccountId", sourceId).queryParam("amount", "not-a-decimal")
                .queryParam("downPayment", "2.00").post("/requestLoan");

        Assert.assertEquals(response.statusCode(), expectedStatus("api.status.invalidInput"));
        Assert.assertEquals(accountList(customers.accounts(customerId)), beforeAccounts);
        money(account(accounts.get(sourceId)).balance(), funding.balance());
        Assert.assertEquals(transactionList(transactions.list(sourceId)), ledgerBefore);
    }

    private static void assertDecisionMetadata(LoanResponse result) {
        Assert.assertNotNull(result.loanProviderName());
        Assert.assertFalse(result.loanProviderName().isBlank(), "Loan decisions must identify the provider");
        Assert.assertNotNull(ApiDates.transactionDate(result.responseDate()), "The provider must supply a valid response date");
    }
}
