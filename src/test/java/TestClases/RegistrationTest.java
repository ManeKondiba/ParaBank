package TestClases;

import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import PageObjects.HomePage;
import PageObjects.RegistrationPage;
import TestBases.BaseClass;

public class RegistrationTest extends BaseClass {
	
	@Test()
	
	 public void testRegistration() {
		 		
		
	try {
             HomePage hm=new HomePage(driver);
             
             hm.clickRegisterLink();
             
		 RegistrationPage registrationPage=new RegistrationPage(driver);
				 registrationPage.enterFirstName("John");
				 registrationPage.enterLastName("Doe");
				 registrationPage.enterAddress("123 Main St");
				 registrationPage.enterCity("Springfield");
				 registrationPage.enterState("IL");
				 registrationPage.enterZipCode("62701");
				 registrationPage.enterPhone("555-1234");
				 registrationPage.enterSSN("123-45-6789");
				 String uniqueUsername = "johndoe" + System.currentTimeMillis();
				 registrationPage.enterUsername(uniqueUsername);
				 registrationPage.enterPassword("password123");
				 registrationPage.enterConfirmPassword("password123");
				 registrationPage.clickRegisterButton();
				 registrationPage.isSuccessMessageDisplayed();
				 System.out.println(registrationPage.getSuccessMessageText());
				 Assert.assertTrue(registrationPage.getSuccessMessageText().contains("Welcome"));
				 
		 }
		 catch(Exception e) {
			 e.printStackTrace();
			 Assert.fail();
		 }
				 
				 
				 
				 
				 
				 
				 
				 
				 
				 
				 
				 
				 
				 
				 
				 
				 
				 
	 }
	
	
	

}
