package utilities;

import java.util.UUID;

/**
 * Enterprise Test Data Factory for provisioning typed, synthetic Customer profiles.
 * Replaces ad-hoc string literals and brittle data setup across test classes.
 */
public final class CustomerDataFactory {

    private CustomerDataFactory() {
    }

    public static CustomerProfile defaultProfile() {
        RegistrationData credentials = RegistrationData.unique();
        return new CustomerProfile(
                "Automation",
                "Tester",
                "123 Test Street",
                "Springfield",
                "IL",
                "62701",
                "5551234567",
                "123456789",
                credentials.username(),
                credentials.password()
        );
    }

    public static CustomerProfile unique() {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String ssn = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return new CustomerProfile(
                "Auto" + token.substring(0, 4),
                "User" + token.substring(4, 8),
                token + " Main Road",
                "Denver",
                "CO",
                "80201",
                "555" + (1000000 + (int) (Math.random() * 8999999)),
                ssn,
                "usr" + token,
                "Pass!" + token
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String firstName = "Automation";
        private String lastName = "Tester";
        private String street = "123 Test Street";
        private String city = "Springfield";
        private String state = "IL";
        private String zipCode = "62701";
        private String phoneNumber = "5551234567";
        private String ssn = "123456789";
        private String username;
        private String password;

        public Builder() {
            RegistrationData credentials = RegistrationData.unique();
            this.username = credentials.username();
            this.password = credentials.password();
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder street(String street) {
            this.street = street;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder zipCode(String zipCode) {
            this.zipCode = zipCode;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder ssn(String ssn) {
            this.ssn = ssn;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public CustomerProfile build() {
            return new CustomerProfile(firstName, lastName, street, city, state, zipCode, phoneNumber, ssn, username, password);
        }
    }
}
