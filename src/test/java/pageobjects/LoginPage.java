package pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class LoginPage extends BasePage {
    private static final By USERNAME_FIELD = By.cssSelector("#loginPanel input[name='username']");
    private static final By PASSWORD_FIELD = By.cssSelector("#loginPanel input[name='password']");
    private static final By LOGIN_BUTTON = By.cssSelector("#loginPanel input[value='Log In']");
    private static final By ACCOUNTS_OVERVIEW_HEADING =
            By.xpath("//div[@id='rightPanel']//h1[normalize-space()='Accounts Overview']");
    private static final By ERROR_MESSAGE = By.cssSelector("#rightPanel .error");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage enterUsername(String username) {
        type(USERNAME_FIELD, username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        type(PASSWORD_FIELD, password);
        return this;
    }

    public AccountsOverviewPage clickLogin() {
        click(LOGIN_BUTTON);
        return new AccountsOverviewPage(driver);
    }

    public AccountsOverviewPage login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        return clickLogin();
    }

    public AccountsOverviewPage submitWithEnter() {
        wait.until(ExpectedConditions.elementToBeClickable(PASSWORD_FIELD)).sendKeys(Keys.ENTER);
        return new AccountsOverviewPage(driver);
    }

    public boolean isPasswordMasked() {
        return "password".equalsIgnoreCase(wait.until(
                ExpectedConditions.visibilityOfElementLocated(PASSWORD_FIELD)).getDomAttribute("type"));
    }

    public boolean isLoginSuccessDisplayed() {
        return visible(ACCOUNTS_OVERVIEW_HEADING);
    }

    public String getAccountsOverviewUrl() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(ACCOUNTS_OVERVIEW_HEADING));
        return driver.getCurrentUrl();
    }

    public boolean isLoginErrorDisplayed() {
        return visible(ERROR_MESSAGE);
    }

    public String getLoginErrorText() {
        return text(ERROR_MESSAGE);
    }

    public boolean isLoginFormDisplayed() {
        return visible(LOGIN_BUTTON);
    }
}
