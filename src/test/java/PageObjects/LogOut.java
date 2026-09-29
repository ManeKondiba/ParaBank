package PageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class LogOut extends BasePage {
    private static final By LOGOUT_LINK = By.linkText("Log Out");
    private static final By LOGIN_USERNAME_FIELD = By.cssSelector("#loginPanel input[name='username']");

    public LogOut(WebDriver driver) {
        super(driver);
    }

    public void clickLogout() {
        click(LOGOUT_LINK);
        wait.until(ExpectedConditions.visibilityOfElementLocated(LOGIN_USERNAME_FIELD));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(LOGOUT_LINK));
    }

    public boolean isLoggedOut() {
        return new LoginPage(driver).isLoginFormDisplayed() && driver.findElements(LOGOUT_LINK).isEmpty();
    }

    public void refreshPage() {
        driver.navigate().refresh();
    }

    public void revisitPage(String pageUrl) {
        driver.navigate().to(pageUrl);
    }
}
