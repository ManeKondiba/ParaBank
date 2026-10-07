package utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.testng.IAlterSuiteListener;
import org.testng.IExecutionListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

/** Optional UI concurrency and timings without recording test parameters. */
public final class ExecutionTimingListener implements IAlterSuiteListener, IExecutionListener, IInvokedMethodListener {
    private static final String START = "timing.start.nanos";
    private final ConcurrentLinkedQueue<Timing> timings = new ConcurrentLinkedQueue<>();
    private long executionStart;
    private record Timing(String context, String method, String kind, int status, long nanos) { }

    @Override
    public void alter(List<XmlSuite> suites) {
        int workers;
        try {
            workers = Integer.parseInt(System.getProperty("ui.threads", "1"));
            if (workers < 1) { throw new NumberFormatException(); }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("ui.threads must be a positive integer", exception);
        }
        for (XmlSuite suite : suites) {
            if (suite.getName().equals("ParaBank UI regression")) {
                suite.setParallel(workers == 1 ? XmlSuite.ParallelMode.NONE : XmlSuite.ParallelMode.TESTS);
                suite.setThreadCount(workers);
            } else if (suite.getName().equals("ParaBank smoke")) {
                // Smoke has one XML test; keep each class's methods on the same worker.
                suite.setParallel(workers == 1 ? XmlSuite.ParallelMode.NONE : XmlSuite.ParallelMode.CLASSES);
                suite.setThreadCount(workers);
            }
        }
    }

    @Override
    public void onExecutionStart() {
        timings.clear();
        executionStart = System.nanoTime();
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult result) {
        result.setAttribute(START, System.nanoTime());
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (result.getAttribute(START) instanceof Long start) {
            timings.add(new Timing(result.getTestContext().getName(),
                    result.getTestClass().getName() + "." + result.getName(),
                    method.isTestMethod() ? "test" : "configuration", result.getStatus(), System.nanoTime() - start));
        }
    }

    @Override
    public void onExecutionFinish() {
        double wallSeconds = (System.nanoTime() - executionStart) / 1_000_000_000.0;
        StringBuilder csv = new StringBuilder("context,method,kind,status,duration_seconds\n");
        timings.stream().sorted(Comparator.comparingLong(Timing::nanos).reversed()).forEach(timing ->
                csv.append(quote(timing.context())).append(',').append(quote(timing.method())).append(',')
                        .append(timing.kind()).append(',').append(timing.status()).append(',')
                        .append(String.format(Locale.ROOT, "%.6f", timing.nanos() / 1_000_000_000.0)).append('\n'));
        try {
            Path directory = Path.of("target", "reports");
            Files.createDirectories(directory);
            String runId = UUID.randomUUID().toString();
            Path report = directory.resolve("execution-timing-" + runId + ".csv");
            Files.writeString(report, csv);
            String summary = String.format(Locale.ROOT,
                    "TestNG wall time: %.3f seconds%nInvoked tests: %d%nConfiguration invocations: %d%n"
                    + "Duration totals overlap during parallel execution. Status: 1=pass, 2=fail, 3=skip."
                    + "%nMaven build/download time is excluded.%n", wallSeconds,
                    timings.stream().filter(timing -> timing.kind().equals("test")).count(),
                    timings.stream().filter(timing -> timing.kind().equals("configuration")).count());
            Files.writeString(directory.resolve("execution-timing-" + runId + ".txt"), summary);
            System.out.println(summary + "Timing CSV: " + report);
        } catch (IOException exception) {
            System.err.println("Cannot write execution timings: " + exception.getMessage());
        }
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
