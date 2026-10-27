package utilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Enterprise Smart Retry Analyzer.
 * Retries transient infrastructure/browser exceptions while strictly rejecting retries on
 * deterministic assertion errors (genuine defects or rule violations).
 */
public class SmartRetryAnalyzer implements IRetryAnalyzer {
    private static final Logger LOG = LogManager.getLogger(SmartRetryAnalyzer.class);
    private static final int MAX_RETRIES = 1;

    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        Throwable throwable = result != null ? result.getThrowable() : null;

        // Guardrail: Never retry genuine assertion errors or known defect checks
        if (throwable instanceof AssertionError) {
            LOG.debug("Assertion failure in {}.{} - skipping retry to preserve defect integrity",
                    getTestClassName(result), getTestMethodName(result));
            return false;
        }

        if (retryCount < MAX_RETRIES) {
            retryCount++;
            LOG.warn("Transient failure in {}.{} ({}: {}) - Retrying ({}/{})",
                    getTestClassName(result),
                    getTestMethodName(result),
                    throwable != null ? throwable.getClass().getSimpleName() : "Unknown",
                    throwable != null ? throwable.getMessage() : "no message",
                    retryCount, MAX_RETRIES);
            return true;
        }

        return false;
    }

    private String getTestClassName(ITestResult result) {
        if (result != null && result.getTestClass() != null && result.getTestClass().getRealClass() != null) {
            return result.getTestClass().getRealClass().getSimpleName();
        }
        return "UnknownClass";
    }

    private String getTestMethodName(ITestResult result) {
        if (result != null && result.getMethod() != null) {
            return result.getMethod().getMethodName();
        }
        return "unknownMethod";
    }
}
