package PageObjects;

import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class UpdateContactInfoPage extends BasePage {
    private static final By UPDATE_BUTTON = By.cssSelector("#updateProfileForm input[value='Update Profile']");
    private static final By RESULT_HEADING = By.cssSelector("#updateProfileResult h1");
    private static final Map<String, String> ERROR_IDS = Map.of(
            "customer.firstName", "firstName-error",
            "customer.lastName", "lastName-error",
            "customer.address.street", "street-error",
            "customer.address.city", "city-error",
            "customer.address.state", "state-error",
            "customer.address.zipCode", "zipCode-error");

    public UpdateContactInfoPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        click(By.linkText("Update Contact Info"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(UPDATE_BUTTON));
        // The form appears before the customer's existing profile is loaded by AJAX.
        wait.until(currentDriver -> ERROR_IDS.keySet().stream().allMatch(fieldId -> {
            String value = currentDriver.findElement(By.id(fieldId)).getDomProperty("value");
            return value != null && !value.isBlank();
        }));
    }

    public void fill(Map<String, String> contactInfo) {
        contactInfo.forEach((fieldId, value) -> type(By.id(fieldId), value));
    }

    public void clearField(String fieldId) {
        type(By.id(fieldId), "");
    }

    public String getFieldValue(String fieldId) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(fieldId)))
                .getDomProperty("value");
    }

    public String getFieldError(String fieldId) {
        String errorId = ERROR_IDS.get(fieldId);
        if (errorId == null) {
            throw new IllegalArgumentException("No required-field error exists for " + fieldId);
        }
        return text(By.id(errorId));
    }

    public void submit() {
        click(UPDATE_BUTTON);
    }

    public String getConfirmationHeading() {
        return text(RESULT_HEADING);
    }
}
