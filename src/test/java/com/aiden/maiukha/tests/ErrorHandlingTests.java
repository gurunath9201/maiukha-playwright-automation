package com.aiden.maiukha.tests;

import com.aiden.maiukha.base.BaseTest;
import com.aiden.maiukha.pages.LoginPage;
import com.aiden.maiukha.utils.TestDataUtil;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.util.concurrent.atomic.AtomicInteger;

public class ErrorHandlingTests extends BaseTest {

    private LoginPage login;

    @BeforeMethod(alwaysRun = true)
    public void openLoginPage() {
        login = new LoginPage(page);
        login.open();
    }

    private void fillValidLookingCredentials() {
        login.emailInput().fill(TestDataUtil.uniqueEmail());
        login.passwordInput().fill(TestDataUtil.STRONG_PASSWORD);
    }

    @Test(groups = {"reliability", "negative"}, description = "TC_ERR_001 - User sees an error when the internet is offline")
    public void offlineSubmit() {
        fillValidLookingCredentials();
        context.setOffline(true);
        login.submitButton().click();
        page.waitForTimeout(2500);
        context.setOffline(false);

        SoftAssert sa = new SoftAssert();
        sa.assertFalse(login.bodyText().trim().isEmpty(), "Page went blank when offline");
        sa.assertFalse(login.errorText().isEmpty(), "No error message shown to the user when the network is down");
        sa.assertAll();
    }

    @Test(groups = {"reliability", "negative"}, description = "TC_ERR_002 - Server error (HTTP 500) is handled gracefully")
    public void serverError500() {
        page.route("**/*", route -> {
            if ("POST".equals(route.request().method())) {
                route.fulfill(new Route.FulfillOptions().setStatus(500)
                        .setContentType("application/json").setBody("{\"message\":\"Internal Server Error\"}"));
            } else {
                route.resume();
            }
        });
        fillValidLookingCredentials();
        login.submitButton().click();
        login.settle();

        SoftAssert sa = new SoftAssert();
        sa.assertFalse(login.bodyText().trim().isEmpty(), "Page went blank after a 500 error");
        sa.assertFalse(login.errorText().isEmpty(), "No error message shown after server error");
        sa.assertTrue(login.submitButton().isEnabled(), "Sign-in button stays disabled forever after server error");
        sa.assertTrue(login.isOnLoginPage(), "User must stay on the login page");
        sa.assertAll();
    }

    @Test(groups = {"reliability", "negative"}, description = "TC_ERR_003 - Too many requests (HTTP 429) is handled gracefully")
    public void rateLimit429() {
        page.route("**/*", route -> {
            if ("POST".equals(route.request().method())) {
                route.fulfill(new Route.FulfillOptions().setStatus(429)
                        .setContentType("application/json").setBody("{\"message\":\"Too many requests\"}"));
            } else {
                route.resume();
            }
        });
        fillValidLookingCredentials();
        login.submitButton().click();
        login.settle();
        Assert.assertFalse(login.errorText().isEmpty(), "No message shown to user after HTTP 429");
    }

    @Test(groups = {"reliability", "negative"}, description = "TC_ERR_004 - Request failure / timeout is handled gracefully")
    public void requestAborted() {
        page.route("**/*", route -> {
            if ("POST".equals(route.request().method())) {
                route.abort();
            } else {
                route.resume();
            }
        });
        fillValidLookingCredentials();
        login.submitButton().click();
        login.settle();

        SoftAssert sa = new SoftAssert();
        sa.assertFalse(login.bodyText().trim().isEmpty(), "Page went blank after failed request");
        sa.assertFalse(login.errorText().isEmpty(), "No error message shown after failed request");
        sa.assertAll();
    }

    @Test(groups = {"reliability", "negative"}, description = "TC_ERR_005 - Double-click on Sign in does not send duplicate requests")
    public void doubleClickSubmit() {
        AtomicInteger posts = new AtomicInteger();
        page.onRequest(request -> {
            if ("POST".equals(request.method())) {
                posts.incrementAndGet();
            }
        });
        fillValidLookingCredentials();
        Locator button = login.submitButton();
        button.click(new Locator.ClickOptions().setClickCount(2).setDelay(50));
        login.settle();
        Assert.assertTrue(posts.get() <= 1, "Double click sent " + posts.get() + " POST requests (button should disable while loading)");
    }

    @Test(groups = {"reliability"}, description = "TC_ERR_006 - Unknown URL shows a proper 404 / fallback page")
    public void unknownUrlHandled() {
        page.navigate(com.aiden.maiukha.config.ConfigReader.get("base.url") + "/this-page-does-not-exist-12345");
        login.settle();
        Assert.assertFalse(login.bodyText().trim().isEmpty(), "Unknown URL shows a completely blank page (no 404 page)");
    }
}