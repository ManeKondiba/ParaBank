package utilities;

/**
 * Immutable strongly-typed customer profile representation for banking test scenarios.
 * Masks sensitive attributes (password, ssn) in toString() to enforce PII/secret security guardrails.
 */
public record CustomerProfile(
        String firstName,
        String lastName,
        String street,
        String city,
        String state,
        String zipCode,
        String phoneNumber,
        String ssn,
        String username,
        String password
) {
    public RegistrationData toRegistrationData() {
        return new RegistrationData(username, password);
    }

    @Override
    public String toString() {
        return "CustomerProfile[username=" + username + ", firstName=" + firstName + ", lastName=" + lastName
                + ", ssn=<redacted>, password=<redacted>]";
    }
}
