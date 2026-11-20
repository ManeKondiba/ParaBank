package testclasses.performance;

import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.Test;

import base.ApiBaseTest;
import io.restassured.response.Response;

/**
 * Performance & SLA Regression Suite.
 * Validates endpoint latency budgets against established SLA thresholds.
 */
public class ApiSlaTest extends ApiBaseTest {

    private static final long SLA_FAST_QUERY_MS = 1500;
    private static final long SLA_TRANSACTION_QUERY_MS = 2000;

    @Test(groups = {"Performance", "SLA"}, description = "Performance SLA: Customer account overview response time must be under SLA budget")
    public void accountOverviewMustRespondWithinSlaBudget() {
        int customerId = fixture.customer().id();

        Response response = requests.newRequest()
                .pathParam("customerId", customerId)
                .when()
                .get("/customers/{customerId}/accounts")
                .then()
                .extract().response();

        long responseTimeMs = response.getTimeIn(TimeUnit.MILLISECONDS);
        Assert.assertEquals(response.getStatusCode(), 200, "Account query must succeed");
        Assert.assertTrue(responseTimeMs <= SLA_FAST_QUERY_MS,
                String.format("Accounts endpoint took %d ms, exceeding SLA budget of %d ms",
                        responseTimeMs, SLA_FAST_QUERY_MS));
    }

    @Test(groups = {"Performance", "SLA"}, description = "Performance SLA: Account transaction history response time must be under SLA budget")
    public void transactionHistoryMustRespondWithinSlaBudget() {
        int accountId = fixture.fundingAccount().id();

        Response response = requests.newRequest()
                .pathParam("accountId", accountId)
                .when()
                .get("/accounts/{accountId}/transactions")
                .then()
                .extract().response();

        long responseTimeMs = response.getTimeIn(TimeUnit.MILLISECONDS);
        Assert.assertEquals(response.getStatusCode(), 200, "Transaction query must succeed");
        Assert.assertTrue(responseTimeMs <= SLA_TRANSACTION_QUERY_MS,
                String.format("Transactions endpoint took %d ms, exceeding SLA budget of %d ms",
                        responseTimeMs, SLA_TRANSACTION_QUERY_MS));
    }
}
