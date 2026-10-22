package utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import base.BaseClass;
import utilities.api.ApiEvidenceFilter;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.testng.IExecutionListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

/** One report per execution; each invocation owns its node, including parallel data rows. */
public final class ExtentReportManager implements ITestListener, IExecutionListener, IInvokedMethodListener {
    private static final Logger LOG = LogManager.getLogger(ExtentReportManager.class);
    private static final ThreadLocal<ExtentTest> CURRENT_TEST = new ThreadLocal<>();
    private static final String TEST_NODE_ATTRIBUTE = "extent.node";
    private static final String SCREENSHOT_ATTRIBUTE = "failure.screenshot";
    private static final String BROWSER_DIAGNOSTICS_ATTRIBUTE = "failure.browser.diagnostics";
    private static final Path REPORT_DIRECTORY = Path.of("target", "reports");

    private ExtentReports extentReports;
    private int invocationSequence;

    public static void logStep(String message) {
        LOG.info(message);
        ExtentTest node = CURRENT_TEST.get();
        if (node != null) {
            node.info(message);
        }
    }

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
            reporter.config().setReportName("ParaBank automation results");

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
        String testMethod = result.getMethod().getMethodName();
        ThreadContext.put("testName", testMethod);
        LOG.info("=== STARTING TEST: {}.{} ===", result.getTestClass().getRealClass().getSimpleName(), testMethod);
        ExtentTest testNode = getOrCreateTestNode(result);
        CURRENT_TEST.set(testNode);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        try {
            LOG.info("=== TEST PASSED: {}.{} (Duration: {} ms) ===", result.getTestClass().getRealClass().getSimpleName(),
                    result.getMethod().getMethodName(), (result.getEndMillis() - result.getStartMillis()));
            attachApiEvidence(result);
            getOrCreateTestNode(result).pass("Passed");
        } finally {
            CURRENT_TEST.remove();
            ThreadContext.remove("testName");
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        try {
            KnownDefect defect = findKnownDefect(result);
            boolean isAssertion = result.getThrowable() instanceof AssertionError;
            String className = result.getTestClass().getRealClass().getSimpleName();
            String methodName = result.getMethod().getMethodName();
            String errorMsg = result.getThrowable() != null ? result.getThrowable().getMessage() : "Unknown error";

            if (defect != null && isAssertion) {
                LOG.error("=== APPLICATION DEFECT DETECTED [{}]: {} in {}.{} - Assertion: {} ===",
                        defect.id(), defect.description(), className, methodName, errorMsg);
            } else if (defect != null) {
                LOG.error("=== AUTOMATION ERROR IN DEFECT TEST [{}] {}.{}: {} ===",
                        defect.id(), className, methodName, errorMsg, result.getThrowable());
            } else {
                LOG.error("=== TEST FAILED: {}.{} - Error: {} ===", className, methodName, errorMsg, result.getThrowable());
            }

            captureFailureScreenshot(result);
            captureFailureDiagnostics(result);
            recordFailure(result);
        } finally {
            CURRENT_TEST.remove();
            ThreadContext.remove("testName");
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        try {
            LOG.warn("=== TEST SKIPPED: {}.{} ===", result.getTestClass().getRealClass().getSimpleName(), result.getMethod().getMethodName());
            attachApiEvidence(result);
            ExtentTest testNode = getOrCreateTestNode(result);
            if (result.getThrowable() == null) {
                testNode.skip("Skipped");
            } else {
                testNode.skip(result.getThrowable());
            }
        } finally {
            CURRENT_TEST.remove();
            ThreadContext.remove("testName");
        }
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult result) {
        if (method.isConfigurationMethod()) {
            LOG.debug("Starting configuration method: {}.{}", method.getTestMethod().getRealClass().getSimpleName(),
                    method.getTestMethod().getMethodName());
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (method.isConfigurationMethod()) {
            LOG.debug("Completed configuration method: {}.{} (Status: {})", method.getTestMethod().getRealClass().getSimpleName(),
                    method.getTestMethod().getMethodName(), result.getStatus());
        }
        // Runs before @AfterMethod quits the session, including setup failures.
        if (result.getStatus() == ITestResult.FAILURE) {
            captureFailureScreenshot(result);
            captureFailureDiagnostics(result);
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

    private static KnownDefect findKnownDefect(ITestResult result) {
        if (result.getMethod() != null && result.getMethod().getConstructorOrMethod() != null) {
            KnownDefect defect = result.getMethod().getConstructorOrMethod().getMethod().getAnnotation(KnownDefect.class);
            if (defect != null) {
                return defect;
            }
        }
        if (result.getTestClass() != null && result.getTestClass().getRealClass() != null) {
            return result.getTestClass().getRealClass().getAnnotation(KnownDefect.class);
        }
        return null;
    }

    private void recordFailure(ITestResult result) {
        attachApiEvidence(result);
        ExtentTest testNode = getOrCreateTestNode(result);
        KnownDefect defect = findKnownDefect(result);
        boolean isAssertion = result.getThrowable() instanceof AssertionError;

        if (defect != null && isAssertion) {
            testNode.assignCategory("App-Defect", defect.id());
            testNode.warning("<div style='background-color:#fff3cd; color:#856404; padding:8px 12px; border-radius:4px; margin-bottom:8px; border:1px solid #ffeeba;'>"
                    + "<strong>Known Application Defect [" + defect.id() + "]:</strong> " + defect.description()
                    + "<br/><small>Cataloged in DEFECT_REGISTER.md; verified application defect behavior.</small></div>");
        } else {
            testNode.assignCategory("Automation-Failure");
        }

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
        String diagnosticsPath = (String) result.getAttribute(BROWSER_DIAGNOSTICS_ATTRIBUTE);
        if (diagnosticsPath != null) {
            Path relativePath = REPORT_DIRECTORY.toAbsolutePath().normalize()
                    .relativize(Path.of(diagnosticsPath).toAbsolutePath().normalize());
            testNode.info("<a href=\"" + relativePath.toString().replace('\\', '/')
                    + "\">Sanitized browser diagnostics</a>");
        }
    }

    private void captureFailureDiagnostics(ITestResult result) {
        if (result.getAttribute(BROWSER_DIAGNOSTICS_ATTRIBUTE) != null) {
            return;
        }
        if (result.getInstance() instanceof BaseClass testInstance && testInstance.getDriverOrNull() != null) {
            try {
                String path = testInstance.captureFailureDiagnostics(result.getName());
                if (path != null) {
                    result.setAttribute(BROWSER_DIAGNOSTICS_ATTRIBUTE, path);
                }
            } catch (IOException | RuntimeException exception) {
                LOG.warn("Could not capture browser diagnostics for {}", result.getName(), exception);
            }
        }
    }

    private void attachApiEvidence(ITestResult result) {
        if (result.getAttribute("api.evidence.attached") != null) {
            return;
        }
        Path path = ApiEvidenceFilter.currentFile();
        if (path == null && result.getAttribute("api.evidence.path") instanceof String saved) {
            path = Path.of(saved);
        }
        if (path != null) {
            String relative = REPORT_DIRECTORY.toAbsolutePath().normalize()
                    .relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
            getOrCreateTestNode(result).info("<a href=\"" + relative + "\">Sanitized API request/response evidence</a>");
            if (result.getAttribute("ui.fixture.seconds") instanceof Double seconds) {
                getOrCreateTestNode(result).info(String.format(java.util.Locale.ROOT,
                        "HTTP fixture provisioning: %.3f seconds (includes lock wait)", seconds));
            }
            result.setAttribute("api.evidence.attached", true);
        }
    }
}
