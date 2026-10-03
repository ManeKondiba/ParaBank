package utilities.api.assertions;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.restassured.common.mapper.TypeRef;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.testng.Assert;
import utilities.api.models.Account;
import utilities.api.models.Customer;
import utilities.api.models.Transaction;

public final class ApiAssertions {
    private ApiAssertions() { }

    public static Response success(Response response) {
        Assert.assertEquals(response.statusCode(), 200, "HTTP status; see sanitized API evidence");
        return response;
    }

    public static Response json(Response response, int status, String schemaFile) {
        Assert.assertEquals(response.statusCode(), status, "HTTP status; see sanitized API evidence");
        Assert.assertTrue(response.contentType().toLowerCase(java.util.Locale.ROOT).contains("application/json"),
                "Expected application/json response");
        try {
            Assert.assertTrue(JsonSchemaValidator.matchesJsonSchemaInClasspath("api/schemas/" + schemaFile)
                    .matches(response.asString()), "Response violates " + schemaFile + "; see sanitized API evidence");
        } catch (RuntimeException exception) {
            throw new AssertionError("Cannot validate response against " + schemaFile + "; see sanitized API evidence");
        }
        return response;
    }

    public static Account account(Response response) {
        return decode(json(response, 200, "account.json"), Account.class);
    }

    public static Customer customer(Response response) {
        return decode(json(response, 200, "customer.json"), Customer.class);
    }

    public static Transaction transaction(Response response) {
        return decode(json(response, 200, "transaction.json"), Transaction.class);
    }

    public static List<Account> accountList(Response response) {
        json(response, 200, "accounts.json");
        try {
            return response.as(new TypeRef<List<Account>>() { });
        } catch (RuntimeException exception) {
            throw new AssertionError("Cannot decode account collection; see sanitized API evidence");
        }
    }

    public static List<Transaction> transactionList(Response response) {
        json(response, 200, "transactions.json");
        try {
            return response.as(new TypeRef<List<Transaction>>() { });
        } catch (RuntimeException exception) {
            throw new AssertionError("Cannot decode transaction collection; see sanitized API evidence");
        }
    }

    public static <T> T decode(Response response, Class<T> type) {
        try {
            return response.as(type);
        } catch (RuntimeException exception) {
            throw new AssertionError("Cannot decode " + type.getSimpleName() + "; see sanitized API evidence");
        }
    }

    public static void money(BigDecimal actual, BigDecimal expected) {
        Assert.assertNotNull(actual, "Money value must be present");
        Assert.assertEquals(actual.compareTo(expected), 0, "Expected amount " + expected + ", got " + actual);
    }

    public static List<Transaction> newTransactions(List<Transaction> before, List<Transaction> after) {
        Set<Integer> oldIds = new HashSet<>();
        before.forEach(value -> Assert.assertTrue(oldIds.add(value.id()), "Duplicate transaction ID before action"));
        Set<Integer> newIds = new HashSet<>();
        after.forEach(value -> Assert.assertTrue(newIds.add(value.id()), "Duplicate transaction ID after action"));
        Assert.assertTrue(newIds.containsAll(oldIds), "Existing transactions disappeared");
        var previous = before.stream().collect(java.util.stream.Collectors.toMap(Transaction::id, value -> value));
        after.stream().filter(value -> previous.containsKey(value.id())).forEach(value ->
                Assert.assertTrue(value.equals(previous.get(value.id())), "An existing transaction was modified"));
        return after.stream().filter(value -> !oldIds.contains(value.id())).toList();
    }

    public static Transaction singleTransaction(List<Transaction> before, List<Transaction> after,
            int accountId, String type, BigDecimal amount) {
        List<Transaction> added = newTransactions(before, after);
        Assert.assertEquals(added.size(), 1, "Expected exactly one new ledger entry");
        Transaction transaction = added.get(0);
        Assert.assertEquals(transaction.accountId(), accountId, "Transaction account");
        Assert.assertEquals(transaction.type(), type, "Transaction direction");
        money(transaction.amount(), amount);
        Assert.assertNotNull(transaction.date(), "Transaction date");
        return transaction;
    }
}
