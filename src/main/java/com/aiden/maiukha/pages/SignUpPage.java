package com.aiden.maiukha.pages;

import com.aiden.maiukha.config.ConfigReader;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;

import java.util.regex.Pattern;

public class SignUpPage extends BasePage {

    private static final String NAME_SEL =
            "input[name*='name' i], input[id*='name' i], input[placeholder*='name' i], input[autocomplete='name']";

    public SignUpPage(Page page) {
        super(page);
    }

    public Response open() {
        Response response = open(ConfigReader.signupUrl());
        waitForForm();
        // Fallback: if /signup is not the right URL, go through the link on the login page
        if (page.locator(EMAIL_SEL).count() == 0) {
            page.navigate(ConfigReader.loginUrl());
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions()
                    .setName(Pattern.compile("sign\\s*up|register|create.*account", Pattern.CASE_INSENSITIVE)))
                    .first().click();
            waitForForm();
        }
        return response;
    }

    // ---------- Locators ----------
    public Locator nameInput() { return page.locator(NAME_SEL).first(); }
    public boolean hasNameField() { return page.locator(NAME_SEL).count() > 0; }

    public boolean hasConfirmPassword() { return page.locator(PASSWORD_SEL).count() > 1; }
    public Locator confirmPasswordInput() { return page.locator(PASSWORD_SEL).nth(1); }

    public Locator termsCheckbox() { return page.locator("input[type='checkbox']"); }

    public Locator submitButton() {
        Locator typed = page.locator("button[type='submit'], input[type='submit']");
        if (typed.count() > 0) {
            return typed.first();
        }
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions()
                .setName(Pattern.compile("sign\\s*up|register|create|get started", Pattern.CASE_INSENSITIVE))).first();
    }

    public Locator loginLink() {
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions()
                .setName(Pattern.compile("sign\\s*in|log\\s*in|login", Pattern.CASE_INSENSITIVE))).first();
    }

    // ---------- Actions ----------
    public void fillForm(String name, String email, String password, String confirmPassword) {
        if (hasNameField()) {
            nameInput().fill(name);
        }
        emailInput().fill(email);
        passwordInput().fill(password);
        if (hasConfirmPassword()) {
            confirmPasswordInput().fill(confirmPassword);
        }
    }

    public void acceptTermsIfPresent() {
        Locator box = termsCheckbox();
        if (box.count() > 0 && !box.first().isChecked()) {
            box.first().check(new Locator.CheckOptions().setForce(true));
        }
    }

    public void clickSubmit() {
        Locator button = submitButton();
        if (button.isEnabled()) {
            button.click();
        }
        settle();
    }

    /** Fill everything, accept terms and submit. */
    public void register(String name, String email, String password) {
        fillForm(name, email, password, password);
        acceptTermsIfPresent();
        clickSubmit();
    }

    // ---------- State checks ----------
    public boolean isOnSignUpPage() {
        return url().contains(ConfigReader.get("signup.path"));
    }

    public boolean isSuccess() {
        boolean redirected = !isOnSignUpPage();
        boolean successText = Pattern.compile(
                "success|verify|verification|check your email|welcome|account created|registered",
                Pattern.CASE_INSENSITIVE).matcher(bodyText()).find();
        return redirected || successText;
    }

    public boolean isSubmitDisabled() {
        return submitButton().count() > 0 && !submitButton().isEnabled();
    }

    public boolean hasFeedback() {
        return !feedbackText().isEmpty() || isSubmitDisabled();
    }
}