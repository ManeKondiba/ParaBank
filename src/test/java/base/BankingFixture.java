package base;

import pageobjects.HomePage;
import pageobjects.OpenAccountPage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;

/** Creates independent accounts using the application UI; never resets the shared database. */
public final class BankingFixture {
    private static final Logger LOG = LogManager.getLogger(BankingFixture.class);

    private BankingFixture() {
    }

    public record Accounts(String sourceId, String destinationId) {
    }

    public static Accounts prepareTransferAccounts(WebDriver driver) {
        return UiApiFixture.enabled(utilities.FrameworkConfig.load())
                ? UiApiFixture.prepareTransferAccounts(driver) : registerWithTwoAccounts(driver);
    }

    public static Accounts registerWithTwoAccounts(WebDriver driver) {
        AccountFixture.register(driver);
        new HomePage(driver).clickOpenNewAccount();
        OpenAccountPage openAccount = new OpenAccountPage(driver);
        String source = openAccount.openAccount("SAVINGS");
        String destination = openAccount.getNewAccountId();
        Assert.assertEquals(openAccount.getConfirmationHeading(), "Account Opened!");
        Assert.assertNotEquals(source, destination, "Banking scenarios require two distinct accounts");
        LOG.debug("Created 2 distinct banking accounts: source={}, destination={}", source, destination);
        return new Accounts(source, destination);
    }
}
