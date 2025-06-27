package PageObjects;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class RegistrationPage extends BasePage {

	public RegistrationPage(WebDriver driver) {
		super(driver);
		
	}
	
	  @FindBy(id = "customer.firstName")
	    WebElement firstNameInput;

	    @FindBy(id = "customer.lastName")
	    WebElement lastNameInput;

	    @FindBy(id = "customer.address.street")
	    WebElement addressInput;

	    @FindBy(id = "customer.address.city")
	    WebElement cityInput;

	    @FindBy(id = "customer.address.state")
	    WebElement stateInput;

	    @FindBy(id = "customer.address.zipCode")
	    WebElement zipCodeInput;

	    @FindBy(id = "customer.phoneNumber")
	    WebElement phoneInput;

	    @FindBy(id = "customer.ssn")
	    WebElement ssnInput;

	    @FindBy(id = "customer.username")
	    WebElement usernameInput;

	    @FindBy(id = "customer.password")
	    WebElement passwordInput;

	    @FindBy(id = "repeatedPassword")
	    WebElement confirmPasswordInput;

	    @FindBy(xpath = "//input[@value='Register']")
	    WebElement registerBtn;

	    @FindBy(xpath = "//h1[@class='title' and contains(text(), 'Welcome')]")
	    WebElement successMessage;
	  //h1[@class='title __web-inspector-hide-shortcut__']/text()
	    
	    public void enterFirstName(String firstName) {
	        firstNameInput.clear();
	        firstNameInput.sendKeys(firstName);
	    }

	    public void enterLastName(String lastName) {
	        lastNameInput.clear();
	        lastNameInput.sendKeys(lastName);
	    }

	    public void enterAddress(String address) {
	        addressInput.clear();
	        addressInput.sendKeys(address);
	    }

	    public void enterCity(String city) {
	        cityInput.clear();
	        cityInput.sendKeys(city);
	    }

	    public void enterState(String state) {
	        stateInput.clear();
	        stateInput.sendKeys(state);
	    }

	    public void enterZipCode(String zip) {
	        zipCodeInput.clear();
	        zipCodeInput.sendKeys(zip);
	    }

	    public void enterPhone(String phone) {
	        phoneInput.clear();
	        phoneInput.sendKeys(phone);
	    }

	    public void enterSSN(String ssn) {
	        ssnInput.clear();
	        ssnInput.sendKeys(ssn);
	    }

	    public void enterUsername(String username) {
	        usernameInput.clear();
	        usernameInput.sendKeys(username);
	    }

	    public void enterPassword(String password) {
	        passwordInput.clear();
	        passwordInput.sendKeys(password);
	    }

	    public void enterConfirmPassword(String confirmPassword) {
	        confirmPasswordInput.clear();
	        confirmPasswordInput.sendKeys(confirmPassword);
	    }

	    public void clickRegisterButton() {
	        registerBtn.click();
	    }

	    public boolean isSuccessMessageDisplayed() {
	        return successMessage.isDisplayed();

	    }
	    public String getSuccessMessageText() {
			return successMessage.getText();
	    	
	    }
}

