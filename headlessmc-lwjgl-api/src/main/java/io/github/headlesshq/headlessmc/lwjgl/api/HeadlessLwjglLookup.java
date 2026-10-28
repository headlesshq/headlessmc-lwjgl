package io.github.headlesshq.headlessmc.lwjgl.api;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Finds the {@link HeadlessLwjgl} implementation, either registered explicitly or via {@link ServiceLoader}.
 */
final class HeadlessLwjglLookup {
    private static volatile HeadlessLwjgl instance;

    private HeadlessLwjglLookup() {
        throw new AssertionError();
    }

    static void register(HeadlessLwjgl implementation) {
        if (implementation == null) {
            throw new NullPointerException("implementation");
        }

        instance = implementation;
    }

    static HeadlessLwjgl get() {
        HeadlessLwjgl result = instance;
        if (result != null) {
            return result;
        }

        synchronized (HeadlessLwjglLookup.class) {
            if (instance == null) {
                instance = load(HeadlessLwjgl.class.getClassLoader());
            }

            if (instance == null) {
                instance = load(Thread.currentThread().getContextClassLoader());
            }

            if (instance == null) {
                throw new IllegalStateException("No patched headless LWJGL found");
            }

            return instance;
        }
    }

    private static HeadlessLwjgl load(ClassLoader classLoader) {
        Iterator<HeadlessLwjgl> iterator = ServiceLoader.load(HeadlessLwjgl.class, classLoader).iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

}
