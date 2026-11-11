package com.aiden.maiukha.tests;

import com.aiden.maiukha.pages.LoginPage;
import com.aiden.maiukha.pages.SignUpPage;
import com.aiden.maiukha.utils.BrowserFactory;
import com.aiden.maiukha.utils.ScreenshotUtil;
import com.aiden.maiukha.utils.TestDataUtil;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

/** Runs the same checks in Chromium, Firefox and WebKit (Safari engine). */
public class CrossBrowserTests {

    @DataProvider(name = "browsers")
    public Object[][] browsers() {
        return new Object[][] {{"chromium"}, {"firefox"}, {"webkit"}};
    }

    @Test(dataProvider = "browsers", groups = {"compatibility", "regression"},
          description = "TC_COMPAT_001 - Login & Sign-up pages work in all browsers")
    public void pagesWorkInBrowser(String browserName) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = BrowserFactory.launch(playwright, browserName);
            BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1366, 768));
            Page page = context.newPage();
            SoftAssert sa = new SoftAssert();

            // ---- Login page ----
            LoginPage login = new LoginPage(page);
            login.open();
            ScreenshotUtil.capture(page, "compat_login_" + browserName);
            sa.assertTrue(login.emailInput().isVisible(), browserName + ": login email not visible");
            sa.assertTrue(login.passwordInput().isVisible(), browserName + ": login password not visible");
            sa.assertTrue(login.submitButton().isVisible(), browserName + ": login button not visible");

            // ---- Invalid login behaves the same ----
            login.login(TestDataUtil.uniqueEmail(), TestDataUtil.WRONG_PASSWORD);
            sa.assertTrue(login.isOnLoginPage(), browserName + ": user logged in with wrong credentials!");
            sa.assertTrue(login.hasFeedback(), browserName + ": no error message after wrong credentials");
            if (!login.hasFeedback()) {
                ScreenshotUtil.captureBug(page, "BUG_compat_no_error_" + browserName);
            }

            // ---- Sign-up page ----
            SignUpPage signUp = new SignUpPage(page);
            signUp.open();
            ScreenshotUtil.capture(page, "compat_signup_" + browserName);
            sa.assertTrue(signUp.emailInput().isVisible(), browserName + ": signup email not visible");
            sa.assertTrue(signUp.passwordInput().isVisible(), browserName + ": signup password not visible");
            sa.assertTrue(signUp.submitButton().isVisible(), browserName + ": signup button not visible");

            context.close();
            browser.close();
            sa.assertAll();
        }
    }
}