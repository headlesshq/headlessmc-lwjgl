package io.github.headlesshq.headlessmc.lwjgl3.runtime.glfw;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Config;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Ids;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Input;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWCharModsCallback;
import org.lwjgl.glfw.GLFWCharModsCallbackI;
import org.lwjgl.glfw.GLFWCursorEnterCallback;
import org.lwjgl.glfw.GLFWCursorEnterCallbackI;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.glfw.GLFWWindowCloseCallbackI;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallbackI;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallbackI;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Locale;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Patches for {@code org.lwjgl.glfw.GLFW}: windows, callbacks, event dispatching and the polling input state.
 * Setting a callback returns the previous one, wrapped in an LWJGL callback object like GLFW does.
 */
public final class GlfwInputPatches {
    private static final String GLFW = "org/lwjgl/glfw/GLFW";
    private static final long WAIT_MILLIS = 10L;
    private static final int[] CONTEXT_VERSION = contextVersion();

    private GlfwInputPatches() {
        throw new AssertionError();
    }

    @Patch(owner = GLFW)
    public static boolean glfwInit() {
        Input.register();
        return true;
    }

    @Patch(owner = GLFW)
    public static long glfwCreateWindow(int width, int height, CharSequence title, long monitor, long share) {
        long window = createWindow(width, height);
        GlfwWindows.get(window).title = title == null ? "" : title.toString();
        return window;
    }

    @Patch(owner = GLFW)
    public static long glfwCreateWindow(int width, int height, ByteBuffer title, long monitor, long share) {
        return createWindow(width, height);
    }

    private static long createWindow(int width, int height) {
        long handle = Ids.nextLong();
        Map<Integer, Integer> attributes = GlfwWindows.get(handle).attributes;
        attributes.putAll(GlfwWindows.HINTS);
        attributes.remove(GLFW_OPENGL_PROFILE, GLFW_OPENGL_ANY_PROFILE);
        Input.setSize(width, height);
        Input.register();
        return handle;
    }

    @Patch(owner = GLFW)
    public static void glfwDestroyWindow(long window) {
        GlfwWindows.remove(window);
    }

    @Patch(owner = GLFW)
    public static boolean glfwWindowShouldClose(long window) {
        return GlfwWindows.get(window).shouldClose;
    }

    @Patch(owner = GLFW)
    public static void glfwSetWindowShouldClose(long window, boolean value) {
        GlfwWindows.get(window).shouldClose = value;
    }

    @Patch(owner = GLFW)
    public static void glfwPollEvents() {
        GlfwWindows.dispatch();
    }

    @Patch(owner = GLFW)
    public static void glfwWaitEvents() {
        if (!GlfwWindows.dispatch()) {
            sleep(WAIT_MILLIS);
            GlfwWindows.dispatch();
        }
    }

    @Patch(owner = GLFW)
    public static void glfwWaitEventsTimeout(double timeout) {
        if (!GlfwWindows.dispatch()) {
            sleep(Math.min(WAIT_MILLIS, (long) (timeout * 1000.0)));
            GlfwWindows.dispatch();
        }
    }

