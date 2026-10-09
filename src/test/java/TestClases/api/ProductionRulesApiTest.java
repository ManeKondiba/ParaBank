package TestClases.api;

import static utilities.api.assertions.ApiAssertions.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import TestBases.ApiBaseTest;

/** Production acceptance rules intentionally expose unsupported demo behavior. */
@Test(groups = {"ApiRegression", "ProductionRules"})
public class ProductionRulesApiTest extends ApiBaseTest {
    @DataProvider
    public Object[][] rejectedAmounts() {
        return List.of("deposit", "withdraw", "transfer", "billpay").stream()
                .flatMap(op -> List.of("0", "-0.01", "1.001", "99999999999999999999999999", "12abc", "Infinity")
                        .stream().map(amount -> new Object[] {op, amount})).toArray(Object[][]::new);
    }

    @Test(dataProvider = "rejectedAmounts", description = "VAL-005/019: invalid production amounts cannot change financial state")
    public void invalidAmountsPreserveFinancialState(String operation, String amount) {
        checkRejectedDebit(operation, amount);
    }

    @DataProvider
    public Object[][] debitOperations() { return new Object[][] {{"withdraw"}, {"transfer"}, {"billpay"}}; }

    @Test(dataProvider = "debitOperations", description = "VAL-005: debit operations cannot overdraw an account")
    public void insufficientFundsPreserveFinancialState(String operation) {
        // Account creation inside the helper also debits the funding account; capture afterward.
        checkRejectedDebit(operation, "OVERDRAFT");
    }

    private void checkRejectedDebit(String operation, String amount) {
        var destination = fixture.createAccount(1);
        int id = fixture.fundingAccount().id();
        var before = account(accounts.get(id));
        var otherBefore = account(accounts.get(destination.id()));
        var ledger = transactionList(transactions.list(id));
        var otherLedger = transactionList(transactions.list(destination.id()));
        if (amount.equals("OVERDRAFT")) {
            amount = before.balance().max(BigDecimal.ZERO).add(new BigDecimal("0.01")).toPlainString();
        }
        var request = requests.newRequest().queryParam("amount", amount);
        if (operation.equals("transfer")) {
            request.queryParam("fromAccountId", id).queryParam("toAccountId", destination.id());
        } else { request.queryParam("accountId", id); }
        if (operation.equals("billpay")) { request.contentType("application/json").body(fixture.payee()); }
        var response = request.post("/" + operation);
        SoftAssert checks = new SoftAssert();
        checks.assertTrue(response.statusCode() >= 400 && response.statusCode() < 500,
                "Invalid financial input must return a client error; got " + response.statusCode());
        checks.assertEquals(account(accounts.get(id)).balance().compareTo(before.balance()), 0, "Source balance changed");
        checks.assertEquals(account(accounts.get(destination.id())).balance().compareTo(otherBefore.balance()), 0, "Destination balance changed");
        checks.assertEquals(transactionList(transactions.list(id)), ledger, "Source ledger changed");
        checks.assertEquals(transactionList(transactions.list(destination.id())), otherLedger, "Destination ledger changed");
        checks.assertAll();
    }

    @DataProvider
    public Object[][] protectedReads() { return new Object[][] {{"customer"}, {"account"}, {"accounts"}, {"transactions"}}; }

    @Test(dataProvider = "protectedReads", description = "VAL-002: anonymous banking reads require authentication")
    public void anonymousBankingReadsRequireAuthentication(String resource) {
        String path = switch (resource) {
            case "customer" -> "/customers/" + fixture.customer().id();
            case "accounts" -> "/customers/" + fixture.customer().id() + "/accounts";
            case "transactions" -> "/accounts/" + fixture.fundingAccount().id() + "/transactions";
            default -> "/accounts/" + fixture.fundingAccount().id();
        };
        int status = requests.newRequest().get(path).statusCode();
        Assert.assertTrue(status == 401 || status == 403, "Anonymous banking read must return 401/403; got " + status);
    }

