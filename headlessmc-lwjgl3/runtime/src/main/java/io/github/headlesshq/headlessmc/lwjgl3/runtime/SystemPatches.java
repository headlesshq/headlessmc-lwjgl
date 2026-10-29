package io.github.headlesshq.headlessmc.lwjgl3.runtime;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;

import java.io.PrintStream;

/**
 * Patches for {@code org.lwjgl.system}.
 */
public final class SystemPatches {
    private SystemPatches() {
        throw new AssertionError();
    }

    /**
     * The size of a native pointer, which LWJGL uses for {@code Pointer.POINTER_SIZE} and every struct layout.
     *
     * @return 8 on 64 bit JVMs, 4 on 32 bit JVMs.
     */
    @Patch(owner = "org/lwjgl/system/MemoryAccessJNI")
    public static int getPointerSize() {
        return "32".equals(System.getProperty("sun.arch.data.model")) ? 4 : 8;
    }

    @Patch(owner = "org/lwjgl/system/APIUtil")
    public static PrintStream getDebugStream() {
        return System.err;
    }

}
