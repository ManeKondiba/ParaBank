package TestClases.api;

import static utilities.api.assertions.ApiAssertions.account;
import static utilities.api.assertions.ApiAssertions.accountList;
import static utilities.api.assertions.ApiAssertions.money;
import static utilities.api.assertions.ApiAssertions.transactionList;

import java.util.HashSet;
import java.util.Set;
import java.math.BigDecimal;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import TestBases.ApiBaseTest;
import utilities.api.models.Account;

@Test(groups = "ApiRegression")
public class AccountsApiTest extends ApiBaseTest {
    @DataProvider
    public Object[][] accountTypes() {
        return new Object[][] {{0, "CHECKING"}, {1, "SAVINGS"}};
    }

    @Test(dataProvider = "accountTypes", groups = {"ApiSmoke", "ApiContract"},
            description = "ACCT-001/002: create checking and savings accounts with reconciled opening funds")
    public void createAccountAndReadBack(int type, String expectedType) {
        Set<Integer> idsBefore = new HashSet<>(accountList(customers.accounts(fixture.customer().id()))
                .stream().map(Account::id).toList());
        // The fixture also verifies the configured opening transfer and both ledger entries.
        Account created = fixture.createAccount(type);
        Assert.assertFalse(idsBefore.contains(created.id()), "Creation must return a new account ID");
        Assert.assertEquals(created.customerId(), fixture.customer().id());
        Assert.assertEquals(created.type(), expectedType);
        Account persisted = account(accounts.get(created.id()));
        Assert.assertEquals(persisted.id(), created.id());
        Assert.assertEquals(persisted.customerId(), created.customerId());
        Assert.assertEquals(persisted.type(), expectedType);
        money(persisted.balance(), created.balance());
        idsBefore.add(created.id());
        var after = accountList(customers.accounts(fixture.customer().id()));
        Assert.assertEquals(new HashSet<>(after.stream().map(Account::id).toList()), idsBefore,
                "Creation must add exactly one account to the customer's account list");
        Assert.assertEquals(after.size(), idsBefore.size(), "The account list must not contain duplicate IDs");
        Assert.assertTrue(after.stream().allMatch(a -> a.customerId() == fixture.customer().id()));
    }

    @Test(groups = {"ApiSmoke", "ApiContract"}, description = "ACCT-003: account detail agrees with the customer account list")
    public void accountDetailsMatchList() {
        Account listed = accountList(customers.accounts(fixture.customer().id())).stream()
                .filter(a -> a.id() == fixture.fundingAccount().id()).findFirst().orElseThrow();
        Account detail = account(accounts.get(listed.id()));
        Assert.assertEquals(detail.id(), listed.id());
        Assert.assertEquals(detail.customerId(), listed.customerId());
        Assert.assertEquals(detail.type(), listed.type());
        money(detail.balance(), listed.balance());
    }

    @Test(groups = "ApiNegative", description = "ACCT-004: nonexistent account returns the characterized missing-resource response")
    public void missingAccount() {
        Assert.assertEquals(accounts.get(-1).statusCode(), expectedStatus("api.status.missingResource"));
    }

    @Test(groups = "ApiNegative", description = "ACCT-005: malformed account type fails before creation or funding-account debit")
    public void malformedAccountTypePreservesFinancialState() {
        int customerId = fixture.customer().id();
        int sourceId = fixture.fundingAccount().id();
        var beforeAccounts = accountList(customers.accounts(customerId));
        var source = account(accounts.get(sourceId));
        var sourceLedger = transactionList(transactions.list(sourceId));

        var response = requests.newRequest().queryParam("customerId", customerId)
                .queryParam("newAccountType", "not-an-integer").queryParam("fromAccountId", sourceId)
                .post("/createAccount");

        Assert.assertEquals(response.statusCode(), expectedStatus("api.status.invalidInput"));
        Assert.assertEquals(accountList(customers.accounts(customerId)), beforeAccounts);
        money(account(accounts.get(sourceId)).balance(), source.balance());
        Assert.assertEquals(transactionList(transactions.list(sourceId)), sourceLedger);
    }

    @Test(groups = "ApiContract", description = "ACCT-006: XML and JSON account representations expose the same identity, type, and balance")
    public void xmlAccountRepresentationMatchesJson() {
        int id = fixture.fundingAccount().id();
        Account expected = account(accounts.get(id));

        var response = requests.newRequest().accept("application/xml").get("/accounts/" + id);

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.contentType().toLowerCase(java.util.Locale.ROOT).contains("application/xml"));
        var xml = response.xmlPath();
        Assert.assertEquals(xml.getInt("account.id"), expected.id());
        Assert.assertEquals(xml.getInt("account.customerId"), expected.customerId());
        Assert.assertEquals(xml.getString("account.type"), expected.type());
        money(new BigDecimal(xml.getString("account.balance")), expected.balance());
    }
}
