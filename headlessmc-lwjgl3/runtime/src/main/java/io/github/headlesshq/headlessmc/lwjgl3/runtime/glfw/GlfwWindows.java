package io.github.headlesshq.headlessmc.lwjgl3.runtime.glfw;

import io.github.headlesshq.headlessmc.lwjgl.runtime.CallbackSlots;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Input;
import io.github.headlesshq.headlessmc.lwjgl.runtime.InputEvent;
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWCharModsCallbackI;
import org.lwjgl.glfw.GLFWCursorEnterCallbackI;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWWindowCloseCallbackI;
import org.lwjgl.glfw.GLFWWindowFocusCallbackI;
import org.lwjgl.glfw.GLFWWindowSizeCallbackI;
import org.lwjgl.system.Callback;
import org.lwjgl.system.CallbackI;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The GLFW windows of the application, and the dispatching of injected {@link Input} events to their callbacks.
 * Injected input goes to the main window, the oldest window that is still open.
 * The callbacks of a window are the function pointers in its {@link CallbackSlots}, stored by the patched
 * {@code nglfwSet*Callback} setters, which map back to the callback objects through {@link Callback#get}.
 */
public final class GlfwWindows {
    /**
     * The window hints for the next window, see {@code glfwWindowHint}.
     */
    static final Map<Integer, Integer> HINTS = new ConcurrentHashMap<>();
    private static final Map<Long, GlfwWindow> WINDOWS = new LinkedHashMap<>();

    private GlfwWindows() {
        throw new AssertionError();
    }

    /**
     * @param handle the window handle.
     * @return the state of the window, created if the handle is unknown.
     */
    public static GlfwWindow get(long handle) {
        synchronized (WINDOWS) {
            return WINDOWS.computeIfAbsent(handle, k -> new GlfwWindow());
        }
    }

    /**
     * Removes a destroyed window and its callbacks, which, like with GLFW, are not freed.
     *
     * @param handle the window handle.
     */
    public static void remove(long handle) {
        synchronized (WINDOWS) {
            WINDOWS.remove(handle);
        }

        CallbackSlots.clear(handle);
    }

    /**
     * @param handle the window handle.
     * @param setter the native setter of the callback, e.g. {@code nglfwSetKeyCallback}.
     * @return the callback, or {@code null} if none is set or it has been freed.
     */
    private static <T extends CallbackI> T callback(long handle, String setter) {
        long function = CallbackSlots.get(setter, handle);
        return function == 0L ? null : Callback.<T>get(function);
    }

    private static Map.Entry<Long, GlfwWindow> main() {
        synchronized (WINDOWS) {
            return WINDOWS.isEmpty() ? null : WINDOWS.entrySet().iterator().next();
        }
    }

    /**
     * Dispatches all queued events to the callbacks of the main window, on the calling thread.
     *
     * @return {@code true} if there was at least one event.
     */
    public static boolean dispatch() {
        boolean dispatched = false;
        InputEvent event;
        while ((event = Input.poll()) != null) {
            dispatched = true;
            Map.Entry<Long, GlfwWindow> main = main();
            if (main != null) {
                dispatch(main.getKey(), main.getValue(), event);
            }
        }

        return dispatched;
    }

    private static void dispatch(long handle, GlfwWindow window, InputEvent event) {
        switch (event.type) {
            case InputEvent.KEY: {
                GLFWKeyCallbackI callback = callback(handle, "nglfwSetKeyCallback");
                if (callback != null) {
                    callback.invoke(handle, event.code, event.code, event.action, event.mods);
                }

                break;
            }
            case InputEvent.CHAR: {
                GLFWCharCallbackI callback = callback(handle, "nglfwSetCharCallback");
                if (callback != null) {
                    callback.invoke(handle, event.code);
                }

                GLFWCharModsCallbackI modsCallback = callback(handle, "nglfwSetCharModsCallback");
                if (modsCallback != null) {
                    modsCallback.invoke(handle, event.code, event.mods);
                }

                break;
            }
            case InputEvent.MOUSE_BUTTON: {
                GLFWMouseButtonCallbackI callback = callback(handle, "nglfwSetMouseButtonCallback");
                if (callback != null) {
                    callback.invoke(handle, event.code, event.action, event.mods);
                }

                break;
            }
            case InputEvent.CURSOR_POS: {
                if (!window.cursorEntered) {
                    window.cursorEntered = true;
                    GLFWCursorEnterCallbackI enterCallback = callback(handle, "nglfwSetCursorEnterCallback");
                    if (enterCallback != null) {
                        enterCallback.invoke(handle, true);
                    }
                }

                GLFWCursorPosCallbackI callback = callback(handle, "nglfwSetCursorPosCallback");
                if (callback != null) {
                    callback.invoke(handle, event.x, event.y);
                }

                break;
            }
            case InputEvent.SCROLL: {
                GLFWScrollCallbackI callback = callback(handle, "nglfwSetScrollCallback");
                if (callback != null) {
                    callback.invoke(handle, event.x, event.y);
                }

                break;
            }
            case InputEvent.RESIZE: {
                GLFWWindowSizeCallbackI sizeCallback = callback(handle, "nglfwSetWindowSizeCallback");
                if (sizeCallback != null) {
                    sizeCallback.invoke(handle, event.code, event.action);
                }

                GLFWFramebufferSizeCallbackI framebufferCallback = callback(handle, "nglfwSetFramebufferSizeCallback");
                if (framebufferCallback != null) {
                    framebufferCallback.invoke(handle, event.code, event.action);
                }

                break;
            }
            case InputEvent.FOCUS: {
                GLFWWindowFocusCallbackI callback = callback(handle, "nglfwSetWindowFocusCallback");
                if (callback != null) {
                    callback.invoke(handle, event.code != 0);
                }

                break;
            }
            case InputEvent.CLOSE: {
                window.shouldClose = true;
                GLFWWindowCloseCallbackI callback = callback(handle, "nglfwSetWindowCloseCallback");
                if (callback != null) {
                    callback.invoke(handle);
                }

                break;
            }
            default:
                break;
        }
    }

}
