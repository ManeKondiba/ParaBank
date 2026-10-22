package testclasses;

import pageobjects.LogoutPage;
import pageobjects.LoginPage;
import base.AccountFixture;
import base.BaseClass;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import java.net.URI;
import utilities.AppRoute;
import utilities.FrameworkConfig;
import utilities.RegistrationData;
import utilities.KnownDefect;

public class LogoutTest extends BaseClass {

    @Test(groups = {"Logout", "Master", "Sanity", "Regression"})
    public void testLogout() {
        AccountFixture.register(getDriver());
        LogoutPage logoutPage = new LogoutPage(getDriver());
        logoutPage.clickLogout();

        Assert.assertTrue(logoutPage.isLoggedOut(),
                "Logout must restore the login form and remove account navigation");
    }

    @Test(groups = {"Logout", "Master", "Regression"})
    public void testLogoutRemainsEffectiveAfterRefresh() {
        AccountFixture.register(getDriver());
        LogoutPage logoutPage = new LogoutPage(getDriver());
        logoutPage.clickLogout();
        logoutPage.refreshPage();

        Assert.assertTrue(logoutPage.isLoggedOut(),
                "Refreshing after logout must keep the user logged out");
    }

    @KnownDefect(id = "PB-UI-001", description = "Revisiting protected routes after logout yields generic 500 error instead of login required")
    @Test(dataProvider = "protectedRoutes", groups = {"Logout", "Master", "Regression", "ProductionRules"})
    public void testLoggedOutUserCannotRevisitAccountsOverview(String route) {
        RegistrationData account = AccountFixture.registerAndLogout(getDriver());
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(account.username(), account.password());
        Assert.assertTrue(loginPage.isLoginSuccessDisplayed(), "Login must open Accounts Overview");
        String accountsOverviewUrl = URI.create(loginPage.getAccountsOverviewUrl()).resolve(route).toString();

        LogoutPage logoutPage = new LogoutPage(getDriver());
        logoutPage.clickLogout();
        logoutPage.revisitPage(accountsOverviewUrl);

        Assert.assertEquals(loginPage.getLoginErrorText(), "You must be logged in to use this feature.",
                "Revisiting Accounts Overview after logout must require authentication");
        Assert.assertTrue(logoutPage.isLoggedOut(),
                "A saved protected URL must not restore the previous authenticated session");
    }
    @DataProvider
    public Object[][] protectedRoutes() {
        return new Object[][] {
                {AppRoute.OVERVIEW},
                {AppRoute.activity("1")},
                {AppRoute.transaction("1")},
                {AppRoute.OPEN_ACCOUNT},
                {AppRoute.TRANSFER},
                {AppRoute.BILL_PAY},
                {AppRoute.FIND_TRANSACTIONS},
                {AppRoute.UPDATE_PROFILE},
                {AppRoute.REQUEST_LOAN}
        };
    }

    @KnownDefect(id = "PB-UI-002", description = "Anonymous access to protected routes throws 500 error instead of requiring login")
    @Test(dataProvider = "protectedRoutes", groups = {"Logout", "Master", "Regression", "ProductionRules"},
            description = "VAL-001: anonymous access to every protected route requires login")
    public void testAnonymousProtectedRouteRequiresLogin(String route) {
        getDriver().navigate().to(URI.create(FrameworkConfig.load().get("appUrl")).resolve(route).toString());
        LoginPage login = new LoginPage(getDriver());
        Assert.assertTrue(login.isLoginFormDisplayed());
        Assert.assertEquals(login.getLoginErrorText(), "You must be logged in to use this feature.", route);
        Assert.assertTrue(new LogoutPage(getDriver()).isLoggedOut());
    }

}
