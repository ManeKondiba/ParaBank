package TestClases.api;

import static utilities.api.assertions.ApiAssertions.account;
import static utilities.api.assertions.ApiAssertions.decode;
import static utilities.api.assertions.ApiAssertions.json;
import static utilities.api.assertions.ApiAssertions.money;
import static utilities.api.assertions.ApiAssertions.singleTransaction;
import static utilities.api.assertions.ApiAssertions.transactionList;

import java.math.BigDecimal;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import TestBases.ApiBaseTest;
import utilities.api.models.BillPayResult;

@Test(groups = "ApiRegression")
public class BillPayApiTest extends ApiBaseTest {
    @Test(groups = {"ApiSmoke", "ApiContract"}, description = "BILL-001: bill payment confirms payee and debits the selected account exactly once")
    public void billPaymentReconcilesBalanceAndLedger() {
        int id = fixture.fundingAccount().id();
        var before = account(accounts.get(id));
        var ledgerBefore = transactionList(transactions.list(id));
        var payee = fixture.payee();
        BigDecimal amount = new BigDecimal("23.45");
        Assert.assertTrue(before.balance().compareTo(amount) >= 0, "The isolated fixture must fund the payment");

        var result = decode(json(accounts.billPay(id, amount, payee), 200, "billpay-result.json"), BillPayResult.class);

        Assert.assertTrue(result.payeeName().equals(payee.name()), "The result must identify the requested payee");
        Assert.assertEquals(result.accountId(), id);
        money(result.amount(), amount);
        money(account(accounts.get(id)).balance(), before.balance().subtract(amount));
        var debit = singleTransaction(ledgerBefore, transactionList(transactions.list(id)), id, "Debit", amount);
        Assert.assertTrue(debit.description().contains(payee.name()), "The bill-payment record must identify its payee");
    }

    @Test(groups = {"ApiNegative", "BILL-002"}, description = "BILL-002: malformed JSON fails without debit or additional ledger records")
    public void malformedPayeeDoesNotDebitAccount() {
        int id = fixture.fundingAccount().id();
        var before = account(accounts.get(id));
        var ledgerBefore = transactionList(transactions.list(id));

        var response = requests.newRequest().contentType("application/json")
                .queryParam("accountId", id).queryParam("amount", "1.00").body("{\"name\":")
                .post("/billpay");

        var afterResponse = accounts.get(id);
        var ledgerAfterResponse = transactions.list(id);
        SoftAssert checks = new SoftAssert();
        checks.assertEquals(response.statusCode(), expectedStatus("api.status.malformedJson"),
            "Malformed request status; see sanitized API evidence");
        checks.assertEquals(afterResponse.statusCode(), 200, "Could not read account state after malformed bill pay");
        checks.assertEquals(ledgerAfterResponse.statusCode(), 200, "Could not read ledger after malformed bill pay");
        if (afterResponse.statusCode() == 200) {
            checks.assertEquals(account(afterResponse).balance().compareTo(before.balance()), 0,
                "Malformed bill pay changed the account balance");
        }
        if (ledgerAfterResponse.statusCode() == 200) {
            checks.assertEquals(transactionList(ledgerAfterResponse), ledgerBefore,
                "Malformed bill pay changed the transaction ledger");
        }
        checks.assertAll();
    }

    @Test(groups = "ApiNegative", description = "BILL-003: unsupported request media type fails without financial changes")
    public void unsupportedMediaTypeDoesNotDebitAccount() {
        int id = fixture.fundingAccount().id();
        var before = account(accounts.get(id));
        var ledgerBefore = transactionList(transactions.list(id));

        var response = requests.newRequest().contentType("text/plain")
                .queryParam("accountId", id).queryParam("amount", "1.00").body("synthetic payee")
                .post("/billpay");

        Assert.assertEquals(response.statusCode(), expectedStatus("api.status.unsupportedMediaType"));
        money(account(accounts.get(id)).balance(), before.balance());
        Assert.assertEquals(transactionList(transactions.list(id)), ledgerBefore);
    }
}
