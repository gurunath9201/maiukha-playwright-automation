package com.aiden.maiukha.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public final class TestDataUtil {

    public static final String VALID_NAME = "Aiden QA Tester";
    public static final String STRONG_PASSWORD = "Qa@Test#2026xyz";
    public static final String WRONG_PASSWORD = "Wr0ng@Pass#999";

    private static final Random RANDOM = new Random();

    private TestDataUtil() { }

    /** Always-unique email so every sign-up run uses a brand new account. */
    public static String uniqueEmail() {
        return "aiden.qa+" + System.currentTimeMillis() + RANDOM.nextInt(999) + "@example.com";
    }

    public static String repeat(char c, int times) {
        return String.valueOf(c).repeat(times);
    }

    public static String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
    }
}