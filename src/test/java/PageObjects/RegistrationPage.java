package PageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import utilities.RegistrationData;
import utilities.ProvisioningLock;

public class RegistrationPage extends BasePage {
    private static final By FIRST_NAME_FIELD = By.id("customer.firstName");
    private static final By LAST_NAME_FIELD = By.id("customer.lastName");
    private static final By ADDRESS_FIELD = By.id("customer.address.street");
    private static final By CITY_FIELD = By.id("customer.address.city");
    private static final By STATE_FIELD = By.id("customer.address.state");
    private static final By ZIP_CODE_FIELD = By.id("customer.address.zipCode");
    private static final By PHONE_FIELD = By.id("customer.phoneNumber");
    private static final By SSN_FIELD = By.id("customer.ssn");
    private static final By USERNAME_FIELD = By.id("customer.username");
    private static final By PASSWORD_FIELD = By.id("customer.password");
    private static final By CONFIRM_PASSWORD_FIELD = By.id("repeatedPassword");
    private static final By REGISTER_BUTTON = By.cssSelector("#customerForm input[value='Register']");
    private static final By SUCCESS_MESSAGE =
            By.xpath("//div[@id='rightPanel']//h1[starts-with(normalize-space(),'Welcome ')]");

    public RegistrationPage(WebDriver driver) {
        super(driver);
    }

    public void enterFirstName(String firstName) {
        type(FIRST_NAME_FIELD, firstName);
    }

    public void enterLastName(String lastName) {
        type(LAST_NAME_FIELD, lastName);
    }

    public void enterAddress(String address) {
        type(ADDRESS_FIELD, address);
    }

    public void enterCity(String city) {
        type(CITY_FIELD, city);
    }

    public void enterState(String state) {
        type(STATE_FIELD, state);
    }

    public void enterZipCode(String zipCode) {
        type(ZIP_CODE_FIELD, zipCode);
    }

    public void enterPhone(String phone) {
        type(PHONE_FIELD, phone);
    }

    public void enterSSN(String ssn) {
        type(SSN_FIELD, ssn);
    }

    public void enterUsername(String username) {
        type(USERNAME_FIELD, username);
    }

    public void enterPassword(String password) {
        type(PASSWORD_FIELD, password);
    }

    public void enterConfirmPassword(String password) {
        type(CONFIRM_PASSWORD_FIELD, password);
    }

    public void clickRegisterButton() {
        synchronized (ProvisioningLock.MONITOR) {
            // Normal page loading waits for the registration response before releasing the lock.
            click(REGISTER_BUTTON);
        }
    }

    public boolean isSuccessMessageDisplayed() {
        return visible(SUCCESS_MESSAGE);
    }

    public String getSuccessMessageText() {
        return text(SUCCESS_MESSAGE);
    }

    public String getFieldError(String fieldId) {
        return text(By.id(fieldId + ".errors"));
    }

    public void clearField(String fieldId) {
        type(By.id(fieldId), "");
    }

    public boolean isPasswordMasked() {
        return "password".equals(wait.until(
                ExpectedConditions.visibilityOfElementLocated(PASSWORD_FIELD)).getDomAttribute("type"));
    }

    public boolean isConfirmPasswordMasked() {
        return "password".equals(wait.until(
                ExpectedConditions.visibilityOfElementLocated(CONFIRM_PASSWORD_FIELD)).getDomAttribute("type"));
    }

    public void fill(RegistrationData data) {
        enterFirstName("Automation");
        enterLastName("Tester");
        enterAddress("123 Test Street");
        enterCity("Springfield");
        enterState("IL");
        enterZipCode("62701");
        enterPhone("5551234567");
        enterSSN("123456789");
        enterUsername(data.username());
        enterPassword(data.password());
        enterConfirmPassword(data.password());
    }
}
