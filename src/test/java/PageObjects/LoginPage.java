package PageObjects;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class LoginPage extends BasePage {
	

    public LoginPage(WebDriver driver) {
		super(driver);
		
	}

	@FindBy(name = "username")
    WebElement txtUsername;

    @FindBy(name = "password")
    WebElement txtPassword;

    @FindBy(xpath = "//input[@value='Log In']")
    WebElement btnLogin;

    @FindBy(xpath = "//h1[contains(text(), 'Accounts Overview')]")
    WebElement loginSuccessHeading;

    @FindBy(css = ".error")
    WebElement loginErrorMessage;

    public void enterUsername(String uname) {
        txtUsername.clear();
        txtUsername.sendKeys(uname);
    }

    public void enterPassword(String pwd) {
        txtPassword.clear();
        txtPassword.sendKeys(pwd);
    }

    public void clickLogin() {
        btnLogin.click();
    }

    public boolean isLoginSuccessDisplayed() {
        try {
            return loginSuccessHeading.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isLoginErrorDisplayed() {
        try {
            return loginErrorMessage.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getLoginErrorText() {
        return loginErrorMessage.getText();
    }
}


