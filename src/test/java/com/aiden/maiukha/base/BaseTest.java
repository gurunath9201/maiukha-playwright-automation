package com.aiden.maiukha.base;

import com.aiden.maiukha.config.ConfigReader;
import com.aiden.maiukha.utils.BrowserFactory;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Parent of every test class.
 * - 1 browser per class, 1 fresh context + page per test (clean state each time)
 * - records console errors and failed network calls for every test
 */
public class BaseTest {

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;
    protected String browserName;

    protected final List<String> consoleErrors = new ArrayList<>();
    protected final List<String> failedRequests = new ArrayList<>();
    private final List<BrowserContext> extraContexts = new ArrayList<>();

    @Parameters("browser")
    @BeforeClass(alwaysRun = true)
    public void launchBrowser(@Optional("") String browserParam) {
        playwright = Playwright.create();
        browserName = browserParam.isEmpty() ? ConfigReader.get("browser") : browserParam;
        browser = BrowserFactory.launch(playwright, browserName);
    }

    @BeforeMethod(alwaysRun = true)
    public void createContext() {
        consoleErrors.clear();
        failedRequests.clear();
        context = browser.newContext(defaultContextOptions());
        context.setDefaultTimeout(ConfigReader.getInt("timeout"));
        page = context.newPage();
        attachListeners(page);
    }

    @AfterMethod(alwaysRun = true)
    public void closeContext() {
        for (BrowserContext c : extraContexts) {
            c.close();
        }
        extraContexts.clear();
        if (context != null) {
            context.close();
        }
    }

    @AfterClass(alwaysRun = true)
    public void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    protected Browser.NewContextOptions defaultContextOptions() {
        return new Browser.NewContextOptions().setViewportSize(1366, 768);
    }

    /** Replace the current page by a new one with different options (viewport, dark mode, ...). */
    protected Page switchContext(Browser.NewContextOptions options) {
        extraContexts.add(context);
        context = browser.newContext(options);
        context.setDefaultTimeout(ConfigReader.getInt("timeout"));
        page = context.newPage();
        attachListeners(page);
        return page;
    }

    private void attachListeners(Page p) {
        p.onConsoleMessage(msg -> {
            if ("error".equals(msg.type())) {
                consoleErrors.add(msg.text());
            }
        });
        p.onPageError(err -> consoleErrors.add("PAGE ERROR: " + err));
        p.onResponse(r -> {
            if (r.status() >= 400) {
                failedRequests.add(r.status() + " " + r.url());
            }
        });
    }

    protected void skipIfNoCredentials() {
        if (!ConfigReader.hasValidCredentials()) {
            throw new SkipException("Set valid.email and valid.password in config.properties to run this test");
        }
    }

    public Page getPage() {
        return page;
    }
}