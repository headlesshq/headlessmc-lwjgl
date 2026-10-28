package io.github.headlesshq.headlessmc.lwjgl.api;

/**
 * Mouse of a headless LWJGL.
 * Coordinates are in screen coordinates relative to the top left corner of the window's content area.
 * Buttons are identified by the constants in {@link Buttons}.
 */
public interface Mouse {
    /**
     * Moves the cursor to the given position.
     *
     * @param x the new x coordinate.
     * @param y the new y coordinate.
     */
    void moveTo(double x, double y);

    /**
     * Moves the cursor relative to its current position.
     * When the cursor is captured by the application this is the way to produce mouse motion.
     *
     * @param dx the distance on the x axis.
     * @param dy the distance on the y axis.
     */
    default void moveBy(double dx, double dy) {
        moveTo(x() + dx, y() + dy);
    }

    /**
     * Presses the given button, without modifiers.
     *
     * @param button the button, see {@link Buttons}.
     */
    default void press(int button) {
        press(button, Modifiers.NONE);
    }

    /**
     * Presses the given button.
     *
     * @param button the button, see {@link Buttons}.
     * @param modifiers the modifier bit flags, see {@link Modifiers}.
     */
    void press(int button, int modifiers);

    /**
     * Releases the given button, without modifiers.
     *
     * @param button the button, see {@link Buttons}.
     */
    default void release(int button) {
        release(button, Modifiers.NONE);
    }

    /**
     * Releases the given button.
     *
     * @param button the button, see {@link Buttons}.
     * @param modifiers the modifier bit flags, see {@link Modifiers}.
     */
    void release(int button, int modifiers);

    /**
     * Presses and releases the given button.
     *
     * @param button the button, see {@link Buttons}.
     */
    default void click(int button) {
        press(button);
        release(button);
    }

    /**
     * Scrolls the mouse wheel.
     *
     * @param dx the horizontal scroll offset.
     * @param dy the vertical scroll offset, positive values scroll up.
     */
    void scroll(double dx, double dy);

    /**
     * @return the x coordinate of the cursor.
     */
    double x();

    /**
     * @return the y coordinate of the cursor.
     */
    double y();

    /**
     * @param button the button, see {@link Buttons}.
     * @return {@code true} if the given button is currently pressed.
     */
    boolean isPressed(int button);

}
