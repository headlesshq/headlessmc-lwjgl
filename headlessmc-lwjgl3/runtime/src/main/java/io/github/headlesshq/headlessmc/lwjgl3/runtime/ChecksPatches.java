package io.github.headlesshq.headlessmc.lwjgl3.runtime;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import org.lwjgl.system.CustomBuffer;

import java.nio.Buffer;

/**
 * Patches for {@code org.lwjgl.system.Checks}, whose helpers return the checked value, e.g. the index of an element
 * or an address. They return it without throwing, like LWJGL with checks disabled.
 */
public final class ChecksPatches {
    private static final String CHECKS = "org/lwjgl/system/Checks";

    private ChecksPatches() {
        throw new AssertionError();
    }

    @Patch(owner = CHECKS)
    public static long check(int index, int length) {
        return Integer.toUnsignedLong(index);
    }

    @Patch(owner = CHECKS)
    public static long check(long pointer) {
        return pointer;
    }

    @Patch(owner = CHECKS)
    public static int lengthSafe(short[] array) {
        return array == null ? 0 : array.length;
    }

    @Patch(owner = CHECKS)
    public static int lengthSafe(int[] array) {
        return array == null ? 0 : array.length;
    }

    @Patch(owner = CHECKS)
    public static int lengthSafe(long[] array) {
        return array == null ? 0 : array.length;
    }

    @Patch(owner = CHECKS)
    public static int lengthSafe(float[] array) {
        return array == null ? 0 : array.length;
    }

    @Patch(owner = CHECKS)
    public static int lengthSafe(double[] array) {
        return array == null ? 0 : array.length;
    }

    @Patch(owner = CHECKS)
    public static int remainingSafe(Buffer buffer) {
        return buffer == null ? 0 : buffer.remaining();
    }

    @Patch(owner = CHECKS)
    public static int remainingSafe(CustomBuffer<?> buffer) {
        return buffer == null ? 0 : buffer.remaining();
    }

}