    @Test(groups = "AdditionalRules", description = "VAL-007: duplicate transfer using the same idempotency key applies once")
    public void duplicateTransferWithSameKeyIsAppliedOnce() {
        var destination = fixture.createAccount(1);
        int id = fixture.fundingAccount().id();
        var before = account(accounts.get(id));
        var otherBefore = account(accounts.get(destination.id()));
        var ledger = transactionList(transactions.list(id));
        var otherLedger = transactionList(transactions.list(destination.id()));
        BigDecimal amount = new BigDecimal("1.00");
        String key = UUID.randomUUID().toString();
        for (int attempt = 0; attempt < 2; attempt++) {
            success(requests.newRequest().header("Idempotency-Key", key).queryParam("fromAccountId", id)
                    .queryParam("toAccountId", destination.id()).queryParam("amount", amount.toPlainString()).post("/transfer"));
        }
        var after = account(accounts.get(id));
        var otherAfter = account(accounts.get(destination.id()));
        var ledgerAfter = transactionList(transactions.list(id));
        var otherLedgerAfter = transactionList(transactions.list(destination.id()));
        SoftAssert checks = new SoftAssert();
        checks.assertEquals(after.balance().compareTo(before.balance().subtract(amount)), 0, "Duplicate request debited more than once");
        checks.assertEquals(otherAfter.balance().compareTo(otherBefore.balance().add(amount)), 0, "Duplicate request credited more than once");
        try { singleTransaction(ledger, ledgerAfter, id, "Debit", amount); }
        catch (AssertionError failure) { checks.fail(failure.getMessage()); }
        try { singleTransaction(otherLedger, otherLedgerAfter, destination.id(), "Credit", amount); }
        catch (AssertionError failure) { checks.fail(failure.getMessage()); }
        checks.assertAll();
    }
    @Test(groups = "AdditionalRules", description = "VAL-013: concurrent withdrawals must not lose updates or create duplicate ledger IDs")
    public void concurrentWithdrawalsReconcileExactly() throws Exception {
        int id = fixture.fundingAccount().id();
        var before = account(accounts.get(id));
        var ledger = transactionList(transactions.list(id));
        int count = 8;
        Assert.assertTrue(before.balance().compareTo(BigDecimal.valueOf(count)) >= 0);
        var start = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(count);
        var tasks = new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
        try {
            for (int i = 0; i < count; i++) {
                tasks.add(pool.submit(() -> {
                    start.await();
                    // Begin thread-owned evidence rather than sharing the main thread's collector.
                    utilities.api.ApiEvidenceFilter.begin("concurrent-withdrawal");
                    try {
                        return requests.newRequest().queryParam("accountId", id).queryParam("amount", "1.00")
                                .post("/withdraw").statusCode();
                    } finally { utilities.api.ApiEvidenceFilter.clear(); }
                }));
            }
            start.countDown();
            for (var task : tasks) { Assert.assertEquals(task.get(45, java.util.concurrent.TimeUnit.SECONDS).intValue(), 200); }
        } finally {
            start.countDown();
            pool.shutdownNow();
            Assert.assertTrue(pool.awaitTermination(45, java.util.concurrent.TimeUnit.SECONDS), "Withdrawal workers did not stop");
        }
        var after = account(accounts.get(id));
        var ledgerAfter = transactionList(transactions.list(id));
        SoftAssert checks = new SoftAssert();
        checks.assertEquals(after.balance().compareTo(before.balance().subtract(BigDecimal.valueOf(count))), 0,
                "Concurrent withdrawals lost a balance update");
        try {
            var added = newTransactions(ledger, ledgerAfter);
            checks.assertEquals(added.size(), count);
            for (var entry : added) {
                checks.assertEquals(entry.accountId(), id);
                checks.assertEquals(entry.type(), "Debit");
                checks.assertEquals(entry.amount().compareTo(BigDecimal.ONE), 0);
            }
        } catch (AssertionError failure) { checks.fail(failure.getMessage()); }
        checks.assertAll();
    }

