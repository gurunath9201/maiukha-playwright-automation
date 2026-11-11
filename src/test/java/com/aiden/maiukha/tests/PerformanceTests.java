package com.aiden.maiukha.tests;

import com.aiden.maiukha.base.BaseTest;
import com.aiden.maiukha.config.ConfigReader;
import com.aiden.maiukha.pages.LoginPage;
import com.aiden.maiukha.utils.TestDataUtil;
import com.google.gson.JsonObject;
import com.microsoft.playwright.CDPSession;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

public class PerformanceTests extends BaseTest {

    private long timedLoad(String url) {
        long start = System.currentTimeMillis();
        page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.LOAD));
        return System.currentTimeMillis() - start;
    }

    @Test(groups = "performance", description = "TC_PERF_001 - Login page loads within the time limit")
    public void loginPageLoadTime() {
        long ms = timedLoad(ConfigReader.loginUrl());
        System.out.println("Login page load time: " + ms + " ms");
        Assert.assertTrue(ms <= ConfigReader.getInt("max.load.time"),
                "Login page took " + ms + " ms (limit " + ConfigReader.get("max.load.time") + " ms)");
    }

    @Test(groups = "performance", description = "TC_PERF_002 - Sign-up page loads within the time limit")
    public void signUpPageLoadTime() {
        long ms = timedLoad(ConfigReader.signupUrl());
        System.out.println("Sign-up page load time: " + ms + " ms");
        Assert.assertTrue(ms <= ConfigReader.getInt("max.load.time"),
                "Sign-up page took " + ms + " ms (limit " + ConfigReader.get("max.load.time") + " ms)");
    }

    @Test(groups = "performance", description = "TC_PERF_003 - Average load time over 5 visits")
    public void averageLoadTime() {
        long total = 0;
        for (int i = 0; i < 5; i++) {
            total += timedLoad(ConfigReader.loginUrl());
        }
        long average = total / 5;
        System.out.println("Average login load time (5 runs): " + average + " ms");
        Assert.assertTrue(average <= ConfigReader.getInt("max.load.time"), "Average load time too high: " + average + " ms");
    }

    @Test(groups = "performance", description = "TC_PERF_004 - Page weight (transferred KB) is within the limit")
    public void pageWeight() {
        page.navigate(ConfigReader.loginUrl());
        page.waitForLoadState(LoadState.NETWORKIDLE);
        Number bytes = (Number) page.evaluate(
                "() => performance.getEntriesByType('resource').reduce((a, r) => a + (r.transferSize || 0), 0)");
        long kb = bytes.longValue() / 1024;
        System.out.println("Page weight: " + kb + " KB");
        Assert.assertTrue(kb <= ConfigReader.getInt("max.page.weight.kb"), "Page weight " + kb + " KB is too high");
    }

    @Test(groups = "performance", description = "TC_PERF_005 - Number of network requests is within the limit")
    public void numberOfRequests() {
        page.navigate(ConfigReader.loginUrl());
        page.waitForLoadState(LoadState.NETWORKIDLE);
        Number count = (Number) page.evaluate("() => performance.getEntriesByType('resource').length");
        System.out.println("Requests: " + count);
        Assert.assertTrue(count.intValue() <= ConfigReader.getInt("max.requests"), "Too many requests: " + count);
    }

    @Test(groups = "performance", description = "TC_PERF_006 - Login response time after clicking Sign in")
    public void loginResponseTime() {
        LoginPage login = new LoginPage(page);
        login.open();
        login.emailInput().fill(TestDataUtil.uniqueEmail());
        login.passwordInput().fill(TestDataUtil.WRONG_PASSWORD);
        long start = System.currentTimeMillis();
        login.submitButton().click();
        login.settle();
        long ms = System.currentTimeMillis() - start - 500; // minus the fixed 500 ms settle pause
        System.out.println("Login response time: " + ms + " ms");
        Assert.assertTrue(ms <= ConfigReader.getInt("max.load.time"), "Login took too long to respond: " + ms + " ms");
    }

    @Test(groups = "performance", description = "TC_PERF_007 - Page is usable on a slow 3G network (Chromium only)")
    public void slow3gLoad() {
        if (!browserName.equals("chromium") && !browserName.equals("chrome") && !browserName.equals("edge")) {
            throw new SkipException("Network throttling needs a Chromium based browser");
        }
        CDPSession cdp = context.newCDPSession(page);
        JsonObject conditions = new JsonObject();
        conditions.addProperty("offline", false);
        conditions.addProperty("latency", 400);                 // ms
        conditions.addProperty("downloadThroughput", 50 * 1024); // bytes/sec (~400 kbps)
        conditions.addProperty("uploadThroughput", 25 * 1024);
        cdp.send("Network.enable");
        cdp.send("Network.emulateNetworkConditions", conditions);

        long ms = timedLoad(ConfigReader.loginUrl());
        System.out.println("Slow 3G load time: " + ms + " ms");
        Assert.assertTrue(new LoginPage(page).emailInput().isVisible(), "Form not visible on slow network");
        Assert.assertTrue(ms <= 15000, "Slow 3G load took " + ms + " ms (limit 15000)");
    }
}