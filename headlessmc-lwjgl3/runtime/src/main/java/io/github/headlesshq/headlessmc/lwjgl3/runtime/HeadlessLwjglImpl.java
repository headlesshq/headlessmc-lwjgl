package io.github.headlesshq.headlessmc.lwjgl3.runtime;

import io.github.headlesshq.headlessmc.lwjgl.api.HeadlessLwjgl;
import io.github.headlesshq.headlessmc.lwjgl.api.Keyboard;
import io.github.headlesshq.headlessmc.lwjgl.api.Mouse;
import io.github.headlesshq.headlessmc.lwjgl.api.Window;

/**
 * Implementation of the input API on top of {@link Input}.
 * Only this class refers to the API, so that the patched LWJGL works without the API jar.
 */
public final class HeadlessLwjglImpl implements HeadlessLwjgl {
    private static final HeadlessLwjglImpl INSTANCE = new HeadlessLwjglImpl();

    private final Keyboard keyboard = new KeyboardImpl();
    private final Mouse mouse = new MouseImpl();
    private final Window window = new WindowImpl();

    /**
     * Public for {@link java.util.ServiceLoader}, use {@link HeadlessLwjgl#get()}.
     */
    public HeadlessLwjglImpl() {
    }

    static void register() {
        HeadlessLwjgl.register(INSTANCE);
    }

    @Override
    public Keyboard keyboard() {
        return keyboard;
    }

    @Override
    public Mouse mouse() {
        return mouse;
    }

    @Override
    public Window window() {
        return window;
    }

    private static final class KeyboardImpl implements Keyboard {
        @Override
        public void press(int key, int modifiers) {
            Input.key(key, Input.PRESS, modifiers);
        }

        @Override
        public void release(int key, int modifiers) {
            Input.key(key, Input.RELEASE, modifiers);
        }

        @Override
        public void type(int codepoint) {
            Input.character(codepoint, 0);
        }

        @Override
        public boolean isPressed(int key) {
            return Input.isKeyPressed(key);
        }
    }

    private static final class MouseImpl implements Mouse {
        @Override
        public void moveTo(double x, double y) {
            Input.moveCursor(x, y);
        }

        @Override
        public void moveBy(double dx, double dy) {
            Input.moveCursorBy(dx, dy);
        }

        @Override
        public void press(int button, int modifiers) {
            Input.mouseButton(button, Input.PRESS, modifiers);
        }

        @Override
        public void release(int button, int modifiers) {
            Input.mouseButton(button, Input.RELEASE, modifiers);
        }

        @Override
        public void scroll(double dx, double dy) {
            Input.scroll(dx, dy);
        }

        @Override
        public double x() {
            return Input.cursorX();
        }

        @Override
        public double y() {
            return Input.cursorY();
        }

        @Override
        public boolean isPressed(int button) {
            return Input.isButtonPressed(button);
        }
    }

    private static final class WindowImpl implements Window {
        @Override
        public int width() {
            return Input.width();
        }

        @Override
        public int height() {
            return Input.height();
        }

        @Override
        public void resize(int width, int height) {
            Input.resize(width, height);
        }

        @Override
        public boolean isFocused() {
            return Input.isFocused();
        }

        @Override
        public void setFocused(boolean focused) {
            Input.focus(focused);
        }

        @Override
        public void requestClose() {
            Input.requestClose();
        }
    }

}
