package utilities.api.clients;

import java.math.BigDecimal;
import java.util.Objects;

import io.restassured.response.Response;
import utilities.api.ApiRequestFactory;

public final class TransactionClient {
    private final ApiRequestFactory requests;

    public TransactionClient(ApiRequestFactory requests) {
        this.requests = Objects.requireNonNull(requests, "requests");
    }

    public Response list(int accountId) {
        return requests.newRequest().pathParam("accountId", accountId)
                .get("/accounts/{accountId}/transactions");
    }

    public Response get(int transactionId) {
        return requests.newRequest().pathParam("transactionId", transactionId)
                .get("/transactions/{transactionId}");
    }

    public Response byAmount(int accountId, BigDecimal amount) {
        return requests.newRequest().pathParam("accountId", accountId)
                .pathParam("amount", amount.toPlainString())
                .get("/accounts/{accountId}/transactions/amount/{amount}");
    }

    public Response onDate(int accountId, String date) {
        return requests.newRequest().pathParam("accountId", accountId).pathParam("onDate", date)
                .get("/accounts/{accountId}/transactions/onDate/{onDate}");
    }

    public Response betweenDates(int accountId, String fromDate, String toDate) {
        return requests.newRequest().pathParam("accountId", accountId).pathParam("fromDate", fromDate)
                .pathParam("toDate", toDate)
                .get("/accounts/{accountId}/transactions/fromDate/{fromDate}/toDate/{toDate}");
    }

    public Response byMonthAndType(int accountId, String month, String type) {
        return requests.newRequest().pathParam("accountId", accountId).pathParam("month", month)
                .pathParam("type", type)
                .get("/accounts/{accountId}/transactions/month/{month}/type/{type}");
    }
}
