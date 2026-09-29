package TestClases;

import java.util.Map;
import PageObjects.LogOut;
import PageObjects.LoginPage;
import PageObjects.UpdateContactInfoPage;
import TestBases.AccountFixture;
import TestBases.BaseClass;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utilities.RegistrationData;

public class UpdateContactInfoTest extends BaseClass {
    private static final Map<String, String> REGISTERED_CONTACT = Map.of(
            "customer.firstName", "Automation",
            "customer.lastName", "Tester",
            "customer.address.street", "123 Test Street",
            "customer.address.city", "Springfield",
            "customer.address.state", "IL",
            "customer.address.zipCode", "62701",
            "customer.phoneNumber", "5551234567");
    private static final Map<String, String> UPDATED_CONTACT = Map.of(
            "customer.firstName", "Updated",
            "customer.lastName", "Customer",
            "customer.address.street", "456 New Street",
            "customer.address.city", "Madison",
            "customer.address.state", "WI",
            "customer.address.zipCode", "53703",
            "customer.phoneNumber", "5559876543");

    @DataProvider(name = "requiredContactFields")
    public String[][] requiredContactFields() {
        return new String[][] {
            {"customer.firstName", "First name is required."},
            {"customer.lastName", "Last name is required."},
            {"customer.address.street", "Address is required."},
            {"customer.address.city", "City is required."},
            {"customer.address.state", "State is required."},
            {"customer.address.zipCode", "Zip Code is required."}
        };
    }

    @Test(groups = {"UpdateContactInfo", "Banking", "Master", "Regression"})
    public void testProfilePrefillsRegisteredContactDetails() {
        AccountFixture.register(getDriver());
        UpdateContactInfoPage profile = new UpdateContactInfoPage(getDriver());
        profile.open();

        assertContactDetails(profile, REGISTERED_CONTACT);
    }

    @Test(groups = {"UpdateContactInfo", "Banking", "Master", "Regression"})
    public void testAllRequiredContactFieldsAndUnchangedSavedProfile() {
        AccountFixture.register(getDriver());
        UpdateContactInfoPage profile = new UpdateContactInfoPage(getDriver());
        profile.open();
        for (String[] field : requiredContactFields()) {
            profile.clearField(field[0]);
        }
        profile.submit();

        for (String[] field : requiredContactFields()) {
            Assert.assertEquals(profile.getFieldError(field[0]), field[1], field[0]);
        }
        profile.open();
        assertContactDetails(profile, REGISTERED_CONTACT);
    }

    @Test(dataProvider = "requiredContactFields",
            groups = {"UpdateContactInfo", "Banking", "Master", "Regression"})
    public void testEachRequiredContactField(String fieldId, String expectedMessage) {
        AccountFixture.register(getDriver());
        UpdateContactInfoPage profile = new UpdateContactInfoPage(getDriver());
        profile.open();
        profile.clearField(fieldId);
        profile.submit();

        Assert.assertEquals(profile.getFieldError(fieldId), expectedMessage,
                "An empty required field must prevent the profile update: " + fieldId);
    }

    @Test(groups = {"UpdateContactInfo", "Banking", "Master", "Regression"})
    public void testProfileUpdateAfterCorrectingRequiredField() {
        AccountFixture.register(getDriver());
        UpdateContactInfoPage profile = new UpdateContactInfoPage(getDriver());
        profile.open();
        profile.clearField("customer.firstName");
        profile.submit();
        Assert.assertEquals(profile.getFieldError("customer.firstName"), "First name is required.");

        profile.fill(UPDATED_CONTACT);
        profile.submit();
        Assert.assertEquals(profile.getConfirmationHeading(), "Profile Updated");
        profile.open();
        assertContactDetails(profile, UPDATED_CONTACT);
    }

    @Test(groups = {"UpdateContactInfo", "Banking", "Master", "Regression"})
    public void testUpdatedProfilePersistsAfterNewLogin() {
        RegistrationData customer = AccountFixture.register(getDriver());
        UpdateContactInfoPage profile = new UpdateContactInfoPage(getDriver());
        profile.open();
        profile.fill(UPDATED_CONTACT);
        profile.submit();
        Assert.assertEquals(profile.getConfirmationHeading(), "Profile Updated");

        new LogOut(getDriver()).clickLogout();
        LoginPage login = new LoginPage(getDriver());
        login.login(customer.username(), customer.password());
        Assert.assertTrue(login.isLoginSuccessDisplayed(),
                "The original credentials must remain valid after changing the contact information");
        profile.open();
        assertContactDetails(profile, UPDATED_CONTACT);
    }

    @Test(groups = {"UpdateContactInfo", "Banking", "Master", "Regression"})
    public void testOptionalPhoneNumberCanBeRemoved() {
        AccountFixture.register(getDriver());
        UpdateContactInfoPage profile = new UpdateContactInfoPage(getDriver());
        profile.open();
        profile.clearField("customer.phoneNumber");
        profile.submit();

        Assert.assertEquals(profile.getConfirmationHeading(), "Profile Updated");
        profile.open();
        Assert.assertEquals(profile.getFieldValue("customer.phoneNumber"), "",
                "Clearing the optional phone number must persist");
        for (String[] field : requiredContactFields()) {
            Assert.assertEquals(profile.getFieldValue(field[0]), REGISTERED_CONTACT.get(field[0]),
                    "Clearing the phone number must preserve the other contact fields");
        }
    }

    private void assertContactDetails(UpdateContactInfoPage profile, Map<String, String> expectedContact) {
        expectedContact.forEach((fieldId, expectedValue) ->
                Assert.assertEquals(profile.getFieldValue(fieldId), expectedValue, fieldId));
    }
}
