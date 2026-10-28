package io.github.headlesshq.headlessmc.lwjgl3;

import io.github.headlesshq.headlessmc.lwjgl3.runtime.StaticInit;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AllClassesTest {
    /**
     * Classes that LWJGL only uses on newer Java versions, with the minimum Java version.
     */
    private static final Map<String, Integer> MINIMUM_JAVA_VERSION = new HashMap<>();

    static {
        MINIMUM_JAVA_VERSION.put("org.lwjgl.system.MemoryBackendUnsafeLegacy9", 9);
        MINIMUM_JAVA_VERSION.put("org.lwjgl.system.MemoryBackendUnsafeLegacy17", 17);
        MINIMUM_JAVA_VERSION.put("org.lwjgl.system.MemoryBackendUnsafeLegacy22", 22);
    }

    @Test
    void everyClassInitializes() throws Exception {
        int javaVersion = javaVersion();
        List<String> failures = new ArrayList<>();
        int count = 0;
        for (File jar : PatchedJars.jars()) {
            for (String className : PatchedJars.classNames(jar)) {
                if (MINIMUM_JAVA_VERSION.getOrDefault(className, 8) > javaVersion) {
                    continue;
                }

                count++;
                try {
                    Class.forName(className, true, AllClassesTest.class.getClassLoader());
                } catch (Throwable t) {
                    failures.add(className + ": " + t);
                }
            }
        }

        for (Map.Entry<String, Throwable> failure : StaticInit.failures().entrySet()) {
            if (MINIMUM_JAVA_VERSION.getOrDefault(failure.getKey(), 8) <= javaVersion) {
                failures.add(failure.getKey() + " static initializer: " + failure.getValue());
            }
        }

        assertTrue(count > 5000, "Expected all LWJGL classes, found " + count);
        assertTrue(failures.isEmpty(), failures.size() + " of " + count + " classes failed:\n" + String.join("\n", failures));
    }

    private static int javaVersion() {
        String version = System.getProperty("java.specification.version");
        return version.startsWith("1.") ? Integer.parseInt(version.substring(2)) : Integer.parseInt(version);
    }

}
