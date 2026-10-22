package PageObjects;

import java.math.BigDecimal;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class BillPayPage extends BasePage {
    private static final By BILL_PAY_LINK = By.linkText("Bill Pay");
    private static final By FORM = By.id("billpayForm");
    private static final By PAYEE_NAME = By.name("payee.name");
    private static final By PAYEE_STREET = By.name("payee.address.street");
    private static final By PAYEE_CITY = By.name("payee.address.city");
    private static final By PAYEE_STATE = By.name("payee.address.state");
    private static final By PAYEE_ZIP_CODE = By.name("payee.address.zipCode");
    // The phone input's generated ID changes on every page load; its name is stable.
    private static final By PAYEE_PHONE_NUMBER = By.name("payee.phoneNumber");
    private static final By PAYEE_ACCOUNT_NUMBER = By.name("payee.accountNumber");
    private static final By VERIFY_ACCOUNT = By.name("verifyAccount");
    private static final By AMOUNT = By.name("amount");
    private static final By FUNDING_ACCOUNT = By.name("fromAccountId");
    private static final By SEND_PAYMENT = By.cssSelector("#billpayForm input[value='Send Payment']");
    private static final By CONFIRMATION_HEADING = By.cssSelector("#billpayResult h1");
    private static final By CONFIRMED_PAYEE = By.id("payeeName");
    private static final By CONFIRMED_AMOUNT = By.id("amount");
    private static final By CONFIRMED_ACCOUNT = By.id("fromAccountId");

    public record Payee(String name, String street, String city, String state,
            String zipCode, String phoneNumber, String accountNumber) {
    }

    public BillPayPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        click(BILL_PAY_LINK);
        wait.until(ExpectedConditions.visibilityOfElementLocated(FORM));
        getFundingAccountIds();
    }

    public void fill(Payee payee, String accountConfirmation, String amount, String fundingAccountId) {
        type(PAYEE_NAME, payee.name());
        type(PAYEE_STREET, payee.street());
        type(PAYEE_CITY, payee.city());
        type(PAYEE_STATE, payee.state());
        type(PAYEE_ZIP_CODE, payee.zipCode());
        type(PAYEE_PHONE_NUMBER, payee.phoneNumber());
        type(PAYEE_ACCOUNT_NUMBER, payee.accountNumber());
        type(VERIFY_ACCOUNT, accountConfirmation);
        type(AMOUNT, amount);
        wait.until(ignored -> fundingAccountIds().contains(fundingAccountId));
        new Select(driver.findElement(FUNDING_ACCOUNT)).selectByValue(fundingAccountId);
    }

    public void sendPayment() {
        click(SEND_PAYMENT);
    }

    public List<String> getFundingAccountIds() {
        return wait.until(ignored -> {
            List<String> ids = fundingAccountIds();
            return ids.isEmpty() ? null : ids;
        });
    }

    private List<String> fundingAccountIds() {
        return new Select(driver.findElement(FUNDING_ACCOUNT)).getOptions().stream()
                .map(option -> option.getDomAttribute("value"))
                .filter(value -> value != null && value.matches("\\d+"))
                .toList();
    }

    public String getValidationError(String marker) {
        return text(By.id("validationModel-" + marker));
    }

    public boolean isFormDisplayed() {
        return visible(FORM);
    }

    public String getConfirmationHeading() {
        return text(CONFIRMATION_HEADING);
    }

    public String getConfirmedPayee() {
        return text(CONFIRMED_PAYEE);
    }

    public BigDecimal getConfirmedAmount() {
        return money(CONFIRMED_AMOUNT);
    }

    public String getConfirmedAccountId() {
        return text(CONFIRMED_ACCOUNT);
    }

    public void clearField(String name) {
        type(By.name(name), "");
    }
}
