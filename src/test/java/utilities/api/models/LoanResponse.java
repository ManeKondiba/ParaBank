package utilities.api.models;

public record LoanResponse(String responseDate, String loanProviderName, boolean approved, String message,
        Integer accountId) { }
