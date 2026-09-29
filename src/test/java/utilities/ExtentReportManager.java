package utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import TestBases.BaseClass;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IExecutionListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

/** One report per execution; each invocation owns its node, including parallel data rows. */
public final class ExtentReportManager implements ITestListener, IExecutionListener, IInvokedMethodListener {
    private static final Logger LOG = LogManager.getLogger(ExtentReportManager.class);
    private static final String TEST_NODE_ATTRIBUTE = "extent.node";
    private static final String SCREENSHOT_ATTRIBUTE = "failure.screenshot";
    private static final Path REPORT_DIRECTORY = Path.of("target", "reports");

    private ExtentReports extentReports;
    private int invocationSequence;

    @Override
    public void onExecutionStart() {
        invocationSequence = 0;
        try {
            Files.createDirectories(REPORT_DIRECTORY);
            String runId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                    + "-" + UUID.randomUUID();
            Path reportPath = REPORT_DIRECTORY.resolve("ParaBank-" + runId + ".html");
            ExtentSparkReporter reporter = new ExtentSparkReporter(reportPath.toString());
            reporter.config().setDocumentTitle("ParaBank Automation");
            reporter.config().setReportName("ParaBank UI results");

            extentReports = new ExtentReports();
            extentReports.attachReporter(reporter);
            extentReports.setSystemInfo("Java", System.getProperty("java.version"));
            extentReports.setSystemInfo("OS", System.getProperty("os.name"));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create report directory", exception);
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        getOrCreateTestNode(result);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        getOrCreateTestNode(result).pass("Passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        captureFailureScreenshot(result);
        recordFailure(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest testNode = getOrCreateTestNode(result);
        if (result.getThrowable() == null) {
            testNode.skip("Skipped");
        } else {
            testNode.skip(result.getThrowable());
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        // Runs before @AfterMethod quits the session, including setup failures.
        if (result.getStatus() == ITestResult.FAILURE) {
            captureFailureScreenshot(result);
            if (method.isConfigurationMethod()) {
                recordFailure(result);
            }
        }
    }

    @Override
    public void onExecutionFinish() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }

    private synchronized ExtentTest getOrCreateTestNode(ITestResult result) {
        ExtentTest testNode = (ExtentTest) result.getAttribute(TEST_NODE_ATTRIBUTE);
        if (testNode == null) {
            String testName = result.getTestContext().getName() + " / "
                    + result.getTestClass().getName() + "." + result.getName()
                    + " [invocation " + ++invocationSequence + "]";
            testNode = extentReports.createTest(testName);
            testNode.assignCategory(result.getMethod().getGroups());
            result.setAttribute(TEST_NODE_ATTRIBUTE, testNode);
        }
        return testNode;
    }

    private void captureFailureScreenshot(ITestResult result) {
        if (result.getAttribute(SCREENSHOT_ATTRIBUTE) != null) {
            return;
        }

        if (result.getInstance() instanceof BaseClass testInstance && testInstance.getDriverOrNull() != null) {
            try {
                result.setAttribute(SCREENSHOT_ATTRIBUTE, testInstance.captureScreen(result.getName()));
            } catch (IOException | RuntimeException exception) {
                LOG.warn("Could not capture screenshot for {}", result.getName(), exception);
            }
        }
    }

    private void recordFailure(ITestResult result) {
        ExtentTest testNode = getOrCreateTestNode(result);
        if (result.getThrowable() == null) {
            testNode.fail("Failed without an exception");
        } else {
            testNode.fail(result.getThrowable());
        }

        String screenshotPath = (String) result.getAttribute(SCREENSHOT_ATTRIBUTE);
        if (screenshotPath != null) {
            Path relativePath = REPORT_DIRECTORY.toAbsolutePath().normalize()
                    .relativize(Path.of(screenshotPath).toAbsolutePath().normalize());
            testNode.addScreenCaptureFromPath(relativePath.toString().replace('\\', '/'));
        }
    }
}
