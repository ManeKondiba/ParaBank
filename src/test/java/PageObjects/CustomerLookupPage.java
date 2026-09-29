package PageObjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import utilities.RegistrationData;

public class CustomerLookupPage extends BasePage {
    private static final By FIND_BUTTON = By.cssSelector("#lookupForm input[value='Find My Login Info']");
    private static final By LOOKUP_ERROR = By.cssSelector("#rightPanel p.error");
    private static final By RECOVERED_CREDENTIALS = By.xpath(
            "//div[@id='rightPanel']/p[b[normalize-space()='Username'] and b[normalize-space()='Password']]");

    public CustomerLookupPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        click(By.linkText("Forgot login info?"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(FIND_BUTTON));
    }

    public void fill(String firstName, String lastName, String street, String city,
            String state, String zipCode, String ssn) {
        type(By.id("firstName"), firstName);
        type(By.id("lastName"), lastName);
        type(By.id("address.street"), street);
        type(By.id("address.city"), city);
        type(By.id("address.state"), state);
        type(By.id("address.zipCode"), zipCode);
        type(By.id("ssn"), ssn);
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
        click(By.linkText("Accounts Overview"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[@id='rightPanel']//h1[normalize-space()='Accounts Overview']")));
    }
}
