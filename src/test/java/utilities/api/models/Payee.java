package utilities.api.models;

public record Payee(String name, Address address, String phoneNumber, Integer accountNumber) {
    @Override
    public String toString() { return "Payee[redacted]"; }
}
