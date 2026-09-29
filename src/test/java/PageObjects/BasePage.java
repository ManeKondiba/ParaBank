package PageObjects;

import java.math.BigDecimal;
import java.util.Objects;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utilities.FrameworkConfig;

public abstract class BasePage {
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = Objects.requireNonNull(driver, "driver");
        this.wait = new WebDriverWait(driver, FrameworkConfig.load().getDuration("timeout.explicit.seconds"));
    }

    protected void type(By locator, String value) {
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        element.clear();
        element.sendKeys(value);
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected String text(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).getText().trim();
    }

    protected BigDecimal money(By locator) {
        String amount = wait.until(ignored -> {
            String value = driver.findElement(locator).getText().trim();
            return value.isBlank() ? null : value;
        });
        return parseMoney(amount);
    }

    protected BigDecimal parseMoney(String amount) {
        return new BigDecimal(amount.replace("$", "").replace(",", "").trim());
    }

    protected void waitForRequests() {
        // ParaBank's account/activity templates populate tables with jQuery AJAX.
        wait.until(ignored -> Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
                "return document.readyState === 'complete' && "
                + "typeof window.jQuery !== 'undefined' && window.jQuery.active === 0;")));
    }

    protected boolean visible(By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).isDisplayed();
        } catch (TimeoutException e) {
            return false;
        }
    }
}
