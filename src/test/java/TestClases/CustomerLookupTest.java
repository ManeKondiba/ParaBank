package TestClases;

import java.util.UUID;
import PageObjects.CustomerLookupPage;
import PageObjects.LogOut;
import PageObjects.LoginPage;
import TestBases.AccountFixture;
import TestBases.BaseClass;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utilities.RegistrationData;

public class CustomerLookupTest extends BaseClass {

    @DataProvider(name = "requiredLookupFields")
    public String[][] requiredLookupFields() {
        return new String[][] {
            {"firstName", "First name is required."},
            {"lastName", "Last name is required."},
            {"address.street", "Address is required."},
            {"address.city", "City is required."},
            {"address.state", "State is required."},
            {"address.zipCode", "Zip Code is required."},
            {"ssn", "Social Security Number is required."}
        };
    }

    @Test(groups = {"CustomerLookup", "Banking", "Master", "Regression"})
    public void testAllRequiredLookupFields() {
        CustomerLookupPage lookup = new CustomerLookupPage(getDriver());
        lookup.open();
        lookup.submit();

        for (String[] field : requiredLookupFields()) {
            Assert.assertEquals(lookup.getFieldError(field[0]), field[1], field[0]);
        }
    }

    @Test(dataProvider = "requiredLookupFields",
            groups = {"CustomerLookup", "Banking", "Master", "Regression"})
    public void testEachRequiredLookupField(String fieldId, String expectedMessage) {
        CustomerLookupPage lookup = new CustomerLookupPage(getDriver());
        lookup.open();
        fillUnregisteredCustomer(lookup);
        lookup.clearField(fieldId);
        lookup.submit();

        Assert.assertEquals(lookup.getFieldError(fieldId), expectedMessage,
                "Lookup must reject the missing required field: " + fieldId);
    }

    @Test(groups = {"CustomerLookup", "Banking", "Master", "Regression", "BankingCompatibility"})
    public void testUnknownCustomerIsRejected() {
        CustomerLookupPage lookup = new CustomerLookupPage(getDriver());
        lookup.open();
        fillUnregisteredCustomer(lookup);
        lookup.submit();

        Assert.assertEquals(lookup.getLookupError(), "The customer information provided could not be found.");
        Assert.assertTrue(new LoginPage(getDriver()).isLoginFormDisplayed(),
                "An unsuccessful lookup must leave the visitor signed out");
    }

    @Test(groups = {"CustomerLookup", "Banking", "Master", "Regression"})
    public void testCustomerCanRecoverOwnLoginInformation() {
        String ssn = uniqueSyntheticSsn();
        RegistrationData customer = AccountFixture.register(getDriver(), ssn);
        new LogOut(getDriver()).clickLogout();
        CustomerLookupPage lookup = new CustomerLookupPage(getDriver());
        lookup.open();
        lookup.fill("Automation", "Tester", "123 Test Street", "Springfield", "IL", "62701", ssn);
        boolean credentialsMatch;
        Throwable lookupFailure = null;
        try {
            lookup.submit();
            credentialsMatch = lookup.recoveredCredentialsMatch(customer);
        } catch (RuntimeException | Error failure) {
            lookupFailure = failure;
            throw failure;
        } finally {
            // Leave the credential result before assertions and failure screenshots are produced.
            try {
                lookup.openAccountsOverview();
            } catch (RuntimeException | Error cleanupFailure) {
                if (lookupFailure == null) {
                    throw cleanupFailure;
                }
                lookupFailure.addSuppressed(cleanupFailure);
            }
        }

        Assert.assertTrue(credentialsMatch, "Lookup must recover only this test customer's credentials");
        Assert.assertTrue(new LoginPage(getDriver()).isLoginSuccessDisplayed(),
                "Successful customer lookup must create a signed-in customer session");
    }

    private void fillUnregisteredCustomer(CustomerLookupPage lookup) {
        // ParaBank looks up customers by SSN, so never use the shared registration SSN here.
        lookup.fill("Missing", "Customer", "999 Unknown Street", "Springfield", "IL", "62701",
                uniqueSyntheticSsn());
    }

    private String uniqueSyntheticSsn() {
        // The demo validates presence only and stores at most 15 characters.
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
    }
    @DataProvider
    public Object[][] mismatchedIdentityFields() {
        return new Object[][] {{0}, {1}, {2}, {3}, {4}, {5}};
    }

    @Test(dataProvider = "mismatchedIdentityFields", groups = {"CustomerLookup", "Banking", "Master", "Regression", "ProductionRules"},
            description = "VAL-011: a valid SSN alone cannot recover credentials with mismatched identity")
    public void testMismatchedIdentityCannotRecoverCredentials(int field) {
        String ssn = uniqueSyntheticSsn();
        AccountFixture.register(getDriver(), ssn);
        new LogOut(getDriver()).clickLogout();
        String[] identity = {"Automation", "Tester", "123 Test Street", "Springfield", "IL", "62701"};
        identity[field] = "Mismatched";
        CustomerLookupPage lookup = new CustomerLookupPage(getDriver());
        lookup.open();
        lookup.fill(identity[0], identity[1], identity[2], identity[3], identity[4], identity[5], ssn);
        lookup.submit();
        boolean revealed = lookup.isRecoveredCredentialsDisplayed();
        // Clear a sensitive recovery result before any assertion can trigger a screenshot.
        if (revealed) { lookup.openAccountsOverview(); }
        Assert.assertFalse(revealed, "Recovery must reject mismatched identity field " + field);
        Assert.assertEquals(lookup.getLookupError(), "The customer information provided could not be found.");
        Assert.assertTrue(new LoginPage(getDriver()).isLoginFormDisplayed());
    }

}
