package utilities.api;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

import utilities.FrameworkConfig;

/** Validated API settings, independent of the browser configuration. */
public final class ApiConfig {
    private final URI baseUri;
    private final String environment;
    private final String fixtureMode;
    private final String fixtureFile;
    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;

    public ApiConfig(FrameworkConfig config) {
        Objects.requireNonNull(config, "config");
        baseUri = validatedUri(config.get("api.baseUrl"));
        environment = config.get("api.environment");
        if (environment.isBlank()) {
            throw new IllegalArgumentException("api.environment must not be blank");
        }
        fixtureMode = config.get("api.fixture.mode").toLowerCase(Locale.ROOT);
        if (!fixtureMode.equals("web") && !fixtureMode.equals("file")) {
            throw new IllegalArgumentException("api.fixture.mode must be web or file");
        }
        fixtureFile = config.get("api.fixture.file");
        if (fixtureMode.equals("file") && fixtureFile.isBlank()) {
            throw new IllegalArgumentException("api.fixture.file is required for file fixture mode");
        }
        connectTimeoutSeconds = timeout(config, "api.connectTimeout.seconds");
        readTimeoutSeconds = timeout(config, "api.readTimeout.seconds");
    }

    public static ApiConfig load() {
        return new ApiConfig(FrameworkConfig.load());
    }

    public String baseUrl() { return baseUri.toString(); }
    public String environment() { return environment; }
    public String fixtureMode() { return fixtureMode; }
    public String fixtureFile() { return fixtureFile; }
    public int connectTimeoutSeconds() { return connectTimeoutSeconds; }
    public int readTimeoutSeconds() { return readTimeoutSeconds; }

    /** Registration is an explicitly separate web adapter, derived from this API's deployment. */
    public String webBaseUrl() {
        String suffix = "/services/bank";
        if (!baseUri.getPath().endsWith(suffix)) {
            throw new IllegalArgumentException("Web fixture mode requires api.baseUrl to end with /services/bank");
        }
        return baseUrl().substring(0, baseUrl().length() - suffix.length());
    }

    public boolean isSharedPublicHost() {
        return isSharedPublicHost(baseUri.getHost());
    }

    public static boolean isSharedPublicHost(String host) {
        String normalized = Objects.requireNonNull(host, "host").toLowerCase(Locale.ROOT)
                .replaceAll("\\.+$", "");
        return normalized.equals("parabank.parasoft.com")
                || normalized.endsWith(".parabank.parasoft.com");
    }

    public void requireControlledEnvironment() {
        if (isSharedPublicHost()) {
            throw new IllegalArgumentException("API fixtures and writes require a controlled deployment; "
                    + "the shared ParaBank demo cannot be used");
        }
    }

    private static int timeout(FrameworkConfig config, String key) {
        int seconds = config.getInt(key);
        if (seconds > Integer.MAX_VALUE / 1000) {
            throw new IllegalArgumentException(key + " exceeds the supported timeout range");
        }
        return seconds;
    }

    private static URI validatedUri(String value) {
        URI uri;
        try {
            uri = URI.create(value.replaceAll("/+$", ""));
        } catch (IllegalArgumentException exception) {
            // Do not echo a malformed URL: it may contain credentials.
            throw new IllegalArgumentException("api.baseUrl must be an absolute HTTP(S) URL");
        }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null
                || uri.getPort() > 65535 || uri.getPort() == 0) {
            throw new IllegalArgumentException("api.baseUrl must be an absolute HTTP(S) URL "
                    + "without credentials, query or fragment and with a valid port");
        }
        return uri;
    }
}
