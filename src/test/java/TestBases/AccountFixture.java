package TestBases;

import PageObjects.HomePage;
import PageObjects.LogOut;
import PageObjects.RegistrationPage;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import utilities.RegistrationData;

public final class AccountFixture {

    private AccountFixture() {
    }

    public static RegistrationData register(WebDriver driver) {
        return register(driver, "123456789");
    }

    public static RegistrationData register(WebDriver driver, String ssn) {
        RegistrationData registrationData = RegistrationData.unique();
        new HomePage(driver).clickRegisterLink();
        RegistrationPage registrationPage = new RegistrationPage(driver);
        registrationPage.fill(registrationData);
        registrationPage.enterSSN(ssn);
        registrationPage.clickRegisterButton();

        Assert.assertEquals(registrationPage.getSuccessMessageText(), "Welcome " + registrationData.username(),
                "The test account must be registered successfully");
        return registrationData;
    }

    public static RegistrationData registerAndLogout(WebDriver driver) {
        RegistrationData registrationData = register(driver);
        new LogOut(driver).clickLogout();
        return registrationData;
    }
}
