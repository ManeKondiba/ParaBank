package PageObjects;

import java.math.BigDecimal;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import utilities.TransactionData;

/** Shared table component for account activity and transaction search results. */
public class TransactionTable extends BasePage {
    public TransactionTable(WebDriver driver) {
        super(driver);
    }

    public List<TransactionData> rows() {
        waitForJQueryRequestsToFinish();
        // A failed activity request hides the table but can leave its old rows in the DOM.
        wait.until(ignored -> driver.findElements(By.id("transactionTable")).stream().anyMatch(WebElement::isDisplayed)
                || driver.findElements(By.id("noTransactions")).stream().anyMatch(WebElement::isDisplayed));
        return driver.findElements(By.cssSelector("#transactionTable tbody tr")).stream().map(row -> {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            String href = cells.get(1).findElement(By.tagName("a")).getDomAttribute("href");
            String id = href.substring(href.lastIndexOf("id=") + 3);
            String debit = cells.get(2).getText().trim();
            String credit = cells.get(3).getText().trim();
            return new TransactionData(id, cells.get(0).getText().trim(), cells.get(1).getText().trim(),
                    debit.isEmpty() ? BigDecimal.ZERO : parseMoney(debit),
                    credit.isEmpty() ? BigDecimal.ZERO : parseMoney(credit));
        }).toList();
    }

    public void openTransaction(String transactionId) {
        if (!transactionId.matches("\\d+")) {
            throw new IllegalArgumentException("Transaction ID must be numeric");
        }
        click(By.cssSelector("#transactionTable a[href$='id=" + transactionId + "']"));
    }
}
