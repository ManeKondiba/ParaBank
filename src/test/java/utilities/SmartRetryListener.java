package utilities;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

/**
 * Automatically binds SmartRetryAnalyzer to all test methods across suites
 * without requiring boilerplate retryAnalyzer declarations on every @Test.
 */
public class SmartRetryListener implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        if (annotation.getRetryAnalyzerClass() == null) {
            annotation.setRetryAnalyzer(SmartRetryAnalyzer.class);
        }
    }
}
