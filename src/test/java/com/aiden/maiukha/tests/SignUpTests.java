package com.aiden.maiukha.tests;

import com.aiden.maiukha.base.BaseTest;
import com.aiden.maiukha.config.ConfigReader;
import com.aiden.maiukha.pages.LoginPage;
import com.aiden.maiukha.pages.SignUpPage;
import com.aiden.maiukha.utils.ScreenshotUtil;
import com.aiden.maiukha.utils.TestDataUtil;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class SignUpTests extends BaseTest {

    private SignUpPage signUp;

    @BeforeMethod(alwaysRun = true)
    public void openSignUpPage() {
        signUp = new SignUpPage(page);
        signUp.open();
    }

    private void assertSignUpRejected(String scenario) {
        SoftAssert sa = new SoftAssert();
        sa.assertTrue(signUp.isOnSignUpPage(), "Account must NOT be created -> " + scenario);
        sa.assertTrue(signUp.hasFeedback(), "No error / validation message shown -> " + scenario);
        sa.assertAll();
    }

    // ======================= POSITIVE =======================

    @Test(groups = {"functional", "regression"}, description = "TC_SIGNUP_001 - Sign-up page loads")
    public void signUpPageLoads() {
        SoftAssert sa = new SoftAssert();
        sa.assertTrue(signUp.isOnSignUpPage() || signUp.emailInput().isVisible(), "Sign-up page did not load");
        sa.assertFalse(page.title().trim().isEmpty(), "Page title is empty");
        sa.assertAll();
    }

    @Test(groups = {"functional", "regression"}, description = "TC_SIGNUP_002 - All sign-up form elements are displayed")
    public void signUpFormElementsDisplayed() {
        SoftAssert sa = new SoftAssert();
        sa.assertTrue(signUp.emailInput().isVisible(), "Email field missing");
        sa.assertTrue(signUp.passwordInput().isVisible(), "Password field missing");
        sa.assertTrue(signUp.submitButton().isVisible(), "Sign-up button missing");
        sa.assertTrue(signUp.loginLink().count() > 0, "Link back to Sign in missing");
        sa.assertAll();
    }

    @Test(groups = {"functional", "smoke", "regression"}, description = "TC_SIGNUP_003 - Register with valid data")
    public void registerWithValidData() {
        signUp.register(TestDataUtil.VALID_NAME, TestDataUtil.uniqueEmail(), TestDataUtil.STRONG_PASSWORD);
        ScreenshotUtil.capture(page, "signup_valid_result");
        Assert.assertTrue(signUp.isSuccess(), "Registration did not succeed. Feedback: " + signUp.feedbackText());
    }

    @Test(dataProvider = "validNames", groups = {"functional", "equivalence"},
          description = "TC_SIGNUP_004 - Valid names (apostrophe, accents, unicode) are accepted")
    public void registerWithSpecialNames(String name) {
        signUp.register(name, TestDataUtil.uniqueEmail(), TestDataUtil.STRONG_PASSWORD);
        Assert.assertTrue(signUp.isSuccess(), "Valid name [" + name + "] was rejected. Feedback: " + signUp.feedbackText());
    }

    @DataProvider(name = "validNames")
    public Object[][] validNames() {
        return new Object[][] {{"O'Brien-Smith"}, {"José Núñez"}, {"李雷"}, {"Dr. Anna Maria"}};
    }

    @Test(groups = {"functional", "regression"}, description = "TC_SIGNUP_005 - Enter key submits the form")
    public void enterKeySubmitsForm() {
        signUp.fillForm(TestDataUtil.VALID_NAME, TestDataUtil.uniqueEmail(), TestDataUtil.STRONG_PASSWORD, TestDataUtil.STRONG_PASSWORD);
        signUp.acceptTermsIfPresent();
        signUp.passwordInput().press("Enter");
        signUp.settle();
        Assert.assertTrue(signUp.isSuccess() || signUp.hasFeedback(), "Pressing Enter did nothing");
    }

    @Test(groups = {"functional", "regression"}, description = "TC_SIGNUP_006 - 'Sign in' link opens the login page")
    public void loginLinkNavigates() {
        signUp.loginLink().click();
        signUp.settle();
        Assert.assertTrue(page.url().contains(ConfigReader.get("login.path")), "Did not reach login page");
    }

    @Test(groups = {"functional", "regression"}, description = "TC_SIGNUP_007 - Password is masked")
    public void passwordFieldIsMasked() {
        Assert.assertEquals(signUp.passwordInput().getAttribute("type"), "password", "Password is NOT masked");
    }

    // ======================= NEGATIVE =======================

    @Test(groups = {"negative", "regression"}, description = "TC_SIGNUP_008 - Duplicate email is rejected")
    public void duplicateEmailRejected() {
        String email = TestDataUtil.uniqueEmail();
        signUp.register(TestDataUtil.VALID_NAME, email, TestDataUtil.STRONG_PASSWORD);
        if (!signUp.isSuccess()) {
            throw new SkipException("First registration failed, cannot test duplicate: " + signUp.feedbackText());
        }
        signUp.open();
        signUp.register(TestDataUtil.VALID_NAME, email, TestDataUtil.STRONG_PASSWORD);
        assertSignUpRejected("duplicate email " + email);
    }

    @Test(groups = {"negative", "regression"}, description = "TC_SIGNUP_009 - Duplicate email with different letter case is rejected")
    public void duplicateEmailDifferentCaseRejected() {
        String email = TestDataUtil.uniqueEmail();
        signUp.register(TestDataUtil.VALID_NAME, email, TestDataUtil.STRONG_PASSWORD);
        if (!signUp.isSuccess()) {
            throw new SkipException("First registration failed: " + signUp.feedbackText());
        }
        signUp.open();
        signUp.register(TestDataUtil.VALID_NAME, email.toUpperCase(), TestDataUtil.STRONG_PASSWORD);
        assertSignUpRejected("same email in UPPERCASE " + email.toUpperCase());
    }

    @Test(groups = {"negative", "regression"}, description = "TC_SIGNUP_010 - All fields empty")
    public void allFieldsEmpty() {
        signUp.clickSubmit();
        assertSignUpRejected("all fields empty");
    }

    @Test(groups = {"negative", "regression"}, description = "TC_SIGNUP_011 - Name empty")
    public void emptyName() {
        if (!signUp.hasNameField()) {
            throw new SkipException("No name field on the form");
        }
        signUp.register("", TestDataUtil.uniqueEmail(), TestDataUtil.STRONG_PASSWORD);
        assertSignUpRejected("empty name");
    }

    @Test(groups = {"negative", "regression"}, description = "TC_SIGNUP_012 - Email empty")
    public void emptyEmail() {
        signUp.register(TestDataUtil.VALID_NAME, "", TestDataUtil.STRONG_PASSWORD);
        assertSignUpRejected("empty email");
    }

    @Test(groups = {"negative", "regression"}, description = "TC_SIGNUP_013 - Password empty")
    public void emptyPassword() {
        signUp.register(TestDataUtil.VALID_NAME, TestDataUtil.uniqueEmail(), "");
        assertSignUpRejected("empty password");
    }

    @Test(dataProvider = "invalidEmails", groups = {"negative", "equivalence"},
          description = "TC_SIGNUP_014 - Invalid email formats are rejected")
    public void invalidEmailFormats(String badEmail) {
        signUp.register(TestDataUtil.VALID_NAME, badEmail, TestDataUtil.STRONG_PASSWORD);
        assertSignUpRejected("invalid email [" + badEmail + "]");
    }

    @DataProvider(name = "invalidEmails")
    public Object[][] invalidEmails() {
        return new Object[][] {
            {"plainaddress"}, {"@nodomain.com"}, {"user@"}, {"user@@domain.com"},
            {"user name@domain.com"}, {"user@.com"}, {"user@domain..com"}
        };
    }

    @Test(dataProvider = "weakPasswords", groups = {"negative", "equivalence"},
          description = "TC_SIGNUP_015 - Weak passwords are rejected (assumes a password policy exists)")
    public void weakPasswordsRejected(String weak) {
        signUp.register(TestDataUtil.VALID_NAME, TestDataUtil.uniqueEmail(), weak);
        assertSignUpRejected("weak password [" + weak + "]");
    }

    @DataProvider(name = "weakPasswords")
    public Object[][] weakPasswords() {
        return new Object[][] {{"1"}, {"123456"}, {"password"}, {"aaaaaaaa"}, {"        "}};
    }

    @Test(groups = {"negative", "regression"}, description = "TC_SIGNUP_016 - Password and Confirm Password do not match")
    public void passwordMismatch() {
        if (!signUp.hasConfirmPassword()) {
            throw new SkipException("No confirm-password field");
        }
        signUp.fillForm(TestDataUtil.VALID_NAME, TestDataUtil.uniqueEmail(), TestDataUtil.STRONG_PASSWORD, "Different@123");
        signUp.acceptTermsIfPresent();
        signUp.clickSubmit();
        assertSignUpRejected("password mismatch");
    }

    @Test(groups = {"negative", "regression"}, description = "TC_SIGNUP_017 - Terms checkbox is mandatory")
    public void termsMustBeAccepted() {
        if (signUp.termsCheckbox().count() == 0) {
            throw new SkipException("No terms checkbox on the form");
        }
        signUp.fillForm(TestDataUtil.VALID_NAME, TestDataUtil.uniqueEmail(), TestDataUtil.STRONG_PASSWORD, TestDataUtil.STRONG_PASSWORD);
        signUp.clickSubmit();
        assertSignUpRejected("terms not accepted");
    }

    // ======================= BOUNDARY VALUE ANALYSIS =======================

    @Test(dataProvider = "passwordLengths", groups = {"boundary", "regression"},
          description = "TC_SIGNUP_018 - Password length boundaries (assumed minimum = 8)")
    public void passwordLengthBoundaries(String password, boolean shouldBeAccepted) {
        signUp.register(TestDataUtil.VALID_NAME, TestDataUtil.uniqueEmail(), password);
        if (shouldBeAccepted) {
            Assert.assertTrue(signUp.isSuccess(), "Password of length " + password.length() + " should be ACCEPTED. " + signUp.feedbackText());
        } else {
            assertSignUpRejected("password length " + password.length() + " should be rejected");
        }
    }

    @DataProvider(name = "passwordLengths")
    public Object[][] passwordLengths() {
        return new Object[][] {
            {"Aa1!aaa", false},                         // 7 chars  (below min)
            {"Aa1!aaaa", true},                         // 8 chars  (min)
            {"Aa1!aaaaa", true},                        // 9 chars  (min + 1)
            {"Aa1!" + TestDataUtil.repeat('a', 60), true}  // 64 chars
        };
    }

    @Test(dataProvider = "nameLengths", groups = {"boundary", "regression"},
          description = "TC_SIGNUP_019 - Name length boundaries are handled gracefully (no crash / 5xx)")
    public void nameLengthBoundaries(int length) {
        signUp.register(TestDataUtil.repeat('A', length), TestDataUtil.uniqueEmail(), TestDataUtil.STRONG_PASSWORD);
        SoftAssert sa = new SoftAssert();
        sa.assertTrue(failedRequests.stream().noneMatch(r -> r.startsWith("5")), "Server error (5xx) for name length " + length + ": " + failedRequests);
        sa.assertTrue(signUp.isSuccess() || signUp.hasFeedback(), "No success and no error message for name length " + length);
        sa.assertAll();
    }

    @DataProvider(name = "nameLengths")
    public Object[][] nameLengths() {
        return new Object[][] {{1}, {2}, {50}, {255}, {256}, {1000}};
    }

    @Test(groups = {"boundary", "regression"}, description = "TC_SIGNUP_020 - Very long email (300 chars) handled gracefully")
    public void veryLongEmail() {
        signUp.register(TestDataUtil.VALID_NAME, TestDataUtil.repeat('a', 300) + "@example.com", TestDataUtil.STRONG_PASSWORD);
        Assert.assertTrue(failedRequests.stream().noneMatch(r -> r.startsWith("5")), "Server error (5xx): " + failedRequests);
        assertSignUpRejected("300-char email");
    }

    // ======================= END-TO-END =======================

    @Test(groups = {"e2e", "regression"}, description = "TC_E2E_001 - Register a new user, then sign in with it")
    public void registerThenLogin() {
        String email = TestDataUtil.uniqueEmail();
        signUp.register(TestDataUtil.VALID_NAME, email, TestDataUtil.STRONG_PASSWORD);
        if (!signUp.isSuccess()) {
            throw new SkipException("Registration failed so login cannot be tested: " + signUp.feedbackText());
        }
        LoginPage login = new LoginPage(page);
        login.open();
        login.login(email, TestDataUtil.STRONG_PASSWORD);
        ScreenshotUtil.capture(page, "e2e_register_then_login");
        Assert.assertFalse(login.isOnLoginPage(),
                "Newly registered user cannot sign in (if e-mail verification is required this is expected). Feedback: " + login.feedbackText());
    }
}