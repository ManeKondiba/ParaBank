package TestClases;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.Logs;
import org.testng.Assert;
import org.testng.annotations.Test;
import utilities.BrowserDiagnostics;
import utilities.DataProviders;
import utilities.DriverFactory;
import utilities.FrameworkConfig;
import utilities.RegistrationData;
import utilities.TransactionData;
import utilities.UiLedgerAssertions;
import java.math.BigDecimal;

public class UtilityUnitTest {
    @Test(groups = "Unit")
    public void resolvesConfigurationOverridesInOrder() {
        FrameworkConfig config = new FrameworkConfig(
                properties("setting", "file"), properties("setting", "jvm"),
                Map.of("PARABANK_SETTING", "environment"));
        Assert.assertEquals(config.parameter("setting", "xml"), "jvm");

        config = new FrameworkConfig(properties("setting", "file"), new Properties(),
                Map.of("PARABANK_SETTING", "environment"));
        Assert.assertEquals(config.parameter("setting", "xml"), "environment");

        config = new FrameworkConfig(properties("setting", "file"), new Properties(), Map.of());
        Assert.assertEquals(config.parameter("setting", "xml"), "xml");
        Assert.assertEquals(config.parameter("setting", " "), "file");
    }

    @Test(groups = "Unit")
    public void validatesBooleanIntegerAndDurationSettings() {
        FrameworkConfig config = new FrameworkConfig(
                properties("enabled", "true", "count", "3"), new Properties(), Map.of());
        Assert.assertTrue(config.getBoolean("enabled"));
        Assert.assertEquals(config.getInt("count"), 3);
        Assert.assertEquals(config.getDuration("count"), Duration.ofSeconds(3));

        FrameworkConfig invalid = new FrameworkConfig(
                properties("enabled", "yes", "count", "0"), new Properties(), Map.of());
        Assert.expectThrows(IllegalArgumentException.class, () -> invalid.getBoolean("enabled"));
        Assert.expectThrows(IllegalArgumentException.class, () -> invalid.getInt("count"));
    }

    @Test(groups = "Unit")
    public void validatesLoginRowsAndSupportsLegacyResultHeader() {
        String[][] rows = DataProviders.validateRows(List.of(
                List.of(" Username ", "password", "res"),
                List.of("alice", "secret", "Valid"),
                List.of("", "", ""),
                List.of("unknown", "", "Invalid")));

        Assert.assertEquals(rows.length, 2);
        Assert.assertEquals(rows[0], new String[] {"alice", "secret", "Valid"});
        Assert.assertEquals(rows[1], new String[] {"unknown", "", "Invalid"});
    }

    @Test(groups = "Unit")
    public void rejectsMalformedLoginRows() {
        Assert.expectThrows(IllegalArgumentException.class,
                () -> DataProviders.validateRows(List.of(List.of("username", "password"))));
        Assert.expectThrows(IllegalArgumentException.class,
                () -> DataProviders.validateRows(List.of(List.of("username", "password", "expected"),
                        List.of("alice", "secret", "unknown"))));
        Assert.expectThrows(IllegalArgumentException.class,
                () -> DataProviders.validateRows(List.of(List.of("username", "password", "expected"),
                        List.of("", "", "Valid"))));
        Assert.expectThrows(IllegalArgumentException.class,
                () -> DataProviders.validateRows(List.of(List.of("username", "password", "expected"),
                        List.of(DataProviders.FRESH_ACCOUNT, "secret", "Invalid"))));
    }

    @Test(groups = "Unit")
    public void recognizesFreshAccountMarkerOnlyForValidPlaceholder() {
        Assert.assertTrue(DataProviders.isFreshAccount(
                DataProviders.FRESH_ACCOUNT, DataProviders.FRESH_ACCOUNT, "Valid"));
        Assert.assertFalse(DataProviders.isFreshAccount(
                DataProviders.FRESH_ACCOUNT, DataProviders.FRESH_ACCOUNT, "Invalid"));
        Assert.assertFalse(DataProviders.isFreshAccount("alice", "secret", "Valid"));
    }

    @Test(groups = "Unit")
    public void createsUniqueRegistrationDataAndRedactsPassword() {
        RegistrationData first = RegistrationData.unique();
        RegistrationData second = RegistrationData.unique();
        Assert.assertNotEquals(first.username(), second.username());
        Assert.assertTrue(first.toString().contains("password=<redacted>"));
        Assert.assertFalse(first.toString().contains(first.password()));
    }

    @Test(groups = "Unit")
    public void acceptsOnlyAbsoluteHttpUrlsWithoutCredentials() {
        Assert.assertEquals(DriverFactory.httpUri("https://example.test/app", "appUrl").getHost(),
                "example.test");
        Assert.expectThrows(IllegalArgumentException.class,
                () -> DriverFactory.httpUri("ftp://example.test/app", "appUrl"));
        Assert.expectThrows(IllegalArgumentException.class,
                () -> DriverFactory.httpUri("https://user@example.test/app", "remote.url"));
    }

        @Test(groups = "Unit")
        public void sanitizesBrowserDiagnosticUrlsAndPersonalIdentifiers() {
                Assert.assertEquals(BrowserDiagnostics.sanitizeUrl(
                                "https://user:pass@localhost/parabank/login/alice/secret?token=QUERY_SECRET#fragment"),
                                "https://localhost/parabank/login/{username}/{password}");
                String sanitized = BrowserDiagnostics.sanitizeText(
                                "password=BODY_SECRET token: TOKEN_SECRET ssn=123-45-6789 user=person@example.test "
                                                + "at https://localhost/path?accountId=123");
                Assert.assertFalse(sanitized.contains("SECRET"));
                Assert.assertFalse(sanitized.contains("123-45-6789"));
                Assert.assertFalse(sanitized.contains("person@example.test"));
                Assert.assertFalse(sanitized.contains("accountId=123"));
        }

