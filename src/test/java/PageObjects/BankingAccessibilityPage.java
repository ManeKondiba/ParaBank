package PageObjects;

import java.net.URI;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utilities.FrameworkConfig;

/** Structural accessibility checks supplement, rather than replace, keyboard/screen-reader review. */
public class BankingAccessibilityPage extends BasePage {
    public record Control(String field, String accessibleName) { }
    public BankingAccessibilityPage(WebDriver driver) { super(driver); }
    public void open(String route) {
        driver.navigate().to(URI.create(FrameworkConfig.load().get("appUrl")).resolve(route).toString());
        waitForJQueryRequestsToFinish();
    }
    public List<Control> visibleFormControls() {
        return driver.findElements(By.cssSelector("#rightPanel input:not([type='hidden']):not([type='submit']):not([type='button']),"
                + "#rightPanel select,#rightPanel textarea,#loginPanel input:not([type='submit'])"))
                .stream().filter(element -> element.isDisplayed() && element.isEnabled())
                .map(element -> new Control(element.getDomAttribute("id") == null
                        ? element.getDomAttribute("name") : element.getDomAttribute("id"), element.getAccessibleName())).toList();
    }
}