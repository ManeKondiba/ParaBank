package pageobjects;

import java.math.BigDecimal;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class TransferFundsPage extends BasePage {
    private static final By TRANSFER_FUNDS_LINK = By.linkText("Transfer Funds");
    private static final By AMOUNT_FIELD = By.id("amount");
    private static final By FROM_ACCOUNT_DROPDOWN = By.id("fromAccountId");
    private static final By TO_ACCOUNT_DROPDOWN = By.id("toAccountId");
    private static final By TRANSFER_BUTTON = By.cssSelector("#transferForm input[type='submit']");
    private static final By CONFIRMATION_HEADING = By.cssSelector("#showResult h1");
    private static final By CONFIRMED_AMOUNT = By.id("amountResult");
    private static final By CONFIRMED_FROM_ACCOUNT = By.id("fromAccountIdResult");
    private static final By CONFIRMED_TO_ACCOUNT = By.id("toAccountIdResult");
    private static final By ERROR_MESSAGE = By.cssSelector("#showError .error");

    public TransferFundsPage(WebDriver driver) {
        super(driver);
    }

    public TransferFundsPage open() {
        click(TRANSFER_FUNDS_LINK);
        wait.until(ignored -> !new Select(driver.findElement(FROM_ACCOUNT_DROPDOWN)).getOptions().isEmpty()
                && !new Select(driver.findElement(TO_ACCOUNT_DROPDOWN)).getOptions().isEmpty());
        return this;
    }

    public TransferFundsPage transfer(String amount, String fromAccountId, String toAccountId) {
        type(AMOUNT_FIELD, amount);
        new Select(driver.findElement(FROM_ACCOUNT_DROPDOWN)).selectByValue(fromAccountId);
        new Select(driver.findElement(TO_ACCOUNT_DROPDOWN)).selectByValue(toAccountId);
        click(TRANSFER_BUTTON);
        return this;
    }

    public TransferFundsPage doubleClickTransfer(String amount, String fromAccountId, String toAccountId) {
        type(AMOUNT_FIELD, amount);
        new Select(driver.findElement(FROM_ACCOUNT_DROPDOWN)).selectByValue(fromAccountId);
        new Select(driver.findElement(TO_ACCOUNT_DROPDOWN)).selectByValue(toAccountId);
        var submitBtn = wait.until(ExpectedConditions.elementToBeClickable(TRANSFER_BUTTON));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click(); arguments[0].click();", submitBtn);
        return this;
    }

    public String getConfirmationHeading() {
        return text(CONFIRMATION_HEADING);
    }

    public BigDecimal getConfirmedAmount() {
        return money(CONFIRMED_AMOUNT);
    }

    public String getFromAccountId() {
        return text(CONFIRMED_FROM_ACCOUNT);
    }

    public String getToAccountId() {
        return text(CONFIRMED_TO_ACCOUNT);
    }

    public String getError() {
        return text(ERROR_MESSAGE);
    }
}
