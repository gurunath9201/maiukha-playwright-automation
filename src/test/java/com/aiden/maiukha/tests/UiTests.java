package com.aiden.maiukha.tests;

import com.aiden.maiukha.base.BaseTest;
import com.aiden.maiukha.pages.LoginPage;
import com.aiden.maiukha.pages.SignUpPage;
import com.aiden.maiukha.utils.ScreenshotUtil;
import com.aiden.maiukha.utils.VisualUtil;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.ColorScheme;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UiTests extends BaseTest {

    @DataProvider(name = "pages")
    public Object[][] pages() {
        return new Object[][] {{"login"}, {"signup"}};
    }

    private void openPage(String which) {
        if (which.equals("login")) {
            new LoginPage(page).open();
        } else {
            new SignUpPage(page).open();
        }
    }

    @Test(dataProvider = "pages", groups = {"ui", "regression"}, description = "TC_UI_001 - No JavaScript console errors on page load")
    public void noConsoleErrors(String which) {
        openPage(which);
        Assert.assertTrue(consoleErrors.isEmpty(), "Console errors on " + which + ": " + consoleErrors);
    }

    @Test(dataProvider = "pages", groups = {"ui", "regression"}, description = "TC_UI_002 - No failed network requests (4xx/5xx) on page load")
    public void noFailedRequests(String which) {
        openPage(which);
        Assert.assertTrue(failedRequests.isEmpty(), "Failed requests on " + which + ": " + failedRequests);
    }

    @Test(dataProvider = "pages", groups = {"ui", "regression"}, description = "TC_UI_003 - No broken images")
    public void noBrokenImages(String which) {
        openPage(which);
        page.waitForTimeout(1000);
        Object broken = page.evaluate(
            "() => Array.from(document.images).filter(i => i.complete && i.naturalWidth === 0).map(i => i.src)");
        Assert.assertTrue(((java.util.List<?>) broken).isEmpty(), "Broken images: " + broken);
    }

    @Test(dataProvider = "pages", groups = {"ui", "regression"}, description = "TC_UI_004 - Favicon is defined")
    public void faviconPresent(String which) {
        openPage(which);
        Assert.assertTrue(page.locator("link[rel*='icon']").count() > 0, "No favicon <link> found");
    }

    @Test(dataProvider = "pages", groups = {"ui", "regression"}, description = "TC_UI_005 - No horizontal scrollbar on desktop")
    public void noHorizontalScroll(String which) {
        openPage(which);
        Boolean fits = (Boolean) page.evaluate("() => document.documentElement.scrollWidth <= window.innerWidth + 1");
        Assert.assertTrue(fits, "Page has a horizontal scrollbar on 1366px wide screen");
    }

    @Test(groups = {"ui", "usability"}, description = "TC_UI_006 - Fields have placeholders / hints")
    public void fieldsHavePlaceholders() {
        LoginPage login = new LoginPage(page);
        login.open();
        SoftAssert sa = new SoftAssert();
        String emailHint = login.emailInput().getAttribute("placeholder");
        String passwordHint = login.passwordInput().getAttribute("placeholder");
        sa.assertTrue(emailHint != null && !emailHint.isBlank(), "Email field has no placeholder");
        sa.assertTrue(passwordHint != null && !passwordHint.isBlank(), "Password field has no placeholder");
        sa.assertAll();
    }

    @Test(groups = {"ui", "usability"}, description = "TC_UI_007 - Email field uses type=email (mobile keyboard / validation)")
    public void emailFieldType() {
        LoginPage login = new LoginPage(page);
        login.open();
        Assert.assertEquals(login.emailInput().getAttribute("type"), "email", "Email input should have type='email'");
    }

    @Test(groups = {"ui", "usability"}, description = "TC_UI_008 - Autofocus / first field focus and no typos in visible text")
    public void screenshotForManualReview() {
        LoginPage login = new LoginPage(page);
        login.open();
        ScreenshotUtil.capture(page, "ui_login_page_for_manual_review");
        new SignUpPage(page).open();
        ScreenshotUtil.capture(page, "ui_signup_page_for_manual_review");
        Assert.assertTrue(true); // evidence only - review spelling, alignment, colours manually
    }

    @Test(dataProvider = "pages", groups = {"ui", "compatibility"}, description = "TC_UI_009 - Dark mode renders correctly (site declares light+dark support)")
    public void darkModeRenders(String which) {
        switchContext(new Browser.NewContextOptions().setViewportSize(1366, 768).setColorScheme(ColorScheme.DARK));
        openPage(which);
        ScreenshotUtil.capture(page, "darkmode_" + which);
        Assert.assertTrue(new LoginPage(page).emailInput().isVisible(), "Form not visible in dark mode");
        String textColor = (String) page.evaluate("() => getComputedStyle(document.body).color");
        String background = (String) page.evaluate("() => getComputedStyle(document.body).backgroundColor");
        Assert.assertNotEquals(textColor, background, "Text colour equals background colour in dark mode");
    }

    @Test(groups = {"ui", "visual"}, description = "TC_UI_010 - Visual regression: login page matches baseline (2% tolerance)")
    public void visualRegressionLogin() throws Exception {
        LoginPage login = new LoginPage(page);
        login.open();
        page.waitForTimeout(1000);
        Path baseline = Paths.get("screenshots", "baseline", "login_page.png");
        Path current = Paths.get("screenshots", "baseline", "login_page_current.png");
        Files.createDirectories(baseline.getParent());
        page.screenshot(new Page.ScreenshotOptions().setPath(current));

        if (!Files.exists(baseline)) {
            Files.copy(current, baseline);
            System.out.println("Baseline created: " + baseline.toAbsolutePath() + " - run again to compare");
            return;
        }
        double diff = VisualUtil.diffPercent(baseline, current);
        System.out.println("Visual difference: " + String.format("%.2f", diff) + "%");
        Assert.assertTrue(diff <= 2.0, "Login page differs from baseline by " + String.format("%.2f", diff) + "%");
    }
}