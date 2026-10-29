package io.github.headlesshq.headlessmc.lwjgl.runtime;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal logging to {@link System#err}, the patched LWJGL has no logging dependencies.
 */
public final class Log {
    private static final String PREFIX = "[HeadlessMc-" + Config.FLAVOR + "] ";
    private static final Set<String> ONCE = ConcurrentHashMap.newKeySet();
    private static final boolean DEBUG = Config.getBoolean("debug", false);

    private Log() {
        throw new AssertionError();
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    public static void debug(String message) {
        if (DEBUG) {
            System.err.println(PREFIX + message);
        }
    }

    public static void warn(String message) {
        System.err.println(PREFIX + message);
    }

    /**
     * Logs the message only the first time this key is used.
     *
     * @param key identifies the message.
     * @param message the message to log.
     */
    public static void warnOnce(String key, String message) {
        if (ONCE.add(key)) {
            warn(message);
        }
    }

}
