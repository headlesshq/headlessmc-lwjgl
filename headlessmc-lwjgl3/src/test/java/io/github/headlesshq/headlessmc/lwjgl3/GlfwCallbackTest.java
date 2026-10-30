package io.github.headlesshq.headlessmc.lwjgl3;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCharModsCallback;
import org.lwjgl.glfw.GLFWCursorEnterCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWDropCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWIMEStatusCallback;
import org.lwjgl.glfw.GLFWJoystickCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMonitorCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWPreeditCallback;
import org.lwjgl.glfw.GLFWPreeditCandidateCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.glfw.GLFWWindowContentScaleCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.glfw.GLFWWindowIconifyCallback;
import org.lwjgl.glfw.GLFWWindowMaximizeCallback;
import org.lwjgl.glfw.GLFWWindowPosCallback;
import org.lwjgl.glfw.GLFWWindowRefreshCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.system.Callback;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Like GLFW, every callback setter returns the callback that was set before, a callback object with the same address,
 * so that applications can restore and free it. Minecraft's {@code GLFWErrorScope} sets its own error callback,
 * restores the previous one and fails if the setter does not return its callback.
 */
class GlfwCallbackTest {
    private static final Map<Class<?>, Supplier<Callback>> CALLBACKS = new HashMap<>();

    static {
        CALLBACKS.put(GLFWCharCallback.class, () -> GLFWCharCallback.create((window, codepoint) -> { }));
        CALLBACKS.put(GLFWCharModsCallback.class, () -> GLFWCharModsCallback.create((window, codepoint, mods) -> { }));
        CALLBACKS.put(GLFWCursorEnterCallback.class, () -> GLFWCursorEnterCallback.create((window, entered) -> { }));
        CALLBACKS.put(GLFWCursorPosCallback.class, () -> GLFWCursorPosCallback.create((window, x, y) -> { }));
        CALLBACKS.put(GLFWDropCallback.class, () -> GLFWDropCallback.create((window, count, names) -> { }));
        CALLBACKS.put(GLFWErrorCallback.class, () -> GLFWErrorCallback.create((error, description) -> { }));
        CALLBACKS.put(GLFWFramebufferSizeCallback.class, () -> GLFWFramebufferSizeCallback.create((window, width, height) -> { }));
        CALLBACKS.put(GLFWIMEStatusCallback.class, () -> GLFWIMEStatusCallback.create(window -> { }));
        CALLBACKS.put(GLFWJoystickCallback.class, () -> GLFWJoystickCallback.create((jid, event) -> { }));
        CALLBACKS.put(GLFWKeyCallback.class, () -> GLFWKeyCallback.create((window, key, scancode, action, mods) -> { }));
        CALLBACKS.put(GLFWMonitorCallback.class, () -> GLFWMonitorCallback.create((monitor, event) -> { }));
        CALLBACKS.put(GLFWMouseButtonCallback.class, () -> GLFWMouseButtonCallback.create((window, button, action, mods) -> { }));
        CALLBACKS.put(GLFWPreeditCallback.class, () -> GLFWPreeditCallback.create(
            (window, count, string, blocks, sizes, focused, caret) -> { }));
        CALLBACKS.put(GLFWPreeditCandidateCallback.class, () -> GLFWPreeditCandidateCallback.create(
            (window, count, selected, first, size) -> { }));
        CALLBACKS.put(GLFWScrollCallback.class, () -> GLFWScrollCallback.create((window, x, y) -> { }));
        CALLBACKS.put(GLFWWindowCloseCallback.class, () -> GLFWWindowCloseCallback.create(window -> { }));
        CALLBACKS.put(GLFWWindowContentScaleCallback.class, () -> GLFWWindowContentScaleCallback.create((window, x, y) -> { }));
        CALLBACKS.put(GLFWWindowFocusCallback.class, () -> GLFWWindowFocusCallback.create((window, focused) -> { }));
        CALLBACKS.put(GLFWWindowIconifyCallback.class, () -> GLFWWindowIconifyCallback.create((window, iconified) -> { }));
        CALLBACKS.put(GLFWWindowMaximizeCallback.class, () -> GLFWWindowMaximizeCallback.create((window, maximized) -> { }));
        CALLBACKS.put(GLFWWindowPosCallback.class, () -> GLFWWindowPosCallback.create((window, x, y) -> { }));
        CALLBACKS.put(GLFWWindowRefreshCallback.class, () -> GLFWWindowRefreshCallback.create(window -> { }));
        CALLBACKS.put(GLFWWindowSizeCallback.class, () -> GLFWWindowSizeCallback.create((window, width, height) -> { }));
    }

