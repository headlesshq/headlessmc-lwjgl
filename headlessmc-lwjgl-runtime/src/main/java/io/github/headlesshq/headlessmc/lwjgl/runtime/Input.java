package io.github.headlesshq.headlessmc.lwjgl.runtime;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * The input and window state of the headless application, shared by the API and the window backends.
 * The polling state changes immediately when input is injected, the events are queued until the backend dispatches
 * them on the thread that pumps events. Keys, buttons, actions and modifiers use the GLFW values.
 */
public final class Input {
    public static final int RELEASE = 0;
    public static final int PRESS = 1;
    public static final int REPEAT = 2;
    private static final int KEYS = 512;
    private static final int BUTTONS = 8;

    private static final AtomicIntegerArray PRESSED_KEYS = new AtomicIntegerArray(KEYS);
    private static final AtomicIntegerArray PRESSED_BUTTONS = new AtomicIntegerArray(BUTTONS);
    private static final Queue<InputEvent> EVENTS = new ConcurrentLinkedQueue<>();
    private static final Object LOCK = new Object();
    private static volatile double cursorX;
    private static volatile double cursorY;
    private static volatile int width = WindowConfig.WIDTH;
    private static volatile int height = WindowConfig.HEIGHT;
    private static volatile boolean focused = true;
    private static volatile boolean registered;

    private Input() {
        throw new AssertionError();
    }

    /**
     * Registers the API implementation, called when the application initialises a window backend.
     * Does nothing if the API jar is not available.
     */
    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        try {
            HeadlessLwjglImpl.register();
        } catch (LinkageError e) {
            Log.debug("headlessmc-lwjgl-api is not available, input cannot be injected: " + e);
        }
    }

    public static void key(int key, int action, int mods) {
        if (key >= 0 && key < KEYS) {
            int previous = PRESSED_KEYS.getAndSet(key, action == RELEASE ? 0 : 1);
            if (action == PRESS && previous != 0) {
                action = REPEAT;
            }
        }

        EVENTS.add(new InputEvent(InputEvent.KEY, key, action, mods, 0.0, 0.0));
    }

    public static void character(int codepoint, int mods) {
        EVENTS.add(new InputEvent(InputEvent.CHAR, codepoint, 0, mods, 0.0, 0.0));
    }

    public static void mouseButton(int button, int action, int mods) {
        if (button >= 0 && button < BUTTONS) {
            PRESSED_BUTTONS.set(button, action == RELEASE ? 0 : 1);
        }

        EVENTS.add(new InputEvent(InputEvent.MOUSE_BUTTON, button, action, mods, 0.0, 0.0));
    }

    public static void moveCursor(double x, double y) {
        synchronized (LOCK) {
            cursorX = x;
            cursorY = y;
        }

        EVENTS.add(new InputEvent(InputEvent.CURSOR_POS, 0, 0, 0, x, y));
    }

    public static void moveCursorBy(double dx, double dy) {
        double x;
        double y;
        synchronized (LOCK) {
            x = cursorX + dx;
            y = cursorY + dy;
            cursorX = x;
            cursorY = y;
        }

        EVENTS.add(new InputEvent(InputEvent.CURSOR_POS, 0, 0, 0, x, y));
    }

    /**
     * Sets the cursor position without an event, like {@code glfwSetCursorPos} does.
     */
    public static void setCursor(double x, double y) {
        synchronized (LOCK) {
            cursorX = x;
            cursorY = y;
        }
    }

    public static void scroll(double dx, double dy) {
        EVENTS.add(new InputEvent(InputEvent.SCROLL, 0, 0, 0, dx, dy));
    }

    public static void resize(int newWidth, int newHeight) {
        setSize(newWidth, newHeight);
        EVENTS.add(new InputEvent(InputEvent.RESIZE, newWidth, newHeight, 0, 0.0, 0.0));
    }

    /**
     * Sets the window size without an event, e.g. when the application creates its window.
     */
    public static void setSize(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;
    }

    public static void focus(boolean focus) {
        focused = focus;
        EVENTS.add(new InputEvent(InputEvent.FOCUS, focus ? 1 : 0, 0, 0, 0.0, 0.0));
    }

    public static void requestClose() {
        EVENTS.add(new InputEvent(InputEvent.CLOSE, 0, 0, 0, 0.0, 0.0));
    }

    /**
     * @return the next event to dispatch, or {@code null} if there is none.
     */
    public static InputEvent poll() {
        return EVENTS.poll();
    }

    public static boolean isKeyPressed(int key) {
        return key >= 0 && key < KEYS && PRESSED_KEYS.get(key) != 0;
    }

    public static boolean isButtonPressed(int button) {
        return button >= 0 && button < BUTTONS && PRESSED_BUTTONS.get(button) != 0;
    }

    public static double cursorX() {
        return cursorX;
    }

    public static double cursorY() {
        return cursorY;
    }

    public static int width() {
        return width;
    }

    public static int height() {
        return height;
    }

    public static boolean isFocused() {
        return focused;
    }

}
