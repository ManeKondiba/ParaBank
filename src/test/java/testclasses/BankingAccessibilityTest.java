package testclasses;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pageobjects.BankingAccessibilityPage;
import base.AccountFixture;
import base.BaseClass;
import utilities.AppRoute;
import utilities.KnownDefect;

public class BankingAccessibilityTest extends BaseClass {
    @DataProvider
    public Object[][] formRoutes() {
        return new Object[][] {
                {AppRoute.INDEX, false},
                {AppRoute.REGISTER, false},
                {AppRoute.LOOKUP, false},
                {AppRoute.OPEN_ACCOUNT, true},
                {AppRoute.TRANSFER, true},
                {AppRoute.BILL_PAY, true},
                {AppRoute.FIND_TRANSACTIONS, true},
                {AppRoute.UPDATE_PROFILE, true},
                {AppRoute.REQUEST_LOAN, true}
        };
    }
    @KnownDefect(id = "PB-A11Y-001", description = "Visible banking form fields lack programmatic accessible names")
    @Test(dataProvider = "formRoutes", groups = {"Accessibility", "Master", "Regression", "ProductionRules"},
            description = "VAL-025: visible banking form fields have programmatic accessible names")
    public void testBankingFormFieldsHaveAccessibleNames(String route, boolean authenticated) {
        if (authenticated) { AccountFixture.register(getDriver()); }
        BankingAccessibilityPage page = new BankingAccessibilityPage(getDriver());
        page.open(route);
        var controls = page.visibleFormControls();
        Assert.assertFalse(controls.isEmpty(), "Expected banking form controls: " + route);
        SoftAssert checks = new SoftAssert();
        for (var control : controls) {
            checks.assertTrue(control.accessibleName() != null && !control.accessibleName().isBlank(),
                    "WCAG 1.3.1/4.1.2: missing accessible name for field " + control.field() + " on " + route);
        }
        checks.assertAll();
    }
}