    @DataProvider
    public Object[][] protectedWrites() { return new Object[][] {{"deposit"}, {"withdraw"}, {"transfer"}, {"billpay"}}; }

    @Test(dataProvider = "protectedWrites", description = "VAL-002: anonymous writes must not change banking state")
    public void anonymousWritesRequireAuthentication(String operation) {
        var destination = fixture.createAccount(1);
        int id = fixture.fundingAccount().id();
        var before = account(accounts.get(id));
        var otherBefore = account(accounts.get(destination.id()));
        var ledger = transactionList(transactions.list(id));
        var otherLedger = transactionList(transactions.list(destination.id()));
        var request = requests.newRequest().queryParam("amount", "1.00");
        if (operation.equals("transfer")) {
            request.queryParam("fromAccountId", id).queryParam("toAccountId", destination.id());
        } else { request.queryParam("accountId", id); }
        if (operation.equals("billpay")) { request.contentType("application/json").body(fixture.payee()); }
        int status = request.post("/" + operation).statusCode();
        SoftAssert checks = new SoftAssert();
        checks.assertTrue(status == 401 || status == 403, "Anonymous write must return 401/403; got " + status);
        checks.assertEquals(account(accounts.get(id)).balance().compareTo(before.balance()), 0);
        checks.assertEquals(account(accounts.get(destination.id())).balance().compareTo(otherBefore.balance()), 0);
        checks.assertEquals(transactionList(transactions.list(id)), ledger);
        checks.assertEquals(transactionList(transactions.list(destination.id())), otherLedger);
        checks.assertAll();
    }

    @DataProvider
    public Object[][] invalidLoanAmounts() {
        return List.of("0", "-0.01", "1.001", "99999999999999999999999999").stream()
                .flatMap(value -> List.of("amount", "downPayment").stream()
                        .filter(field -> !(field.equals("downPayment") && value.equals("0")))
                        .map(field -> new Object[] {field, value}))
                .toArray(Object[][]::new);
    }

    @Test(dataProvider = "invalidLoanAmounts", description = "VAL-009: invalid loan numeric fields cannot create accounts or debit funds")
    public void invalidLoanValuesPreserveFinancialState(String field, String value) {
        int customerId = fixture.customer().id();
        int id = fixture.fundingAccount().id();
        var beforeAccounts = accountList(customers.accounts(customerId));
        var ledger = transactionList(transactions.list(id));
        var response = requests.newRequest().queryParam("customerId", customerId).queryParam("fromAccountId", id)
                .queryParam("amount", field.equals("amount") ? value : "10.00")
                .queryParam("downPayment", field.equals("downPayment") ? value : "2.00").post("/requestLoan");
        SoftAssert checks = new SoftAssert();
        checks.assertTrue(response.statusCode() >= 400 && response.statusCode() < 500,
                "Invalid loan field must return a client error; got " + response.statusCode());
        checks.assertEquals(accountList(customers.accounts(customerId)), beforeAccounts);
        checks.assertEquals(transactionList(transactions.list(id)), ledger);
        checks.assertAll();
    }
    @DataProvider
    public Object[][] invalidProfileFields() {
        return new Object[][] {
            {"firstName", "F".repeat(31)}, {"lastName", "L".repeat(31)}, {"street", "A".repeat(46)},
            {"city", "C".repeat(21)}, {"state", "S".repeat(21)}, {"zipCode", "1".repeat(21)},
            {"phoneNumber", "2".repeat(21)}, {"ssn", "3".repeat(16)}, {"username", "U".repeat(21)},
            {"password", "P".repeat(21)}, {"firstName", "   "}, {"lastName", "   "}, {"street", "   "},
            {"city", "   "}, {"state", "   "}, {"zipCode", "   "}, {"ssn", "   "},
            {"username", "   "}, {"password", "   "}
        };
    }

