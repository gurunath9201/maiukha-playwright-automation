package com.aiden.maiukha.config;

import java.io.InputStream;
import java.util.Properties;

/** Reads config.properties. Any value can be overridden with -Dkey=value. */
public final class ConfigReader {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties not found in src/test/resources");
            }
            PROPS.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load config.properties", e);
        }
    }

    private ConfigReader() { }

    public static String get(String key) {
        return System.getProperty(key, PROPS.getProperty(key, "")).trim();
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static boolean getBool(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static String loginUrl()  { return get("base.url") + get("login.path"); }
    public static String signupUrl() { return get("base.url") + get("signup.path"); }

    public static boolean hasValidCredentials() {
        return !get("valid.email").isEmpty() && !get("valid.password").isEmpty();
    }
}