package io.github.headlesshq.headlessmc.lwjgl3.runtime;

/**
 * Identity based {@link Object#equals(Object)} and {@link Object#hashCode()} for patched LWJGL classes,
 * whose original implementations compare native memory.
 */
public final class Identity {
    private Identity() {
        throw new AssertionError();
    }

    public static boolean equals(Object self, Object other) {
        return self == other;
    }

    public static int hashCode(Object self) {
        return System.identityHashCode(self);
    }

}
