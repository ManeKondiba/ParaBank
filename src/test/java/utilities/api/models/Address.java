package utilities.api.models;

public record Address(String street, String city, String state, String zipCode) {
    @Override
    public String toString() { return "Address[redacted]"; }
}
