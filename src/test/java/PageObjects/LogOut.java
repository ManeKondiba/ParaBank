package PageObjects;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LogOut extends BasePage {

	public LogOut(WebDriver driver) {
		super(driver);
	}
	
	
	@FindBy(linkText = "Log Out")
	private WebElement linkLogout;
	
	@FindBy(xpath = "//h2[text()='Customer Login']")
	private WebElement loginHeading;
	
	public void clickLogout() {
	    linkLogout.click();
	}
	
	public boolean isLoggedOutLinkExist() {
	    return loginHeading.isDisplayed();
	}
}
