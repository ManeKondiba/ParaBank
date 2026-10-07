package TestBases;

import java.net.URI;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;
import PageObjects.LoginPage;
import org.openqa.selenium.WebDriver;
import utilities.DriverFactory;
import utilities.FrameworkConfig;
import utilities.ProvisioningLock;
import utilities.api.ApiConfig;
import utilities.api.ApiEvidenceFilter;
import utilities.api.ApiRequestFactory;
import utilities.api.clients.AccountClient;
import utilities.api.clients.CustomerClient;
import static utilities.api.assertions.ApiAssertions.account;

/** HTTP provisioning for the transfer UI pilot; the tested transfer stays in Selenium. */
public final class UiApiFixture {
    private UiApiFixture() { }

    public static boolean enabled(FrameworkConfig config) {
        String mode = config.get("ui.fixture.mode");
        if (!mode.equals("ui") && !mode.equals("api")) {
            throw new IllegalArgumentException("ui.fixture.mode must be ui or api");
        }
        if (mode.equals("api")) {
            validateDeployment(config, new ApiConfig(config));
            return true;
        }
        return false;
    }

    public static void validateDeployment(FrameworkConfig config, ApiConfig api) {
        api.requireControlledEnvironment();
        URI web = URI.create(api.webBaseUrl());
        URI app = DriverFactory.httpUri(config.get("appUrl"), "appUrl");
        String context = web.getPath();
        if (!app.getScheme().equalsIgnoreCase(web.getScheme())
                || !app.getHost().equalsIgnoreCase(web.getHost())
                || port(app) != port(web)
                || !(app.getPath().equals(context) || app.getPath().startsWith(context + "/"))) {
            throw new IllegalArgumentException("API-backed UI fixtures require appUrl and api.baseUrl "
                    + "to target the same origin and application context");
        }
    }

    private static int port(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : uri.getScheme().equalsIgnoreCase("https") ? 443 : 80;
    }

    public static BankingFixture.Accounts prepareTransferAccounts(WebDriver driver) {
        FrameworkConfig config = FrameworkConfig.load();
        ApiConfig api = new ApiConfig(config);
        validateDeployment(config, api);
        ApiFixture fixture;
        String destination;
        long started = System.nanoTime();
        ApiEvidenceFilter.begin("UI-TransferFunds-provisioning");
        try {
            // Shares the UI provisioning lock; never replay a failed registration or POST.
            synchronized (ProvisioningLock.MONITOR) {
                ApiRequestFactory requests = new ApiRequestFactory(api);
                AccountClient accounts = new AccountClient(requests);
                fixture = ApiFixture.provision(api, requests, new CustomerClient(requests), accounts);
                var response = account(accounts.create(fixture.customer().id(), 1, fixture.fundingAccount().id()));
                var created = account(accounts.get(response.id()));
                Assert.assertEquals(created.customerId(), fixture.customer().id(), "Savings account owner");
                Assert.assertEquals(created.type(), "SAVINGS");
                Assert.assertTrue(created.balance().signum() > 0, "Destination must be funded");
                var source = account(accounts.get(fixture.fundingAccount().id()));
                Assert.assertEquals(source.customerId(), fixture.customer().id(), "Funding account owner");
                Assert.assertNotEquals(source.id(), created.id(), "Transfer needs distinct accounts");
                Assert.assertTrue(source.balance().signum() > 0, "Transfer source must be funded");
                destination = Integer.toString(created.id());
            }
        } finally {
            ITestResult result = Reporter.getCurrentTestResult();
            if (result != null) {
                if (ApiEvidenceFilter.currentFile() != null) {
                    result.setAttribute("api.evidence.path", ApiEvidenceFilter.currentFile().toString());
                }
                result.setAttribute("ui.fixture.seconds", (System.nanoTime() - started) / 1_000_000_000.0);
            }
            ApiEvidenceFilter.clear();
        }
        LoginPage login = new LoginPage(driver);
        login.login(fixture.username(), fixture.password());
        Assert.assertTrue(login.isLoginSuccessDisplayed(), "API-provisioned customer must log in through the UI");
        return new BankingFixture.Accounts(Integer.toString(fixture.fundingAccount().id()), destination);
    }
}
