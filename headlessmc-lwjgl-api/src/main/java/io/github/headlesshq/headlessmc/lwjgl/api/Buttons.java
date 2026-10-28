package io.github.headlesshq.headlessmc.lwjgl.api;

/**
 * Mouse buttons, with the same values as GLFW's {@code GLFW_MOUSE_BUTTON_*} constants.
 */
public final class Buttons {
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int MIDDLE = 2;
    public static final int BUTTON_4 = 3;
    public static final int BUTTON_5 = 4;
    public static final int BUTTON_6 = 5;
    public static final int BUTTON_7 = 6;
    public static final int BUTTON_8 = 7;

    private Buttons() {
        throw new AssertionError();
    }

}
