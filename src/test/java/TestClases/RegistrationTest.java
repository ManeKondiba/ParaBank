package TestClases;

import PageObjects.HomePage;
import PageObjects.RegistrationPage;
import TestBases.AccountFixture;
import TestBases.BaseClass;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utilities.RegistrationData;

public class RegistrationTest extends BaseClass {

    @DataProvider(name = "requiredRegistrationFields")
    public String[][] requiredRegistrationFields() {
        return new String[][] {
            {"customer.firstName", "First name is required."},
            {"customer.lastName", "Last name is required."},
            {"customer.address.street", "Address is required."},
            {"customer.address.city", "City is required."},
            {"customer.address.state", "State is required."},
            {"customer.address.zipCode", "Zip Code is required."},
            {"customer.ssn", "Social Security Number is required."},
            {"customer.username", "Username is required."},
            {"customer.password", "Password is required."},
            {"repeatedPassword", "Password confirmation is required."}
        };
    }

    @Test(groups = {"Registration", "Master", "Sanity", "Regression"})
    public void testRegistration() {
        AccountFixture.register(getDriver());
    }

    @Test(groups = {"Registration", "Master", "Regression"})
    public void testRequiredFields() {
        new HomePage(getDriver()).clickRegisterLink();
        RegistrationPage registrationPage = new RegistrationPage(getDriver());
        registrationPage.clickRegisterButton();

        for (String[] expectedError : requiredRegistrationFields()) {
            String fieldId = expectedError[0];
            String expectedMessage = expectedError[1];
            Assert.assertEquals(registrationPage.getFieldError(fieldId), expectedMessage, fieldId);
        }
    }

    @Test(dataProvider = "requiredRegistrationFields", groups = {"Registration", "Master", "Regression"})
    public void testEachRequiredField(String fieldId, String expectedMessage) {
        new HomePage(getDriver()).clickRegisterLink();
        RegistrationPage registrationPage = new RegistrationPage(getDriver());
        registrationPage.fill(RegistrationData.unique());
        registrationPage.clearField(fieldId);
        registrationPage.clickRegisterButton();

        Assert.assertEquals(registrationPage.getFieldError(fieldId), expectedMessage,
                "Registration must reject the missing required field: " + fieldId);
    }

    @Test(groups = {"Registration", "Master", "Regression"})
    public void testRegistrationAfterCompletingRequiredFields() {
        RegistrationData account = RegistrationData.unique();
        new HomePage(getDriver()).clickRegisterLink();
        RegistrationPage registrationPage = new RegistrationPage(getDriver());
        registrationPage.clickRegisterButton();
        Assert.assertEquals(registrationPage.getFieldError("customer.firstName"), "First name is required.");

        registrationPage.fill(account);
        registrationPage.clickRegisterButton();

        Assert.assertEquals(registrationPage.getSuccessMessageText(), "Welcome " + account.username(),
                "Completing the required fields after validation errors must allow registration");
    }

    @Test(groups = {"Registration", "Master", "Regression"})
    public void testPasswordFieldsAreMasked() {
        new HomePage(getDriver()).clickRegisterLink();
        RegistrationPage registrationPage = new RegistrationPage(getDriver());
        RegistrationData account = RegistrationData.unique();
        registrationPage.enterPassword(account.password());
        registrationPage.enterConfirmPassword(account.password());

        Assert.assertTrue(registrationPage.isPasswordMasked(), "Password must use a masked input");
        Assert.assertTrue(registrationPage.isConfirmPasswordMasked(),
                "Password confirmation must use a masked input");
    }

    @Test(groups = {"Registration", "Master", "Regression"})
    public void testPasswordMismatch() {
        new HomePage(getDriver()).clickRegisterLink();
        RegistrationPage registrationPage = new RegistrationPage(getDriver());
        registrationPage.fill(RegistrationData.unique());
        registrationPage.enterConfirmPassword("different-password");
        registrationPage.clickRegisterButton();

        Assert.assertEquals(registrationPage.getFieldError("repeatedPassword"), "Passwords did not match.");
    }

    @Test(groups = {"Registration", "Master", "Regression"})
    public void testDuplicateUsername() {
        RegistrationData existingAccount = AccountFixture.registerAndLogout(getDriver());
        new HomePage(getDriver()).clickRegisterLink();
        RegistrationPage registrationPage = new RegistrationPage(getDriver());
        registrationPage.fill(existingAccount);
        registrationPage.clickRegisterButton();

        Assert.assertEquals(registrationPage.getFieldError("customer.username"),
                "This username already exists.");
    }

    @Test(groups = {"Registration", "Master", "Regression"})
    public void testRegistrationWithoutPhoneNumber() {
        RegistrationData account = RegistrationData.unique();
        new HomePage(getDriver()).clickRegisterLink();
        RegistrationPage registrationPage = new RegistrationPage(getDriver());
        registrationPage.fill(account);
        registrationPage.enterPhone("");
        registrationPage.clickRegisterButton();

        Assert.assertEquals(registrationPage.getSuccessMessageText(), "Welcome " + account.username(),
                "Registration must succeed without an optional phone number");
    }

    @Test(groups = {"Registration", "Master", "Regression"})
    public void testRegistrationAfterCorrectingPasswordMismatch() {
        RegistrationData account = RegistrationData.unique();
        new HomePage(getDriver()).clickRegisterLink();
        RegistrationPage registrationPage = new RegistrationPage(getDriver());
        registrationPage.fill(account);
        registrationPage.enterConfirmPassword("different-password");
        registrationPage.clickRegisterButton();
        Assert.assertEquals(registrationPage.getFieldError("repeatedPassword"), "Passwords did not match.");

        // Password inputs may be cleared by the server when it renders validation errors.
        registrationPage.enterPassword(account.password());
        registrationPage.enterConfirmPassword(account.password());
        registrationPage.clickRegisterButton();

        Assert.assertEquals(registrationPage.getSuccessMessageText(), "Welcome " + account.username(),
                "Correcting the password confirmation must allow registration");
    }
}
