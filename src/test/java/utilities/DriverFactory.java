package utilities;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Locale;
import java.util.logging.Level;

import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

public final class DriverFactory {
    private DriverFactory() {
    }

    public static MutableCapabilities options(FrameworkConfig config, String browser) {
        boolean headless = config.getBoolean("headless");
        String browserBinary = config.get("browser.binary");

        switch (browser.toLowerCase(Locale.ROOT)) {
            case "chrome": {
                ChromeOptions options = new ChromeOptions();
                options.setCapability("goog:loggingPrefs", chromiumLoggingPreferences());
                if (headless) {
                    options.addArguments("--headless=new");
                }
                if (!browserBinary.isBlank()) {
                    options.setBinary(browserBinary);
                }
                return options;
            }
            case "edge": {
                EdgeOptions options = new EdgeOptions();
                options.setCapability("goog:loggingPrefs", chromiumLoggingPreferences());
                if (headless) {
                    options.addArguments("--headless=new");
                }
                if (!browserBinary.isBlank()) {
                    options.setBinary(browserBinary);
                }
                return options;
            }
            case "firefox": {
                FirefoxOptions options = new FirefoxOptions();
                if (headless) {
                    options.addArguments("-headless");
                }
                if (!browserBinary.isBlank()) {
                    options.setBinary(browserBinary);
                }
                return options;
            }
            default:
                throw new IllegalArgumentException("Unsupported browser: " + browser
                        + ". Use chrome, edge or firefox.");
        }
    }

    public static WebDriver create(FrameworkConfig config, String xmlBrowser, String xmlOs) {
        String browser = config.parameter("browser", xmlBrowser).toLowerCase(Locale.ROOT);
        MutableCapabilities options = options(config, browser);
        String remoteUrl = config.get("remote.url");

        if (!remoteUrl.isBlank()) {
            String platform = config.parameter("os", xmlOs);
            if (!platform.isBlank()) {
                options.setCapability("platformName", platform);
            }
            try {
                return new RemoteWebDriver(httpUri(remoteUrl, "remote.url").toURL(), options);
            } catch (MalformedURLException exception) {
                throw new IllegalArgumentException("Invalid remote.url", exception);
            }
        }

        // Selenium Manager resolves local drivers automatically.
        return switch (browser) {
            case "chrome" -> new ChromeDriver((ChromeOptions) options);
            case "edge" -> new EdgeDriver((EdgeOptions) options);
            case "firefox" -> new FirefoxDriver((FirefoxOptions) options);
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        };
    }

    public static URI httpUri(String value, String key) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            boolean httpScheme = "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme);
            if (!httpScheme || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(key + " must be an absolute HTTP(S) URL without embedded credentials");
        }
    }

    private static LoggingPreferences chromiumLoggingPreferences() {
        LoggingPreferences preferences = new LoggingPreferences();
        preferences.enable(LogType.BROWSER, Level.ALL);
        preferences.enable(LogType.PERFORMANCE, Level.ALL);
        return preferences;
    }
}
