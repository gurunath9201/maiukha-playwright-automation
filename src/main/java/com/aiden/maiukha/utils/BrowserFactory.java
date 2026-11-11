package com.aiden.maiukha.utils;

import com.aiden.maiukha.config.ConfigReader;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

public final class BrowserFactory {

    private BrowserFactory() { }

    public static Browser launch(Playwright playwright, String name) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(ConfigReader.getBool("headless"))
                .setSlowMo(ConfigReader.getInt("slowmo"));

        switch (name.toLowerCase()) {
            case "firefox": return playwright.firefox().launch(options);
            case "webkit":  return playwright.webkit().launch(options);
            case "chrome":  return playwright.chromium().launch(options.setChannel("chrome"));
            case "edge":    return playwright.chromium().launch(options.setChannel("msedge"));
            default:        return playwright.chromium().launch(options);
        }
    }
}