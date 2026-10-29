package io.github.headlesshq.headlessmc.lwjgl3.runtime.sdl;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.sdl.SDLKeycode.*;
import static org.lwjgl.sdl.SDLScancode.*;

/**
 * Translates the GLFW key codes, modifiers and mouse buttons of the input API into SDL scancodes, keycodes,
 * modifiers and buttons, as SDL reports them for a US keyboard layout.
 */
public final class SdlKeys {
    private static final int[] SCANCODES = new int[GLFW_KEY_LAST + 1];
    private static final int[] KEYS = new int[SDL_SCANCODE_COUNT];

    static {
        Arrays.fill(SCANCODES, SDL_SCANCODE_UNKNOWN);
        for (int i = 0; i < 26; i++) {
            map(GLFW_KEY_A + i, SDL_SCANCODE_A + i, 'a' + i);
        }

        map(GLFW_KEY_0, SDL_SCANCODE_0, '0');
        for (int i = 1; i <= 9; i++) {
            map(GLFW_KEY_0 + i, SDL_SCANCODE_1 + i - 1, '0' + i);
        }

        map(GLFW_KEY_SPACE, SDL_SCANCODE_SPACE, ' ');
        map(GLFW_KEY_APOSTROPHE, SDL_SCANCODE_APOSTROPHE, '\'');
        map(GLFW_KEY_COMMA, SDL_SCANCODE_COMMA, ',');
        map(GLFW_KEY_MINUS, SDL_SCANCODE_MINUS, '-');
        map(GLFW_KEY_PERIOD, SDL_SCANCODE_PERIOD, '.');
        map(GLFW_KEY_SLASH, SDL_SCANCODE_SLASH, '/');
        map(GLFW_KEY_SEMICOLON, SDL_SCANCODE_SEMICOLON, ';');
        map(GLFW_KEY_EQUAL, SDL_SCANCODE_EQUALS, '=');
        map(GLFW_KEY_LEFT_BRACKET, SDL_SCANCODE_LEFTBRACKET, '[');
        map(GLFW_KEY_BACKSLASH, SDL_SCANCODE_BACKSLASH, '\\');
        map(GLFW_KEY_RIGHT_BRACKET, SDL_SCANCODE_RIGHTBRACKET, ']');
        map(GLFW_KEY_GRAVE_ACCENT, SDL_SCANCODE_GRAVE, '`');
        map(GLFW_KEY_ESCAPE, SDL_SCANCODE_ESCAPE, 0x1B);
        map(GLFW_KEY_ENTER, SDL_SCANCODE_RETURN, '\r');
        map(GLFW_KEY_TAB, SDL_SCANCODE_TAB, '\t');
        map(GLFW_KEY_BACKSPACE, SDL_SCANCODE_BACKSPACE, '\b');
        map(GLFW_KEY_DELETE, SDL_SCANCODE_DELETE, 0x7F);
        map(GLFW_KEY_INSERT, SDL_SCANCODE_INSERT);
        map(GLFW_KEY_RIGHT, SDL_SCANCODE_RIGHT);
        map(GLFW_KEY_LEFT, SDL_SCANCODE_LEFT);
        map(GLFW_KEY_DOWN, SDL_SCANCODE_DOWN);
        map(GLFW_KEY_UP, SDL_SCANCODE_UP);
        map(GLFW_KEY_PAGE_UP, SDL_SCANCODE_PAGEUP);
        map(GLFW_KEY_PAGE_DOWN, SDL_SCANCODE_PAGEDOWN);
        map(GLFW_KEY_HOME, SDL_SCANCODE_HOME);
        map(GLFW_KEY_END, SDL_SCANCODE_END);
        map(GLFW_KEY_CAPS_LOCK, SDL_SCANCODE_CAPSLOCK);
        map(GLFW_KEY_SCROLL_LOCK, SDL_SCANCODE_SCROLLLOCK);
        map(GLFW_KEY_NUM_LOCK, SDL_SCANCODE_NUMLOCKCLEAR);
        map(GLFW_KEY_PRINT_SCREEN, SDL_SCANCODE_PRINTSCREEN);
        map(GLFW_KEY_PAUSE, SDL_SCANCODE_PAUSE);
        for (int i = 0; i < 12; i++) {
            map(GLFW_KEY_F1 + i, SDL_SCANCODE_F1 + i);
        }

        for (int i = 0; i < 12; i++) {
            map(GLFW_KEY_F13 + i, SDL_SCANCODE_F13 + i);
        }

        map(GLFW_KEY_KP_0, SDL_SCANCODE_KP_0);
        for (int i = 1; i <= 9; i++) {
            map(GLFW_KEY_KP_0 + i, SDL_SCANCODE_KP_1 + i - 1);
        }

        map(GLFW_KEY_KP_DECIMAL, SDL_SCANCODE_KP_PERIOD);
        map(GLFW_KEY_KP_DIVIDE, SDL_SCANCODE_KP_DIVIDE);
        map(GLFW_KEY_KP_MULTIPLY, SDL_SCANCODE_KP_MULTIPLY);
        map(GLFW_KEY_KP_SUBTRACT, SDL_SCANCODE_KP_MINUS);
        map(GLFW_KEY_KP_ADD, SDL_SCANCODE_KP_PLUS);
        map(GLFW_KEY_KP_ENTER, SDL_SCANCODE_KP_ENTER);
        map(GLFW_KEY_KP_EQUAL, SDL_SCANCODE_KP_EQUALS);
        map(GLFW_KEY_LEFT_SHIFT, SDL_SCANCODE_LSHIFT);
        map(GLFW_KEY_LEFT_CONTROL, SDL_SCANCODE_LCTRL);
        map(GLFW_KEY_LEFT_ALT, SDL_SCANCODE_LALT);
        map(GLFW_KEY_LEFT_SUPER, SDL_SCANCODE_LGUI);
        map(GLFW_KEY_RIGHT_SHIFT, SDL_SCANCODE_RSHIFT);
        map(GLFW_KEY_RIGHT_CONTROL, SDL_SCANCODE_RCTRL);
        map(GLFW_KEY_RIGHT_ALT, SDL_SCANCODE_RALT);
        map(GLFW_KEY_RIGHT_SUPER, SDL_SCANCODE_RGUI);
        map(GLFW_KEY_MENU, SDL_SCANCODE_APPLICATION);
    }

