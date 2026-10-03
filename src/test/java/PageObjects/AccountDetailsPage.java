package PageObjects;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;
import utilities.FrameworkConfig;
import utilities.TransactionData;

public class AccountDetailsPage extends BasePage {
    public AccountDetailsPage(WebDriver driver) {
        super(driver);
    }

    public void open(String accountId) {
        if (!accountId.matches("\\d+")) {
            throw new IllegalArgumentException("Account ID must be numeric");
        }
        driver.navigate().to(URI.create(FrameworkConfig.load().get("appUrl"))
                .resolve("activity.htm?id=" + accountId).toString());
        wait.until(ignored -> accountId.equals(driver.findElement(By.id("accountId")).getText().trim()));
    }

    public String getAccountId() {
        return wait.until(ignored -> {
            String id = text(By.id("accountId"));
            return id.matches("\\d+") ? id : null;
        });
    }

    public String getAccountType() {
        return wait.until(ignored -> {
            String type = text(By.id("accountType"));
            return type.isBlank() ? null : type;
        });
    }

    public BigDecimal getBalance() {
        return money(By.id("balance"));
    }

    public BigDecimal getAvailableBalance() {
        return money(By.id("availableBalance"));
    }

    public List<TransactionData> getTransactions() {
        getAccountId();
        return new TransactionTable(driver).rows();
    }

    public void filterActivity(String period, String type) {
        waitForJQueryRequestsToFinish();
        new Select(driver.findElement(By.id("month"))).selectByVisibleText(period);
        new Select(driver.findElement(By.id("transactionType"))).selectByVisibleText(type);
        click(By.cssSelector("#activityForm input[value='Go']"));
        waitForJQueryRequestsToFinish();
    }

    public boolean isNoTransactionsDisplayed() {
        waitForJQueryRequestsToFinish();
        return visible(By.id("noTransactions"));
    }
}
