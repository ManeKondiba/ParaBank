package TestBases;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Properties;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;


import io.github.bonigarcia.wdm.WebDriverManager;

public class BaseClass {
	
    public static WebDriver driver;       // This holds the real browser driver
    public Properties p;
	
	
	
	@BeforeClass(groups= {"Master","Sanity","Regression"})
	@Parameters({ "browser","os"})
	public void setup(@Optional("firefox")String browser,@Optional("windows")String os) throws IOException {
		
	FileReader file=new FileReader("./src/test/resources/config.properties");
	 p= new Properties();
	p.load(file);
		
	DesiredCapabilities capabilities=new DesiredCapabilities();
	
	/*switch(os.toLowerCase()) {
    case "windows":
    	capabilities.setPlatform(Platform.WINDOWS);
      break;
    case "mac":
    	capabilities.setPlatform(Platform.MAC);
      break;
    case "linux":
    	capabilities.setPlatform(Platform.LINUX);
      break;
    default:
    	capabilities.setPlatform(Platform.ANY);
    	}*/
	if(os.equalsIgnoreCase("windows"))
	{
		capabilities.setPlatform(Platform.WIN10);
	}
	else if(os.equalsIgnoreCase("linux"))
	{
		capabilities.setPlatform(Platform.LINUX);
		
	}
	else if (os.equalsIgnoreCase("mac"))
	{
		capabilities.setPlatform(Platform.MAC);
	}
	else
	{
		System.out.println("No matching os");
		return;
	}

	switch (browser.toLowerCase()) {
	
	case "chrome":
		WebDriverManager.chromedriver().setup();
		driver=new ChromeDriver(); break;
	case "firefox":
		 WebDriverManager.firefoxdriver().setup();
		 driver=new FirefoxDriver(); break;
	case "edge":
		WebDriverManager.edgedriver().setup();
		driver=new EdgeDriver(); break;
	default:System.out.println("Browser not supported");return;
	
	
	}
	
	driver.manage().deleteAllCookies();
	driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

	driver.get(p.getProperty("appUrl"));
	driver.manage().window().maximize();
	
	
		
	}
	
	@AfterClass(groups= {"Master","Sanity","Regression"})
	public void TearDown() {
		driver.quit();
	}	
	public String captureScreen(String tname) throws IOException {
		
		 
		 
		String timeStamp = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
				
		TakesScreenshot takesScreenshot = (TakesScreenshot) driver;
		File sourceFile = takesScreenshot.getScreenshotAs(OutputType.FILE);
		
		String targetFilePath=System.getProperty("user.dir")+"\\screenshots\\" + tname + "_" + timeStamp + ".png";
		File targetFile=new File(targetFilePath);
		
		sourceFile.renameTo(targetFile);
		//FileHandler.copy(sourceFile, targetFile); // safer than renameTo
			
		return targetFilePath;
		
	}

}
