package PageObjects;

import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class AccountsOverviewPage extends BasePage {
    private static final By ACCOUNT_LINKS = By.cssSelector("#accountTable tbody a");
    private static final By TOTAL = By.cssSelector("#accountTable tbody tr:last-child td:nth-child(2)");

    public AccountsOverviewPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        click(By.linkText("Accounts Overview"));
        getAccountIds();
    }

    public List<String> getAccountIds() {
        return wait.until(ignored -> {
            List<String> ids = driver.findElements(ACCOUNT_LINKS).stream()
                    .map(element -> element.getText().trim()).filter(value -> value.matches("\\d+")).toList();
            return ids.isEmpty() ? null : ids;
        });
    }

    private By accountCell(String accountId, int column) {
        if (!accountId.matches("\\d+")) {
            throw new IllegalArgumentException("Account ID must be numeric");
        }
        return By.xpath("//table[@id='accountTable']/tbody/tr[td/a[normalize-space()='"
                + accountId + "']]/td[" + column + "]");
    }

    public BigDecimal getBalance(String accountId) {
        return money(accountCell(accountId, 2));
    }

    public BigDecimal getAvailableBalance(String accountId) {
        return money(accountCell(accountId, 3));
    }

    public BigDecimal getTotalBalance() {
        getAccountIds();
        waitForRequests();
        return money(TOTAL);
    }

    public void openAccount(String accountId) {
        if (!accountId.matches("\\d+")) {
            throw new IllegalArgumentException("Account ID must be numeric");
        }
        click(By.xpath("//table[@id='accountTable']//a[normalize-space()='" + accountId + "']"));
    }
}
