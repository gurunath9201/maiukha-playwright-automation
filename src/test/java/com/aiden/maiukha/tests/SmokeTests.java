package com.aiden.maiukha.tests;

import com.aiden.maiukha.base.BaseTest;
import com.aiden.maiukha.config.ConfigReader;
import com.aiden.maiukha.pages.LoginPage;
import com.aiden.maiukha.pages.SignUpPage;
import com.microsoft.playwright.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SmokeTests extends BaseTest {

    @Test(groups = "smoke", description = "TC_SMOKE_001 - Login page responds with HTTP 200")
    public void loginPageReturns200() {
        Response response = page.navigate(ConfigReader.loginUrl());
        Assert.assertEquals(response.status(), 200, "Login URL did not return 200");
    }

    @Test(groups = "smoke", description = "TC_SMOKE_002 - Sign-up page responds with HTTP 200")
    public void signUpPageReturns200() {
        Response response = page.navigate(ConfigReader.signupUrl());
        Assert.assertEquals(response.status(), 200, "Sign-up URL did not return 200");
    }

    @Test(groups = "smoke", description = "TC_SMOKE_003 - Login form is rendered")
    public void loginFormRendered() {
        LoginPage login = new LoginPage(page);
        login.open();
        Assert.assertTrue(login.emailInput().isVisible(), "Email field not visible");
        Assert.assertTrue(login.passwordInput().isVisible(), "Password field not visible");
        Assert.assertTrue(login.submitButton().isVisible(), "Sign-in button not visible");
    }

    @Test(groups = "smoke", description = "TC_SMOKE_004 - Sign-up form is rendered")
    public void signUpFormRendered() {
        SignUpPage signUp = new SignUpPage(page);
        signUp.open();
        Assert.assertTrue(signUp.emailInput().isVisible(), "Email field not visible");
        Assert.assertTrue(signUp.passwordInput().isVisible(), "Password field not visible");
        Assert.assertTrue(signUp.submitButton().isVisible(), "Sign-up button not visible");
    }

    @Test(groups = "smoke", description = "TC_SMOKE_005 - Navigate Login -> Sign up -> back")
    public void navigationBetweenPages() {
        LoginPage login = new LoginPage(page);
        login.open();
        login.signUpLink().click();
        login.settle();
        Assert.assertFalse(page.url().contains(ConfigReader.get("login.path")),
                "Clicking the sign-up link did not leave the login page");
        page.goBack();
        login.settle();
        Assert.assertTrue(page.url().contains(ConfigReader.get("login.path")),
                "Browser Back did not return to login page");
    }
}