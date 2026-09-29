package TestClases;

import java.math.BigDecimal;

import PageObjects.AccountsOverviewPage;
import PageObjects.BillPayPage;
import PageObjects.BillPayPage.Payee;
import TestBases.AccountFixture;
import TestBases.BaseClass;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class BillPayTest extends BaseClass {
    private static final String PAYEE_ACCOUNT = "987654";
    private static final BigDecimal PAYMENT_AMOUNT = new BigDecimal("12.34");

    @Test(groups = {"BillPay", "Banking", "BankingSmoke", "Master", "Regression"})
    public void testPaymentConfirmationAndFundingAccountDebit() {
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        String fundingAccountId = overview.getAccountIds().get(0);
        BigDecimal balanceBefore = overview.getBalance(fundingAccountId);
        Assert.assertTrue(balanceBefore.compareTo(PAYMENT_AMOUNT) >= 0,
                "The test customer must have enough funds for the bill payment");

        BillPayPage billPay = new BillPayPage(getDriver());
        billPay.open();
        Assert.assertTrue(billPay.getFundingAccountIds().contains(fundingAccountId),
                "Bill Pay must offer the customer's account as a funding source");
        Payee payee = payee(PAYEE_ACCOUNT);
        billPay.fill(payee, PAYEE_ACCOUNT, PAYMENT_AMOUNT.toPlainString(), fundingAccountId);
        billPay.sendPayment();

        Assert.assertEquals(billPay.getConfirmationHeading(), "Bill Payment Complete");
        Assert.assertEquals(billPay.getConfirmedPayee(), payee.name());
        Assert.assertEquals(billPay.getConfirmedAccountId(), fundingAccountId);
        Assert.assertEquals(billPay.getConfirmedAmount().compareTo(PAYMENT_AMOUNT), 0);

        overview.open();
        Assert.assertEquals(overview.getBalance(fundingAccountId).compareTo(balanceBefore.subtract(PAYMENT_AMOUNT)),
                0, "A successful bill payment must debit the selected account by the exact amount");
    }

    @Test(groups = {"BillPay", "Banking", "Master", "Regression"})
    public void testEmptyPaymentShowsRequiredFieldsWithoutDebitingAccount() {
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        String fundingAccountId = overview.getAccountIds().get(0);
        BigDecimal balanceBefore = overview.getBalance(fundingAccountId);

        BillPayPage billPay = new BillPayPage(getDriver());
        billPay.open();
        billPay.sendPayment();

        String[][] expectedErrors = {
            {"name", "Payee name is required."},
            {"address", "Address is required."},
            {"city", "City is required."},
            {"state", "State is required."},
            {"zipCode", "Zip Code is required."},
            {"phoneNumber", "Phone number is required."},
            {"account-empty", "Account number is required."},
            {"verifyAccount-empty", "Account number is required."},
            {"amount-empty", "The amount cannot be empty."}
        };
        for (String[] expectedError : expectedErrors) {
            Assert.assertEquals(billPay.getValidationError(expectedError[0]), expectedError[1], expectedError[0]);
        }
        Assert.assertTrue(billPay.isFormDisplayed(), "Validation must leave the payment form available for correction");
        assertBalanceUnchanged(overview, fundingAccountId, balanceBefore);
    }

    @Test(groups = {"BillPay", "Banking", "Master", "Regression"})
    public void testMismatchedAccountConfirmationDoesNotDebitAccount() {
        assertInvalidPayment(PAYEE_ACCOUNT, "987655", PAYMENT_AMOUNT.toPlainString(),
                "verifyAccount-mismatch", "The account numbers do not match.");
    }

    @DataProvider
    public Object[][] invalidPaymentNumbers() {
        // Completely nonnumeric strings exercise the page's parseFloat validation.
        return new Object[][] {
            {"invalid", "invalid", "12.34", "account-invalid", "Please enter a valid number."},
            {PAYEE_ACCOUNT, "invalid", "12.34", "verifyAccount-invalid", "Please enter a valid number."},
            {PAYEE_ACCOUNT, PAYEE_ACCOUNT, "invalid", "amount-invalid", "Please enter a valid amount."}
        };
    }

    @Test(dataProvider = "invalidPaymentNumbers", groups = {"BillPay", "Banking", "Master", "Regression"})
    public void testNonnumericPaymentFieldsDoNotDebitAccount(String accountNumber, String accountConfirmation,
            String amount, String errorMarker, String expectedMessage) {
        assertInvalidPayment(accountNumber, accountConfirmation, amount, errorMarker, expectedMessage);
    }

    private void assertInvalidPayment(String accountNumber, String accountConfirmation, String amount,
            String errorMarker, String expectedMessage) {
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        String fundingAccountId = overview.getAccountIds().get(0);
        BigDecimal balanceBefore = overview.getBalance(fundingAccountId);

        BillPayPage billPay = new BillPayPage(getDriver());
        billPay.open();
        billPay.fill(payee(accountNumber), accountConfirmation, amount, fundingAccountId);
        billPay.sendPayment();

        Assert.assertEquals(billPay.getValidationError(errorMarker), expectedMessage);
        Assert.assertTrue(billPay.isFormDisplayed(), "Invalid payment data must leave the form available for correction");
        assertBalanceUnchanged(overview, fundingAccountId, balanceBefore);
    }

    private void assertBalanceUnchanged(AccountsOverviewPage overview, String accountId, BigDecimal balanceBefore) {
        overview.open();
        Assert.assertEquals(overview.getBalance(accountId).compareTo(balanceBefore), 0,
                "A rejected payment must not change the customer's balance");
    }

    private Payee payee(String accountNumber) {
        return new Payee("Automation Utilities", "456 Test Avenue", "Springfield", "IL", "62701",
                "5551234567", accountNumber);
    }
}
