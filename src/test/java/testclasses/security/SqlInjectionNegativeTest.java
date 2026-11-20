package testclasses.security;

import org.testng.Assert;
import org.testng.annotations.Test;

import base.ApiBaseTest;
import io.restassured.response.Response;

/**
 * OWASP API3:2023 - Injection Flaws.
 * Verifies that SQL injection payloads do not bypass authentication or expose database internals.
 */
public class SqlInjectionNegativeTest extends ApiBaseTest {

    private static final String SQLI_PAYLOAD = "' OR '1'='1' --";

    @Test(groups = {"Security", "OWASP"}, description = "OWASP Injection: SQL injection payload in login endpoint must be safely rejected")
    public void sqlInjectionPayloadInLoginMustBeRejected() {
        Response response = requests.newRequest()
                .pathParam("username", SQLI_PAYLOAD)
                .pathParam("password", SQLI_PAYLOAD)
                .when()
                .get("/login/{username}/{password}")
                .then()
                .extract().response();

        // Must NOT authenticate successfully (i.e. status must not be 200 with customer data)
        int statusCode = response.getStatusCode();
        Assert.assertNotEquals(statusCode, 200,
                "SQL Injection payload in login must not return HTTP 200 OK");

        String body = response.getBody().asString();
        Assert.assertFalse(body.toLowerCase().contains("syntax error in sql"),
                "Response must not leak raw SQL syntax errors or database engine specifics");
    }
}
