package utilities.api.models;

public record Customer(int id, String firstName, String lastName, Address address, String phoneNumber, String ssn) {
    @Override
    public String toString() { return "Customer[id=" + id + ", profile=redacted]"; }
}
