package TestClases;

import org.testng.Assert;
import org.testng.annotations.Test;

import PageObjects.LogOut;
import PageObjects.LoginPage;
import TestBases.BaseClass;
import utilities.DataProviders;

public class LoginDDTtest extends BaseClass {
	
	
	@Test(dataProvider="LoginData",dataProviderClass=DataProviders.class,groups="Datadriven")
	
	public void loginDDTest(String username, String password,String expected) {
		try {
		
		LoginPage lp=new LoginPage(driver);
		lp.enterUsername(username);
		lp.enterPassword(password);
		lp.clickLogin();
		
		
		LogOut logout=new LogOut(driver);
		boolean Targetlink=logout.isLoggedOutLinkExist();
		
		if(expected.equalsIgnoreCase("Valid")) 
		{
			if(Targetlink==true)
			logout.clickLogout();
			Assert.assertTrue(true);
			
		}
		else 
		{
			Assert.assertTrue(false);
			
		}
		
		if(expected.equalsIgnoreCase("Invalid")){
			if(Targetlink==true) {
				
				
				logout.clickLogout();
				Assert.assertTrue(false);
			}
			else
			{
				Assert.assertTrue(true);
			}
			
			
			
		}
		
			
		}catch(Exception e) {
			Assert.fail();
			}
		}
		
		
		
	}


