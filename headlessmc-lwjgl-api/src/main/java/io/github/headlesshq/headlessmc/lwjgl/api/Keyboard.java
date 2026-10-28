package io.github.headlesshq.headlessmc.lwjgl.api;

/**
 * Keyboard of a headless LWJGL.
 * Keys are identified by the key codes in {@link Keys}, modifiers by the bit flags in {@link Modifiers}.
 */
public interface Keyboard {
    /**
     * Presses the given key, without modifiers.
     *
     * @param key the key code, see {@link Keys}.
     */
    default void press(int key) {
        press(key, Modifiers.NONE);
    }

    /**
     * Presses the given key.
     * Pressing a key that is already pressed produces a repeat event.
     *
     * @param key the key code, see {@link Keys}.
     * @param modifiers the modifier bit flags, see {@link Modifiers}.
     */
    void press(int key, int modifiers);

    /**
     * Releases the given key, without modifiers.
     *
     * @param key the key code, see {@link Keys}.
     */
    default void release(int key) {
        release(key, Modifiers.NONE);
    }

    /**
     * Releases the given key.
     *
     * @param key the key code, see {@link Keys}.
     * @param modifiers the modifier bit flags, see {@link Modifiers}.
     */
    void release(int key, int modifiers);

    /**
     * Presses and releases the given key.
     *
     * @param key the key code, see {@link Keys}.
     */
    default void click(int key) {
        press(key);
        release(key);
    }

    /**
     * Types a unicode character, the way a text input event would deliver it.
     *
     * @param codepoint the unicode code point.
     */
    void type(int codepoint);

    /**
     * Types every code point of the given text.
     *
     * @param text the text to type.
     */
    default void type(CharSequence text) {
        text.codePoints().forEach(this::type);
    }

    /**
     * @param key the key code, see {@link Keys}.
     * @return {@code true} if the given key is currently pressed.
     */
    boolean isPressed(int key);

}
