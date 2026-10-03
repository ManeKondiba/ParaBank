package utilities;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

/** Captures bounded, best-effort browser diagnostics without request/response bodies. */
public final class BrowserDiagnostics {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path OUTPUT_DIRECTORY = Path.of("target", "browser-diagnostics");
    private static final Pattern URL = Pattern.compile("https?://[^\\s\\\"'<>]+");
    private static final Pattern SECRET = Pattern.compile(
            "(?i)\\b(password|passwd|token|authorization|cookie|ssn|account(?:Number|Id))\\b"
                    + "(\\s*[:=]\\s*)([^\\s,&;]+)");
    private static final Pattern EMAIL = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final int MAX_ENTRIES = 300;
    private static final int MAX_MESSAGE_LENGTH = 1500;

    private BrowserDiagnostics() { }

    public static Path capture(WebDriver driver, String testName) throws IOException {
        String safeName = testName.replaceAll("[^A-Za-z0-9._-]", "_");
        Path output = OUTPUT_DIRECTORY.resolve(safeName + "-" + UUID.randomUUID() + ".txt");
        StringBuilder report = new StringBuilder();
        appendBrowserDetails(report, driver);
        appendPageState(report, driver);
        appendLogEntries(report, driver, LogType.BROWSER, "Browser console");
        appendNetworkEvents(report, driver);
        Files.createDirectories(OUTPUT_DIRECTORY);
        Files.writeString(output, report.toString(), StandardCharsets.UTF_8);
        return output;
    }

    public static String sanitizeUrl(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (host == null || uri.getScheme() == null) {
                return "[url omitted]";
            }
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            path = path.replaceAll("(?i)(/login/)[^/]+/[^/]+", "$1{username}/{password}");
            path = path.replaceAll("(?i)(/customers/update/)[^/]+", "$1{customerId}");
            int port = uri.getPort();
            return uri.getScheme() + "://" + host + (port < 0 ? "" : ":" + port) + path;
        } catch (IllegalArgumentException exception) {
            return "[url omitted]";
        }
    }

    public static String sanitizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        Matcher urls = URL.matcher(value);
        StringBuffer sanitized = new StringBuffer();
        while (urls.find()) {
            urls.appendReplacement(sanitized, Matcher.quoteReplacement(sanitizeUrl(urls.group())));
        }
        urls.appendTail(sanitized);
        String result = SECRET.matcher(sanitized.toString()).replaceAll("$1$2[redacted]");
        return EMAIL.matcher(result).replaceAll("[email redacted]");
    }

    private static void appendPageState(StringBuilder report, WebDriver driver) {
        try {
            report.append("url=").append(sanitizeUrl(driver.getCurrentUrl())).append('\n');
        } catch (RuntimeException exception) {
            report.append("url=[unavailable: ").append(exception.getClass().getSimpleName()).append("]\n");
        }
        try {
            report.append("title=").append(sanitizeText(driver.getTitle())).append('\n');
        } catch (RuntimeException exception) {
            report.append("title=[unavailable: ").append(exception.getClass().getSimpleName()).append("]\n");
        }
        try {
            var size = driver.manage().window().getSize();
            report.append("viewport=").append(size.getWidth()).append('x').append(size.getHeight()).append('\n');
        } catch (RuntimeException exception) {
            report.append("viewport=[unavailable: ").append(exception.getClass().getSimpleName()).append("]\n");
        }
    }

    private static void appendBrowserDetails(StringBuilder report, WebDriver driver) {
        if (driver instanceof HasCapabilities capable) {
            var capabilities = capable.getCapabilities();
            report.append("browser=").append(sanitizeText(capabilities.getBrowserName())).append('\n');
            report.append("browserVersion=").append(sanitizeText(capabilities.getBrowserVersion())).append('\n');
        } else {
            report.append("browser=").append(driver.getClass().getSimpleName()).append('\n');
            report.append("browserVersion=[unavailable]\n");
        }
    }

    private static void appendLogEntries(StringBuilder report, WebDriver driver, String type, String heading) {
        report.append('\n').append("## ").append(heading).append('\n');
        try {
            Set<String> available = driver.manage().logs().getAvailableLogTypes();
            if (!available.contains(type)) {
                report.append("[unavailable]\n");
                return;
            }
            List<LogEntry> entries = driver.manage().logs().get(type).getAll();
            int start = Math.max(0, entries.size() - MAX_ENTRIES);
            for (int index = start; index < entries.size(); index++) {
                LogEntry entry = entries.get(index);
                report.append(entry.getTimestamp()).append(' ')
                        .append(entry.getLevel().getName()).append(' ')
                        .append(truncate(sanitizeText(entry.getMessage()))).append('\n');
            }
            if (entries.isEmpty()) {
                report.append("[no entries]\n");
            }
        } catch (RuntimeException exception) {
            report.append("[unavailable: ").append(exception.getClass().getSimpleName()).append("]\n");
        }
    }

    private static void appendNetworkEvents(StringBuilder report, WebDriver driver) {
        report.append('\n').append("## Chromium network events (metadata only)").append('\n');
        try {
            Set<String> available = driver.manage().logs().getAvailableLogTypes();
            if (!available.contains(LogType.PERFORMANCE)) {
                report.append("[unavailable]\n");
                return;
            }
            List<LogEntry> entries = driver.manage().logs().get(LogType.PERFORMANCE).getAll();
            int start = Math.max(0, entries.size() - MAX_ENTRIES);
            int written = 0;
            for (int index = start; index < entries.size(); index++) {
                String event = networkSummary(entries.get(index).getMessage());
                if (event != null) {
                    report.append(event).append('\n');
                    written++;
                }
            }
            if (written == 0) {
                report.append("[no network events available]\n");
            }
        } catch (RuntimeException exception) {
            report.append("[unavailable: ").append(exception.getClass().getSimpleName()).append("]\n");
        }
    }

    private static String networkSummary(String raw) {
        try {
            JsonNode message = JSON.readTree(raw).path("message");
            String method = message.path("method").asText();
            JsonNode params = message.path("params");
            if (method.equals("Network.requestWillBeSent")) {
                JsonNode request = params.path("request");
                return "request " + request.path("method").asText()
                        + " " + sanitizeUrl(request.path("url").asText());
            }
            if (method.equals("Network.responseReceived")) {
                JsonNode response = params.path("response");
                return "response " + response.path("status").asText()
                        + " " + sanitizeUrl(response.path("url").asText())
                        + " type=" + sanitizeText(response.path("mimeType").asText());
            }
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private static String truncate(String value) {
        if (value.length() <= MAX_MESSAGE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_MESSAGE_LENGTH) + " [truncated]";
    }
}