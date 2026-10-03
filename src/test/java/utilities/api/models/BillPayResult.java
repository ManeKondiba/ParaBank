package utilities.api.models;

import java.math.BigDecimal;

public record BillPayResult(String payeeName, BigDecimal amount, int accountId) {
    @Override
    public String toString() {
        return "BillPayResult[payeeName=[redacted], amount=" + amount + ", accountId=" + accountId + "]";
    }
}
