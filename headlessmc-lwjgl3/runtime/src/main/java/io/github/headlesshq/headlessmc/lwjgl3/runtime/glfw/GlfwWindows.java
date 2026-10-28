package io.github.headlesshq.headlessmc.lwjgl3.runtime.glfw;

import io.github.headlesshq.headlessmc.lwjgl3.runtime.Input;
import io.github.headlesshq.headlessmc.lwjgl3.runtime.InputEvent;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The GLFW windows of the application with their callbacks, and the dispatching of injected {@link Input} events.
 * Injected input goes to the main window, the oldest window that is still open.
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

    public static void remove(long handle) {
        synchronized (WINDOWS) {
            WINDOWS.remove(handle);
        }
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
                GLFWKeyCallbackI callback = window.key;
                if (callback != null) {
                    callback.invoke(handle, event.code, event.code, event.action, event.mods);
                }

                break;
            }
            case InputEvent.CHAR: {
                GLFWCharCallbackI callback = window.character;
                if (callback != null) {
                    callback.invoke(handle, event.code);
                }

                GLFWCharModsCallbackI modsCallback = window.charMods;
                if (modsCallback != null) {
                    modsCallback.invoke(handle, event.code, event.mods);
                }

                break;
            }
            case InputEvent.MOUSE_BUTTON: {
                GLFWMouseButtonCallbackI callback = window.mouseButton;
                if (callback != null) {
                    callback.invoke(handle, event.code, event.action, event.mods);
                }

                break;
            }
            case InputEvent.CURSOR_POS: {
                if (!window.cursorEntered) {
                    window.cursorEntered = true;
                    GLFWCursorEnterCallbackI enterCallback = window.cursorEnter;
                    if (enterCallback != null) {
                        enterCallback.invoke(handle, true);
                    }
                }

                GLFWCursorPosCallbackI callback = window.cursorPos;
                if (callback != null) {
                    callback.invoke(handle, event.x, event.y);
                }

                break;
            }
            case InputEvent.SCROLL: {
                GLFWScrollCallbackI callback = window.scroll;
                if (callback != null) {
                    callback.invoke(handle, event.x, event.y);
                }

                break;
            }
            case InputEvent.RESIZE: {
                GLFWWindowSizeCallbackI sizeCallback = window.windowSize;
                if (sizeCallback != null) {
                    sizeCallback.invoke(handle, event.code, event.action);
                }

                GLFWFramebufferSizeCallbackI framebufferCallback = window.framebufferSize;
                if (framebufferCallback != null) {
                    framebufferCallback.invoke(handle, event.code, event.action);
                }

                break;
            }
            case InputEvent.FOCUS: {
                GLFWWindowFocusCallbackI callback = window.focus;
                if (callback != null) {
                    callback.invoke(handle, event.code != 0);
                }

                break;
            }
            case InputEvent.CLOSE: {
                window.shouldClose = true;
                GLFWWindowCloseCallbackI callback = window.close;
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
