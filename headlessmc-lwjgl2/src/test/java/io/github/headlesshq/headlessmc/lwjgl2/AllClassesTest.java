package io.github.headlesshq.headlessmc.lwjgl2;

import io.github.headlesshq.headlessmc.lwjgl.runtime.StaticInit;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AllClassesTest {
    /**
     * Classes of lwjgl_util that need ASM 3, an optional dependency of LWJGL 2 that Minecraft does not ship.
     */
    private static final String NEEDS_ASM_3 = "org.lwjgl.util.mapped.MappedObjectTransformer";

    @Test
    void everyClassInitializes() throws Exception {
        List<String> failures = new ArrayList<>();
        int count = 0;
        for (File jar : PatchedJars.jars()) {
            for (String className : PatchedJars.classNames(jar)) {
                if (className.startsWith(NEEDS_ASM_3)) {
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
            failures.add(failure.getKey() + " static initializer: " + failure.getValue());
        }

        assertTrue(count > 700, "Expected all LWJGL classes, found " + count);
        assertTrue(failures.isEmpty(), failures.size() + " of " + count + " classes failed:\n" + String.join("\n", failures));
    }

}
