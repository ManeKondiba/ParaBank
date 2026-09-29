package PageObjects;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import utilities.TransactionData;

public class FindTransactionsPage extends BasePage {
    public enum SearchType { ID, DATE, DATE_RANGE, AMOUNT }

    public FindTransactionsPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        click(By.linkText("Find Transactions"));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("accountId")));
    }

    public void search(SearchType searchType, String accountId, String value, String endDate) {
        new Select(driver.findElement(By.id("accountId"))).selectByValue(accountId);
        switch (searchType) {
            case ID -> {
                type(By.id("transactionId"), value);
                click(By.id("findById"));
            }
            case DATE -> {
                type(By.id("transactionDate"), value);
                click(By.id("findByDate"));
            }
            case DATE_RANGE -> {
                type(By.id("fromDate"), value);
                type(By.id("toDate"), endDate);
                click(By.id("findByDateRange"));
            }
            case AMOUNT -> {
                type(By.id("amount"), value);
                click(By.id("findByAmount"));
            }
        }
    }

    public List<TransactionData> getResults() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("resultContainer")));
        return new TransactionTable(driver).rows();
    }

    public String getValidationError(SearchType searchType) {
        String id = switch (searchType) {
            case ID -> "transactionIdError";
            case DATE -> "transactionDateError";
            case DATE_RANGE -> "dateRangeError";
            case AMOUNT -> "amountError";
        };
        return text(By.id(id));
    }

    public boolean isSearchFormDisplayed() {
        return visible(By.id("formContainer"));
    }
}
