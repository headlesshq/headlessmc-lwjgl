package io.github.headlesshq.headlessmc.lwjgl3.runtime.glfw;

import io.github.headlesshq.headlessmc.lwjgl3.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl3.runtime.WindowConfig;

import java.nio.IntBuffer;

/**
 * Patches for {@code org.lwjgl.glfw.GLFW}: time and window geometry.
 */
public final class GlfwPatches {
    private static final String GLFW = "org/lwjgl/glfw/GLFW";
    private static final long START = System.nanoTime();

    private static volatile double timeOffset;

    private GlfwPatches() {
        throw new AssertionError();
    }

    @Patch(owner = GLFW)
    public static double glfwGetTime() {
        return (System.nanoTime() - START) / 1_000_000_000.0 + timeOffset;
    }

    @Patch(owner = GLFW)
    public static void glfwSetTime(double time) {
        timeOffset = time - (System.nanoTime() - START) / 1_000_000_000.0;
    }

    @Patch(owner = GLFW)
    public static long glfwGetTimerValue() {
        return System.nanoTime() - START;
    }

    @Patch(owner = GLFW)
    public static long glfwGetTimerFrequency() {
        return 1_000_000_000L;
    }

    @Patch(owner = GLFW)
    public static void glfwGetWindowSize(long window, IntBuffer width, IntBuffer height) {
        putSize(width, height);
    }

    @Patch(owner = GLFW)
    public static void glfwGetWindowSize(long window, int[] width, int[] height) {
        putSize(width, height);
    }

    @Patch(owner = GLFW)
    public static void glfwGetFramebufferSize(long window, IntBuffer width, IntBuffer height) {
        putSize(width, height);
    }

    @Patch(owner = GLFW)
    public static void glfwGetFramebufferSize(long window, int[] width, int[] height) {
        putSize(width, height);
    }

    private static void putSize(IntBuffer width, IntBuffer height) {
        if (width != null) {
            width.put(width.position(), WindowConfig.WIDTH);
        }

        if (height != null) {
            height.put(height.position(), WindowConfig.HEIGHT);
        }
    }

    private static void putSize(int[] width, int[] height) {
        if (width != null) {
            width[0] = WindowConfig.WIDTH;
        }

        if (height != null) {
            height[0] = WindowConfig.HEIGHT;
        }
    }

}
