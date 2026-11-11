package com.aiden.maiukha.listeners;

import com.aiden.maiukha.base.BaseTest;
import com.aiden.maiukha.utils.ScreenshotUtil;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.Reporter;

/** Takes a BUG screenshot automatically whenever a test fails. */
public class TestListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        System.out.println("▶ START : " + result.getMethod().getDescription());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.println("✔ PASS  : " + result.getMethod().getDescription());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        System.out.println("✘ FAIL  : " + result.getMethod().getDescription()
                + "\n          Reason: " + result.getThrowable().getMessage());

        Object instance = result.getInstance();
        if (instance instanceof BaseTest baseTest && baseTest.getPage() != null) {
            String path = ScreenshotUtil.captureBug(baseTest.getPage(), result.getName());
            Reporter.log("BUG SCREENSHOT: " + path, true);
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        System.out.println("⏭ SKIP  : " + result.getMethod().getDescription());
    }
}