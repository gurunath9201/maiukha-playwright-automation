package com.aiden.maiukha.utils;

import com.microsoft.playwright.Page;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ScreenshotUtil {

    public static final Path ROOT = Paths.get("screenshots");

    private ScreenshotUtil() { }

    /** Normal evidence screenshot -> screenshots/evidence */
    public static String capture(Page page, String name) {
        return save(page, ROOT.resolve("evidence"), name);
    }

    /** Bug screenshot (called automatically on test failure) -> screenshots/bugs */
    public static String captureBug(Page page, String name) {
        return save(page, ROOT.resolve("bugs"), name);
    }

    private static String save(Page page, Path folder, String name) {
        try {
            Files.createDirectories(folder);
            Path file = folder.resolve(name.replaceAll("[^a-zA-Z0-9_-]", "_")
                    + "_" + TestDataUtil.timestamp() + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(file).setFullPage(true));
            return file.toAbsolutePath().toString();
        } catch (Exception e) {
            System.err.println("Could not take screenshot: " + e.getMessage());
            return "";
        }
    }
}