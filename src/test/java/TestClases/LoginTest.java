package TestClases;

import PageObjects.LoginPage;
import PageObjects.AccountsOverviewPage;
import java.util.List;
import PageObjects.LogOut;
import TestBases.AccountFixture;
import TestBases.BaseClass;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import utilities.RegistrationData;

public class LoginTest extends BaseClass {

    @Test(groups = {"Login", "Master", "Sanity", "Regression"})
    public void testValidLogin() {
        RegistrationData account = AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        List<String> ownedIds = overview.getAccountIds();
        new LogOut(getDriver()).clickLogout();
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(account.username(), account.password());

        Assert.assertTrue(loginPage.isLoginSuccessDisplayed(),
                "A registered user must reach Accounts Overview");
        Assert.assertEquals(overview.getAccountIds(), ownedIds, "Login must restore the same customer's owned accounts");
    }

    @Test(groups = {"Login", "Master", "Regression"})
    public void testWrongPassword() {
        RegistrationData account = AccountFixture.registerAndLogout(getDriver());
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(account.username(), "incorrect-password");

        Assert.assertEquals(loginPage.getLoginErrorText(), "The username and password could not be verified.");
        Assert.assertTrue(loginPage.isLoginFormDisplayed(),
                "Invalid credentials must leave the user logged out");
    }

    @Test(groups = {"Login", "Master", "Regression"})
    public void testPasswordIsMasked() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.enterPassword("Masking-check-123!");

        Assert.assertTrue(loginPage.isPasswordMasked(),
                "The login password input must mask typed characters");
    }

    @Test(groups = {"Login", "Master", "Regression"})
    public void testLoginWithEnterKey() {
        RegistrationData account = AccountFixture.registerAndLogout(getDriver());
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.enterUsername(account.username());
        loginPage.enterPassword(account.password());
        loginPage.submitWithEnter();

        Assert.assertTrue(loginPage.isLoginSuccessDisplayed(),
                "Pressing Enter in the password field must submit valid credentials");
    }

    @Test(groups = {"Login", "Master", "Regression"})
    public void testLoginAfterCorrectingWrongPassword() {
        RegistrationData account = AccountFixture.registerAndLogout(getDriver());
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(account.username(), "incorrect-password");
        Assert.assertEquals(loginPage.getLoginErrorText(), "The username and password could not be verified.");
        Assert.assertTrue(loginPage.isLoginFormDisplayed(),
                "A failed login must allow another attempt");

        loginPage.login(account.username(), account.password());

        Assert.assertTrue(loginPage.isLoginSuccessDisplayed(),
                "Corrected credentials must open Accounts Overview in the same browser session");
    }

    @DataProvider(name = "missingCredentials")
    public Object[][] missingCredentials() {
        return new Object[][] {
                {"username", false, true},
                {"password", true, false},
                {"username and password", false, false}
        };
    }

    @Test(dataProvider = "missingCredentials", groups = {"Login", "Master", "Regression"})
    public void testLoginAfterProvidingMissingCredentials(String missingFields, boolean includeUsername,
            boolean includePassword) {
        RegistrationData account = AccountFixture.registerAndLogout(getDriver());
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(includeUsername ? account.username() : "", includePassword ? account.password() : "");

        Assert.assertEquals(loginPage.getLoginErrorText(), "Please enter a username and password.",
                "Missing " + missingFields + " must show the required-credentials error");
        Assert.assertTrue(loginPage.isLoginFormDisplayed(),
                "Missing credentials must leave the login form available for correction");

        loginPage.login(account.username(), account.password());

        Assert.assertTrue(loginPage.isLoginSuccessDisplayed(),
                "Providing the missing " + missingFields + " must allow login in the same browser session");
    }

    @Test(groups = {"Login", "Master", "Regression"})
    public void testLoginAgainAfterLogout() {
        RegistrationData account = AccountFixture.registerAndLogout(getDriver());
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(account.username(), account.password());
        Assert.assertTrue(loginPage.isLoginSuccessDisplayed(), "The first login must succeed");

        LogOut logoutPage = new LogOut(getDriver());
        logoutPage.clickLogout();
        Assert.assertTrue(logoutPage.isLoggedOut(), "Logout must end the authenticated session");

        loginPage.login(account.username(), account.password());
        Assert.assertTrue(loginPage.isLoginSuccessDisplayed(),
                "The same account must be able to log in again after logout");
    }
}
