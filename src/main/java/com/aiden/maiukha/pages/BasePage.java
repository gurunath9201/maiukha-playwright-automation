package com.aiden.maiukha.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;

/**
 * Common code for all pages.
 * If a field is not found on the real site, fix the selectors HERE.
 */
public abstract class BasePage {

    // ====== CENTRAL LOCATORS (edit these if the site uses different attributes) ======
    protected static final String EMAIL_SEL =
            "input[type='email'], input[name*='email' i], input[id*='email' i], "
          + "input[placeholder*='email' i], input[autocomplete='username'], input[name*='user' i]";
    protected static final String PASSWORD_SEL = "input[type='password']";
    protected static final String ERROR_SEL =
            "[role='alert'], [aria-live='assertive'], .error, .error-message, .alert, .alert-danger, "
          + "[class*='error' i], [class*='toast' i], [data-sonner-toast], [class*='invalid' i], [class*='danger' i]";
    // ================================================================================

    protected final Page page;

    protected BasePage(Page page) {
        this.page = page;
    }

    protected Response open(String url) {
        return page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    }

    /** SPA pages render after load, so wait for the first form field. */
    protected void waitForForm() {
        try {
            page.locator(EMAIL_SEL).first().waitFor(new Locator.WaitForOptions().setTimeout(15000));
        } catch (PlaywrightException e) {
            System.out.println("WARN: form field not detected - check locators in BasePage");
        }
    }

    /** Wait until the page calms down after a click/submit. */
    public void settle() {
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(5000));
        } catch (PlaywrightException ignored) {
            // some apps keep polling - that is fine
        }
        page.waitForTimeout(500);
    }

    public Locator emailInput()    { return page.locator(EMAIL_SEL).first(); }
    public Locator passwordInput() { return page.locator(PASSWORD_SEL).first(); }

    public String url()      { return page.url(); }
    public String bodyText() { return page.locator("body").innerText(); }

    /** Text of all visible error/toast/alert elements. */
    public String errorText() {
        Locator all = page.locator(ERROR_SEL);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < all.count(); i++) {
            Locator item = all.nth(i);
            if (item.isVisible()) {
                String text = item.innerText().trim();
                if (!text.isEmpty()) {
                    sb.append(text).append(" | ");
                }
            }
        }
        return sb.toString();
    }

    /** Browser built-in (HTML5) validation message of a field, e.g. "Please fill out this field". */
    public String validationMessage(Locator field) {
        if (field.count() == 0) {
            return "";
        }
        Object value = field.first().evaluate("el => el.validationMessage || ''");
        return value == null ? "" : value.toString();
    }

    /** Any feedback the user can see: custom error text + HTML5 validation bubbles. */
    public String feedbackText() {
        return (errorText() + " " + validationMessage(emailInput())
                + " " + validationMessage(passwordInput())).trim();
    }
}