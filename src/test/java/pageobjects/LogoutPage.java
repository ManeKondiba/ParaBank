package pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class LogoutPage extends BasePage {
    private static final By LOGOUT_LINK = By.linkText("Log Out");
    private static final By LOGIN_USERNAME_FIELD = By.cssSelector("#loginPanel input[name='username']");

    public LogoutPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage clickLogout() {
        click(LOGOUT_LINK);
        wait.until(ExpectedConditions.visibilityOfElementLocated(LOGIN_USERNAME_FIELD));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(LOGOUT_LINK));
        return new LoginPage(driver);
    }

    public boolean isLoggedOut() {
        return new LoginPage(driver).isLoginFormDisplayed() && driver.findElements(LOGOUT_LINK).isEmpty();
    }

    public LogoutPage refreshPage() {
        driver.navigate().refresh();
        return this;
    }

    public LogoutPage revisitPage(String pageUrl) {
        driver.navigate().to(pageUrl);
        return this;
    }
}
