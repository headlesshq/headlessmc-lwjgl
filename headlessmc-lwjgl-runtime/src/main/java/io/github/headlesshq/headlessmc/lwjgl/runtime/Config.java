package io.github.headlesshq.headlessmc.lwjgl.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration of the patched LWJGL.
 * Values are read from system properties in the {@code hmc.lwjgl3.} namespace for LWJGL 3 and the {@code hmc.lwjgl2.}
 * namespace for LWJGL 2, falling back to the properties file given by the system property {@code <namespace>config}.
 */
public final class Config {
    /**
     * {@code lwjgl3} or {@code lwjgl2}, depending on the patched LWJGL this runtime is embedded into.
     */
    public static final String FLAVOR = flavor();
    public static final String PREFIX = "hmc." + FLAVOR + ".";
    public static final String FILE_PROPERTY = PREFIX + "config";

    private static final Properties FILE = load();

    private Config() {
        throw new AssertionError();
    }

    /**
     * @param key the key without the {@link #PREFIX} prefix.
     * @return the configured value or {@code null}.
     */
    public static String get(String key) {
        String value = System.getProperty(PREFIX + key);
        return value != null ? value : FILE.getProperty(PREFIX + key);
    }

    public static String getString(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value != null ? Boolean.parseBoolean(value.trim()) : defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.decode(value.trim());
        } catch (NumberFormatException e) {
            Log.warnOnce("config:" + key, "Invalid integer for " + PREFIX + key + ": " + value);
            return defaultValue;
        }
    }

    public static long getLong(String key, long defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Long.decode(value.trim());
        } catch (NumberFormatException e) {
            Log.warnOnce("config:" + key, "Invalid long for " + PREFIX + key + ": " + value);
            return defaultValue;
        }
    }

    private static String flavor() {
        try {
            Class.forName("org.lwjgl.system.MemoryUtil", false, Config.class.getClassLoader());
            return "lwjgl3";
        } catch (ClassNotFoundException | LinkageError e) {
            return "lwjgl2";
        }
    }

    private static Properties load() {
        Properties properties = new Properties();
        String file = System.getProperty(FILE_PROPERTY);
        if (file != null) {
            try (InputStream is = Files.newInputStream(Paths.get(file))) {
                properties.load(is);
            } catch (IOException e) {
                System.err.println("[HeadlessMc-" + FLAVOR + "] Failed to read " + file + ": " + e);
            }
        }

        return properties;
    }

}
