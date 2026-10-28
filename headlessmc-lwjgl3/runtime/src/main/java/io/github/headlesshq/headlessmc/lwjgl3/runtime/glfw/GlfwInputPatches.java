package io.github.headlesshq.headlessmc.lwjgl3.runtime.glfw;

import io.github.headlesshq.headlessmc.lwjgl3.runtime.Ids;
import io.github.headlesshq.headlessmc.lwjgl3.runtime.Input;
import io.github.headlesshq.headlessmc.lwjgl3.runtime.Patch;
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

import static org.lwjgl.glfw.GLFW.*;

/**
 * Patches for {@code org.lwjgl.glfw.GLFW}: windows, callbacks, event dispatching and the polling input state.
 * Setting a callback returns the previous one only if it is an LWJGL callback object, lambdas cannot be returned.
 */
public final class GlfwInputPatches {
    private static final String GLFW = "org/lwjgl/glfw/GLFW";
    private static final long WAIT_MILLIS = 10L;

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
        GlfwWindows.get(handle);
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
        return key;
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
    public static int glfwGetWindowAttrib(long window, int attrib) {
        switch (attrib) {
            case GLFW_FOCUSED:
                return Input.isFocused() ? GLFW_TRUE : GLFW_FALSE;
            case GLFW_VISIBLE:
            case GLFW_HOVERED:
            case GLFW_RESIZABLE:
            case GLFW_DECORATED:
                return GLFW_TRUE;
            default:
                return GLFW_FALSE;
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
        return previous instanceof GLFWKeyCallback ? (GLFWKeyCallback) previous : null;
    }

    @Patch(owner = GLFW)
    public static GLFWCharCallback glfwSetCharCallback(long window, GLFWCharCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWCharCallbackI previous = state.character;
        state.character = callback;
        return previous instanceof GLFWCharCallback ? (GLFWCharCallback) previous : null;
    }

    @Patch(owner = GLFW)
    public static GLFWCharModsCallback glfwSetCharModsCallback(long window, GLFWCharModsCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWCharModsCallbackI previous = state.charMods;
        state.charMods = callback;
        return previous instanceof GLFWCharModsCallback ? (GLFWCharModsCallback) previous : null;
    }

    @Patch(owner = GLFW)
    public static GLFWMouseButtonCallback glfwSetMouseButtonCallback(long window, GLFWMouseButtonCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWMouseButtonCallbackI previous = state.mouseButton;
        state.mouseButton = callback;
        return previous instanceof GLFWMouseButtonCallback ? (GLFWMouseButtonCallback) previous : null;
    }

    @Patch(owner = GLFW)
    public static GLFWCursorPosCallback glfwSetCursorPosCallback(long window, GLFWCursorPosCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWCursorPosCallbackI previous = state.cursorPos;
        state.cursorPos = callback;
        return previous instanceof GLFWCursorPosCallback ? (GLFWCursorPosCallback) previous : null;
    }

    @Patch(owner = GLFW)
    public static GLFWCursorEnterCallback glfwSetCursorEnterCallback(long window, GLFWCursorEnterCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWCursorEnterCallbackI previous = state.cursorEnter;
        state.cursorEnter = callback;
        return previous instanceof GLFWCursorEnterCallback ? (GLFWCursorEnterCallback) previous : null;
    }

    @Patch(owner = GLFW)
    public static GLFWScrollCallback glfwSetScrollCallback(long window, GLFWScrollCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWScrollCallbackI previous = state.scroll;
        state.scroll = callback;
        return previous instanceof GLFWScrollCallback ? (GLFWScrollCallback) previous : null;
    }

    @Patch(owner = GLFW)
    public static GLFWWindowSizeCallback glfwSetWindowSizeCallback(long window, GLFWWindowSizeCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWWindowSizeCallbackI previous = state.windowSize;
        state.windowSize = callback;
        return previous instanceof GLFWWindowSizeCallback ? (GLFWWindowSizeCallback) previous : null;
    }

    @Patch(owner = GLFW)
    public static GLFWFramebufferSizeCallback glfwSetFramebufferSizeCallback(long window, GLFWFramebufferSizeCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWFramebufferSizeCallbackI previous = state.framebufferSize;
        state.framebufferSize = callback;
        return previous instanceof GLFWFramebufferSizeCallback ? (GLFWFramebufferSizeCallback) previous : null;
    }

    @Patch(owner = GLFW)
    public static GLFWWindowFocusCallback glfwSetWindowFocusCallback(long window, GLFWWindowFocusCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWWindowFocusCallbackI previous = state.focus;
        state.focus = callback;
        return previous instanceof GLFWWindowFocusCallback ? (GLFWWindowFocusCallback) previous : null;
    }

    @Patch(owner = GLFW)
    public static GLFWWindowCloseCallback glfwSetWindowCloseCallback(long window, GLFWWindowCloseCallbackI callback) {
        GlfwWindow state = GlfwWindows.get(window);
        GLFWWindowCloseCallbackI previous = state.close;
        state.close = callback;
        return previous instanceof GLFWWindowCloseCallback ? (GLFWWindowCloseCallback) previous : null;
    }

}
