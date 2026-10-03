package TestClases.api;

import static utilities.api.assertions.ApiAssertions.account;
import static utilities.api.assertions.ApiAssertions.money;
import static utilities.api.assertions.ApiAssertions.singleTransaction;
import static utilities.api.assertions.ApiAssertions.success;
import static utilities.api.assertions.ApiAssertions.transaction;
import static utilities.api.assertions.ApiAssertions.transactionList;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.Test;
import TestBases.ApiBaseTest;
import utilities.api.ApiDates;
import utilities.api.models.Transaction;

@Test(groups = "ApiRegression")
public class TransactionsApiTest extends ApiBaseTest {
    private static final BigDecimal CREDIT = new BigDecimal("14.17");
    private static final BigDecimal DEBIT = new BigDecimal("3.21");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    @Test(groups = {"ApiSmoke", "ApiContract"}, description = "TXN-001/002: transaction list and detail agree for a discovered deposit")
    public void transactionDetailsMatchLedger() {
        Transaction created = deposit();
        Transaction detail = transaction(transactions.get(created.id()));
        Assert.assertEquals(detail, created, "Transaction detail must exactly match the account ledger record");
        Assert.assertEquals(detail.accountId(), fixture.fundingAccount().id());
        Assert.assertNotNull(ApiDates.transactionDate(detail.date()), "The transaction date must be interpretable");
    }

    @Test(groups = "ApiContract", description = "TXN-003: amount filtering returns all and only matching records, including an empty result")
    public void searchByAmount() {
        deposit();
        withdraw();
        int id = fixture.fundingAccount().id();
        var all = transactionList(transactions.list(id));
        assertSameRecords(transactionList(transactions.byAmount(id, CREDIT)),
                all.stream().filter(t -> t.amount().compareTo(CREDIT) == 0).toList());
        BigDecimal absent = new BigDecimal("9999999.87");
        Assert.assertTrue(all.stream().noneMatch(t -> t.amount().compareTo(absent) == 0), "The no-match amount must be absent");
        Assert.assertTrue(transactionList(transactions.byAmount(id, absent)).isEmpty());
    }

    @Test(groups = "ApiContract", description = "TXN-004: date filtering uses the server transaction date and excludes another date")
    public void searchOnServerDate() {
        Transaction created = deposit();
        int id = fixture.fundingAccount().id();
        LocalDate day = ApiDates.transactionDate(created.date());
        var all = transactionList(transactions.list(id));
        assertSameRecords(transactionList(transactions.onDate(id, ApiDates.day(created.date()))),
                all.stream().filter(t -> ApiDates.transactionDate(t.date()).equals(day)).toList());
        LocalDate absentDay = day.plusDays(7);
        assertSameRecords(transactionList(transactions.onDate(id, absentDay.format(DAY))),
                all.stream().filter(t -> ApiDates.transactionDate(t.date()).equals(absentDay)).toList());
    }

    @Test(groups = "ApiContract", description = "TXN-005: date range includes both endpoints and a reversed range returns no records")
    public void searchInclusiveDateRange() {
        Transaction created = deposit();
        int id = fixture.fundingAccount().id();
        LocalDate day = ApiDates.transactionDate(created.date());
        var all = transactionList(transactions.list(id));
        var sameDay = all.stream().filter(t -> ApiDates.transactionDate(t.date()).equals(day)).toList();
        // A same-day interval verifies the transaction falls on both inclusive boundaries.
        assertSameRecords(transactionList(transactions.betweenDates(id, day.format(DAY), day.format(DAY))), sameDay);
        assertSameRecords(transactionList(transactions.betweenDates(id, day.minusDays(1).format(DAY), day.plusDays(1).format(DAY))),
                all.stream().filter(t -> !ApiDates.transactionDate(t.date()).isBefore(day.minusDays(1))
                        && !ApiDates.transactionDate(t.date()).isAfter(day.plusDays(1))).toList());
        Assert.assertTrue(transactionList(transactions.betweenDates(id, day.plusDays(1).format(DAY), day.minusDays(1).format(DAY))).isEmpty(),
                "A reversed range must not fabricate matching records");
    }

    @Test(groups = "ApiContract", description = "TXN-006: month and direction jointly filter records and exclude an adjacent month")
    public void searchByMonthAndType() {
        Transaction created = deposit();
        withdraw();
        int id = fixture.fundingAccount().id();
        var all = transactionList(transactions.list(id));
        int month = ApiDates.transactionDate(created.date()).getMonthValue();
        for (String type : List.of("Credit", "Debit")) {
            assertSameRecords(transactionList(transactions.byMonthAndType(id, ApiDates.month(created.date()), type)),
                    all.stream().filter(t -> t.type().equals(type)
                            && ApiDates.transactionDate(t.date()).getMonthValue() == month).toList());
        }
        // The pinned implementation accepts English month abbreviations and ignores the year.
        LocalDate adjacentMonth = ApiDates.transactionDate(created.date()).plusMonths(1);
        String otherMonth = adjacentMonth.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH));
        assertSameRecords(transactionList(transactions.byMonthAndType(id, otherMonth, "Credit")),
                all.stream().filter(t -> t.type().equals("Credit")
                        && ApiDates.transactionDate(t.date()).getMonthValue() == adjacentMonth.getMonthValue()).toList());
    }

    @Test(groups = "ApiNegative", description = "TXN-007: nonexistent transaction does not return a ledger record")
    public void missingTransaction() {
        Assert.assertEquals(transactions.get(-1).statusCode(), expectedStatus("api.status.missingResource"));
    }

    private Transaction deposit() {
        int id = fixture.fundingAccount().id();
        var before = account(accounts.get(id));
        var ledgerBefore = transactionList(transactions.list(id));
        success(accounts.deposit(id, CREDIT));
        money(account(accounts.get(id)).balance(), before.balance().add(CREDIT));
        return singleTransaction(ledgerBefore, transactionList(transactions.list(id)), id, "Credit", CREDIT);
    }

    private void withdraw() {
        int id = fixture.fundingAccount().id();
        var before = account(accounts.get(id));
        var ledgerBefore = transactionList(transactions.list(id));
        success(accounts.withdraw(id, DEBIT));
        money(account(accounts.get(id)).balance(), before.balance().subtract(DEBIT));
        singleTransaction(ledgerBefore, transactionList(transactions.list(id)), id, "Debit", DEBIT);
    }

    private static void assertSameRecords(List<Transaction> actual, List<Transaction> expected) {
        Set<Integer> actualIds = actual.stream().map(Transaction::id).collect(Collectors.toSet());
        Assert.assertEquals(actualIds.size(), actual.size(), "Filtered results must not duplicate records");
        Assert.assertEquals(new HashSet<>(actual), new HashSet<>(expected), "Filter must return all and only expected records");
    }
}
