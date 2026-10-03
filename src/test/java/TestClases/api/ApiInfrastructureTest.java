package TestClases.api;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import utilities.FrameworkConfig;
import utilities.api.ApiConfig;
import utilities.api.ApiDates;
import utilities.api.ApiEvidenceFilter;
import utilities.api.ApiRequestFactory;
import utilities.api.assertions.ApiAssertions;
import utilities.api.clients.AccountClient;
import utilities.api.clients.CustomerClient;
import utilities.api.models.Account;
import utilities.api.models.Address;
import utilities.api.models.BillPayResult;
import utilities.api.models.Payee;
import utilities.api.models.Transaction;

/** Deterministic infrastructure checks use an ephemeral loopback HTTP server, never a banking deployment. */
public class ApiInfrastructureTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @AfterMethod(alwaysRun = true)
    public void clearEvidence() {
        ApiEvidenceFilter.clear();
    }

    @Test(groups = "Unit")
    public void configurationRejectsUnsafeUrlsWithoutEchoingSecrets() {
        for (String url : List.of("file:///tmp/bank", "https://user:URL_SECRET@localhost/bank",
                "https://localhost/bank?password=URL_SECRET", "https://localhost/bank#URL_SECRET",
                "https://localhost:99999/bank", "https://localhost:0/bank", "https:// bad URL_SECRET")) {
            IllegalArgumentException error = Assert.expectThrows(IllegalArgumentException.class,
                    () -> config(url));
            Assert.assertFalse(error.toString().contains("URL_SECRET"), "Configuration error leaked a URL secret");
            Assert.assertNull(error.getCause(), "Rejected URL must not survive in a nested exception");
        }
    }

    @Test(groups = "Unit")
    public void configurationRejectsTimeoutOverflowAndIncompleteFileFixtures() {
        Properties values = defaults("http://127.0.0.1:8080/parabank/services/bank");
        for (String timeout : List.of("0", "-1", "2147483647", "not-a-number")) {
            values.setProperty("api.connectTimeout.seconds", timeout);
            Assert.expectThrows(IllegalArgumentException.class,
                    () -> new ApiConfig(new FrameworkConfig(values, new Properties(), Map.of())));
        }
        values.setProperty("api.connectTimeout.seconds", "1");
        values.setProperty("api.fixture.mode", "file");
        Assert.expectThrows(IllegalArgumentException.class,
                () -> new ApiConfig(new FrameworkConfig(values, new Properties(), Map.of())));
        values.setProperty("api.fixture.mode", "unsupported");
        Assert.expectThrows(IllegalArgumentException.class,
                () -> new ApiConfig(new FrameworkConfig(values, new Properties(), Map.of())));
    }

    @Test(groups = "Unit")
    public void fixtureGuardRejectsSharedDemoHostnameVariants() {
        for (String host : List.of("parabank.parasoft.com", "PARABANK.PARASOFT.COM.",
                "child.parabank.parasoft.com")) {
            ApiConfig config = config("https://" + host + "/parabank/services/bank");
            Assert.expectThrows(IllegalArgumentException.class, config::requireControlledEnvironment);
        }
        config("http://127.0.0.1:8080/parabank/services/bank").requireControlledEnvironment();
    }

    @Test(groups = "Unit")
    public void redactionKeepsDiagnosticMoneyWhileRemovingNestedProfileAndTextSecrets() throws IOException {
        String response = """
                {"id":7,"balance":123.45,"type":"CHECKING","password":"BODY_SECRET",
                 "ssn":"SSN_SECRET","address":{"street":"STREET_SECRET"},
                 "nested":[{"username":"USER_SECRET","description":"DESCRIPTION_SECRET"}],
                 "message":"MESSAGE_SECRET","KEY_SECRET":123456789}
                """;
        String sanitized = ApiEvidenceFilter.sanitize(response);
        JsonNode body = JSON.readTree(sanitized);
        Assert.assertEquals(body.path("id").intValue(), 7);
        Assert.assertEquals(body.path("balance").decimalValue(), new BigDecimal("123.45"));
        Assert.assertEquals(body.path("type").textValue(), "CHECKING");
        Assert.assertFalse(sanitized.contains("SECRET"), "Nested profile data survived redaction");
        Assert.assertEquals(body.path("[unrecognized-field]").textValue(), "[redacted]",
                "Unknown keys and numeric values may be credentials and must not be retained");
        Assert.assertEquals(ApiEvidenceFilter.sanitize("<html>PLAIN_SECRET</html>"), "[non-JSON body omitted]");
        Assert.assertEquals(ApiEvidenceFilter.endpoint(
                "http://localhost/parabank/services/bank/login/USER_SECRET/PASS_SECRET?token=QUERY_SECRET"),
                "/parabank/services/bank/login/{username}/{password}");
    }

    @Test(groups = "Unit")
    public void clientsEncodeCredentialsAndPutWriteParametersInTheirContractLocations() throws Exception {
        List<Received> received = new CopyOnWriteArrayList<>();
        try (LocalServer server = new LocalServer(exchange -> {
            received.add(receive(exchange));
            respond(exchange, "{\"id\":7,\"ssn\":\"RESPONSE_SECRET\"}");
        })) {
            ApiEvidenceFilter.begin("unit-wire-contract");
            ApiRequestFactory requests = new ApiRequestFactory(config(server.baseUrl()));
            String username = "user+tag mail@example.test";
            String password = "PASSWORD_SECRET/a?b#c% d";
            new CustomerClient(requests).login(username, password);
            AccountClient accounts = new AccountClient(requests);
            accounts.transfer(11, 22, new BigDecimal("100.05"));
            accounts.billPay(11, new BigDecimal("2.30"), new Payee("PAYEE_SECRET",
                    new Address("STREET_SECRET", "CITY_SECRET", "STATE_SECRET", "ZIP_SECRET"),
                    "PHONE_SECRET", 98765));

            Assert.assertEquals(received.size(), 3);
            Received login = received.get(0);
            Assert.assertEquals(login.method(), "GET");
            Assert.assertNull(login.query());
            String credentialPath = login.path().substring("/parabank/services/bank/login/".length());
            String[] segments = credentialPath.split("/", -1);
            Assert.assertEquals(segments.length, 2, "A slash in a credential must remain inside its encoded segment");
            Assert.assertEquals(decode(segments[0]), username);
            Assert.assertEquals(decode(segments[1]), password);
            Assert.assertTrue(login.accept().contains("application/json"));

            Received transfer = received.get(1);
            Assert.assertEquals(transfer.method(), "POST");
            Assert.assertEquals(transfer.path(), "/parabank/services/bank/transfer");
            Assert.assertEquals(query(transfer.query()), Map.of("fromAccountId", "11", "toAccountId", "22", "amount", "100.05"));
            Assert.assertTrue(transfer.body().isEmpty(), "Transfer parameters belong in the URL query, not a form or JSON body");

            Received billPay = received.get(2);
            Assert.assertEquals(billPay.path(), "/parabank/services/bank/billpay");
            Assert.assertEquals(query(billPay.query()), Map.of("accountId", "11", "amount", "2.30"));
            Assert.assertTrue(billPay.contentType().contains("application/json"));
            JsonNode payee = JSON.readTree(billPay.body());
            Assert.assertEquals(payee.path("name").textValue(), "PAYEE_SECRET");
            Assert.assertEquals(payee.path("address").path("street").textValue(), "STREET_SECRET");
            Assert.assertEquals(payee.path("accountNumber").intValue(), 98765);

            Assert.assertNotNull(ApiEvidenceFilter.currentFile(), "HTTP calls must persist evidence");
            String evidence = Files.readString(ApiEvidenceFilter.currentFile());
            Assert.assertFalse(evidence.contains("SECRET"), "Wire evidence leaked a request or response secret");
            Assert.assertFalse(evidence.contains("example.test"), "Wire evidence leaked the username");
            Assert.assertTrue(evidence.contains("100.05"), "Evidence should retain safe monetary diagnostics");
            JsonNode entries = JSON.readTree(evidence);
            Assert.assertEquals(entries.size(), 3);
            Assert.assertEquals(entries.get(0).path("endpoint").textValue(),
                    "/parabank/services/bank/login/{username}/{password}");
        }
    }

    @Test(groups = "Unit")
    public void jsonPathAndDtoRetainDecimalPrecision() throws Exception {
        String decimal = "0.100000000000000000000000001";
        try (LocalServer server = new LocalServer(exchange -> {
            receive(exchange);
            respond(exchange, "{\"id\":11,\"customerId\":7,\"type\":\"CHECKING\",\"balance\":" + decimal + "}");
        })) {
            Response response = new AccountClient(new ApiRequestFactory(config(server.baseUrl()))).get(11);
            Object parsed = response.jsonPath().get("balance");
            Assert.assertTrue(parsed instanceof BigDecimal, "JsonPath must not round financial values through double");
            Assert.assertEquals(parsed, new BigDecimal(decimal));
            Assert.assertEquals(response.as(Account.class).balance(), new BigDecimal(decimal));
        }
    }

    @Test(groups = "Unit")
    public void ambiguousPostIsSentExactlyOnceAndHasTransportEvidence() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (LocalServer server = new LocalServer(exchange -> {
            receive(exchange);
            calls.incrementAndGet();
            // Simulate a server committing a write then losing its response.
            exchange.close();
        })) {
            ApiEvidenceFilter.begin("unit-ambiguous-write");
            AccountClient accounts = new AccountClient(new ApiRequestFactory(config(server.baseUrl())));
            Assert.expectThrows(Exception.class, () -> accounts.deposit(11, new BigDecimal("1.01")));
            Assert.assertEquals(calls.get(), 1, "An ambiguous write must never be replayed automatically");
            Assert.assertNotNull(ApiEvidenceFilter.currentFile());
            JsonNode evidence = JSON.readTree(Files.readString(ApiEvidenceFilter.currentFile())).get(0);
            Assert.assertTrue(evidence.hasNonNull("transportError"), "Failed transport needs diagnostic evidence");
            Assert.assertFalse(evidence.has("status"), "A missing response must not be presented as an HTTP response");
        }
    }

    @Test(groups = "Unit")
    public void credentialTransportFailureCannotLeakTheRawLoginUri() throws Exception {
        try (LocalServer server = new LocalServer(exchange -> {
            receive(exchange);
            exchange.close();
        })) {
            ApiEvidenceFilter.begin("unit-login-transport");
            CustomerClient customers = new CustomerClient(new ApiRequestFactory(config(server.baseUrl())));
            Exception failure = Assert.expectThrows(Exception.class,
                    () -> customers.login("USER_SECRET", "PASSWORD_SECRET"));
            Assert.assertFalse(failure.toString().contains("SECRET"));
            Assert.assertNull(failure.getCause(), "Underlying transport exceptions can retain credential-bearing URLs");
            Assert.assertTrue(failure.getMessage().contains("/login/{username}/{password}"),
                    "Failure should identify the sanitized endpoint");
            String evidence = Files.readString(ApiEvidenceFilter.currentFile());
            Assert.assertFalse(evidence.contains("SECRET"));
        }
    }

    @Test(groups = "Unit")
    public void redirectsAreReturnedWithoutReplayingAWrite() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (LocalServer server = new LocalServer(exchange -> {
            receive(exchange);
            calls.incrementAndGet();
            exchange.getResponseHeaders().set("Location", "/parabank/services/bank/redirected");
            exchange.sendResponseHeaders(307, -1);
            exchange.close();
        })) {
            Response response = new AccountClient(new ApiRequestFactory(config(server.baseUrl())))
                    .deposit(11, new BigDecimal("1.00"));
            Assert.assertEquals(response.statusCode(), 307);
            Assert.assertEquals(calls.get(), 1, "Redirect following would risk replaying a banking write");
        }
    }

    @Test(groups = "Unit")
    public void datesUseTheServerTimezoneAcrossMidnightAndLeapDay() {
        String epoch = Long.toString(Instant.parse("2024-02-29T23:30:00Z").toEpochMilli());
        Assert.assertEquals(ApiDates.transactionDate(epoch, ZoneId.of("UTC")), LocalDate.of(2024, 2, 29));
        Assert.assertEquals(ApiDates.transactionDate(epoch, ZoneId.of("Asia/Kolkata")), LocalDate.of(2024, 3, 1));
        Assert.assertEquals(ApiDates.transactionDate("2024-03-01T00:30:00+02:00", ZoneId.of("UTC")),
                LocalDate.of(2024, 2, 29));
        Assert.assertEquals(ApiDates.transactionDate("2024-02-29", ZoneId.of("UTC")), LocalDate.of(2024, 2, 29));
        Assert.expectThrows(IllegalArgumentException.class, () -> ApiDates.transactionDate("", ZoneId.of("UTC")));
    }

    @Test(groups = "Unit")
    public void ledgerComparisonIgnoresOrderButRejectsMissingDuplicateAndExtraEntries() {
        Transaction old = transaction(1, "Credit", "10.00");
        Transaction added = transaction(2, "Debit", "1.25");
        Transaction unexpected = transaction(3, "Debit", "1.25");
        List<Transaction> before = List.of(old);
        Assert.assertEquals(ApiAssertions.newTransactions(before, List.of(added, old)), List.of(added));
        Assert.assertEquals(ApiAssertions.singleTransaction(before, List.of(added, old), 11, "Debit",
                new BigDecimal("1.250")), added);
        Assert.expectThrows(AssertionError.class, () -> ApiAssertions.newTransactions(before, List.of(added)));
        Assert.expectThrows(AssertionError.class, () -> ApiAssertions.newTransactions(before, List.of(old, old)));
        Assert.expectThrows(AssertionError.class, () -> ApiAssertions.newTransactions(List.of(old, old), List.of(old)));
        Transaction changedOld = transaction(old.id(), "Credit", "999.00");
        Assert.expectThrows(AssertionError.class,
                () -> ApiAssertions.newTransactions(before, List.of(changedOld, added)));
        Assert.expectThrows(AssertionError.class, () -> ApiAssertions.singleTransaction(before,
                List.of(old, added, unexpected), 11, "Debit", new BigDecimal("1.25")));
        Assert.expectThrows(AssertionError.class, () -> ApiAssertions.singleTransaction(before,
                List.of(old, added), 11, "Credit", new BigDecimal("1.25")));
        Assert.expectThrows(AssertionError.class, () -> ApiAssertions.singleTransaction(before,
                List.of(old, added), 22, "Debit", new BigDecimal("1.25")));
        Assert.expectThrows(AssertionError.class, () -> ApiAssertions.singleTransaction(before,
                List.of(old, added), 11, "Debit", new BigDecimal("1.26")));
        ApiAssertions.money(new BigDecimal("1.0"), new BigDecimal("1.00"));
        Assert.expectThrows(AssertionError.class,
                () -> ApiAssertions.money(new BigDecimal("1.000000000000000001"), BigDecimal.ONE));
    }

    @Test(groups = "Unit")
    public void assertionFailuresCannotPrintPayeeNamesFromTransactionOrBillPaymentModels() {
        Transaction transaction = new Transaction(2, 11, "Debit", "2024-02-29T12:00:00Z",
                new BigDecimal("1.25"), "Bill payment to PAYEE_SECRET");
        AssertionError failure = Assert.expectThrows(AssertionError.class,
                () -> Assert.assertEquals(List.of(transaction), List.of(transaction(3, "Debit", "1.25"))));
        Assert.assertFalse(failure.toString().contains("PAYEE_SECRET"), "Ledger failure printed its sensitive description");
        BillPayResult payment = new BillPayResult("PAYEE_SECRET", new BigDecimal("1.25"), 11);
        AssertionError paymentFailure = Assert.expectThrows(AssertionError.class,
                () -> Assert.assertEquals(payment, new BillPayResult("OTHER_PAYEE_SECRET", new BigDecimal("1.25"), 11)));
        Assert.assertFalse(paymentFailure.toString().contains("PAYEE_SECRET"), "Payment failure printed a payee name");
    }

    private static Transaction transaction(int id, String type, String amount) {
        return new Transaction(id, 11, type, "2024-02-29T12:00:00Z", new BigDecimal(amount), "Synthetic entry");
    }

    private static ApiConfig config(String baseUrl) {
        return new ApiConfig(new FrameworkConfig(defaults(baseUrl), new Properties(), Map.of()));
    }

    private static Properties defaults(String baseUrl) {
        Properties values = new Properties();
        values.setProperty("api.baseUrl", baseUrl);
        values.setProperty("api.environment", "unit");
        values.setProperty("api.fixture.mode", "web");
        values.setProperty("api.fixture.file", "");
        values.setProperty("api.connectTimeout.seconds", "1");
        values.setProperty("api.readTimeout.seconds", "2");
        return values;
    }

    private static Map<String, String> query(String query) {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        if (query != null && !query.isEmpty()) {
            for (String entry : query.split("&")) {
                String[] parts = entry.split("=", 2);
                result.put(decode(parts[0]), parts.length == 2 ? decode(parts[1]) : "");
            }
        }
        return result;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static Received receive(HttpExchange exchange) throws IOException {
        return new Received(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(),
                exchange.getRequestURI().getRawQuery(),
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                exchange.getRequestHeaders().getFirst("Accept"), exchange.getRequestHeaders().getFirst("Content-Type"));
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var stream = exchange.getResponseBody()) {
            stream.write(bytes);
        } finally {
            exchange.close();
        }
    }

    private record Received(String method, String path, String query, String body, String accept, String contentType) { }

    private static final class LocalServer implements AutoCloseable {
        private final HttpServer server;

        private LocalServer(HttpHandler handler) throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", handler);
            server.start();
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/parabank/services/bank";
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
