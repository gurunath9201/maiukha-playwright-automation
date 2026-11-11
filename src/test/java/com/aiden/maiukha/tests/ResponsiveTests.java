package com.aiden.maiukha.tests;

import com.aiden.maiukha.base.BaseTest;
import com.aiden.maiukha.pages.LoginPage;
import com.aiden.maiukha.pages.SignUpPage;
import com.aiden.maiukha.utils.ScreenshotUtil;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.options.BoundingBox;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class ResponsiveTests extends BaseTest {

    @DataProvider(name = "viewports")
    public Object[][] viewports() {
        return new Object[][] {
            // name,          width, height, mobile?
            {"iPhoneSE",       375,  667,  true},
            {"iPhone14",       390,  844,  true},
            {"AndroidMid",     412,  915,  true},
            {"Mobile_Landscape", 667, 375, true},
            {"iPad",           768, 1024,  true},
            {"Laptop",        1366,  768,  false},
            {"FullHD",        1920, 1080,  false}
        };
    }

    @Test(dataProvider = "viewports", groups = {"responsive", "regression"},
          description = "TC_RESP_001 - Login and Sign-up pages work on different screen sizes")
    public void pagesResponsive(String name, int width, int height, boolean mobile) {
        Browser.NewContextOptions options = new Browser.NewContextOptions().setViewportSize(width, height);
        if (mobile) {
            options.setHasTouch(true).setDeviceScaleFactor(2);
            if (!browserName.equals("firefox")) {          // Firefox does not support isMobile
                options.setIsMobile(true);
            }
        }
        switchContext(options);
        SoftAssert sa = new SoftAssert();

        // ---------- Login ----------
        LoginPage login = new LoginPage(page);
        login.open();
        ScreenshotUtil.capture(page, "responsive_login_" + name);
        sa.assertTrue(login.emailInput().isVisible(), name + " (login): email field not visible");
        sa.assertTrue(login.passwordInput().isVisible(), name + " (login): password field not visible");
        sa.assertTrue(login.submitButton().isVisible(), name + " (login): submit button not visible");
        sa.assertTrue(noHorizontalScroll(), name + " (login): horizontal scrollbar / content overflow");
        if (mobile) {
            BoundingBox box = login.submitButton().boundingBox();
            sa.assertTrue(box != null && box.height >= 40,
                    name + " (login): submit button too small to tap (" + (box == null ? "?" : box.height) + "px, recommended >= 44px)");
        }

        // ---------- Sign up ----------
        SignUpPage signUp = new SignUpPage(page);
        signUp.open();
        ScreenshotUtil.capture(page, "responsive_signup_" + name);
        sa.assertTrue(signUp.emailInput().isVisible(), name + " (signup): email field not visible");
        sa.assertTrue(signUp.passwordInput().isVisible(), name + " (signup): password field not visible");
        sa.assertTrue(signUp.submitButton().isVisible(), name + " (signup): submit button not visible");
        sa.assertTrue(noHorizontalScroll(), name + " (signup): horizontal scrollbar / content overflow");

        sa.assertAll();
    }

    private boolean noHorizontalScroll() {
        return (Boolean) page.evaluate("() => document.documentElement.scrollWidth <= window.innerWidth + 1");
    }
}