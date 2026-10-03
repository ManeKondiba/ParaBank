package utilities.api;

import java.net.URI;
import java.util.Objects;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.JsonConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import io.restassured.specification.RequestSpecification;

/** Every call returns its own request; no REST Assured static defaults are modified. */
public final class ApiRequestFactory {
    private final ApiConfig config;

    public ApiRequestFactory(ApiConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public ApiConfig config() { return config; }

    public RequestSpecification newRequest() {
        RestAssuredConfig requestConfig = new RestAssuredConfig()
                .jsonConfig(JsonConfig.jsonConfig().numberReturnType(NumberReturnType.BIG_DECIMAL))
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                        .defaultObjectMapperType(ObjectMapperType.JACKSON_2)
                        .jackson2ObjectMapperFactory((type, charset) -> new ObjectMapper()
                                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)))
                .redirect(RedirectConfig.redirectConfig().followRedirects(false))
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", config.connectTimeoutSeconds() * 1000)
                        .setParam("http.socket.timeout", config.readTimeoutSeconds() * 1000)
                        .httpClientFactory(ApiRequestFactory::httpClientWithoutRetries));
        return RestAssured.given().spec(new RequestSpecBuilder()
                .setBaseUri(config.baseUrl())
                .setAccept(ContentType.JSON)
                .setConfig(requestConfig)
                .addFilter((request, response, context) -> {
                    if (!request.getMethod().equalsIgnoreCase("GET")
                            && !request.getMethod().equalsIgnoreCase("HEAD")
                            && !request.getMethod().equalsIgnoreCase("OPTIONS")) {
                        config.requireControlledEnvironment();
                        if (ApiConfig.isSharedPublicHost(URI.create(request.getURI()).getHost())) {
                            throw new IllegalArgumentException("API writes require a controlled deployment");
                        }
                    }
                    return context.next(request, response);
                })
                .addFilter(new ApiEvidenceFilter())
                .build());
    }

    /** Allows intentionally malformed requests for contract and negative scenarios. */
    public RequestSpecification rawRequest() { return newRequest(); }

    @SuppressWarnings("deprecation")
    private static DefaultHttpClient httpClientWithoutRetries() {
        // REST Assured's Apache 4 integration expects AbstractHttpClient. Never replay a write.
        DefaultHttpClient client = new DefaultHttpClient();
        client.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
        return client;
    }
}
