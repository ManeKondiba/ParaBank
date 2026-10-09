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
    @Test(groups = "ApiContract", description = "VAL-028: parameter locations, types, bodies, responses and security match the reviewed pinned contract")
    public void deployedOperationShapesMatchReviewedContract() throws Exception {
        JsonNode expected;
        try (InputStream input = getClass().getResourceAsStream("/api/contracts/core-operation-shapes.json")) {
            Assert.assertNotNull(input);
            expected = JSON.readTree(input);
        }
        var response = requests.newRequest().get("/openapi.json");
        Assert.assertEquals(response.statusCode(), 200);
        JsonNode paths = JSON.readTree(response.asString()).path("paths");
        var entries = expected.fields();
        while (entries.hasNext()) {
            var path = entries.next();
            var methods = path.getValue().fields();
            while (methods.hasNext()) {
                var method = methods.next();
                JsonNode actual = paths.path(path.getKey()).path(method.getKey());
                var parameters = JSON.createArrayNode();
                for (JsonNode parameter : actual.path("parameters")) {
                    var normalized = JSON.createObjectNode();
                    normalized.put("name", parameter.path("name").asText());
                    normalized.put("location", parameter.path("in").asText());
                    normalized.put("required", parameter.path("required").asBoolean());
                    normalized.set("schema", parameter.path("schema"));
                    parameters.add(normalized);
                }
                Assert.assertEquals(parameters, method.getValue().path("parameters"), "Parameter contract: " + path.getKey());
                for (String key : java.util.List.of("requestBody", "responses", "security")) {
                    JsonNode value = actual.has(key) ? actual.get(key) : JSON.nullNode();
                    Assert.assertEquals(value, method.getValue().path(key), "Contract " + key + ": " + path.getKey());
                }
            }
        }
    }
}
