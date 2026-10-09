package utilities;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;

/** Compare persisted UI activity without depending on row order. */
public final class UiLedgerAssertions {
    private UiLedgerAssertions() { }

    private static Map<String, TransactionData> indexed(List<TransactionData> rows) {
        Map<String, TransactionData> indexed = new LinkedHashMap<>();
        for (TransactionData row : rows) {
            Assert.assertNull(indexed.put(row.id(), row), "Duplicate transaction ID: " + row.id());
        }
        return indexed;
    }

    public static void sameRecords(List<TransactionData> actual, List<TransactionData> expected) {
        Assert.assertEquals(indexed(actual), indexed(expected), "Activity must contain all and only expected records");
    }

    public static TransactionData singleEntry(List<TransactionData> before, List<TransactionData> after,
            String direction, BigDecimal amount) {
        Map<String, TransactionData> old = indexed(before);
        Map<String, TransactionData> current = indexed(after);
        old.forEach((id, row) -> Assert.assertEquals(current.get(id), row,
                "An existing transaction disappeared or changed: " + id));
        List<TransactionData> added = after.stream().filter(row -> !old.containsKey(row.id())).toList();
        Assert.assertEquals(added.size(), 1, "One accepted operation must append exactly one activity entry");
        TransactionData entry = added.get(0);
        Assert.assertTrue(direction.equals("Debit") || direction.equals("Credit"), "Unsupported direction");
        BigDecimal actual = direction.equals("Debit") ? entry.debit() : entry.credit();
        BigDecimal opposite = direction.equals("Debit") ? entry.credit() : entry.debit();
        Assert.assertEquals(actual.compareTo(amount), 0, "Transaction amount");
        Assert.assertEquals(opposite.compareTo(BigDecimal.ZERO), 0, "Opposite amount column");
        return entry;
    }
}
