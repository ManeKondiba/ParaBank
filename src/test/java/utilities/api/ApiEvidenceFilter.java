package utilities.api;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/** Only allowlisted evidence is persisted. Raw credentials, headers and text bodies never reach reports. */
public final class ApiEvidenceFilter implements Filter {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> NUMERIC = Set.of("id", "customerId", "accountId", "positionId", "balance",
            "amount", "shares", "purchasePrice", "closingPrice", "approved", "date");
    private static final Set<String> SAFE_PARAMETERS = Set.of("accountId", "customerId", "fromAccountId",
            "toAccountId", "newAccountType", "amount", "downPayment");
    private static final Set<String> FIELDS = Set.of("id", "customerId", "accountId", "positionId", "balance",
            "amount", "shares", "purchasePrice", "closingPrice", "approved", "date", "responseDate", "type",
            "firstName", "lastName", "address", "street", "city", "state", "zipCode", "phoneNumber", "ssn",
            "username", "password", "name", "accountNumber", "payeeName", "loanProviderName", "message",
            "description", "symbol", "error");
    private static final ThreadLocal<Evidence> CURRENT = new ThreadLocal<>();

    private record Evidence(Path path, List<Object> exchanges) { }

    public static void begin(String caseName) {
        String safeName = caseName.replaceAll("[^A-Za-z0-9_-]", "_");
        CURRENT.set(new Evidence(Path.of("target", "api-evidence", safeName + "-" + UUID.randomUUID() + ".json"),
                new ArrayList<>()));
    }

    public static Path currentFile() {
        Evidence evidence = CURRENT.get();
        return evidence == null || !Files.exists(evidence.path()) ? null : evidence.path();
    }

    public static void clear() {
        CURRENT.remove();
    }

    @Override
    public Response filter(FilterableRequestSpecification request, FilterableResponseSpecification specification,
            FilterContext context) {
        String endpoint = endpoint(request.getURI());
        ObjectNode entry = JSON.createObjectNode();
        entry.put("method", request.getMethod());
        entry.put("endpoint", endpoint);
        entry.set("query", parameters(request.getQueryParams()));
        Object body = request.getBody();
        if (body != null) {
            entry.put("requestBody", sanitize(body instanceof String text ? text : serialize(body)));
        }
        long start = System.nanoTime();
        try {
            Response response = context.next(request, specification);
            entry.put("status", response.statusCode());
            entry.put("contentType", safeContentType(response.contentType()));
            entry.put("responseBody", sanitize(response.asString()));
            return response;
        } catch (Exception exception) {
            entry.put("transportError", exception.getClass().getSimpleName());
            // HTTP exceptions can include a credential-bearing URI. Do not retain their message/cause.
            throw new IllegalStateException("API transport failure at " + endpoint + " ("
                    + exception.getClass().getSimpleName() + "); a write may already have committed.");
        } finally {
            entry.put("durationMs", (System.nanoTime() - start) / 1_000_000);
            persist(entry);
        }
    }

    public static String endpoint(String uri) {
        try {
            String path = URI.create(uri).getRawPath();
            if (path == null) {
                return "/";
            }
            int login = path.indexOf("/login");
            if (login >= 0) {
                return path.substring(0, login) + "/login/{username}/{password}";
            }
            int parameter = path.indexOf("/setParameter/");
            if (parameter >= 0) {
                return path.substring(0, parameter) + "/setParameter/{name}/{value}";
            }
            // Only fixed endpoint names, numbers and documented date/enum tokens are retained.
            return path.replaceAll("(?i)(?<=/)[^/]*(?:%|@)[^/]*", "{redacted}");
        } catch (IllegalArgumentException exception) {
            return "{invalid-uri}";
        }
    }

    public static String sanitize(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            return JSON.writeValueAsString(scrub(JSON.readTree(body), ""));
        } catch (IOException | RuntimeException exception) {
            return "[non-JSON body omitted]";
        }
    }

    private static JsonNode scrub(JsonNode node, String key) {
        if (node == null || node.isNull()) {
            return JSON.nullNode();
        }
        if (node.isObject()) {
            ObjectNode result = JSON.createObjectNode();
            node.fields().forEachRemaining(field -> {
                if (FIELDS.contains(field.getKey())) {
                    result.set(field.getKey(), scrub(field.getValue(), field.getKey()));
                } else {
                    result.put("[unrecognized-field]", "[redacted]");
                }
            });
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = JSON.createArrayNode();
            node.forEach(value -> result.add(scrub(value, key)));
            return result;
        }
        if (NUMERIC.contains(key) && (node.isNumber() || node.isBoolean())) {
            return node;
        }
        if (key.equals("type") && Set.of("CHECKING", "SAVINGS", "LOAN", "Credit", "Debit").contains(node.asText())) {
            return node;
        }
        if ((key.equals("date") || key.equals("responseDate")) && node.asText().matches("[0-9TZ:+.\\-]+")) {
            return node;
        }
        return JSON.getNodeFactory().textNode("[redacted]");
    }

    private static ObjectNode parameters(Map<String, ?> parameters) {
        ObjectNode result = JSON.createObjectNode();
        parameters.forEach((key, value) -> {
            String text = String.valueOf(value);
            result.put(key, SAFE_PARAMETERS.contains(key) && text.matches("-?[0-9]+(?:\\.[0-9]+)?")
                    ? text : "[redacted]");
        });
        return result;
    }

    private static String serialize(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (IOException exception) {
            return "";
        }
    }

    private static String safeContentType(String value) {
        if (value == null) {
            return "";
        }
        return value.split(";", 2)[0].replaceAll("[^A-Za-z0-9/+.-]", "");
    }

    private static void persist(ObjectNode entry) {
        Evidence evidence = CURRENT.get();
        if (evidence == null) {
            return;
        }
        evidence.exchanges().add(entry);
        try {
            Files.createDirectories(evidence.path().getParent());
            Files.writeString(evidence.path(), JSON.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(evidence.exchanges()), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot save sanitized API evidence");
        }
    }
}