    @Test(dataProvider = "invalidProfileFields", description = "VAL-014/016/017: invalid profile fields cannot truncate or partly update a customer")
    public void invalidProfileUpdatePreservesCustomer(String field, String value) {
        int id = fixture.customer().id();
        var before = customer(customers.get(id));
        var ownedAccounts = accountList(customers.accounts(id));
        var update = new java.util.LinkedHashMap<String, String>();
        update.put("firstName", before.firstName());
        update.put("lastName", before.lastName());
        update.put("street", before.address().street());
        update.put("city", before.address().city());
        update.put("state", before.address().state());
        update.put("zipCode", before.address().zipCode());
        update.put("phoneNumber", before.phoneNumber());
        update.put("ssn", before.ssn());
        update.put("username", fixture.username());
        update.put("password", fixture.password());
        update.put(field, value);
        var response = customers.update(id, update);
        SoftAssert checks = new SoftAssert();
        checks.assertTrue(response.statusCode() >= 400 && response.statusCode() < 500,
                "Invalid profile field must return a client error; got " + response.statusCode());
        // Compare with booleans so assertion messages cannot print profile DTO values.
        checks.assertTrue(customer(customers.get(id)).equals(before), "Rejected profile update changed stored customer");
        checks.assertEquals(accountList(customers.accounts(id)), ownedAccounts);
        var login = customers.login(fixture.username(), fixture.password());
        checks.assertEquals(login.statusCode(), 200, "Rejected update invalidated original credentials");
        if (login.statusCode() == 200) { checks.assertEquals(customer(login).id(), id); }
        checks.assertAll();
    }
    @Test(groups = "AdditionalRules", description = "VAL-006: production same-account transfer is rejected without changing balance or ledger")
    public void sameAccountTransferCannotCreateMoneyOrLedgerEntries() {
        int id = fixture.fundingAccount().id();
        var before = account(accounts.get(id));
        var ledger = transactionList(transactions.list(id));
        var response = accounts.transfer(id, id, BigDecimal.ONE);
        SoftAssert checks = new SoftAssert();
        checks.assertTrue(response.statusCode() >= 400 && response.statusCode() < 500, "Same-account transfer must be rejected");
        checks.assertEquals(account(accounts.get(id)).balance().compareTo(before.balance()), 0);
        checks.assertEquals(transactionList(transactions.list(id)), ledger);
        checks.assertAll();
    }

    @DataProvider
    public Object[][] loanThresholds() {
        return new Object[][] {{"1.99", false}, {"2.00", true}, {"2.01", true}};
    }

    @Test(dataProvider = "loanThresholds", groups = "AdditionalRules", description = "VAL-009: pinned loan decision below at and above 20-percent threshold")
    public void pinnedLoanThresholdDecisionAndState(String downPayment, boolean approved) {
        int customerId = fixture.customer().id();
        int id = fixture.fundingAccount().id();
        var before = account(accounts.get(id));
        var ownedAccounts = accountList(customers.accounts(customerId));
        var ledger = transactionList(transactions.list(id));
        BigDecimal down = new BigDecimal(downPayment);
        var result = decode(json(loans.request(customerId, BigDecimal.TEN, down, id), 200, "loan-response.json"),
                utilities.api.models.LoanResponse.class);
        Assert.assertEquals(result.approved(), approved);
        if (approved) {
            Assert.assertNotNull(result.accountId());
            var created = account(accounts.get(result.accountId()));
            Assert.assertEquals(created.customerId(), customerId);
            Assert.assertEquals(created.type(), "LOAN");
            money(created.balance(), BigDecimal.TEN);
            money(account(accounts.get(id)).balance(), before.balance().subtract(down));
            singleTransaction(ledger, transactionList(transactions.list(id)), id, "Debit", down);
            var afterAccounts = accountList(customers.accounts(customerId));
            Assert.assertEquals(afterAccounts.size(), ownedAccounts.size() + 1);
        } else {
            Assert.assertNull(result.accountId());
            Assert.assertEquals(accountList(customers.accounts(customerId)), ownedAccounts);
            Assert.assertEquals(transactionList(transactions.list(id)), ledger);
        }
    }
}