    /**
     * Covers every setter of {@code GLFW}, so that a setter added by a new LWJGL version fails here until it is added
     * to {@link #CALLBACKS}.
     */
    @Test
    void everySetterReturnsThePreviousCallback() throws ReflectiveOperationException {
        assertTrue(GLFW.glfwInit());
        long window = GLFW.glfwCreateWindow(854, 480, "Callbacks", 0L, 0L);
        try {
            int setters = 0;
            for (Method setter : GLFW.class.getMethods()) {
                if (!setter.getName().matches("glfwSet\\w*Callback") || !Callback.class.isAssignableFrom(setter.getReturnType())) {
                    continue;
                }

                Supplier<Callback> factory = CALLBACKS.get(setter.getReturnType());
                assertNotNull(factory, "No callback for " + setter);
                boolean global = setter.getParameterCount() == 1;
                Callback first = factory.get();
                Callback second = factory.get();
                Object initial = set(setter, global, window, first);
                assertEquals(first, set(setter, global, window, second), setter.getName());
                assertEquals(second, set(setter, global, window, null), setter.getName());
                assertNull(set(setter, global, window, initial), setter.getName());
                first.free();
                second.free();
                setters++;
            }

            assertEquals(CALLBACKS.size(), setters);
        } finally {
            GLFW.glfwDestroyWindow(window);
        }
    }

    /**
     * What Minecraft's {@code GLFWErrorScope} does.
     */
    @Test
    void errorScope() {
        GLFWErrorCallback outer = GLFWErrorCallback.create((error, description) -> { });
        GLFWErrorCallback initial = GLFW.glfwSetErrorCallback(outer);
        GLFWErrorCallback expected = GLFWErrorCallback.create((error, description) -> { });
        GLFWErrorCallback previous = GLFW.glfwSetErrorCallback(expected);
        assertEquals(outer.address(), previous.address());
        GLFWErrorCallback current = GLFW.glfwSetErrorCallback(previous);
        assertNotNull(current);
        assertEquals(expected.address(), current.address());
        current.close();
        assertEquals(outer, GLFW.glfwSetErrorCallback(initial));
        outer.free();
    }

    /**
     * A callback set as a lambda is returned as a wrapper of the same function pointer.
     */
    @Test
    void lambdasKeepTheirAddress() {
        assertTrue(GLFW.glfwInit());
        long window = GLFW.glfwCreateWindow(854, 480, "Lambda", 0L, 0L);
        try {
            assertNull(GLFW.glfwSetKeyCallback(window, (handle, key, scancode, action, mods) -> { }));
            GLFWKeyCallback lambda = GLFW.glfwSetKeyCallback(window, null);
            assertNotNull(lambda);
            assertNotNull(Callback.get(lambda.address()));
            lambda.free();
        } finally {
            GLFW.glfwDestroyWindow(window);
        }
    }

    @Test
    void freeCallbacksFreesAndRemovesThem() {
        assertTrue(GLFW.glfwInit());
        long window = GLFW.glfwCreateWindow(854, 480, "Free", 0L, 0L);
        try {
            GLFWKeyCallback key = GLFWKeyCallback.create((handle, k, scancode, action, mods) -> { });
            GLFW.glfwSetKeyCallback(window, key);
            Callbacks.glfwFreeCallbacks(window);
            assertNull(Callback.get(key.address()));
            assertNull(GLFW.glfwSetKeyCallback(window, null));
        } finally {
            GLFW.glfwDestroyWindow(window);
        }
    }

    private static Object set(Method setter, boolean global, long window, Object callback) throws ReflectiveOperationException {
        return global ? setter.invoke(null, callback) : setter.invoke(null, window, callback);
    }

}
