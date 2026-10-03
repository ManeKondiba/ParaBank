package TestBases;

import static utilities.api.assertions.ApiAssertions.account;
import static utilities.api.assertions.ApiAssertions.accountList;
import static utilities.api.assertions.ApiAssertions.money;
import static utilities.api.assertions.ApiAssertions.singleTransaction;
import static utilities.api.assertions.ApiAssertions.transactionList;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.Assert;
import utilities.FrameworkConfig;
import utilities.RegistrationData;
import utilities.api.ApiConfig;
import utilities.api.ApiRequestFactory;
import utilities.api.clients.AccountClient;
import utilities.api.clients.CustomerClient;
import utilities.api.clients.TransactionClient;
import utilities.api.models.Account;
import utilities.api.models.Address;
import utilities.api.models.Customer;
import utilities.api.models.Payee;

/** One isolated customer per invocation, with extra fixtures provisioned by ownership scenarios. */
public final class ApiFixture {
    private static final Map<Path, Integer> LEASES = new HashMap<>();
    private static final java.util.Set<Integer> CUSTOMERS = new java.util.HashSet<>();
    private final String username;
    private final String password;
    private final Customer customer;
    private final Account fundingAccount;
    private final AccountClient accounts;
    private final TransactionClient transactions;

    private ApiFixture(String username, String password, Customer customer, Account fundingAccount,
            AccountClient accounts, TransactionClient transactions) {
        this.username = username;
        this.password = password;
        this.customer = customer;
        this.fundingAccount = fundingAccount;
        this.accounts = accounts;
        this.transactions = transactions;
    }

    public static synchronized void resetLeases() {
        LEASES.clear();
        CUSTOMERS.clear();
    }

    public static ApiFixture provision(ApiConfig config, ApiRequestFactory requests, CustomerClient customers,
            AccountClient accounts) {
        config.requireControlledEnvironment();
        RegistrationData credentials = config.fixtureMode().equals("file")
                ? lease(Path.of(config.fixtureFile())) : register(config, requests);
        Customer customer = utilities.api.assertions.ApiAssertions.customer(
                customers.login(credentials.username(), credentials.password()));
        synchronized (ApiFixture.class) {
            Assert.assertTrue(CUSTOMERS.add(customer.id()), "Fixture customer was already used by this run");
        }
        List<Account> owned = accountList(customers.accounts(customer.id()));
        Account funding = owned.stream().filter(value -> value.type().equals("CHECKING"))
                .findFirst().orElseThrow(() -> new IllegalStateException("Fixture needs a seeded checking account"));
        Assert.assertEquals(funding.customerId(), customer.id(), "Fixture funding account owner");
        Assert.assertTrue(funding.balance().signum() > 0, "Fixture requires a funded checking account");
        return new ApiFixture(credentials.username(), credentials.password(), customer, funding, accounts,
                new TransactionClient(requests));
    }

    private static RegistrationData register(ApiConfig config, ApiRequestFactory requests) {
        RegistrationData credentials = RegistrationData.unique();
        CookieFilter session = new CookieFilter();
        Response form = requests.newRequest().baseUri(config.webBaseUrl()).accept(ContentType.HTML)
                .filter(session).get("/register.htm");
        Assert.assertEquals(form.statusCode(), 200, "Cannot open controlled deployment's registration form");
        String ssn = String.format("%09d", Integer.toUnsignedLong(UUID.randomUUID().hashCode()) % 1_000_000_000);
        Map<String, String> fields = new HashMap<>();
        fields.put("customer.firstName", "Api");
        fields.put("customer.lastName", "Automation");
        fields.put("customer.address.street", "1 Test Street");
        fields.put("customer.address.city", "Test City");
        fields.put("customer.address.state", "CA");
        fields.put("customer.address.zipCode", "90210");
        fields.put("customer.phoneNumber", "2025550100");
        fields.put("customer.ssn", ssn);
        fields.put("customer.username", credentials.username());
        fields.put("customer.password", credentials.password());
        fields.put("repeatedPassword", credentials.password());
        Response registered = requests.newRequest().baseUri(config.webBaseUrl()).accept(ContentType.HTML)
                .contentType(ContentType.URLENC).filter(session).formParams(fields).post("/register.htm");
        Assert.assertEquals(registered.statusCode(), 200, "Web fixture registration HTTP status");
        Assert.assertTrue(registered.asString().contains("Your account was created successfully"),
                "Web fixture registration failed; use a controlled deployment without CAPTCHA or a seeded fixture file");
        return credentials;
    }

    private static synchronized RegistrationData lease(Path supplied) {
        Path path = supplied.toAbsolutePath().normalize();
        try {
            JsonNode entries = new ObjectMapper().readTree(Files.readString(path)).path("customers");
            if (!entries.isArray() || entries.isEmpty()) {
                throw new IllegalArgumentException("Fixture file must contain a nonempty customers array");
            }
            int index = LEASES.getOrDefault(path, 0);
            if (index >= entries.size()) {
                throw new IllegalStateException("Fixture pool exhausted; supply one pristine customer per test invocation");
            }
            // Reject duplicate usernames before any customer can be leased twice in a run.
            java.util.Set<String> users = new java.util.HashSet<>();
            for (JsonNode entry : entries) {
                if (!entry.path("username").isTextual() || entry.path("username").asText().isEmpty()
                        || !entry.path("password").isTextual() || entry.path("password").asText().isEmpty()
                        || !users.add(entry.path("username").asText())) {
                    throw new IllegalArgumentException("Fixture customers need unique usernames and nonempty text credentials");
                }
            }
            JsonNode entry = entries.get(index);
            LEASES.put(path, index + 1);
            return new RegistrationData(entry.path("username").asText(), entry.path("password").asText());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot read API fixture JSON file");
        }
    }

    public Customer customer() { return customer; }
    public String username() { return username; }
    public String password() { return password; }
    public Account fundingAccount() { return fundingAccount; }

    public Account createAccount(int type) {
        Account before = account(accounts.get(fundingAccount.id()));
        var ledgerBefore = transactionList(transactions.list(before.id()));
        Account created = account(accounts.create(customer.id(), type, fundingAccount.id()));
        Account persisted = account(accounts.get(created.id()));
        Assert.assertEquals(persisted.customerId(), customer.id(), "Created account owner");
        BigDecimal opening = new BigDecimal(FrameworkConfig.load().get("api.account.minimumBalance"));
        money(persisted.balance(), opening);
        money(account(accounts.get(fundingAccount.id())).balance(), before.balance().subtract(opening));
        singleTransaction(ledgerBefore, transactionList(transactions.list(before.id())), before.id(), "Debit", opening);
        singleTransaction(List.of(), transactionList(transactions.list(persisted.id())), persisted.id(), "Credit", opening);
        return persisted;
    }

    public Payee payee() {
        return new Payee("API Test Payee", new Address("2 Test Street", "Test City", "CA", "90210"),
                "2025550101", 123456);
    }

    @Override
    public String toString() {
        return "ApiFixture[customerId=" + customer.id() + ", credentials=[redacted]]";
    }
}
