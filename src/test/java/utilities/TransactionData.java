package utilities;

import java.math.BigDecimal;

public record TransactionData(String id, String date, String description, BigDecimal debit, BigDecimal credit) {
}
