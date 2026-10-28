package io.github.headlesshq.headlessmc.lwjgl3.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects failures of patched static initializers.
 * The patcher wraps every static initializer of a patched class, so that a failure leaves the class usable with
 * partially initialized static fields instead of throwing {@link ExceptionInInitializerError}.
 */
public final class StaticInit {
    private static final Map<String, Throwable> FAILURES = Collections.synchronizedMap(new LinkedHashMap<>());

    private StaticInit() {
        throw new AssertionError();
    }

    /**
     * Called by patched static initializers.
     *
     * @param throwable the failure.
     * @param className the class whose static initializer failed.
     */
    public static void failed(Throwable throwable, String className) {
        FAILURES.put(className, throwable);
        Log.warn("Static initializer of " + className + " failed: " + throwable);
        if (Log.isDebug()) {
            throwable.printStackTrace();
        }
    }

    /**
     * @return the classes whose static initializer failed, with the failure.
     */
    public static Map<String, Throwable> failures() {
        synchronized (FAILURES) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(FAILURES));
        }
    }

}
