package utilities;

import java.util.UUID;

public record RegistrationData(String username, String password) {

    public static RegistrationData unique() {
        // Keep normal fixtures short; long credentials can be rejected as duplicate usernames.
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return new RegistrationData("auto" + suffix, "Test!" + suffix.substring(0, 12));
    }

    @Override
    public String toString() {
        return "RegistrationData[username=" + username + ", password=<redacted>]";
    }
}
