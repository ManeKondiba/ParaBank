package TestClases;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import PageObjects.AccountDetailsPage;
import PageObjects.AccountsOverviewPage;
import PageObjects.RequestLoanPage;
import PageObjects.RequestLoanPage.Decision;
import TestBases.AccountFixture;
import TestBases.BaseClass;

public class RequestLoanTest extends BaseClass {
    private static final Set<String> DENIAL_REASONS = Set.of(
            "You do not have sufficient funds for the given down payment.",
            "We cannot grant a loan in that amount with your available funds.",
            "We cannot grant a loan in that amount with the given down payment.",
            "We cannot grant a loan in that amount with your available funds and down payment.");

    @Test(groups = {"RequestLoan", "Banking", "Master", "Regression"})
    public void testLoanFundingAccountsMatchCustomerAccounts() {
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        List<String> customerAccounts = overview.getAccountIds();

        RequestLoanPage loan = new RequestLoanPage(getDriver());
        loan.open();
        Assert.assertEquals(new HashSet<>(loan.getFundingAccountIds()), new HashSet<>(customerAccounts),
                "The loan funding selector must contain the registered customer's accounts");
    }

    @Test(groups = {"RequestLoan", "Banking", "Master", "Regression"})
    public void testLoanDecisionMatchesAccountState() {
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        List<String> accountsBefore = overview.getAccountIds();
        Assert.assertEquals(accountsBefore.size(), 1,
                "This loan scenario requires a fresh customer with one funding account");
        String sourceAccountId = accountsBefore.get(0);
        BigDecimal sourceBalanceBefore = overview.getBalance(sourceAccountId);
        Assert.assertTrue(sourceBalanceBefore.signum() > 0,
                "A positive initial account balance is required for the valid loan scenario");
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal downPayment = sourceBalanceBefore.min(new BigDecimal("10.00"));

        RequestLoanPage loan = new RequestLoanPage(getDriver());
        loan.open();
        loan.apply(amount, downPayment, sourceAccountId);
        assertDecisionMetadata(loan);

        // ParaBank's loan provider, processor and threshold are administrator settings.
        // Check the complete state transition for the returned decision without assuming a threshold.
        if (loan.getDecision() == Decision.APPROVED) {
            Assert.assertTrue(loan.isApprovalDisplayed());
            Assert.assertFalse(loan.isDenialDisplayed());
            String loanAccountId = loan.getNewAccountId();
            Assert.assertFalse(accountsBefore.contains(loanAccountId),
                    "Approval must create a new account");

            loan.openLoanAccount();
            AccountDetailsPage details = new AccountDetailsPage(getDriver());
            Assert.assertEquals(details.getAccountId(), loanAccountId,
                    "The result link must open the approved loan account");
            Assert.assertEquals(details.getAccountType(), "LOAN");
            Assert.assertEquals(details.getBalance().compareTo(amount), 0,
                    "The loan account balance must match the requested amount");

            overview.open();
            Set<String> expectedAccounts = new HashSet<>(accountsBefore);
            expectedAccounts.add(loanAccountId);
            Assert.assertEquals(new HashSet<>(overview.getAccountIds()), expectedAccounts,
                    "Approval must add exactly one loan account to this customer");
            Assert.assertEquals(overview.getBalance(sourceAccountId)
                    .compareTo(sourceBalanceBefore.subtract(downPayment)), 0,
                    "Approval must debit the chosen funding account by the down payment");
        } else {
            assertDenial(loan);
            Assert.assertTrue(DENIAL_REASONS.contains(loan.getDenialMessage()),
                    "A denied loan must give a supported business reason: " + loan.getDenialMessage());
            assertNoFinancialChange(overview, accountsBefore, sourceAccountId, sourceBalanceBefore);
        }
    }

    @Test(groups = {"RequestLoan", "Banking", "Master", "Regression"})
    public void testDownPaymentAboveAvailableFundsIsDeniedWithoutDebit() {
        AccountFixture.register(getDriver());
        AccountsOverviewPage overview = new AccountsOverviewPage(getDriver());
        overview.open();
        List<String> accountsBefore = overview.getAccountIds();
        Assert.assertEquals(accountsBefore.size(), 1,
                "The fresh customer's only account determines total available funds");
        String sourceAccountId = accountsBefore.get(0);
        BigDecimal sourceBalanceBefore = overview.getBalance(sourceAccountId);
        BigDecimal downPayment = sourceBalanceBefore.max(BigDecimal.ZERO).add(BigDecimal.ONE);
        BigDecimal amount = downPayment.add(new BigDecimal("100.00"));

        RequestLoanPage loan = new RequestLoanPage(getDriver());
        loan.open();
        loan.apply(amount, downPayment, sourceAccountId);
        assertDecisionMetadata(loan);
        Assert.assertEquals(loan.getDecision(), Decision.DENIED,
                "The built-in loan processors reject down payments above total available funds");
        assertDenial(loan);
        Assert.assertEquals(loan.getDenialMessage(),
                "You do not have sufficient funds for the given down payment.");
        assertNoFinancialChange(overview, accountsBefore, sourceAccountId, sourceBalanceBefore);
    }

    private void assertDecisionMetadata(RequestLoanPage loan) {
        Assert.assertEquals(loan.getResultHeading(), "Loan Request Processed");
        Assert.assertFalse(loan.getProviderName().isBlank(), "The decision must identify the loan provider");
        LocalDate.parse(loan.getResponseDate(), DateTimeFormatter.ofPattern("MM-d-uuuu")
                .withResolverStyle(ResolverStyle.STRICT));
    }

    private void assertDenial(RequestLoanPage loan) {
        Assert.assertTrue(loan.isDenialDisplayed());
        Assert.assertFalse(loan.isApprovalDisplayed());
        Assert.assertFalse(loan.isNewAccountLinkDisplayed(),
                "A denied loan must not expose a newly created account");
    }

    private void assertNoFinancialChange(AccountsOverviewPage overview, List<String> accountsBefore,
            String sourceAccountId, BigDecimal sourceBalanceBefore) {
        overview.open();
        Assert.assertEquals(new HashSet<>(overview.getAccountIds()), new HashSet<>(accountsBefore),
                "A denied loan must not create or remove customer accounts");
        Assert.assertEquals(overview.getBalance(sourceAccountId).compareTo(sourceBalanceBefore), 0,
                "A denied loan must not debit the funding account");
    }
}
