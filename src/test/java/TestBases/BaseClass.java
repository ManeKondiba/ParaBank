package TestBases;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import utilities.DriverFactory;
import utilities.FrameworkConfig;

public class BaseClass {
    private static final Logger LOG = LogManager.getLogger(BaseClass.class);

    private final ThreadLocal<WebDriver> drivers = new ThreadLocal<>();

    @BeforeMethod(alwaysRun = true)
    @Parameters({"browser", "os"})
    public void setup(@Optional("") String browser, @Optional("") String os) {
        FrameworkConfig config = FrameworkConfig.load();
        String appUrl = DriverFactory.httpUri(config.get("appUrl"), "appUrl").toString();

        // Validate configuration before starting a browser process.
        Duration pageLoadTimeout = config.getDuration("timeout.pageLoad.seconds");
        Duration scriptTimeout = config.getDuration("timeout.script.seconds");
        config.getDuration("timeout.explicit.seconds");
        Dimension windowSize = new Dimension(config.getInt("window.width"), config.getInt("window.height"));

        WebDriver driver = DriverFactory.create(config, browser, os);
        drivers.set(driver);
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().timeouts().pageLoadTimeout(pageLoadTimeout);
        driver.manage().timeouts().scriptTimeout(scriptTimeout);
        driver.manage().window().setSize(windowSize);
        driver.get(appUrl);

        LOG.info("Started {} session on thread {}", config.parameter("browser", browser), Thread.currentThread().getId());
    }

    protected WebDriver getDriver() {
        WebDriver driver = getDriverOrNull();
        if (driver == null) {
            throw new IllegalStateException("No WebDriver session on this thread");
        }
        return driver;
    }

    public WebDriver getDriverOrNull() {
        return drivers.get();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        WebDriver driver = drivers.get();
        try {
            if (driver != null) {
                driver.quit();
            }
        } finally {
            drivers.remove();
        }
    }

    public String captureScreen(String testName) throws IOException {
        String safeName = testName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path screenshotDirectory = Path.of("screenshots").toAbsolutePath().normalize();
        Files.createDirectories(screenshotDirectory);

        Path screenshotPath = screenshotDirectory.resolve(safeName + "_" + UUID.randomUUID() + ".png");
        byte[] screenshot = ((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.BYTES);
        Files.write(screenshotPath, screenshot);
        LOG.info("Saved failure screenshot: {}", screenshotPath);
        return screenshotPath.toString();
    }
}
