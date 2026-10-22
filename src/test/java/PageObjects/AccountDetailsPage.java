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
    private static final By ACCOUNT_ID_LABEL = By.id("accountId");
    private static final By ACCOUNT_TYPE_LABEL = By.id("accountType");
    private static final By BALANCE_LABEL = By.id("balance");
    private static final By AVAILABLE_BALANCE_LABEL = By.id("availableBalance");
    private static final By MONTH_DROPDOWN = By.id("month");
    private static final By TRANSACTION_TYPE_DROPDOWN = By.id("transactionType");
    private static final By FILTER_BUTTON = By.cssSelector("#activityForm input[value='Go']");
    private static final By NO_TRANSACTIONS_MESSAGE = By.id("noTransactions");

    public AccountDetailsPage(WebDriver driver) {
        super(driver);
    }

    public void open(String accountId) {
        if (!accountId.matches("\\d+")) {
            throw new IllegalArgumentException("Account ID must be numeric");
        }
        driver.navigate().to(URI.create(FrameworkConfig.load().get("appUrl"))
                .resolve("activity.htm?id=" + accountId).toString());
        wait.until(ignored -> accountId.equals(driver.findElement(ACCOUNT_ID_LABEL).getText().trim()));
    }

    public String getAccountId() {
        return wait.until(ignored -> {
            String id = text(ACCOUNT_ID_LABEL);
            return id.matches("\\d+") ? id : null;
        });
    }

    public String getAccountType() {
        return wait.until(ignored -> {
            String type = text(ACCOUNT_TYPE_LABEL);
            return type.isBlank() ? null : type;
        });
    }

    public BigDecimal getBalance() {
        return money(BALANCE_LABEL);
    }

    public BigDecimal getAvailableBalance() {
        return money(AVAILABLE_BALANCE_LABEL);
    }

    public List<TransactionData> getTransactions() {
        getAccountId();
        return new TransactionTable(driver).rows();
    }

    public void filterActivity(String period, String type) {
        waitForJQueryRequestsToFinish();
        new Select(driver.findElement(MONTH_DROPDOWN)).selectByVisibleText(period);
        new Select(driver.findElement(TRANSACTION_TYPE_DROPDOWN)).selectByVisibleText(type);
        click(FILTER_BUTTON);
        waitForJQueryRequestsToFinish();
    }

    public boolean isNoTransactionsDisplayed() {
        waitForJQueryRequestsToFinish();
        return visible(NO_TRANSACTIONS_MESSAGE);
    }
}
