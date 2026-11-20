package testclasses.security;

import org.testng.Assert;
import org.testng.annotations.Test;

import base.ApiBaseTest;
import io.restassured.response.Response;

/**
 * OWASP API1:2023 - Broken Object Level Authorization (BOLA / IDOR).
 * Verifies that requests for unauthorized or unowned resources are handled defensively.
 */
public class BrokenAccessControlTest extends ApiBaseTest {

    @Test(groups = {"Security", "OWASP"}, description = "OWASP BOLA: Accessing invalid or unauthorized account ID must not return 200 OK")
    public void unownedAccountAccessMustBeHandledDefensively() {
        int nonExistentOrUnownedAccountId = 99999999;

        Response response = requests.newRequest()
                .pathParam("accountId", nonExistentOrUnownedAccountId)
                .when()
                .get("/accounts/{accountId}")
                .then()
                .extract().response();

        int statusCode = response.getStatusCode();
        Assert.assertNotEquals(statusCode, 200,
                "Requesting unowned or invalid account ID should not return HTTP 200 OK");
        Assert.assertTrue(statusCode == 400 || statusCode == 404 || statusCode == 500,
                "Expected error status code (400, 404, or controlled 500), got: " + statusCode);
    }
}
