package com.aiden.maiukha.pages;

import com.aiden.maiukha.config.ConfigReader;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;

import java.util.regex.Pattern;

public class LoginPage extends BasePage {

    public LoginPage(Page page) {
        super(page);
    }

    public Response open() {
        Response response = open(ConfigReader.loginUrl());
        waitForForm();
        return response;
    }

    // ---------- Locators ----------
    public Locator submitButton() {
        Locator typed = page.locator("button[type='submit'], input[type='submit']");
        if (typed.count() > 0) {
            return typed.first();
        }
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(Pattern.compile("sign\\s*in|log\\s*in|login", Pattern.CASE_INSENSITIVE))).first();
    }

    public Locator signUpLink() {
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions()
                .setName(Pattern.compile("sign\\s*up|register|create.*account", Pattern.CASE_INSENSITIVE))).first();
    }

    public Locator forgotPasswordLink() {
        return page.getByText(Pattern.compile("forgot", Pattern.CASE_INSENSITIVE)).first();
    }

    public Locator passwordToggle() {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(Pattern.compile("show|hide|toggle|eye|visib", Pattern.CASE_INSENSITIVE))).first();
    }

    // ---------- Actions ----------
    public void clickSubmit() {
        Locator button = submitButton();
        if (button.isEnabled()) {
            button.click();
        }
        settle();
    }

    public void login(String email, String password) {
        emailInput().fill(email);
        passwordInput().fill(password);
        clickSubmit();
    }

    // ---------- State checks ----------
    public boolean isOnLoginPage() {
        return url().contains(ConfigReader.get("login.path"));
    }

    public boolean isSubmitDisabled() {
        return submitButton().count() > 0 && !submitButton().isEnabled();
    }

    /** True when the user got some kind of feedback (or the button blocked the submit). */
    public boolean hasFeedback() {
        return !feedbackText().isEmpty() || isSubmitDisabled();
    }
}