package TestClases;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;
import utilities.DataProviders;
import utilities.DriverFactory;
import utilities.FrameworkConfig;
import utilities.RegistrationData;

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

    private Properties properties(String... entries) {
        Properties properties = new Properties();
        for (int index = 0; index < entries.length; index += 2) {
            properties.setProperty(entries[index], entries[index + 1]);
        }
        return properties;
    }
}