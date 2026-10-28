package io.github.headlesshq.headlessmc.lwjgl.api;

/**
 * The window of a headless LWJGL application.
 * If the application created multiple windows, this is the one that was created first and is still open.
 */
public interface Window {
    /**
     * @return the width of the window's content area in screen coordinates.
     */
    int width();

    /**
     * @return the height of the window's content area in screen coordinates.
     */
    int height();

    /**
     * Resizes the window and notifies the application.
     *
     * @param width the new width.
     * @param height the new height.
     */
    void resize(int width, int height);

    /**
     * @return {@code true} if the window has input focus.
     */
    boolean isFocused();

    /**
     * Gives or takes input focus and notifies the application.
     *
     * @param focused whether the window should be focused.
     */
    void setFocused(boolean focused);

    /**
     * Asks the application to close the window, the way clicking the close button would.
     */
    void requestClose();

}
