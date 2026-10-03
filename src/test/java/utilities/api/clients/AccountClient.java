package utilities.api.clients;

import java.math.BigDecimal;
import java.util.Objects;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import utilities.api.ApiRequestFactory;
import utilities.api.models.Payee;

public final class AccountClient {
    private final ApiRequestFactory requests;

    public AccountClient(ApiRequestFactory requests) {
        this.requests = Objects.requireNonNull(requests, "requests");
    }

    public Response get(int accountId) {
        return requests.newRequest().pathParam("accountId", accountId).get("/accounts/{accountId}");
    }

    public Response create(int customerId, int type, int fromAccountId) {
        return requests.newRequest().queryParam("customerId", customerId).queryParam("newAccountType", type)
                .queryParam("fromAccountId", fromAccountId).post("/createAccount");
    }

    public Response deposit(int accountId, BigDecimal amount) {
        return requests.newRequest().queryParam("accountId", accountId)
                .queryParam("amount", amount.toPlainString()).post("/deposit");
    }

    public Response withdraw(int accountId, BigDecimal amount) {
        return requests.newRequest().queryParam("accountId", accountId)
                .queryParam("amount", amount.toPlainString()).post("/withdraw");
    }

    public Response transfer(int fromAccountId, int toAccountId, BigDecimal amount) {
        return requests.newRequest().queryParam("fromAccountId", fromAccountId)
                .queryParam("toAccountId", toAccountId).queryParam("amount", amount.toPlainString())
                .post("/transfer");
    }

    public Response billPay(int accountId, BigDecimal amount, Payee payee) {
        return requests.newRequest().queryParam("accountId", accountId)
                .queryParam("amount", amount.toPlainString()).contentType(ContentType.JSON)
                .body(payee).post("/billpay");
    }
}
