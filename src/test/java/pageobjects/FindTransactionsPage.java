package pageobjects;

import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import utilities.TransactionData;

public class FindTransactionsPage extends BasePage {
    public enum SearchType { ID, DATE, DATE_RANGE, AMOUNT }

    private static final By FIND_TRANSACTIONS_LINK = By.linkText("Find Transactions");
    private static final By ACCOUNT_ID_DROPDOWN = By.id("accountId");
    private static final By TRANSACTION_ID_FIELD = By.id("transactionId");
    private static final By FIND_BY_ID_BUTTON = By.id("findById");
    private static final By TRANSACTION_DATE_FIELD = By.id("transactionDate");
    private static final By FIND_BY_DATE_BUTTON = By.id("findByDate");
    private static final By FROM_DATE_FIELD = By.id("fromDate");
    private static final By TO_DATE_FIELD = By.id("toDate");
    private static final By FIND_BY_DATE_RANGE_BUTTON = By.id("findByDateRange");
    private static final By AMOUNT_FIELD = By.id("amount");
    private static final By FIND_BY_AMOUNT_BUTTON = By.id("findByAmount");
    private static final By RESULT_CONTAINER = By.id("resultContainer");
    private static final By FORM_CONTAINER = By.id("formContainer");

    public FindTransactionsPage(WebDriver driver) {
        super(driver);
    }

    public FindTransactionsPage open() {
        click(FIND_TRANSACTIONS_LINK);
        wait.until(ExpectedConditions.elementToBeClickable(ACCOUNT_ID_DROPDOWN));
        return this;
    }

    public FindTransactionsPage search(SearchType searchType, String accountId, String value, String endDate) {
        new Select(driver.findElement(ACCOUNT_ID_DROPDOWN)).selectByValue(accountId);
        switch (searchType) {
            case ID -> {
                type(TRANSACTION_ID_FIELD, value);
                click(FIND_BY_ID_BUTTON);
            }
            case DATE -> {
                type(TRANSACTION_DATE_FIELD, value);
                click(FIND_BY_DATE_BUTTON);
            }
            case DATE_RANGE -> {
                type(FROM_DATE_FIELD, value);
                type(TO_DATE_FIELD, endDate);
                click(FIND_BY_DATE_RANGE_BUTTON);
            }
            case AMOUNT -> {
                type(AMOUNT_FIELD, value);
                click(FIND_BY_AMOUNT_BUTTON);
            }
        }
        return this;
    }

    public List<TransactionData> getResults() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(RESULT_CONTAINER));
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
        return visible(FORM_CONTAINER);
    }
}
