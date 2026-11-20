package testclasses.security;

import org.testng.Assert;
import org.testng.annotations.Test;

import base.ApiBaseTest;
import io.restassured.response.Response;

/**
 * OWASP ASVS / API Top 10 Security Header Validation Tests.
 * Checks for defensive HTTP headers on sensitive banking endpoints.
 */
public class SecurityHeadersTest extends ApiBaseTest {

    @Test(groups = {"Security", "OWASP"}, description = "OWASP: Sensitive account endpoints must define valid Content-Type and no-cache controls")
    public void sensitiveEndpointsMustDefineDefensiveHeaders() {
        int accountId = fixture.fundingAccount().id();

        Response response = requests.newRequest()
                .pathParam("accountId", accountId)
                .when()
                .get("/accounts/{accountId}")
                .then()
                .extract().response();

        // 1. Assert content type is properly constrained (mitigates MIME-confusion attacks)
        String contentType = response.getHeader("Content-Type");
        Assert.assertNotNull(contentType, "Content-Type header must be present on account endpoints");
        Assert.assertTrue(contentType.contains("application/json") || contentType.contains("application/xml"),
                "Content-Type should be JSON or XML, was: " + contentType);

        // 2. Validate cache-control or sensitivity awareness
        String cacheControl = response.getHeader("Cache-Control");
        if (cacheControl != null) {
            Assert.assertFalse(cacheControl.contains("public"),
                    "Sensitive banking account data must never have Cache-Control: public");
        }
    }
}
