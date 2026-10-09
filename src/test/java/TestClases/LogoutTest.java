package TestClases;

import PageObjects.LogOut;
import PageObjects.LoginPage;
import TestBases.AccountFixture;
import TestBases.BaseClass;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import java.net.URI;
import utilities.FrameworkConfig;
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

    @Test(dataProvider = "protectedRoutes", groups = {"Logout", "Master", "Regression", "ProductionRules"})
    public void testLoggedOutUserCannotRevisitAccountsOverview(String route) {
        RegistrationData account = AccountFixture.registerAndLogout(getDriver());
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(account.username(), account.password());
        Assert.assertTrue(loginPage.isLoginSuccessDisplayed(), "Login must open Accounts Overview");
        String accountsOverviewUrl = URI.create(loginPage.getAccountsOverviewUrl()).resolve(route).toString();

        LogOut logoutPage = new LogOut(getDriver());
        logoutPage.clickLogout();
        logoutPage.revisitPage(accountsOverviewUrl);

        Assert.assertEquals(loginPage.getLoginErrorText(), "You must be logged in to use this feature.",
                "Revisiting Accounts Overview after logout must require authentication");
        Assert.assertTrue(logoutPage.isLoggedOut(),
                "A saved protected URL must not restore the previous authenticated session");
    }
    @DataProvider
    public Object[][] protectedRoutes() {
        return new Object[][] {{"overview.htm"}, {"activity.htm?id=1"}, {"transaction.htm?id=1"},
            {"openaccount.htm"}, {"transfer.htm"}, {"billpay.htm"}, {"findtrans.htm"},
            {"updateprofile.htm"}, {"requestloan.htm"}};
    }

    @Test(dataProvider = "protectedRoutes", groups = {"Logout", "Master", "Regression", "ProductionRules"},
            description = "VAL-001: anonymous access to every protected route requires login")
    public void testAnonymousProtectedRouteRequiresLogin(String route) {
        getDriver().navigate().to(URI.create(FrameworkConfig.load().get("appUrl")).resolve(route).toString());
        LoginPage login = new LoginPage(getDriver());
        Assert.assertTrue(login.isLoginFormDisplayed());
        Assert.assertEquals(login.getLoginErrorText(), "You must be logged in to use this feature.", route);
        Assert.assertTrue(new LogOut(getDriver()).isLoggedOut());
    }

}
