package PageObjects;

import java.math.BigDecimal;
import java.net.URI;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utilities.FrameworkConfig;

public class TransactionDetailsPage extends BasePage {
    public TransactionDetailsPage(WebDriver driver) {
        super(driver);
    }

    public void open(String transactionId) {
        if (!transactionId.matches("\\d+")) {
            throw new IllegalArgumentException("Transaction ID must be numeric");
        }
        driver.navigate().to(URI.create(FrameworkConfig.load().get("appUrl"))
                .resolve("transaction.htm?id=" + transactionId).toString());
    }

    private By value(String label) {
        return By.xpath("//div[@id='rightPanel']//tr[td[normalize-space()='" + label + ":']]/td[2]");
    }

    public String getId() {
        return text(value("Transaction ID"));
    }

    public String getDate() {
        return text(value("Date"));
    }

    public String getDescription() {
        return text(value("Description"));
    }

    public String getType() {
        return text(value("Type"));
    }

    public BigDecimal getAmount() {
        return money(value("Amount"));
    }
}