        @Test(groups = "Unit")
        public void writesBrowserDiagnosticContextWhenLogTypesAreUnavailable() throws IOException {
                Logs logs = proxy(Logs.class, (instance, method, arguments) -> Set.of());
                WebDriver.Options options = proxy(WebDriver.Options.class, (instance, method, arguments) ->
                                method.getName().equals("logs") ? logs : null);
                WebDriver driver = proxy(WebDriver.class, (instance, method, arguments) -> switch (method.getName()) {
                        case "getCurrentUrl" -> "https://user:secret@localhost/parabank/login/alice/pass?token=TOP_SECRET";
                        case "getTitle" -> "Session password=TITLE_SECRET";
                        case "manage" -> options;
                        default -> null;
                });

                var artifact = BrowserDiagnostics.capture(driver, "diagnostics-redaction");
                try {
                        String content = Files.readString(artifact);
                        Assert.assertTrue(content.contains("/parabank/login/{username}/{password}"));
                        Assert.assertTrue(content.contains("[unavailable]"));
                        Assert.assertFalse(content.contains("secret"));
                        Assert.assertFalse(content.contains("TOP_SECRET"));
                        Assert.assertFalse(content.contains("TITLE_SECRET"));
                } finally {
                        Files.deleteIfExists(artifact);
                }
        }

        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> contract, java.lang.reflect.InvocationHandler handler) {
                return (T) Proxy.newProxyInstance(contract.getClassLoader(), new Class<?>[] {contract}, handler);
        }

    @Test(groups = "Unit")
    public void apiBackedUiFixturesRejectMismatchedDeploymentsBeforeProvisioning() throws IOException {
        Properties defaults = new Properties();
        try (var stream = FrameworkConfig.class.getResourceAsStream("/config.properties")) {
            defaults.load(stream);
        }
        Properties overrides = properties("ui.fixture.mode", "api",
                "appUrl", "http://localhost:80/parabank/index.htm",
                "api.baseUrl", "http://localhost/parabank/services/bank");
        FrameworkConfig valid = new FrameworkConfig(defaults, overrides, Map.of());
        Assert.assertTrue(TestBases.UiApiFixture.enabled(valid));
        for (String mismatch : List.of("http://localhost:8081/parabank/index.htm",
                "https://localhost/parabank/index.htm", "http://localhost/other/index.htm",
                "http://127.0.0.1/parabank/index.htm")) {
            overrides.setProperty("appUrl", mismatch);
            FrameworkConfig invalid = new FrameworkConfig(defaults, overrides, Map.of());
            Assert.expectThrows(IllegalArgumentException.class, () -> TestBases.UiApiFixture.enabled(invalid));
        }
        overrides.setProperty("appUrl", "https://parabank.parasoft.com/parabank/index.htm");
        overrides.setProperty("api.baseUrl", "https://parabank.parasoft.com/parabank/services/bank");
        FrameworkConfig shared = new FrameworkConfig(defaults, overrides, Map.of());
        Assert.expectThrows(IllegalArgumentException.class, () -> TestBases.UiApiFixture.enabled(shared));
        overrides.setProperty("ui.fixture.mode", "invalid");
        FrameworkConfig badMode = new FrameworkConfig(defaults, overrides, Map.of());
        Assert.expectThrows(IllegalArgumentException.class, () -> TestBases.UiApiFixture.enabled(badMode));
    }

    private Properties properties(String... entries) {
        Properties properties = new Properties();
        for (int index = 0; index < entries.length; index += 2) {
            properties.setProperty(entries[index], entries[index + 1]);
        }
        return properties;
    }
    @Test(groups = "Unit")
    public void uiLedgerChecksRejectDuplicatesExtraEntriesAndChangedHistory() {
        TransactionData old = new TransactionData("1", "10-07-2026", "Opening", BigDecimal.ZERO, BigDecimal.TEN);
        TransactionData debit = new TransactionData("2", "10-07-2026", "Transfer", BigDecimal.ONE, BigDecimal.ZERO);
        TransactionData extra = new TransactionData("3", "10-07-2026", "Extra", BigDecimal.ONE, BigDecimal.ZERO);
        UiLedgerAssertions.sameRecords(List.of(old, debit), List.of(debit, old));
        UiLedgerAssertions.singleEntry(List.of(old), List.of(debit, old), "Debit", BigDecimal.ONE);
        Assert.expectThrows(AssertionError.class, () -> UiLedgerAssertions.sameRecords(List.of(old, old), List.of(old)));
        Assert.expectThrows(AssertionError.class, () -> UiLedgerAssertions.singleEntry(List.of(old), List.of(old, debit, extra), "Debit", BigDecimal.ONE));
        Assert.expectThrows(AssertionError.class, () -> UiLedgerAssertions.singleEntry(List.of(old), List.of(debit), "Debit", BigDecimal.ONE));
        TransactionData changed = new TransactionData("1", old.date(), old.description(), BigDecimal.ONE, BigDecimal.TEN);
        Assert.expectThrows(AssertionError.class, () -> UiLedgerAssertions.singleEntry(List.of(old), List.of(changed, debit), "Debit", BigDecimal.ONE));
    }

}
