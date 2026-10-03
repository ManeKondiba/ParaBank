package TestClases.api;

import static utilities.api.assertions.ApiAssertions.accountList;
import static utilities.api.assertions.ApiAssertions.customer;
import static utilities.api.assertions.ApiAssertions.success;

import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;
import TestBases.ApiBaseTest;
import TestBases.ApiFixture;
import utilities.api.models.Account;
import utilities.api.models.Customer;

@Test(groups = "ApiRegression")
public class CustomerApiTest extends ApiBaseTest {
    @Test(groups = {"ApiSmoke", "ApiContract"}, description = "AUTH-001/CUST-001: login and customer read identify the isolated customer")
    public void validLoginAndCustomerRead() {
        Customer loggedIn = customer(customers.login(fixture.username(), fixture.password()));
        Customer persisted = customer(customers.get(fixture.customer().id()));
        Assert.assertEquals(loggedIn.id(), fixture.customer().id());
        assertProfile(persisted, fixture.customer());
        assertProfile(loggedIn, persisted);
        var ownedAccounts = accountList(customers.accounts(persisted.id()));
        Assert.assertTrue(ownedAccounts.stream().anyMatch(a -> a.id() == fixture.fundingAccount().id()),
                "The customer's account list must include the provisioned funding account");
        for (Account account : ownedAccounts) {
            Assert.assertEquals(account.customerId(), persisted.id(), "Every returned account must belong to this customer");
        }
    }

    @Test(groups = "ApiNegative", description = "AUTH-002: wrong password is rejected and the valid credentials remain usable")
    public void invalidPassword() {
        Assert.assertEquals(customers.login(fixture.username(), fixture.password() + "-wrong").statusCode(),
                expectedStatus("api.status.invalidLogin"));
        Assert.assertEquals(customer(customers.login(fixture.username(), fixture.password())).id(), fixture.customer().id());
    }

    @Test(groups = "ApiNegative", description = "AUTH-003: unknown username is rejected")
    public void unknownUsername() {
        Assert.assertEquals(customers.login("missing-" + fixture.username(), fixture.password()).statusCode(),
                expectedStatus("api.status.invalidLogin"));
    }

    @Test(groups = "ApiNegative", description = "CUST-003: a nonexistent customer does not return customer data")
    public void missingCustomer() {
        Assert.assertEquals(customers.get(-1).statusCode(), expectedStatus("api.status.missingResource"));
    }

    @Test(groups = {"ApiNegative", "ApiContract"}, description = "CUST-005: account listings contain only accounts owned by each customer")
    public void accountListingsRemainScopedToTheirOwners() {
        ApiFixture other = ApiFixture.provision(apiConfig, requests, customers, accounts);
        var firstAccounts = accountList(customers.accounts(fixture.customer().id()));
        var otherAccounts = accountList(customers.accounts(other.customer().id()));

        Assert.assertTrue(firstAccounts.stream().allMatch(value -> value.customerId() == fixture.customer().id()));
        Assert.assertTrue(otherAccounts.stream().allMatch(value -> value.customerId() == other.customer().id()));
        Assert.assertTrue(firstAccounts.stream().noneMatch(first ->
                otherAccounts.stream().anyMatch(second -> second.id() == first.id())),
                "Customer account listings must not share account IDs");
    }

    @Test(groups = {"ApiContract", "CUST-002"}, description = "CUST-002/AUTH-004: updated profile and URL-encoded credentials persist")
    public void profileAndCredentialUpdate() {
        Customer before = customer(customers.get(fixture.customer().id()));
        var accountsBefore = accountList(customers.accounts(before.id()));
        String username = "api+" + fixture.customer().id();
        String password = "Pw+& " + fixture.customer().id();
        Map<String, String> update = new LinkedHashMap<>();
        update.put("firstName", "Zo\u00eb");
        update.put("lastName", "O'Neil");
        update.put("street", "42 API Lane & Annex");
        update.put("city", "Pune");
        update.put("state", "MH");
        update.put("zipCode", "411001");
        update.put("phoneNumber", "555-010-2020");
        update.put("ssn", before.ssn());
        update.put("username", username);
        update.put("password", password);

        success(customers.update(before.id(), update));

        Customer after = customer(customers.get(before.id()));
        Assert.assertEquals(after.id(), before.id());
        Assert.assertEquals(after.firstName(), update.get("firstName"));
        Assert.assertEquals(after.lastName(), update.get("lastName"));
        Assert.assertEquals(after.address().street(), update.get("street"));
        Assert.assertEquals(after.address().city(), update.get("city"));
        Assert.assertEquals(after.address().state(), update.get("state"));
        Assert.assertEquals(after.address().zipCode(), update.get("zipCode"));
        Assert.assertTrue(after.phoneNumber().equals(update.get("phoneNumber")), "The updated phone number must persist");
        Assert.assertTrue(after.ssn().equals(before.ssn()), "Updating contact information must preserve the SSN");
        Assert.assertEquals(customer(customers.login(username, password)).id(), before.id());
        Assert.assertEquals(customers.login(fixture.username(), fixture.password()).statusCode(),
                expectedStatus("api.status.invalidLogin"), "The previous credentials must stop authenticating");
        Assert.assertEquals(accountList(customers.accounts(before.id())), accountsBefore,
                "A profile update must not alter customer accounts or balances");
    }

    @Test(groups = "ApiNegative", description = "CUST-004: malformed customer ID fails without altering the isolated customer's profile or credentials")
    public void malformedCustomerIdPreservesProfile() {
        int customerId = fixture.customer().id();
        Customer before = customer(customers.get(customerId));
        var beforeAccounts = accountList(customers.accounts(customerId));

        var response = requests.newRequest().queryParam("firstName", "Changed")
                .post("/customers/update/not-an-integer");

        Assert.assertEquals(response.statusCode(), expectedStatus("api.status.invalidInput"));
        assertProfile(customer(customers.get(customerId)), before);
        assertProfile(customer(customers.login(fixture.username(), fixture.password())), before);
        Assert.assertEquals(accountList(customers.accounts(customerId)), beforeAccounts);
    }

    private static void assertProfile(Customer actual, Customer expected) {
        Assert.assertEquals(actual.id(), expected.id());
        Assert.assertTrue(actual.firstName().equals(expected.firstName()), "First name must match the fixture");
        Assert.assertTrue(actual.lastName().equals(expected.lastName()), "Last name must match the fixture");
        Assert.assertTrue(actual.address().equals(expected.address()), "Address must match the fixture");
        Assert.assertTrue(actual.phoneNumber().equals(expected.phoneNumber()), "Phone number must match the fixture");
        Assert.assertTrue(actual.ssn().equals(expected.ssn()), "SSN must match the fixture");
    }
}
