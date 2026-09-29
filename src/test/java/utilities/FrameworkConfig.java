package utilities;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Immutable configuration snapshot: JVM properties > environment > XML parameters > file defaults. */
public final class FrameworkConfig {
    private final Properties defaults = new Properties();
    private final Properties overrides = new Properties();
    private final Map<String, String> environment;

    public FrameworkConfig(Properties defaults, Properties overrides, Map<String, String> environment) {
        this.defaults.putAll(defaults);
        this.overrides.putAll(overrides);
        this.environment = Map.copyOf(environment);
    }

    public static FrameworkConfig load() {
        Properties defaults = new Properties();
        try (InputStream defaultConfig = FrameworkConfig.class.getResourceAsStream("/config.properties")) {
            if (defaultConfig == null) {
                throw new IllegalStateException("Missing classpath config.properties");
            }
            defaults.load(defaultConfig);

            String externalConfigPath = System.getProperty("config.file", System.getenv("PARABANK_CONFIG_FILE"));
            if (externalConfigPath != null && !externalConfigPath.isBlank()) {
                try (InputStream externalConfig = Files.newInputStream(Path.of(externalConfigPath))) {
                    defaults.load(externalConfig);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load framework configuration", exception);
        }
        return new FrameworkConfig(defaults, System.getProperties(), System.getenv());
    }

    public String get(String key) {
        return parameter(key, null);
    }

    /** XML parameters override file defaults; explicit JVM/environment overrides still win. */
    public String parameter(String key, String xmlValue) {
        String value = overrides.getProperty(key);
        if (value == null) {
            value = environment.get(environmentKey(key));
        }
        if (value == null && xmlValue != null && !xmlValue.isBlank()) {
            value = xmlValue;
        }
        if (value == null) {
            value = defaults.getProperty(key);
        }
        if (value == null) {
            throw new IllegalArgumentException("Missing configuration key: " + key);
        }
        return value.trim();
    }

    public static String environmentKey(String key) {
        return "PARABANK_" + key.replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace('.', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
    }

    public boolean getBoolean(String key) {
        String value = get(key);
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException(key + " must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    public int getInt(String key) {
        try {
            int value = Integer.parseInt(get(key));
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be a positive integer", exception);
        }
    }

    public Duration getDuration(String key) {
        return Duration.ofSeconds(getInt(key));
    }
}
