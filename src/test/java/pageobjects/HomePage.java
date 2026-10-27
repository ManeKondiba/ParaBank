package pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class HomePage extends BasePage {
    private static final By REGISTER_LINK = By.linkText("Register");
    private static final By OPEN_NEW_ACCOUNT_LINK = By.linkText("Open New Account");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public RegistrationPage clickRegisterLink() {
        click(REGISTER_LINK);
        return new RegistrationPage(driver);
    }

    public OpenAccountPage clickOpenNewAccount() {
        click(OPEN_NEW_ACCOUNT_LINK);
        return new OpenAccountPage(driver);
    }
}
