package TestClases;

import org.testng.annotations.Test;

import PageObjects.LoginPage;
import TestBases.BaseClass;

public class LoginTest extends BaseClass{

@Test(priority=0)
public void testValidLogin(){
	//Test Case For Valid Login
	LoginPage login=new LoginPage(driver);
	
	login.enterUsername("john");
	login.enterPassword("demo");
	login.clickLogin();
	
if(login.isLoginSuccessDisplayed()) {
	System.out.println("login success");
}
else {
	
	System.out.println("login Failed");
}
}



}
