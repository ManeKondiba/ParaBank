package pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import utilities.RegistrationData;

public class CustomerLookupPage extends BasePage {
    private static final By FORGOT_LOGIN_LINK = By.linkText("Forgot login info?");
    private static final By FIRST_NAME_FIELD = By.id("firstName");
    private static final By LAST_NAME_FIELD = By.id("lastName");
    private static final By STREET_FIELD = By.id("address.street");
    private static final By CITY_FIELD = By.id("address.city");
    private static final By STATE_FIELD = By.id("address.state");
    private static final By ZIP_CODE_FIELD = By.id("address.zipCode");
    private static final By SSN_FIELD = By.id("ssn");
    private static final By FIND_BUTTON = By.cssSelector("#lookupForm input[value='Find My Login Info']");
    private static final By LOOKUP_ERROR = By.cssSelector("#rightPanel p.error");
    private static final By RECOVERED_CREDENTIALS = By.xpath(
            "//div[@id='rightPanel']/p[b[normalize-space()='Username'] and b[normalize-space()='Password']]");
    private static final By ACCOUNTS_OVERVIEW_LINK = By.linkText("Accounts Overview");
    private static final By ACCOUNTS_OVERVIEW_HEADING = By.xpath(
            "//div[@id='rightPanel']//h1[normalize-space()='Accounts Overview']");

    public CustomerLookupPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        click(FORGOT_LOGIN_LINK);
        wait.until(ExpectedConditions.visibilityOfElementLocated(FIND_BUTTON));
    }

    public void fill(String firstName, String lastName, String street, String city,
            String state, String zipCode, String ssn) {
        type(FIRST_NAME_FIELD, firstName);
        type(LAST_NAME_FIELD, lastName);
        type(STREET_FIELD, street);
        type(CITY_FIELD, city);
        type(STATE_FIELD, state);
        type(ZIP_CODE_FIELD, zipCode);
        type(SSN_FIELD, ssn);
    }

    public void clearField(String fieldId) {
        type(By.id(fieldId), "");
    }

    public void submit() {
        click(FIND_BUTTON);
    }

    public String getFieldError(String fieldId) {
        return text(By.id(fieldId + ".errors"));
    }

    public String getLookupError() {
        return text(LOOKUP_ERROR);
    }

    public boolean recoveredCredentialsMatch(RegistrationData customer) {
        String[] recoveredLines = text(RECOVERED_CREDENTIALS).split("\\R");
        return recoveredLines.length == 2
                && recoveredLines[0].trim().equals("Username: " + customer.username())
                && recoveredLines[1].trim().equals("Password: " + customer.password());
    }

    public void openAccountsOverview() {
        click(ACCOUNTS_OVERVIEW_LINK);
        wait.until(ExpectedConditions.visibilityOfElementLocated(ACCOUNTS_OVERVIEW_HEADING));
    }

    public boolean isRecoveredCredentialsDisplayed() {
        return wait.until(ignored -> !driver.findElements(RECOVERED_CREDENTIALS).isEmpty()
                || !driver.findElements(LOOKUP_ERROR).isEmpty())
                && driver.findElements(RECOVERED_CREDENTIALS).stream().anyMatch(element -> element.isDisplayed());
    }
}
