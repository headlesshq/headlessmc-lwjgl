package io.github.headlesshq.headlessmc.lwjgl3.runtime.glfw;

import io.github.headlesshq.headlessmc.lwjgl3.runtime.Config;
import io.github.headlesshq.headlessmc.lwjgl3.runtime.DummyBuffers;
import io.github.headlesshq.headlessmc.lwjgl3.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl3.runtime.WindowConfig;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Patches for {@code org.lwjgl.glfw.GLFW}: the library version and platform, a single monitor, window position,
 * title and opacity, the current context and the absence of joysticks, with the values a real GLFW on X11 reports.
 */
public final class GlfwMonitorPatches {
    private static final String GLFW = "org/lwjgl/glfw/GLFW";
    private static final long MONITOR = DummyBuffers.FAKE_ADDRESS;
    private static final int PLATFORM = Config.getInt("glfw.platform", GLFW_PLATFORM_X11);
    private static final int PHYSICAL_WIDTH_MM = 508;
    private static final int PHYSICAL_HEIGHT_MM = 285;

    private static volatile long currentContext;

    private GlfwMonitorPatches() {
        throw new AssertionError();
    }

    @Patch(owner = GLFW)
    public static void glfwGetVersion(IntBuffer major, IntBuffer minor, IntBuffer rev) {
        put(major, GLFW_VERSION_MAJOR);
        put(minor, GLFW_VERSION_MINOR);
        put(rev, GLFW_VERSION_REVISION);
    }

    @Patch(owner = GLFW)
    public static void glfwGetVersion(int[] major, int[] minor, int[] rev) {
        put(major, GLFW_VERSION_MAJOR);
        put(minor, GLFW_VERSION_MINOR);
        put(rev, GLFW_VERSION_REVISION);
    }

    @Patch(owner = GLFW)
    public static String glfwGetVersionString() {
        return GLFW_VERSION_MAJOR + "." + GLFW_VERSION_MINOR + "." + GLFW_VERSION_REVISION + " HeadlessMc";
    }

    @Patch(owner = GLFW)
    public static int glfwGetPlatform() {
        return PLATFORM;
    }

    @Patch(owner = GLFW)
    public static long glfwGetPrimaryMonitor() {
        return MONITOR;
    }

    @Patch(owner = GLFW)
    public static String glfwGetMonitorName(long monitor) {
        return "HeadlessMc";
    }

    @Patch(owner = GLFW)
    public static void glfwGetMonitorPhysicalSize(long monitor, IntBuffer widthMM, IntBuffer heightMM) {
        put(widthMM, PHYSICAL_WIDTH_MM);
        put(heightMM, PHYSICAL_HEIGHT_MM);
    }

    @Patch(owner = GLFW)
    public static void glfwGetMonitorPhysicalSize(long monitor, int[] widthMM, int[] heightMM) {
        put(widthMM, PHYSICAL_WIDTH_MM);
        put(heightMM, PHYSICAL_HEIGHT_MM);
    }

    @Patch(owner = GLFW)
    public static void glfwGetMonitorPos(long monitor, IntBuffer xpos, IntBuffer ypos) {
        put(xpos, 0);
        put(ypos, 0);
    }

    @Patch(owner = GLFW)
    public static void glfwGetMonitorPos(long monitor, int[] xpos, int[] ypos) {
        put(xpos, 0);
        put(ypos, 0);
    }

    @Patch(owner = GLFW)
    public static void glfwGetMonitorWorkarea(long monitor, IntBuffer xpos, IntBuffer ypos, IntBuffer width, IntBuffer height) {
        put(xpos, 0);
        put(ypos, 0);
        put(width, WindowConfig.MONITOR_WIDTH);
        put(height, WindowConfig.MONITOR_HEIGHT);
    }

    @Patch(owner = GLFW)
    public static void glfwGetMonitorWorkarea(long monitor, int[] xpos, int[] ypos, int[] width, int[] height) {
        put(xpos, 0);
        put(ypos, 0);
        put(width, WindowConfig.MONITOR_WIDTH);
        put(height, WindowConfig.MONITOR_HEIGHT);
    }

    @Patch(owner = GLFW)
    public static void glfwGetMonitorContentScale(long monitor, FloatBuffer xscale, FloatBuffer yscale) {
        put(xscale, 1.0f);
        put(yscale, 1.0f);
    }

    @Patch(owner = GLFW)
    public static void glfwGetMonitorContentScale(long monitor, float[] xscale, float[] yscale) {
        put(xscale, 1.0f);
        put(yscale, 1.0f);
    }

    @Patch(owner = GLFW)
    public static void glfwGetWindowPos(long window, IntBuffer xpos, IntBuffer ypos) {
        put(xpos, 0);
        put(ypos, 0);
    }

    @Patch(owner = GLFW)
    public static void glfwGetWindowPos(long window, int[] xpos, int[] ypos) {
        put(xpos, 0);
        put(ypos, 0);
    }

    @Patch(owner = GLFW)
    public static void glfwGetWindowFrameSize(long window, IntBuffer left, IntBuffer top, IntBuffer right, IntBuffer bottom) {
        put(left, 0);
        put(top, 0);
        put(right, 0);
        put(bottom, 0);
    }

    @Patch(owner = GLFW)
    public static void glfwGetWindowFrameSize(long window, int[] left, int[] top, int[] right, int[] bottom) {
        put(left, 0);
        put(top, 0);
        put(right, 0);
        put(bottom, 0);
    }

    @Patch(owner = GLFW)
    public static float glfwGetWindowOpacity(long window) {
        return 1.0f;
    }

    @Patch(owner = GLFW)
    public static String glfwGetWindowTitle(long window) {
        return GlfwWindows.get(window).title;
    }

    @Patch(owner = GLFW)
    public static void glfwSetWindowTitle(long window, CharSequence title) {
        GlfwWindows.get(window).title = title == null ? "" : title.toString();
    }

    @Patch(owner = GLFW)
    public static void glfwMakeContextCurrent(long window) {
        currentContext = window;
    }

    @Patch(owner = GLFW)
    public static long glfwGetCurrentContext() {
        return currentContext;
    }

    @Patch(owner = GLFW)
    public static boolean glfwRawMouseMotionSupported() {
        return true;
    }

    @Patch(owner = GLFW)
    public static FloatBuffer glfwGetJoystickAxes(int jid) {
        return null;
    }

    @Patch(owner = GLFW)
    public static ByteBuffer glfwGetJoystickButtons(int jid) {
        return null;
    }

    @Patch(owner = GLFW)
    public static ByteBuffer glfwGetJoystickHats(int jid) {
        return null;
    }

    @Patch(owner = GLFW)
    public static String glfwGetJoystickName(int jid) {
        return null;
    }

    @Patch(owner = GLFW)
    public static String glfwGetJoystickGUID(int jid) {
        return null;
    }

    @Patch(owner = GLFW)
    public static String glfwGetGamepadName(int jid) {
        return null;
    }

    private static void put(IntBuffer buffer, int value) {
        if (buffer != null) {
            buffer.put(buffer.position(), value);
        }
    }

    private static void put(int[] array, int value) {
        if (array != null) {
            array[0] = value;
        }
    }

    private static void put(FloatBuffer buffer, float value) {
        if (buffer != null) {
            buffer.put(buffer.position(), value);
        }
    }

    private static void put(float[] array, float value) {
        if (array != null) {
            array[0] = value;
        }
    }

}