    private SdlKeys() {
        throw new AssertionError();
    }

    /**
     * Maps a key without a character, its keycode is the scancode with {@code SDLK_SCANCODE_MASK}.
     */
    private static void map(int glfwKey, int scancode) {
        map(glfwKey, scancode, scancode | SDLK_SCANCODE_MASK);
    }

    private static void map(int glfwKey, int scancode, int keycode) {
        SCANCODES[glfwKey] = scancode;
        KEYS[scancode] = keycode;
    }

    /**
     * @param glfwKey a GLFW key code.
     * @return the SDL scancode, {@code SDL_SCANCODE_UNKNOWN} for unknown keys.
     */
    public static int scancode(int glfwKey) {
        return glfwKey >= 0 && glfwKey < SCANCODES.length ? SCANCODES[glfwKey] : SDL_SCANCODE_UNKNOWN;
    }

    /**
     * @param scancode an SDL scancode.
     * @return the SDL keycode of the scancode, {@code SDLK_UNKNOWN} for unknown scancodes.
     */
    public static int keycode(int scancode) {
        return scancode > 0 && scancode < KEYS.length ? KEYS[scancode] : SDLK_UNKNOWN;
    }

    /**
     * @param keycode an SDL keycode.
     * @return the scancode that produces the keycode, {@code SDL_SCANCODE_UNKNOWN} if there is none.
     */
    public static int scancodeOfKey(int keycode) {
        if (keycode == SDLK_UNKNOWN) {
            return SDL_SCANCODE_UNKNOWN;
        }

        for (int scancode = 1; scancode < KEYS.length; scancode++) {
            if (KEYS[scancode] == keycode) {
                return scancode;
            }
        }

        return SDL_SCANCODE_UNKNOWN;
    }

    /**
     * @param glfwMods GLFW modifier bits.
     * @return the SDL modifier bits, left keys for shift, control, alt and super.
     */
    public static short modifiers(int glfwMods) {
        int result = 0;
        if ((glfwMods & GLFW_MOD_SHIFT) != 0) {
            result |= SDL_KMOD_LSHIFT;
        }

        if ((glfwMods & GLFW_MOD_CONTROL) != 0) {
            result |= SDL_KMOD_LCTRL;
        }

        if ((glfwMods & GLFW_MOD_ALT) != 0) {
            result |= SDL_KMOD_LALT;
        }

        if ((glfwMods & GLFW_MOD_SUPER) != 0) {
            result |= SDL_KMOD_LGUI;
        }

        if ((glfwMods & GLFW_MOD_CAPS_LOCK) != 0) {
            result |= SDL_KMOD_CAPS;
        }

        if ((glfwMods & GLFW_MOD_NUM_LOCK) != 0) {
            result |= SDL_KMOD_NUM;
        }

        return (short) result;
    }

    /**
     * @param glfwButton a GLFW mouse button.
     * @return the SDL mouse button: 1 left, 2 middle, 3 right, 4 and 5 the side buttons.
     */
    public static int button(int glfwButton) {
        switch (glfwButton) {
            case GLFW_MOUSE_BUTTON_LEFT:
                return 1;
            case GLFW_MOUSE_BUTTON_RIGHT:
                return 3;
            case GLFW_MOUSE_BUTTON_MIDDLE:
                return 2;
            default:
                return glfwButton + 1;
        }
    }

}
