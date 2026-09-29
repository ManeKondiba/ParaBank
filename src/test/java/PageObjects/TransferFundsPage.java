package PageObjects;

import java.math.BigDecimal;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

public class TransferFundsPage extends BasePage {
    private static final By FROM = By.id("fromAccountId");
    private static final By TO = By.id("toAccountId");

    public TransferFundsPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        click(By.linkText("Transfer Funds"));
        wait.until(ignored -> !new Select(driver.findElement(FROM)).getOptions().isEmpty()
                && !new Select(driver.findElement(TO)).getOptions().isEmpty());
    }

    public void transfer(String amount, String fromAccountId, String toAccountId) {
        type(By.id("amount"), amount);
        new Select(driver.findElement(FROM)).selectByValue(fromAccountId);
        new Select(driver.findElement(TO)).selectByValue(toAccountId);
        click(By.cssSelector("#transferForm input[type='submit']"));
    }

    public String getConfirmationHeading() {
        return text(By.cssSelector("#showResult h1"));
    }

    public BigDecimal getConfirmedAmount() {
        return money(By.id("amountResult"));
    }

    public String getFromAccountId() {
        return text(By.id("fromAccountIdResult"));
    }

    public String getToAccountId() {
        return text(By.id("toAccountIdResult"));
    }

    public String getError() {
        return text(By.cssSelector("#showError .error"));
    }
}
