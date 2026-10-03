package utilities.api.clients;

import java.math.BigDecimal;
import java.util.Objects;

import io.restassured.response.Response;
import utilities.api.ApiRequestFactory;

public final class LoanClient {
    private final ApiRequestFactory requests;

    public LoanClient(ApiRequestFactory requests) {
        this.requests = Objects.requireNonNull(requests, "requests");
    }

    public Response request(int customerId, BigDecimal amount, BigDecimal downPayment, int fromAccountId) {
        return requests.newRequest().queryParam("customerId", customerId)
                .queryParam("amount", amount.toPlainString()).queryParam("downPayment", downPayment.toPlainString())
                .queryParam("fromAccountId", fromAccountId).post("/requestLoan");
    }
}
