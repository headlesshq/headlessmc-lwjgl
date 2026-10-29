package io.github.headlesshq.headlessmc.lwjgl2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Access to the patched jars produced by the {@code patchLwjgl} task.
 */
final class PatchedJars {
    private PatchedJars() {
        throw new AssertionError();
    }

    static List<File> jars() {
        File directory = new File(System.getProperty("hmc.lwjgl2.test.patchedDirectory"));
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null || files.length == 0) {
            throw new IllegalStateException("No patched jars in " + directory);
        }

        return Arrays.asList(files);
    }

    static List<String> classNames(File jar) throws IOException {
        List<String> result = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jar)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith(".class") && !name.startsWith("META-INF/") && !name.endsWith("module-info.class")) {
                    result.add(name.substring(0, name.length() - ".class".length()).replace('/', '.'));
                }
            }
        }

        return result;
    }

}
