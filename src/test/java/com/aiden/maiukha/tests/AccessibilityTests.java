package com.aiden.maiukha.tests;

import com.aiden.maiukha.base.BaseTest;
import com.aiden.maiukha.config.ConfigReader;
import com.aiden.maiukha.pages.LoginPage;
import com.aiden.maiukha.pages.SignUpPage;
import com.aiden.maiukha.utils.TestDataUtil;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

public class AccessibilityTests extends BaseTest {

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

    @Test(dataProvider = "pages", groups = "accessibility", description = "TC_A11Y_001 - <html> has a lang attribute")
    public void htmlHasLang(String which) {
        openPage(which);
        String lang = page.locator("html").getAttribute("lang");
        Assert.assertTrue(lang != null && !lang.isBlank(), "<html> has no lang attribute (screen readers need it)");
    }

    @Test(dataProvider = "pages", groups = "accessibility", description = "TC_A11Y_002 - Page has a meaningful title")
    public void pageHasTitle(String which) {
        openPage(which);
        String title = page.title().trim();
        Assert.assertFalse(title.isEmpty(), "Page title is empty");
        Assert.assertFalse(title.equalsIgnoreCase("vite app") || title.equalsIgnoreCase("react app"),
                "Page still has the default framework title: " + title);
    }

    @Test(dataProvider = "pages", groups = "accessibility", description = "TC_A11Y_003 - Every input has a label (not just a placeholder)")
    public void inputsHaveLabels(String which) {
        openPage(which);
        List<?> bad = (List<?>) page.evaluate(
            "() => Array.from(document.querySelectorAll('input:not([type=hidden]):not([type=submit]):not([type=button])'))"
          + ".filter(el => !(el.labels && el.labels.length) && !el.getAttribute('aria-label') && !el.getAttribute('aria-labelledby'))"
          + ".map(el => el.outerHTML.substring(0, 120))");
        Assert.assertTrue(bad.isEmpty(), "Inputs without an accessible label: " + bad);
    }

    @Test(dataProvider = "pages", groups = "accessibility", description = "TC_A11Y_004 - Buttons have an accessible name")
    public void buttonsHaveNames(String which) {
        openPage(which);
        List<?> bad = (List<?>) page.evaluate(
            "() => Array.from(document.querySelectorAll('button'))"
          + ".filter(b => !(b.innerText || '').trim() && !b.getAttribute('aria-label') && !b.getAttribute('title'))"
          + ".map(b => b.outerHTML.substring(0, 120))");
        Assert.assertTrue(bad.isEmpty(), "Buttons without text / aria-label: " + bad);
    }

    @Test(dataProvider = "pages", groups = "accessibility", description = "TC_A11Y_005 - Images have alt text")
    public void imagesHaveAlt(String which) {
        openPage(which);
        List<?> bad = (List<?>) page.evaluate(
            "() => Array.from(document.querySelectorAll('img:not([alt])')).map(i => i.outerHTML.substring(0, 120))");
        Assert.assertTrue(bad.isEmpty(), "Images without alt attribute: " + bad);
    }

    @Test(groups = "accessibility", description = "TC_A11Y_006 - Keyboard: Tab moves from Email to Password")
    public void keyboardTabOrder() {
        LoginPage login = new LoginPage(page);
        login.open();
        login.emailInput().focus();
        page.keyboard().press("Tab");
        String focused = (String) page.evaluate("() => document.activeElement.type || document.activeElement.tagName");
        Assert.assertEquals(focused, "password", "Tab from email did not reach password field, reached: " + focused);
    }

    @Test(groups = "accessibility", description = "TC_A11Y_007 - Keyboard-only user can submit the form")
    public void keyboardOnlySubmit() {
        LoginPage login = new LoginPage(page);
        login.open();
        login.emailInput().focus();
        page.keyboard().type(TestDataUtil.uniqueEmail());
        page.keyboard().press("Tab");
        page.keyboard().type(TestDataUtil.WRONG_PASSWORD);
        page.keyboard().press("Enter");
        login.settle();
        Assert.assertTrue(login.hasFeedback(), "Keyboard-only submit gave no response");
    }

    @Test(groups = "accessibility", description = "TC_A11Y_008 - Focused field has a visible focus indicator")
    public void focusIndicatorVisible() {
        LoginPage login = new LoginPage(page);
        login.open();
        login.emailInput().focus();
        Boolean visible = (Boolean) page.evaluate(
            "() => { const s = getComputedStyle(document.activeElement);"
          + " return (s.outlineStyle !== 'none' && parseFloat(s.outlineWidth) > 0) || s.boxShadow !== 'none'"
          + " || s.borderColor !== getComputedStyle(document.body).borderColor; }");
        Assert.assertTrue(visible, "No visible focus indicator on the email field");
    }

    @Test(groups = "accessibility", description = "TC_A11Y_009 - Error messages are announced (role=alert / aria-live)")
    public void errorsAreAnnounced() {
        LoginPage login = new LoginPage(page);
        login.open();
        login.login(TestDataUtil.uniqueEmail(), TestDataUtil.WRONG_PASSWORD);
        int liveRegions = page.locator("[role='alert'], [aria-live], [role='status']").count();
        Assert.assertTrue(liveRegions > 0, "Error messages are not in an aria-live / role=alert region (screen readers will not announce them)");
    }

    @Test(dataProvider = "pages", groups = "accessibility",
          description = "TC_A11Y_010 - Automated axe-core scan (WCAG A/AA, includes colour contrast)")
    public void axeScan(String which) {
        openPage(which);
        try {
            page.addScriptTag(new Page.AddScriptTagOptions()
                    .setUrl("https://cdnjs.cloudflare.com/ajax/libs/axe-core/4.9.1/axe.min.js"));
        } catch (PlaywrightException e) {
            throw new SkipException("axe-core could not be loaded (blocked by CSP / no internet): " + e.getMessage());
        }
        List<?> violations = (List<?>) page.evaluate(
            "async () => { const r = await axe.run(); "
          + "return r.violations.map(v => v.impact + ' | ' + v.id + ' | ' + v.help + ' | elements: ' + v.nodes.length); }");
        Assert.assertTrue(violations.isEmpty(), "axe-core found " + violations.size() + " violation(s) on " + which + ": " + violations);
    }
}