package io.github.headlesshq.headlessmc.lwjgl3.runtime.sdl;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Input;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LAST;
import static org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_COUNT;

/**
 * The SDL state of the application: its windows, relative mouse mode, text input and the keyboard state array.
 * Injected input goes to the main window, which has the window id {@value #MAIN_WINDOW_ID}.
 */
public final class SdlWindows {
    public static final int MAIN_WINDOW_ID = 1;

    private static final Map<Long, Integer> WINDOW_IDS = new ConcurrentHashMap<>();
    private static final Map<Long, Boolean> RELATIVE_MODE = new ConcurrentHashMap<>();
    private static final ByteBuffer KEYBOARD = ByteBuffer.allocate(SDL_SCANCODE_COUNT);
    private static volatile boolean textInput;
    private static float relativeX;
    private static float relativeY;

    private SdlWindows() {
        throw new AssertionError();
    }

    public static int register(long window) {
        int id = WINDOW_IDS.isEmpty() ? MAIN_WINDOW_ID : MAIN_WINDOW_ID + WINDOW_IDS.size();
        WINDOW_IDS.put(window, id);
        return id;
    }

    public static void remove(long window) {
        WINDOW_IDS.remove(window);
        RELATIVE_MODE.remove(window);
    }

    public static int id(long window) {
        return WINDOW_IDS.getOrDefault(window, 0);
    }

    public static long window(int id) {
        for (Map.Entry<Long, Integer> entry : WINDOW_IDS.entrySet()) {
            if (entry.getValue() == id) {
                return entry.getKey();
            }
        }

        return 0L;
    }

    /**
     * @return the open windows, in no particular order.
     */
    public static long[] windows() {
        return WINDOW_IDS.keySet().stream().mapToLong(Long::longValue).toArray();
    }

    public static long mainWindow() {
        return window(MAIN_WINDOW_ID);
    }

    public static boolean isRelativeMode(long window) {
        return RELATIVE_MODE.getOrDefault(window, false);
    }

    public static void setRelativeMode(long window, boolean enabled) {
        RELATIVE_MODE.put(window, enabled);
    }

    public static boolean isTextInputActive() {
        return textInput;
    }

    public static void setTextInputActive(boolean active) {
        textInput = active;
    }

    /**
     * @return the keyboard state array indexed by scancode, updated to the current state. Like SDL's array, the
     * same buffer is returned every time.
     */
    public static synchronized ByteBuffer keyboardState() {
        for (int key = 0; key <= GLFW_KEY_LAST; key++) {
            int scancode = SdlKeys.scancode(key);
            if (scancode > 0) {
                KEYBOARD.put(scancode, (byte) (Input.isKeyPressed(key) ? 1 : 0));
            }
        }

        KEYBOARD.clear();
        return KEYBOARD;
    }

    /**
     * @return the pressed mouse buttons, bit {@code button - 1} for every SDL button.
     */
    public static int buttonMask() {
        int mask = 0;
        for (int button = 0; button < 8; button++) {
            if (Input.isButtonPressed(button)) {
                mask |= 1 << (SdlKeys.button(button) - 1);
            }
        }

        return mask;
    }

    static synchronized void addRelativeMotion(float x, float y) {
        relativeX += x;
        relativeY += y;
    }

    /**
     * @return the relative motion since the last call, see {@code SDL_GetRelativeMouseState}.
     */
    public static synchronized float[] takeRelativeMotion() {
        float[] result = {relativeX, relativeY};
        relativeX = 0;
        relativeY = 0;
        return result;
    }

}
