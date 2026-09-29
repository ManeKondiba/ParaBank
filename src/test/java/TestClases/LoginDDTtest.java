package TestClases;

import PageObjects.LoginPage;
import TestBases.AccountFixture;
import TestBases.BaseClass;
import org.testng.Assert;
import org.testng.annotations.Test;
import utilities.DataProviders;
import utilities.RegistrationData;

public class LoginDDTtest extends BaseClass {

    @Test(dataProvider = "LoginData", dataProviderClass = DataProviders.class,
            groups = {"Login", "Master", "Regression", "Datadriven"})
    public void loginDDTest(String username, String password, String expectedResult) {
        String loginUsername = username;
        String loginPassword = password;
        if (DataProviders.isFreshAccount(username, password, expectedResult)) {
            RegistrationData account = AccountFixture.registerAndLogout(getDriver());
            loginUsername = account.username();
            loginPassword = account.password();
        }

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(loginUsername, loginPassword);

        if ("Valid".equalsIgnoreCase(expectedResult)) {
            Assert.assertTrue(loginPage.isLoginSuccessDisplayed(),
                    "Valid credentials must open Accounts Overview");
        } else if ("Invalid".equalsIgnoreCase(expectedResult)) {
            String expectedError = loginUsername.isBlank() || loginPassword.isBlank()
                    ? "Please enter a username and password."
                    : "The username and password could not be verified.";
            Assert.assertEquals(loginPage.getLoginErrorText(), expectedError,
                    "Invalid login must show the expected validation error");
            Assert.assertTrue(loginPage.isLoginFormDisplayed(), "Invalid login must not authenticate the user");
        } else {
            Assert.fail("Unsupported login expectation; use Valid or Invalid");
        }
    }
}
