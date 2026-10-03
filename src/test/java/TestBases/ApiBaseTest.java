package TestBases;

import java.lang.reflect.Method;

import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import utilities.FrameworkConfig;
import utilities.api.ApiConfig;
import utilities.api.ApiEvidenceFilter;
import utilities.api.ApiRequestFactory;
import utilities.api.clients.AccountClient;
import utilities.api.clients.CustomerClient;
import utilities.api.clients.LoanClient;
import utilities.api.clients.TransactionClient;

/** HTTP lifecycle only. Deliberately independent of Selenium's BaseClass. */
public abstract class ApiBaseTest {
    protected ApiConfig apiConfig;
    protected ApiRequestFactory requests;
    protected CustomerClient customers;
    protected AccountClient accounts;
    protected TransactionClient transactions;
    protected LoanClient loans;
    protected ApiFixture fixture;

    @BeforeSuite(alwaysRun = true)
    public void resetApiFixtureLeases() {
        ApiFixture.resetLeases();
    }

    @BeforeMethod(alwaysRun = true)
    public void setUpApi(Method method) {
        ApiEvidenceFilter.begin(method.getDeclaringClass().getSimpleName() + "-" + method.getName());
        apiConfig = ApiConfig.load();
        apiConfig.requireControlledEnvironment();
        requests = new ApiRequestFactory(apiConfig);
        customers = new CustomerClient(requests);
        accounts = new AccountClient(requests);
        transactions = new TransactionClient(requests);
        loans = new LoanClient(requests);
        fixture = ApiFixture.provision(apiConfig, requests, customers, accounts);
    }

    @AfterMethod(alwaysRun = true)
    public void finishApi(ITestResult result) {
        if (ApiEvidenceFilter.currentFile() != null) {
            result.setAttribute("api.evidence.path", ApiEvidenceFilter.currentFile().toString());
        }
        ApiEvidenceFilter.clear();
        fixture = null;
    }

    protected int expectedStatus(String key) {
        int status = FrameworkConfig.load().getInt(key);
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException(key + " must be an HTTP status");
        }
        return status;
    }
}
