package io.github.headlesshq.headlessmc.lwjgl3.runtime;

/**
 * An input or window event injected through the API, waiting to be dispatched by the window backend (GLFW or SDL)
 * when the application pumps its events.
 * Keys, buttons, actions and modifiers use the GLFW values, backends translate them.
 */
public final class InputEvent {
    public static final int KEY = 0;
    public static final int CHAR = 1;
    public static final int MOUSE_BUTTON = 2;
    public static final int CURSOR_POS = 3;
    public static final int SCROLL = 4;
    public static final int RESIZE = 5;
    public static final int FOCUS = 6;
    public static final int CLOSE = 7;

    public final int type;
    /**
     * The key, code point, button, width, or {@code 1} for gaining focus.
     */
    public final int code;
    /**
     * The GLFW action of a key or button, or the height of a resize.
     */
    public final int action;
    public final int mods;
    public final double x;
    public final double y;

    InputEvent(int type, int code, int action, int mods, double x, double y) {
        this.type = type;
        this.code = code;
        this.action = action;
        this.mods = mods;
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "InputEvent{type=" + type + ", code=" + code + ", action=" + action + ", mods=" + mods + ", x=" + x + ", y=" + y + '}';
    }

}
