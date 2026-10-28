package io.github.headlesshq.headlessmc.lwjgl.api;

/**
 * Modifier key bit flags, with the same values as GLFW's {@code GLFW_MOD_*} constants.
 */
public final class Modifiers {
    public static final int NONE = 0;
    public static final int SHIFT = 0x0001;
    public static final int CONTROL = 0x0002;
    public static final int ALT = 0x0004;
    public static final int SUPER = 0x0008;
    public static final int CAPS_LOCK = 0x0010;
    public static final int NUM_LOCK = 0x0020;

    private Modifiers() {
        throw new AssertionError();
    }

}
