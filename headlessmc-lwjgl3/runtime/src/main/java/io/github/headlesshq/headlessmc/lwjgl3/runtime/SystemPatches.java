package io.github.headlesshq.headlessmc.lwjgl3.runtime;

import java.io.PrintStream;

/**
 * Patches for {@code org.lwjgl.system}.
 */
public final class SystemPatches {
    private SystemPatches() {
        throw new AssertionError();
    }

    @Patch(owner = "org/lwjgl/system/APIUtil")
    public static PrintStream getDebugStream() {
        return System.err;
    }

}