    private static void sleep(long millis) {
        if (millis <= 0L) {
            return;
        }

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Patch(owner = GLFW)
    public static int glfwGetKey(long window, int key) {
        return Input.isKeyPressed(key) ? GLFW_PRESS : GLFW_RELEASE;
    }

    @Patch(owner = GLFW)
    public static int glfwGetMouseButton(long window, int button) {
        return Input.isButtonPressed(button) ? GLFW_PRESS : GLFW_RELEASE;
    }

    @Patch(owner = GLFW)
    public static int glfwGetKeyScancode(int key) {
        return key >= GLFW_KEY_SPACE && key <= GLFW_KEY_LAST ? key : -1;
    }

    /**
     * Printable keys are named by their lower case character on a US layout, like GLFW, other keys have no name.
     */
    @Patch(owner = GLFW)
    public static String glfwGetKeyName(int key, int scancode) {
        int printable = key == GLFW_KEY_UNKNOWN ? scancode : key;
        if (printable >= GLFW_KEY_SPACE + 1 && printable <= GLFW_KEY_GRAVE_ACCENT) {
            return String.valueOf((char) printable).toLowerCase(Locale.ROOT);
        } else if (printable >= GLFW_KEY_KP_0 && printable <= GLFW_KEY_KP_9) {
            return String.valueOf((char) ('0' + printable - GLFW_KEY_KP_0));
        }

        switch (printable) {
            case GLFW_KEY_KP_DECIMAL:
                return ".";
            case GLFW_KEY_KP_DIVIDE:
                return "/";
            case GLFW_KEY_KP_MULTIPLY:
                return "*";
            case GLFW_KEY_KP_SUBTRACT:
                return "-";
            case GLFW_KEY_KP_ADD:
                return "+";
            case GLFW_KEY_KP_EQUAL:
                return "=";
            default:
                return null;
        }
    }

    @Patch(owner = GLFW)
    public static void glfwGetCursorPos(long window, DoubleBuffer x, DoubleBuffer y) {
        if (x != null) {
            x.put(x.position(), Input.cursorX());
        }

        if (y != null) {
            y.put(y.position(), Input.cursorY());
        }
    }

    @Patch(owner = GLFW)
    public static void glfwGetCursorPos(long window, double[] x, double[] y) {
        if (x != null) {
            x[0] = Input.cursorX();
        }

        if (y != null) {
            y[0] = Input.cursorY();
        }
    }

    @Patch(owner = GLFW)
    public static void glfwSetCursorPos(long window, double x, double y) {
        Input.setCursor(x, y);
    }

    @Patch(owner = GLFW)
    public static int glfwGetInputMode(long window, int mode) {
        Integer value = GlfwWindows.get(window).inputModes.get(mode);
        if (value != null) {
            return value;
        }

        return mode == GLFW_CURSOR ? GLFW_CURSOR_NORMAL : GLFW_FALSE;
    }

    @Patch(owner = GLFW)
    public static void glfwSetInputMode(long window, int mode, int value) {
        GlfwWindows.get(window).inputModes.put(mode, value);
    }

    @Patch(owner = GLFW)
    public static void glfwDefaultWindowHints() {
        GlfwWindows.HINTS.clear();
    }

    @Patch(owner = GLFW)
    public static void glfwWindowHint(int hint, int value) {
        GlfwWindows.HINTS.put(hint, value);
    }

    @Patch(owner = GLFW)
    public static void glfwSetWindowAttrib(long window, int attrib, int value) {
        GlfwWindows.get(window).attributes.put(attrib, value);
    }

    /**
     * Returns the window hints the application set and otherwise the values a real GLFW reports for a visible window
     * with the configured OpenGL version. The context version is always the configured one, the hints only request
     * a minimum.
     */
    @Patch(owner = GLFW)
    public static int glfwGetWindowAttrib(long window, int attrib) {
        switch (attrib) {
            case GLFW_FOCUSED:
                return Input.isFocused() ? GLFW_TRUE : GLFW_FALSE;
            case GLFW_CONTEXT_VERSION_MAJOR:
                return CONTEXT_VERSION[0];
            case GLFW_CONTEXT_VERSION_MINOR:
                return CONTEXT_VERSION[1];
            default:
                break;
        }

        Integer value = GlfwWindows.get(window).attributes.get(attrib);
        if (value != null) {
            return value;
        }

        switch (attrib) {
            case GLFW_RESIZABLE:
            case GLFW_VISIBLE:
            case GLFW_DECORATED:
            case GLFW_AUTO_ICONIFY:
            case GLFW_HOVERED:
            case GLFW_FOCUS_ON_SHOW:
            case GLFW_DOUBLEBUFFER:
                return GLFW_TRUE;
            case GLFW_CLIENT_API:
                return GLFW_OPENGL_API;
            case GLFW_OPENGL_PROFILE:
                return GLFW_OPENGL_CORE_PROFILE;
            case GLFW_CONTEXT_ROBUSTNESS:
                return GLFW_NO_ROBUSTNESS;
            case GLFW_CONTEXT_RELEASE_BEHAVIOR:
                return GLFW_ANY_RELEASE_BEHAVIOR;
            case GLFW_CONTEXT_CREATION_API:
                return GLFW_NATIVE_CONTEXT_API;
            default:
                return GLFW_FALSE;
        }
    }

    private static int[] contextVersion() {
        String[] parts = Config.getString("gl.version", "4.6").trim().split("\\.");
        try {
            return new int[]{Integer.parseInt(parts[0]), parts.length > 1 ? Integer.parseInt(parts[1]) : 0};
        } catch (NumberFormatException e) {
            return new int[]{4, 6};
        }
    }

    @Patch(owner = GLFW)
    public static void glfwSetWindowSize(long window, int width, int height) {
        Input.resize(width, height);
    }

    @Patch(owner = GLFW)
    public static void glfwFocusWindow(long window) {
        Input.focus(true);
    }

    @Patch(owner = GLFW)
    public static void glfwGetWindowContentScale(long window, FloatBuffer xscale, FloatBuffer yscale) {
        if (xscale != null) {
            xscale.put(xscale.position(), 1.0f);
        }

        if (yscale != null) {
            yscale.put(yscale.position(), 1.0f);
        }
    }

    @Patch(owner = GLFW)
    public static void glfwGetWindowContentScale(long window, float[] xscale, float[] yscale) {
        if (xscale != null) {
            xscale[0] = 1.0f;
        }

        if (yscale != null) {
            yscale[0] = 1.0f;
        }
    }

    @Patch(owner = GLFW)
    public static GLFWKeyCallback glfwSetKeyCallback(long window, GLFWKeyCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWKeyCallbackI previous = state.key;
        state.key = callback;
        return previous == null ? null : GLFWKeyCallback.create(previous);
    }

    @Patch(owner = GLFW)
    public static GLFWCharCallback glfwSetCharCallback(long window, GLFWCharCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWCharCallbackI previous = state.character;
        state.character = callback;
        return previous == null ? null : GLFWCharCallback.create(previous);
    }

    @Patch(owner = GLFW)
    public static GLFWCharModsCallback glfwSetCharModsCallback(long window, GLFWCharModsCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWCharModsCallbackI previous = state.charMods;
        state.charMods = callback;
        return previous == null ? null : GLFWCharModsCallback.create(previous);
    }

    @Patch(owner = GLFW)
    public static GLFWMouseButtonCallback glfwSetMouseButtonCallback(long window, GLFWMouseButtonCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWMouseButtonCallbackI previous = state.mouseButton;
        state.mouseButton = callback;
        return previous == null ? null : GLFWMouseButtonCallback.create(previous);
    }

    @Patch(owner = GLFW)
    public static GLFWCursorPosCallback glfwSetCursorPosCallback(long window, GLFWCursorPosCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWCursorPosCallbackI previous = state.cursorPos;
        state.cursorPos = callback;
        return previous == null ? null : GLFWCursorPosCallback.create(previous);
    }

    @Patch(owner = GLFW)
    public static GLFWCursorEnterCallback glfwSetCursorEnterCallback(long window, GLFWCursorEnterCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWCursorEnterCallbackI previous = state.cursorEnter;
        state.cursorEnter = callback;
        return previous == null ? null : GLFWCursorEnterCallback.create(previous);
    }

    @Patch(owner = GLFW)
    public static GLFWScrollCallback glfwSetScrollCallback(long window, GLFWScrollCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWScrollCallbackI previous = state.scroll;
        state.scroll = callback;
        return previous == null ? null : GLFWScrollCallback.create(previous);
    }

    @Patch(owner = GLFW)
    public static GLFWWindowSizeCallback glfwSetWindowSizeCallback(long window, GLFWWindowSizeCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWWindowSizeCallbackI previous = state.windowSize;
        state.windowSize = callback;
        return previous == null ? null : GLFWWindowSizeCallback.create(previous);
    }

    @Patch(owner = GLFW)
    public static GLFWFramebufferSizeCallback glfwSetFramebufferSizeCallback(long window, GLFWFramebufferSizeCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWFramebufferSizeCallbackI previous = state.framebufferSize;
        state.framebufferSize = callback;
        return previous == null ? null : GLFWFramebufferSizeCallback.create(previous);
    }

    @Patch(owner = GLFW)
    public static GLFWWindowFocusCallback glfwSetWindowFocusCallback(long window, GLFWWindowFocusCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWWindowFocusCallbackI previous = state.focus;
        state.focus = callback;
        return previous == null ? null : GLFWWindowFocusCallback.create(previous);
    }

    @Patch(owner = GLFW)
    public static GLFWWindowCloseCallback glfwSetWindowCloseCallback(long window, GLFWWindowCloseCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWWindowCloseCallbackI previous = state.close;
        state.close = callback;
        return previous == null ? null : GLFWWindowCloseCallback.create(previous);
    }

}
