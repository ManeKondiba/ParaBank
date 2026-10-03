package TestClases.api;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;
import TestBases.ApiBaseTest;

@Test(groups = "ApiRegression")
public class ApiOpenApiContractTest extends ApiBaseTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test(groups = "ApiContract", description = "CONTRACT-001: deployed OpenAPI retains the reviewed core operation inventory")
    public void deployedOpenApiMatchesCoreOperationManifest() throws Exception {
        JsonNode expected;
        try (InputStream manifest = getClass().getResourceAsStream("/api/contracts/core-operations.json")) {
            Assert.assertNotNull(manifest, "Core OpenAPI operation manifest is required");
            expected = JSON.readTree(manifest);
        }

        var response = requests.newRequest().get("/openapi.json");
        Assert.assertEquals(response.statusCode(), 200, "OpenAPI document must be available on the target deployment");
        Assert.assertTrue(response.contentType().toLowerCase(java.util.Locale.ROOT).contains("application/json"));
        JsonNode deployed = JSON.readTree(response.asString());
        Assert.assertEquals(deployed.path("openapi").asText(), expected.path("openapiVersion").asText());
        Assert.assertEquals(deployed.path("info").path("version").asText(), expected.path("apiVersion").asText());

        JsonNode expectedOperations = expected.path("operations");
        JsonNode deployedPaths = deployed.path("paths");
        Assert.assertTrue(expectedOperations.isObject(), "Manifest operations must be an object");
        Iterator<Map.Entry<String, JsonNode>> paths = expectedOperations.fields();
        while (paths.hasNext()) {
            Map.Entry<String, JsonNode> path = paths.next();
            JsonNode actualPath = deployedPaths.path(path.getKey());
            Assert.assertFalse(actualPath.isMissingNode(), "Missing core API path: " + path.getKey());
            for (JsonNode method : path.getValue()) {
                Assert.assertTrue(actualPath.has(method.asText()),
                        "Missing core API operation: " + method.asText().toUpperCase(java.util.Locale.ROOT)
                                + " " + path.getKey());
            }
        }
    }
}