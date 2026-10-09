package PageObjects;

import java.math.BigDecimal;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class RequestLoanPage extends BasePage {
    private static final By PAGE_LINK = By.linkText("Request Loan");
    private static final By FORM = By.id("requestLoanForm");
    private static final By AMOUNT = By.id("amount");
    private static final By DOWN_PAYMENT = By.id("downPayment");
    private static final By SOURCE_ACCOUNT = By.id("fromAccountId");
    private static final By APPLY_BUTTON = By.cssSelector("#requestLoanForm input[type='button']");
    private static final By RESULT_HEADING = By.cssSelector("#requestLoanResult h1");
    private static final By STATUS = By.id("loanStatus");
    private static final By PROVIDER = By.id("loanProviderName");
    private static final By RESPONSE_DATE = By.id("responseDate");
    private static final By NEW_ACCOUNT = By.id("newAccountId");
    private static final By APPROVAL = By.id("loanRequestApproved");
    private static final By DENIAL = By.id("loanRequestDenied");
    private static final By DENIAL_MESSAGE = By.cssSelector("#loanRequestDenied p.error");
    private static final By SERVICE_ERROR = By.cssSelector("#requestLoanError p.error");

    public enum Decision {
        APPROVED, DENIED
    }

    public RequestLoanPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        click(PAGE_LINK);
        wait.until(ExpectedConditions.visibilityOfElementLocated(FORM));
        getFundingAccountIds();
    }

    public List<String> getFundingAccountIds() {
        return wait.until(ignored -> {
            List<String> ids = new Select(driver.findElement(SOURCE_ACCOUNT)).getOptions().stream()
                    .map(option -> option.getDomAttribute("value"))
                    .filter(value -> value != null && value.matches("\\d+"))
                    .toList();
            return ids.isEmpty() ? null : ids;
        });
    }

    public void apply(BigDecimal amount, BigDecimal downPayment, String sourceAccountId) {
        type(AMOUNT, amount.toPlainString());
        type(DOWN_PAYMENT, downPayment.toPlainString());
        getFundingAccountIds();
        new Select(driver.findElement(SOURCE_ACCOUNT)).selectByValue(sourceAccountId);
        click(APPLY_BUTTON);
        wait.until(ignored -> {
            List<WebElement> errors = driver.findElements(SERVICE_ERROR);
            for (WebElement error : errors) {
                if (error.isDisplayed()) {
                    throw new IllegalStateException("Loan request failed: " + error.getText());
                }
            }
            return driver.findElement(STATUS).isDisplayed()
                    && !driver.findElement(STATUS).getText().isBlank();
        });
    }

    public Decision getDecision() {
        String status = text(STATUS);
        return switch (status) {
            case "Approved" -> Decision.APPROVED;
            case "Denied" -> Decision.DENIED;
            default -> throw new IllegalStateException("Unexpected loan decision: " + status);
        };
    }

    public String getResultHeading() {
        return text(RESULT_HEADING);
    }

    public String getProviderName() {
        return text(PROVIDER);
    }

    public String getResponseDate() {
        return text(RESPONSE_DATE);
    }

    public String getNewAccountId() {
        wait.until(ignored -> driver.findElement(NEW_ACCOUNT).isDisplayed()
                && driver.findElement(NEW_ACCOUNT).getText().trim().matches("[1-9]\\d*"));
        return text(NEW_ACCOUNT);
    }

    public String getDenialMessage() {
        return text(DENIAL_MESSAGE);
    }

    public boolean isApprovalDisplayed() {
        return driver.findElement(APPROVAL).isDisplayed();
    }

    public boolean isDenialDisplayed() {
        return driver.findElement(DENIAL).isDisplayed();
    }

    public boolean isNewAccountLinkDisplayed() {
        return driver.findElement(NEW_ACCOUNT).isDisplayed();
    }

    public void openLoanAccount() {
        getNewAccountId();
        click(NEW_ACCOUNT);
    }
    public void submitRaw(String amount, String downPayment, String sourceAccountId) {
        type(AMOUNT, amount);
        type(DOWN_PAYMENT, downPayment);
        new Select(driver.findElement(SOURCE_ACCOUNT)).selectByValue(sourceAccountId);
        click(APPLY_BUTTON);
    }

    public String getServiceError() { return text(SERVICE_ERROR); }

}
