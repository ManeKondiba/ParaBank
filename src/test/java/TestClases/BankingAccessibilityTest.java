package TestClases;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import PageObjects.BankingAccessibilityPage;
import TestBases.AccountFixture;
import TestBases.BaseClass;

public class BankingAccessibilityTest extends BaseClass {
    @DataProvider
    public Object[][] formRoutes() {
        return new Object[][] {{"index.htm", false}, {"register.htm", false}, {"lookup.htm", false},
                {"openaccount.htm", true}, {"transfer.htm", true}, {"billpay.htm", true},
                {"findtrans.htm", true}, {"updateprofile.htm", true}, {"requestloan.htm", true}};
    }
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