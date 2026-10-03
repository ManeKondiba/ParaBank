package utilities.api.clients;

import java.util.Map;
import java.util.Objects;

import io.restassured.response.Response;
import utilities.api.ApiRequestFactory;

public final class CustomerClient {
    private final ApiRequestFactory requests;

    public CustomerClient(ApiRequestFactory requests) {
        this.requests = Objects.requireNonNull(requests, "requests");
    }

    public Response login(String username, String password) {
        return requests.newRequest().pathParam("username", username).pathParam("password", password)
                .get("/login/{username}/{password}");
    }

    public Response get(int customerId) {
        return requests.newRequest().pathParam("customerId", customerId).get("/customers/{customerId}");
    }

    public Response accounts(int customerId) {
        return requests.newRequest().pathParam("customerId", customerId)
                .get("/customers/{customerId}/accounts");
    }

    public Response update(int customerId, Map<String, ?> query) {
        return requests.newRequest().pathParam("customerId", customerId).queryParams(query)
                .post("/customers/update/{customerId}");
    }
}
