package io.github.headlesshq.headlessmc.lwjgl2.runtime;

import io.github.headlesshq.headlessmc.lwjgl.api.Keys;

import java.util.Arrays;

import static org.lwjgl.input.Keyboard.*;

/**
 * Translates between the GLFW key codes of the input API and the key codes of LWJGL 2's {@code Keyboard}.
 */
public final class Lwjgl2Keys {
    private static final int[] LWJGL_KEYS = new int[Keys.MENU + 1];
    private static final int[] GLFW_KEYS = new int[KEYBOARD_SIZE];

    static {
        Arrays.fill(GLFW_KEYS, -1);
        map(Keys.SPACE, KEY_SPACE);
        map(Keys.APOSTROPHE, KEY_APOSTROPHE);
        map(Keys.COMMA, KEY_COMMA);
        map(Keys.MINUS, KEY_MINUS);
        map(Keys.PERIOD, KEY_PERIOD);
        map(Keys.SLASH, KEY_SLASH);
        map(Keys.DIGIT_0, KEY_0);
        int[] digits = {KEY_1, KEY_2, KEY_3, KEY_4, KEY_5, KEY_6, KEY_7, KEY_8, KEY_9};
        for (int i = 0; i < digits.length; i++) {
            map(Keys.DIGIT_1 + i, digits[i]);
        }

        map(Keys.SEMICOLON, KEY_SEMICOLON);
        map(Keys.EQUAL, KEY_EQUALS);
        int[] letters = {KEY_A, KEY_B, KEY_C, KEY_D, KEY_E, KEY_F, KEY_G, KEY_H, KEY_I, KEY_J, KEY_K, KEY_L, KEY_M,
            KEY_N, KEY_O, KEY_P, KEY_Q, KEY_R, KEY_S, KEY_T, KEY_U, KEY_V, KEY_W, KEY_X, KEY_Y, KEY_Z};
        for (int i = 0; i < letters.length; i++) {
            map(Keys.A + i, letters[i]);
        }

        map(Keys.LEFT_BRACKET, KEY_LBRACKET);
        map(Keys.BACKSLASH, KEY_BACKSLASH);
        map(Keys.RIGHT_BRACKET, KEY_RBRACKET);
        map(Keys.GRAVE_ACCENT, KEY_GRAVE);
        map(Keys.ESCAPE, KEY_ESCAPE);
        map(Keys.ENTER, KEY_RETURN);
        map(Keys.TAB, KEY_TAB);
        map(Keys.BACKSPACE, KEY_BACK);
        map(Keys.INSERT, KEY_INSERT);
        map(Keys.DELETE, KEY_DELETE);
        map(Keys.RIGHT, KEY_RIGHT);
        map(Keys.LEFT, KEY_LEFT);
        map(Keys.DOWN, KEY_DOWN);
        map(Keys.UP, KEY_UP);
        map(Keys.PAGE_UP, KEY_PRIOR);
        map(Keys.PAGE_DOWN, KEY_NEXT);
        map(Keys.HOME, KEY_HOME);
        map(Keys.END, KEY_END);
        map(Keys.CAPS_LOCK, KEY_CAPITAL);
        map(Keys.SCROLL_LOCK, KEY_SCROLL);
        map(Keys.NUM_LOCK, KEY_NUMLOCK);
        map(Keys.PRINT_SCREEN, KEY_SYSRQ);
        map(Keys.PAUSE, KEY_PAUSE);
        int[] functionKeys = {KEY_F1, KEY_F2, KEY_F3, KEY_F4, KEY_F5, KEY_F6, KEY_F7, KEY_F8, KEY_F9, KEY_F10, KEY_F11,
            KEY_F12, KEY_F13, KEY_F14, KEY_F15, KEY_F16, KEY_F17, KEY_F18, KEY_F19};
        for (int i = 0; i < functionKeys.length; i++) {
            map(Keys.F1 + i, functionKeys[i]);
        }

        int[] keypad = {KEY_NUMPAD0, KEY_NUMPAD1, KEY_NUMPAD2, KEY_NUMPAD3, KEY_NUMPAD4, KEY_NUMPAD5, KEY_NUMPAD6,
            KEY_NUMPAD7, KEY_NUMPAD8, KEY_NUMPAD9};
        for (int i = 0; i < keypad.length; i++) {
            map(Keys.KP_0 + i, keypad[i]);
        }

        map(Keys.KP_DECIMAL, KEY_DECIMAL);
        map(Keys.KP_DIVIDE, KEY_DIVIDE);
        map(Keys.KP_MULTIPLY, KEY_MULTIPLY);
        map(Keys.KP_SUBTRACT, KEY_SUBTRACT);
        map(Keys.KP_ADD, KEY_ADD);
        map(Keys.KP_ENTER, KEY_NUMPADENTER);
        map(Keys.KP_EQUAL, KEY_NUMPADEQUALS);
        map(Keys.LEFT_SHIFT, KEY_LSHIFT);
        map(Keys.LEFT_CONTROL, KEY_LCONTROL);
        map(Keys.LEFT_ALT, KEY_LMENU);
        map(Keys.LEFT_SUPER, KEY_LMETA);
        map(Keys.RIGHT_SHIFT, KEY_RSHIFT);
        map(Keys.RIGHT_CONTROL, KEY_RCONTROL);
        map(Keys.RIGHT_ALT, KEY_RMENU);
        map(Keys.RIGHT_SUPER, KEY_RMETA);
        map(Keys.MENU, KEY_APPS);
    }

    private Lwjgl2Keys() {
        throw new AssertionError();
    }

    private static void map(int glfwKey, int lwjglKey) {
        LWJGL_KEYS[glfwKey] = lwjglKey;
        GLFW_KEYS[lwjglKey] = glfwKey;
    }

    /**
     * @param glfwKey a GLFW key code.
     * @return the LWJGL 2 key code, {@code KEY_NONE} for unknown keys.
     */
    public static int lwjglKey(int glfwKey) {
        return glfwKey >= 0 && glfwKey < LWJGL_KEYS.length ? LWJGL_KEYS[glfwKey] : KEY_NONE;
    }

    /**
     * @param lwjglKey an LWJGL 2 key code.
     * @return the GLFW key code, -1 for unknown keys.
     */
    public static int glfwKey(int lwjglKey) {
        return lwjglKey >= 0 && lwjglKey < GLFW_KEYS.length ? GLFW_KEYS[lwjglKey] : -1;
    }

}
