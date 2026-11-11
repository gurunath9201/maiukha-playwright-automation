package com.aiden.maiukha.tests;

import com.aiden.maiukha.base.BaseTest;
import com.aiden.maiukha.config.ConfigReader;
import com.aiden.maiukha.pages.LoginPage;
import com.aiden.maiukha.utils.ScreenshotUtil;
import com.aiden.maiukha.utils.TestDataUtil;
import com.microsoft.playwright.Locator;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class LoginTests extends BaseTest {

    private LoginPage login;

    @BeforeMethod(alwaysRun = true)
    public void openLoginPage() {
        login = new LoginPage(page);
        login.open();
    }

    /** Common check for every "login must fail" scenario. */
    private void assertLoginRejected(String scenario) {
        SoftAssert sa = new SoftAssert();
        sa.assertTrue(login.isOnLoginPage(), "User must NOT be logged in -> " + scenario);
        sa.assertTrue(login.hasFeedback(), "No error / validation message shown -> " + scenario);
        sa.assertAll();
    }

    // ======================= POSITIVE =======================

    @Test(groups = {"functional", "regression"}, description = "TC_LOGIN_001 - Login page loads with correct URL and title")
    public void loginPageLoads() {
        SoftAssert sa = new SoftAssert();
        sa.assertTrue(login.url().contains(ConfigReader.get("login.path")), "URL should contain /login");
        sa.assertFalse(page.title().trim().isEmpty(), "Page title is empty");
        sa.assertAll();
    }

    @Test(groups = {"functional", "regression"}, description = "TC_LOGIN_002 - All login form elements are displayed")
    public void loginFormElementsDisplayed() {
        SoftAssert sa = new SoftAssert();
        sa.assertTrue(login.emailInput().isVisible(), "Email field missing");
        sa.assertTrue(login.passwordInput().isVisible(), "Password field missing");
        sa.assertTrue(login.submitButton().isVisible(), "Sign-in button missing");
        sa.assertTrue(login.signUpLink().count() > 0, "Link to Sign up missing");
        sa.assertAll();
    }

    @Test(groups = {"functional", "smoke", "regression"}, description = "TC_LOGIN_003 - Login with valid credentials")
    public void loginWithValidCredentials() {
        skipIfNoCredentials();
        login.login(ConfigReader.get("valid.email"), ConfigReader.get("valid.password"));
        ScreenshotUtil.capture(page, "login_valid_result");
        Assert.assertFalse(login.isOnLoginPage(), "User was not redirected after valid login. Feedback: " + login.feedbackText());
    }

    @Test(groups = {"functional", "regression"}, description = "TC_LOGIN_004 - Email is case-insensitive (UPPERCASE email)")
    public void loginWithUppercaseEmail() {
        skipIfNoCredentials();
        login.login(ConfigReader.get("valid.email").toUpperCase(), ConfigReader.get("valid.password"));
        Assert.assertFalse(login.isOnLoginPage(), "Login failed with UPPERCASE email (emails should be case-insensitive)");
    }

    @Test(groups = {"functional", "regression"}, description = "TC_LOGIN_005 - Leading/trailing spaces in email are trimmed")
    public void loginWithSpacesAroundEmail() {
        skipIfNoCredentials();
        login.login("  " + ConfigReader.get("valid.email") + "  ", ConfigReader.get("valid.password"));
        Assert.assertFalse(login.isOnLoginPage(), "Login failed when email had surrounding spaces");
    }

    @Test(groups = {"functional", "regression"}, description = "TC_LOGIN_006 - Enter key submits the form")
    public void enterKeySubmitsForm() {
        login.emailInput().fill(TestDataUtil.uniqueEmail());
        login.passwordInput().fill(TestDataUtil.WRONG_PASSWORD);
        login.passwordInput().press("Enter");
        login.settle();
        Assert.assertTrue(login.hasFeedback(), "Pressing Enter did not submit the form / show any response");
    }

    // ======================= NEGATIVE =======================

    @Test(groups = {"negative", "regression"}, description = "TC_LOGIN_007 - Wrong password is rejected")
    public void loginWithWrongPassword() {
        String email = ConfigReader.hasValidCredentials() ? ConfigReader.get("valid.email") : TestDataUtil.uniqueEmail();
        login.login(email, TestDataUtil.WRONG_PASSWORD);
        assertLoginRejected("wrong password");
    }

    @Test(groups = {"negative", "regression"}, description = "TC_LOGIN_008 - Unregistered email is rejected")
    public void loginWithUnregisteredEmail() {
        login.login(TestDataUtil.uniqueEmail(), TestDataUtil.STRONG_PASSWORD);
        assertLoginRejected("unregistered email");
    }

    @Test(groups = {"negative", "regression"}, description = "TC_LOGIN_009 - Both fields empty")
    public void loginWithEmptyFields() {
        login.clickSubmit();
        assertLoginRejected("both fields empty");
    }

    @Test(groups = {"negative", "regression"}, description = "TC_LOGIN_010 - Email empty, password filled")
    public void loginWithEmptyEmail() {
        login.passwordInput().fill(TestDataUtil.STRONG_PASSWORD);
        login.clickSubmit();
        assertLoginRejected("empty email");
    }

    @Test(groups = {"negative", "regression"}, description = "TC_LOGIN_011 - Password empty, email filled")
    public void loginWithEmptyPassword() {
        login.emailInput().fill(TestDataUtil.uniqueEmail());
        login.clickSubmit();
        assertLoginRejected("empty password");
    }

    @Test(groups = {"negative", "regression"}, description = "TC_LOGIN_012 - Only spaces in both fields")
    public void loginWithOnlySpaces() {
        login.login("     ", "     ");
        assertLoginRejected("whitespace-only credentials");
    }

    @Test(dataProvider = "invalidEmails", groups = {"negative", "equivalence"},
          description = "TC_LOGIN_013 - Invalid email formats are rejected")
    public void loginWithInvalidEmailFormat(String badEmail) {
        login.login(badEmail, TestDataUtil.STRONG_PASSWORD);
        assertLoginRejected("invalid email format: [" + badEmail + "]");
    }

    @DataProvider(name = "invalidEmails")
    public Object[][] invalidEmails() {
        return new Object[][] {
            {"plainaddress"}, {"@nodomain.com"}, {"user@"}, {"user@@domain.com"},
            {"user name@domain.com"}, {"user@.com"}, {"user@domain..com"}
        };
    }

    // ======================= BOUNDARY =======================

    @Test(groups = {"boundary", "regression"}, description = "TC_LOGIN_014 - Very long email (300 chars) handled gracefully")
    public void loginWithVeryLongEmail() {
        login.login(TestDataUtil.repeat('a', 300) + "@example.com", TestDataUtil.STRONG_PASSWORD);
        SoftAssert sa = new SoftAssert();
        sa.assertTrue(failedRequests.stream().noneMatch(r -> r.startsWith("5")), "Server error (5xx) for long email: " + failedRequests);
        sa.assertTrue(login.isOnLoginPage(), "User must not be logged in");
        sa.assertAll();
    }

    @Test(groups = {"boundary", "regression"}, description = "TC_LOGIN_015 - Very long password (1000 chars) handled gracefully")
    public void loginWithVeryLongPassword() {
        login.login(TestDataUtil.uniqueEmail(), TestDataUtil.repeat('x', 1000));
        SoftAssert sa = new SoftAssert();
        sa.assertTrue(failedRequests.stream().noneMatch(r -> r.startsWith("5")), "Server error (5xx) for long password: " + failedRequests);
        sa.assertTrue(login.isOnLoginPage(), "User must not be logged in");
        sa.assertAll();
    }

    @Test(groups = {"boundary", "regression"}, description = "TC_LOGIN_016 - Unicode / emoji in credentials handled gracefully")
    public void loginWithUnicodeCharacters() {
        login.login("用户😀@example.com", "пароль🔒密码");
        Assert.assertTrue(login.isOnLoginPage(), "User must not be logged in with unicode junk credentials");
        Assert.assertTrue(failedRequests.stream().noneMatch(r -> r.startsWith("5")), "Server error (5xx): " + failedRequests);
    }

    // ======================= UI BEHAVIOUR / NAVIGATION =======================

    @Test(groups = {"functional", "regression"}, description = "TC_LOGIN_017 - Password is masked")
    public void passwordFieldIsMasked() {
        Assert.assertEquals(login.passwordInput().getAttribute("type"), "password", "Password is NOT masked");
    }

    @Test(groups = {"functional", "regression"}, description = "TC_LOGIN_018 - Show/hide password toggle works")
    public void passwordToggleWorks() {
        if (login.passwordToggle().count() == 0) {
            throw new SkipException("No show/hide password button on the page");
        }
        login.passwordInput().fill(TestDataUtil.STRONG_PASSWORD);
        login.passwordToggle().click();
        Locator visibleField = page.locator("input[value='" + TestDataUtil.STRONG_PASSWORD + "']").first();
        Assert.assertEquals(visibleField.getAttribute("type"), "text", "Password not revealed after clicking toggle");
        login.passwordToggle().click();
        Assert.assertEquals(visibleField.getAttribute("type"), "password", "Password not hidden again");
    }

    @Test(groups = {"functional", "regression"}, description = "TC_LOGIN_019 - 'Forgot password' link works")
    public void forgotPasswordLinkWorks() {
        Locator link = login.forgotPasswordLink();
        if (link.count() == 0) {
            throw new SkipException("No 'Forgot password' link on login page");
        }
        String before = page.url();
        link.click();
        login.settle();
        boolean navigated = !page.url().equals(before) || page.locator("[role='dialog']").count() > 0;
        Assert.assertTrue(navigated, "Forgot password link did nothing");
    }

    @Test(groups = {"functional", "regression"}, description = "TC_LOGIN_020 - 'Sign up' link opens the sign-up page")
    public void signUpLinkNavigates() {
        login.signUpLink().click();
        login.settle();
        Assert.assertFalse(login.isOnLoginPage(), "Still on login page after clicking Sign up");
    }

    @Test(groups = {"functional", "regression"}, description = "TC_LOGIN_021 - Page refresh keeps the login form working")
    public void refreshKeepsForm() {
        page.reload();
        login.settle();
        Assert.assertTrue(login.emailInput().isVisible() && login.passwordInput().isVisible(),
                "Form missing after refresh");
    }

    @Test(groups = {"functional", "regression"}, description = "TC_LOGIN_022 - Browser Back/Forward works")
    public void backForwardNavigation() {
        login.signUpLink().click();
        login.settle();
        page.goBack();
        login.settle();
        Assert.assertTrue(login.isOnLoginPage(), "Back button did not return to login");
        page.goForward();
        login.settle();
        Assert.assertFalse(login.isOnLoginPage(), "Forward button did not go to sign-up");
    }
}