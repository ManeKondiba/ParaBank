package PageObjects;

import java.math.BigDecimal;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class OpenAccountPage extends BasePage {
    private static final By ACCOUNT_TYPE_DROPDOWN = By.id("type");
    private static final By FUNDING_ACCOUNT_DROPDOWN = By.id("fromAccountId");
    private static final By OPEN_ACCOUNT_BUTTON = By.cssSelector("input[value='Open New Account']");
    private static final By NEW_ACCOUNT_LINK = By.id("newAccountId");
    private static final By CONFIRMATION_HEADING = By.cssSelector("#openAccountResult h1");
    private static final By ACCOUNT_TYPE = By.id("accountType");
    private static final By ACCOUNT_ID = By.id("accountId");
    private static final By ACCOUNT_BALANCE = By.id("balance");

    public OpenAccountPage(WebDriver driver) {
        super(driver);
    }

    public String openAccount(String accountType) {
        String fundingAccountId = getFundingAccountIds().get(0);
        openAccount(accountType, fundingAccountId);
        return fundingAccountId;
    }

    public void openAccount(String accountType, String fundingAccountId) {
        Select accountTypes = new Select(wait.until(ExpectedConditions.elementToBeClickable(ACCOUNT_TYPE_DROPDOWN)));
        accountTypes.selectByVisibleText(accountType);

        wait.until(ignored -> fundingAccountIds().contains(fundingAccountId));
        new Select(driver.findElement(FUNDING_ACCOUNT_DROPDOWN)).selectByValue(fundingAccountId);
        click(OPEN_ACCOUNT_BUTTON);
        wait.until(ignored -> driver.findElement(NEW_ACCOUNT_LINK).getText().trim().matches("\\d+"));
    }

    public List<String> getAccountTypes() {
        return new Select(wait.until(ExpectedConditions.elementToBeClickable(ACCOUNT_TYPE_DROPDOWN)))
                .getOptions().stream().map(option -> option.getText().trim()).toList();
    }

    public String getSelectedAccountType() {
        return new Select(wait.until(ExpectedConditions.elementToBeClickable(ACCOUNT_TYPE_DROPDOWN)))
                .getFirstSelectedOption().getText().trim();
    }

    public List<String> getFundingAccountIds() {
        // Options are populated asynchronously after the document loads.
        return wait.until(ignored -> {
            List<String> ids = fundingAccountIds();
            return ids.isEmpty() ? null : ids;
        });
    }

    private List<String> fundingAccountIds() {
        return new Select(driver.findElement(FUNDING_ACCOUNT_DROPDOWN)).getOptions().stream()
                .map(option -> option.getDomAttribute("value"))
                .filter(value -> value != null && value.matches("\\d+"))
                .toList();
    }

    public String getNewAccountId() {
        return text(NEW_ACCOUNT_LINK);
    }

    public String getConfirmationHeading() {
        return text(CONFIRMATION_HEADING);
    }

    public void openAccountDetails() {
        click(NEW_ACCOUNT_LINK);
    }

    public String getAccountType() {
        wait.until(ignored -> !driver.findElement(ACCOUNT_TYPE).getText().isBlank());
        return text(ACCOUNT_TYPE);
    }

    public String getDetailAccountId() {
        wait.until(ignored -> driver.findElement(ACCOUNT_ID).getText().trim().matches("\\d+"));
        return text(ACCOUNT_ID);
    }

    public void refreshAccountDetails() {
        driver.navigate().refresh();
        getDetailAccountId();
    }

    public String getAccountDetailsUrl() {
        getDetailAccountId();
        return driver.getCurrentUrl();
    }

    public void revisitAccountDetails(String detailsUrl) {
        driver.navigate().to(detailsUrl);
        getDetailAccountId();
    }

    public BigDecimal getAccountBalance() {
        String amount = wait.until(ignored -> {
            String value = driver.findElement(ACCOUNT_BALANCE).getText().trim();
            return value.isBlank() ? null : value;
        });
        return new BigDecimal(amount.replace("$", "").replace(",", ""));
    }
}
