package utilities.api.models;

import java.math.BigDecimal;

/** Preserve the deployed timestamp representation; assertions interpret it with the target timezone. */
public record Transaction(int id, int accountId, String type, String date, BigDecimal amount, String description) {
    @Override
    public String toString() {
        return "Transaction[id=" + id + ", accountId=" + accountId + ", type=" + type + ", date=" + date
                + ", amount=" + amount + ", description=[redacted]]";
    }
}
