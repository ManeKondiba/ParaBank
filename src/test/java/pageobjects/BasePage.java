package pageobjects;

import java.math.BigDecimal;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utilities.FrameworkConfig;

public abstract class BasePage {
    protected final Logger log;
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.log = LogManager.getLogger(getClass());
        this.driver = Objects.requireNonNull(driver, "driver");
        this.wait = new WebDriverWait(driver, FrameworkConfig.load().getDuration("timeout.explicit.seconds"));
    }

    protected void type(By locator, String value) {
        log.debug("Typing into [{}]: {}", locator, isSensitive(locator) ? "********" : value);
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        element.clear();
        element.sendKeys(value);
    }

    protected void click(By locator) {
        log.debug("Clicking element: [{}]", locator);
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected String text(By locator) {
        log.debug("Reading text from element: [{}]", locator);
        String text = wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).getText().trim();
        log.debug("Text from [{}]: '{}'", locator, text);
        return text;
    }

    protected BigDecimal money(By locator) {
        log.debug("Reading monetary amount from element: [{}]", locator);
        String amount = wait.until(ignored -> {
            String value = driver.findElement(locator).getText().trim();
            return value.isBlank() ? null : value;
        });
        BigDecimal parsed = parseMoney(amount);
        log.debug("Parsed amount from [{}]: {}", locator, parsed);
        return parsed;
    }

    protected BigDecimal parseMoney(String amount) {
        return new BigDecimal(amount.replace("$", "").replace(",", "").trim());
    }

    protected void waitForJQueryRequestsToFinish() {
        log.debug("Waiting for jQuery AJAX requests to complete...");
        wait.until(ignored -> Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
                "return document.readyState === 'complete' && "
                + "typeof window.jQuery !== 'undefined' && window.jQuery.active === 0;")));
        log.debug("jQuery AJAX requests completed.");
    }

    protected boolean visible(By locator) {
        log.debug("Checking visibility of element: [{}]", locator);
        try {
            boolean isVisible = wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).isDisplayed();
            log.debug("Element [{}] is visible: {}", locator, isVisible);
            return isVisible;
        } catch (TimeoutException e) {
            log.debug("Element [{}] is not visible (timed out)", locator);
            return false;
        }
    }

    private boolean isSensitive(By locator) {
        String locStr = locator.toString().toLowerCase();
        return locStr.contains("password") || locStr.contains("ssn") || locStr.contains("token") || locStr.contains("secret");
    }
}
