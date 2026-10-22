package base;

import pageobjects.HomePage;
import pageobjects.LogoutPage;
import pageobjects.RegistrationPage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import utilities.RegistrationData;

public final class AccountFixture {
    private static final Logger LOG = LogManager.getLogger(AccountFixture.class);

    private AccountFixture() {
    }

    public static RegistrationData register(WebDriver driver) {
        return register(driver, "123456789");
    }

    public static RegistrationData register(WebDriver driver, String ssn) {
        RegistrationData registrationData = RegistrationData.unique();
        LOG.debug("Provisioning new test account: username={}", registrationData.username());
        new HomePage(driver).clickRegisterLink();
        RegistrationPage registrationPage = new RegistrationPage(driver);
        registrationPage.fill(registrationData);
        registrationPage.enterSSN(ssn);
        registrationPage.clickRegisterButton();

        Assert.assertEquals(registrationPage.getSuccessMessageText(), "Welcome " + registrationData.username(),
                "The test account must be registered successfully");
        LOG.debug("Successfully provisioned test account: {}", registrationData.username());
        return registrationData;
    }

    public static RegistrationData registerAndLogout(WebDriver driver) {
        RegistrationData registrationData = register(driver);
        LOG.debug("Logging out freshly registered account: {}", registrationData.username());
        new LogoutPage(driver).clickLogout();
        return registrationData;
    }
}
