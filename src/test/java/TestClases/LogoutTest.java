package TestClases;

import PageObjects.LogOut;
import PageObjects.LoginPage;
import TestBases.AccountFixture;
import TestBases.BaseClass;
import org.testng.Assert;
import org.testng.annotations.Test;
import utilities.RegistrationData;

public class LogoutTest extends BaseClass {

    @Test(groups = {"Logout", "Master", "Sanity", "Regression"})
    public void testLogout() {
        AccountFixture.register(getDriver());
        LogOut logoutPage = new LogOut(getDriver());
        logoutPage.clickLogout();

        Assert.assertTrue(logoutPage.isLoggedOut(),
                "Logout must restore the login form and remove account navigation");
    }

    @Test(groups = {"Logout", "Master", "Regression"})
    public void testLogoutRemainsEffectiveAfterRefresh() {
        AccountFixture.register(getDriver());
        LogOut logoutPage = new LogOut(getDriver());
        logoutPage.clickLogout();
        logoutPage.refreshPage();

        Assert.assertTrue(logoutPage.isLoggedOut(),
                "Refreshing after logout must keep the user logged out");
    }

    @Test(groups = {"Logout", "Master", "Regression"})
    public void testLoggedOutUserCannotRevisitAccountsOverview() {
        RegistrationData account = AccountFixture.registerAndLogout(getDriver());
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(account.username(), account.password());
        Assert.assertTrue(loginPage.isLoginSuccessDisplayed(), "Login must open Accounts Overview");
        String accountsOverviewUrl = loginPage.getAccountsOverviewUrl();

        LogOut logoutPage = new LogOut(getDriver());
        logoutPage.clickLogout();
        logoutPage.revisitPage(accountsOverviewUrl);

        Assert.assertEquals(loginPage.getLoginErrorText(), "You must be logged in to use this feature.",
                "Revisiting Accounts Overview after logout must require authentication");
        Assert.assertTrue(logoutPage.isLoggedOut(),
                "A saved protected URL must not restore the previous authenticated session");
    }
}
