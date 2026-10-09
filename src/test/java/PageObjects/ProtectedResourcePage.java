package PageObjects;

import java.net.URI;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utilities.FrameworkConfig;

public class ProtectedResourcePage extends BasePage {
    public ProtectedResourcePage(WebDriver driver) { super(driver); }
    public void open(String route) {
        driver.navigate().to(URI.create(FrameworkConfig.load().get("appUrl")).resolve(route).toString());
        waitForJQueryRequestsToFinish();
    }
    public boolean exposesResource(String resource, String id) {
        By value = resource.equals("account") ? By.id("accountId")
                : By.xpath("//div[@id='rightPanel']//tr[td[normalize-space()='Transaction ID:']]/td[2]");
        waitForJQueryRequestsToFinish();
        return driver.findElements(value).stream().anyMatch(element -> element.isDisplayed() && element.getText().trim().equals(id));
    }
    public boolean isAccessErrorDisplayed() {
        return driver.findElements(By.cssSelector("#rightPanel .error")).stream()
                .anyMatch(element -> element.isDisplayed() && !element.getText().isBlank());
    }
    public boolean exposesOverviewAccount(String id) {
        waitForJQueryRequestsToFinish();
        return driver.findElements(By.cssSelector("#accountTable tbody a")).stream()
                .anyMatch(element -> element.isDisplayed() && element.getText().trim().equals(id));
    }
}