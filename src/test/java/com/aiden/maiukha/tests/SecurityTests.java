package com.aiden.maiukha.tests;

import com.aiden.maiukha.base.BaseTest;
import com.aiden.maiukha.config.ConfigReader;
import com.aiden.maiukha.pages.LoginPage;
import com.aiden.maiukha.pages.SignUpPage;
import com.aiden.maiukha.utils.TestDataUtil;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.Cookie;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class SecurityTests extends BaseTest {

    @DataProvider(name = "xssPayloads")
    public Object[][] xssPayloads() {
        return new Object[][] {
            {"<script>alert('XSS')</script>"},
            {"\"><img src=x onerror=alert('XSS')>"},
            {"<svg/onload=alert('XSS')>"},
            {"javascript:alert('XSS')"}
        };
    }

    @DataProvider(name = "sqlPayloads")
    public Object[][] sqlPayloads() {
        return new Object[][] {{"' OR '1'='1"}, {"admin'--"}, {"\" OR \"\"=\""}, {"' OR 1=1; DROP TABLE users;--"}};
    }

    @Test(groups = "security", description = "TC_SEC_001 - Site is served over HTTPS and HTTP redirects to HTTPS")
    public void httpsEnforced() {
        Assert.assertTrue(ConfigReader.loginUrl().startsWith("https://"), "Login URL is not HTTPS");
        String httpUrl = ConfigReader.loginUrl().replaceFirst("^https://", "http://");
        page.navigate(httpUrl);
        Assert.assertTrue(page.url().startsWith("https://"), "HTTP was NOT redirected to HTTPS: " + page.url());
    }

    @Test(groups = "security", description = "TC_SEC_002 - Important security headers are present")
    public void securityHeadersPresent() {
        Response response = page.navigate(ConfigReader.loginUrl());
        Map<String, String> headers = response.headers();
        SoftAssert sa = new SoftAssert();
        sa.assertTrue(headers.containsKey("strict-transport-security"), "Missing header: Strict-Transport-Security (HSTS)");
        sa.assertTrue(headers.containsKey("x-content-type-options"), "Missing header: X-Content-Type-Options");
        sa.assertTrue(headers.containsKey("content-security-policy"), "Missing header: Content-Security-Policy");
        sa.assertTrue(headers.containsKey("x-frame-options")
                || headers.getOrDefault("content-security-policy", "").contains("frame-ancestors"),
                "Missing clickjacking protection: X-Frame-Options / CSP frame-ancestors");
        sa.assertTrue(headers.containsKey("referrer-policy"), "Missing header: Referrer-Policy");
        sa.assertAll();
    }

    @Test(groups = "security", description = "TC_SEC_003 - Cookies set by the site use the Secure flag")
    public void cookiesAreSecure() {
        page.navigate(ConfigReader.loginUrl());
        new LoginPage(page).settle();
        SoftAssert sa = new SoftAssert();
        for (Cookie cookie : context.cookies()) {
            sa.assertTrue(cookie.secure, "Cookie [" + cookie.name + "] is missing the Secure flag");
        }
        sa.assertAll();
    }

    @Test(dataProvider = "sqlPayloads", groups = "security", description = "TC_SEC_004 - SQL injection in login does not authenticate")
    public void sqlInjectionLogin(String payload) {
        LoginPage login = new LoginPage(page);
        login.open();
        login.login(payload, payload);
        SoftAssert sa = new SoftAssert();
        sa.assertTrue(login.isOnLoginPage(), "SQL injection payload logged the user in: " + payload);
        sa.assertTrue(failedRequests.stream().noneMatch(r -> r.startsWith("5")), "Server error (5xx) on SQL payload: " + failedRequests);
        sa.assertAll();
    }

    @Test(dataProvider = "xssPayloads", groups = "security", description = "TC_SEC_005 - XSS payload in login fields is not executed")
    public void xssInLogin(String payload) {
        AtomicBoolean dialogShown = new AtomicBoolean(false);
        page.onDialog(dialog -> { dialogShown.set(true); dialog.dismiss(); });
        LoginPage login = new LoginPage(page);
        login.open();
        login.login(payload, payload);
        Assert.assertFalse(dialogShown.get(), "XSS EXECUTED (alert popped up) with payload: " + payload);
        Assert.assertEquals(page.locator("img[src='x']").count(), 0, "Injected <img> was rendered in the DOM");
    }

    @Test(dataProvider = "xssPayloads", groups = "security", description = "TC_SEC_006 - XSS payload in sign-up name is not executed")
    public void xssInSignUp(String payload) {
        AtomicBoolean dialogShown = new AtomicBoolean(false);
        page.onDialog(dialog -> { dialogShown.set(true); dialog.dismiss(); });
        SignUpPage signUp = new SignUpPage(page);
        signUp.open();
        signUp.register(payload, TestDataUtil.uniqueEmail(), TestDataUtil.STRONG_PASSWORD);
        Assert.assertFalse(dialogShown.get(), "XSS EXECUTED (alert popped up) with payload: " + payload);
        Assert.assertEquals(page.locator("img[src='x']").count(), 0, "Injected <img> was rendered in the DOM");
    }

    @Test(groups = "security", description = "TC_SEC_007 - Password never appears in the URL after submit")
    public void passwordNotInUrl() {
        LoginPage login = new LoginPage(page);
        login.open();
        login.login(TestDataUtil.uniqueEmail(), TestDataUtil.WRONG_PASSWORD);
        SoftAssert sa = new SoftAssert();
        sa.assertFalse(page.url().contains(TestDataUtil.WRONG_PASSWORD), "Password leaked in URL: " + page.url());
        sa.assertFalse(page.url().toLowerCase().contains("password="), "Password parameter in URL: " + page.url());
        sa.assertAll();
    }

    @Test(groups = "security", description = "TC_SEC_008 - Protected page cannot be opened without login")
    public void protectedPageRequiresLogin() {
        page.navigate(ConfigReader.get("base.url") + ConfigReader.get("protected.path"));
        LoginPage login = new LoginPage(page);
        login.settle();
        boolean sentToLogin = page.url().contains(ConfigReader.get("login.path")) || login.passwordInput().isVisible();
        Assert.assertTrue(sentToLogin, "Protected page opened WITHOUT login: " + page.url()
                + " (check protected.path in config.properties)");
    }

    @Test(groups = "security", description = "TC_SEC_009 - Brute-force protection (rate limit / lock-out / captcha)")
    public void bruteForceProtection() {
        AtomicInteger tooManyRequests = new AtomicInteger();
        page.onResponse(r -> { if (r.status() == 429) tooManyRequests.incrementAndGet(); });
        LoginPage login = new LoginPage(page);
        login.open();
        String email = ConfigReader.hasValidCredentials() ? ConfigReader.get("valid.email") : TestDataUtil.uniqueEmail();
        for (int i = 1; i <= 8; i++) {
            login.login(email, TestDataUtil.WRONG_PASSWORD + i);
        }
        boolean lockMessage = Pattern.compile("too many|locked|try again later|captcha|rate limit|wait",
                Pattern.CASE_INSENSITIVE).matcher(login.bodyText()).find();
        boolean captcha = page.locator("iframe[src*='captcha' i], iframe[src*='recaptcha' i], [class*='captcha' i]").count() > 0;
        Assert.assertTrue(tooManyRequests.get() > 0 || lockMessage || captcha || login.isSubmitDisabled(),
                "8 wrong passwords in a row and NO lock-out / rate-limit / captcha was triggered (brute-force risk)");
    }

    @Test(groups = "security", description = "TC_SEC_010 - Error message does not reveal whether an email exists")
    public void noUserEnumeration() {
        skipIfNoCredentials();
        LoginPage login = new LoginPage(page);
        login.open();
        login.login(ConfigReader.get("valid.email"), TestDataUtil.WRONG_PASSWORD);
        String messageForExistingUser = login.errorText();

        login.open();
        login.login(TestDataUtil.uniqueEmail(), TestDataUtil.WRONG_PASSWORD);
        String messageForUnknownUser = login.errorText();

        Assert.assertEquals(messageForUnknownUser, messageForExistingUser,
                "Different error messages leak which emails are registered (user enumeration)");
    }
}