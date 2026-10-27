package testclasses;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.testng.Assert;
import org.testng.annotations.Test;

import pageobjects.BasePage;
import utilities.CustomerDataFactory;
import utilities.CustomerProfile;
import utilities.RegistrationData;

/**
 * Automated Architectural & AI Alignment Guardrail Tests.
 * Automatically halts the CI pipeline if an AI agent or developer introduces anti-patterns,
 * breaks page object encapsulation, or leaks sensitive PII/secrets.
 */
public class ArchitectureGuardTest {

    private static final Path SOURCE_ROOT = Path.of("src", "test", "java");

    @Test(groups = "Unit", description = "AI Guardrail: Prohibit arbitrary Thread.sleep() across test code")
    public void noThreadSleepAllowedInTestCode() throws IOException {
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            List<Path> violations = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("ArchitectureGuardTest.java"))
                    .filter(path -> {
                        try {
                            String content = Files.readString(path);
                            return content.contains("Thread.sleep(");
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .toList();

            Assert.assertTrue(violations.isEmpty(),
                    "Arbitrary Thread.sleep() is strictly prohibited. Violating files: " + violations);
        }
    }

    @Test(groups = "Unit", description = "AI Guardrail: Prohibit raw System.out.println in page objects")
    public void noRawSystemOutInPageObjects() throws IOException {
        Path pageObjects = SOURCE_ROOT.resolve("pageobjects");

        if (Files.exists(pageObjects)) {
            try (Stream<Path> paths = Files.walk(pageObjects)) {
                List<Path> violations = paths
                        .filter(path -> path.toString().endsWith(".java"))
                        .filter(path -> {
                            try {
                                String content = Files.readString(path);
                                return content.contains("System.out.println(");
                            } catch (IOException e) {
                                return false;
                            }
                        })
                        .toList();

                Assert.assertTrue(violations.isEmpty(),
                        "Direct System.out.println in PageObjects is prohibited; use BasePage logger. Violating files: " + violations);
            }
        }
    }

    @Test(groups = "Unit", description = "Architecture Guardrail: All Page Objects must extend BasePage")
    public void allPageObjectsMustExtendBasePage() {
        List<Class<?>> pageClasses = List.of(
                pageobjects.AccountDetailsPage.class,
                pageobjects.AccountsOverviewPage.class,
                pageobjects.BankingAccessibilityPage.class,
                pageobjects.BillPayPage.class,
                pageobjects.CustomerLookupPage.class,
                pageobjects.FindTransactionsPage.class,
                pageobjects.HomePage.class,
                pageobjects.LoginPage.class,
                pageobjects.LogoutPage.class,
                pageobjects.OpenAccountPage.class,
                pageobjects.ProtectedResourcePage.class,
                pageobjects.RegistrationPage.class,
                pageobjects.RequestLoanPage.class,
                pageobjects.TransactionDetailsPage.class,
                pageobjects.TransactionTable.class,
                pageobjects.TransferFundsPage.class,
                pageobjects.UpdateContactInfoPage.class
        );

        for (Class<?> pageClass : pageClasses) {
            if (!Modifier.isAbstract(pageClass.getModifiers())) {
                Assert.assertTrue(BasePage.class.isAssignableFrom(pageClass),
                        pageClass.getSimpleName() + " must extend " + BasePage.class.getSimpleName());
            }
        }
    }

    @Test(groups = "Unit", description = "Security Guardrail: Sensitive customer PII and credentials must be redacted in toString()")
    public void sensitiveCustomerDataMustBeRedactedInToString() {
        RegistrationData registration = new RegistrationData("testuser", "SuperSecret123!");
        Assert.assertFalse(registration.toString().contains("SuperSecret123!"),
                "RegistrationData toString() must not disclose plain password");
        Assert.assertTrue(registration.toString().contains("<redacted>"));

        CustomerProfile profile = CustomerDataFactory.builder()
                .password("ConfidentialPass!")
                .ssn("999-88-7777")
                .build();
        Assert.assertFalse(profile.toString().contains("ConfidentialPass!"),
                "CustomerProfile toString() must not disclose plain password");
        Assert.assertFalse(profile.toString().contains("999-88-7777"),
                "CustomerProfile toString() must not disclose plain SSN");
        Assert.assertTrue(profile.toString().contains("ssn=<redacted>"));
    }

    @Test(groups = "Unit", description = "Architecture Guardrail: Test classes must not directly instantiate raw WebDriver")
    public void noRawDriverInstantiationInTestClasses() throws IOException {
        Path testclasses = SOURCE_ROOT.resolve("testclasses");
        try (Stream<Path> paths = Files.walk(testclasses)) {
            List<Path> violations = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("ArchitectureGuardTest.java"))
                    .filter(path -> {
                        try {
                            String content = Files.readString(path);
                            return content.contains("new ChromeDriver(")
                                    || content.contains("new FirefoxDriver(")
                                    || content.contains("new EdgeDriver(");
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .toList();

            Assert.assertTrue(violations.isEmpty(),
                    "Test classes must obtain drivers via BaseClass / DriverFactory. Violating files: " + violations);
        }
    }
}
