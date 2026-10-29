package io.github.headlesshq.headlessmc.lwjgl2.runtime;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Input;
import io.github.headlesshq.headlessmc.lwjgl.runtime.InputEvent;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.lwjgl.input.Keyboard.KEY_NONE;

/**
 * Patches for LWJGL 2's {@code Keyboard} and {@code Mouse}: the injected {@link Input} events become keyboard and
 * mouse events when the application pumps its events ({@code Display.update}, {@code Display.processMessages},
 * {@code Keyboard.poll}, {@code Mouse.poll}), window events update {@link Lwjgl2Display}.
 * Mouse coordinates are converted to LWJGL 2's origin in the bottom left corner of the window, wheel events count
 * 120 per notch.
 */
public final class Lwjgl2Input {
    private static final String KEYBOARD = "org/lwjgl/input/Keyboard";
    private static final String MOUSE = "org/lwjgl/input/Mouse";
    private static final int WHEEL_DELTA = 120;
    private static final int BUTTON_NAMES = 16;
    private static final Deque<KeyEvent> KEY_EVENTS = new ArrayDeque<>();
    private static final Deque<MouseEvent> MOUSE_EVENTS = new ArrayDeque<>();

    private static boolean keyboardCreated;
    private static boolean mouseCreated;
    private static boolean repeatEvents;
    private static boolean grabbed;
    private static KeyEvent keyEvent = new KeyEvent(KEY_NONE, '\0', false, false, 0L);
    private static MouseEvent mouseEvent = new MouseEvent(-1, false, 0, 0, 0, 0, 0, 0L);
    private static int lastX;
    private static int lastY;
    private static int dx;
    private static int dy;
    private static int dWheel;

    private Lwjgl2Input() {
        throw new AssertionError();
    }

    /**
     * Turns all injected input into keyboard, mouse and window events.
     */
    public static synchronized void pump() {
        InputEvent event;
        while ((event = Input.poll()) != null) {
            long nanos = System.nanoTime();
            switch (event.type) {
                case InputEvent.KEY: {
                    int key = Lwjgl2Keys.lwjglKey(event.code);
                    boolean repeat = event.action == Input.REPEAT;
                    if (key != KEY_NONE && (!repeat || repeatEvents)) {
                        KEY_EVENTS.add(new KeyEvent(key, '\0', event.action != Input.RELEASE, repeat, nanos));
                    }

                    break;
                }
                case InputEvent.CHAR:
                    for (char c : Character.toChars(event.code)) {
                        KEY_EVENTS.add(new KeyEvent(KEY_NONE, c, true, false, nanos));
                    }

                    break;
                case InputEvent.MOUSE_BUTTON:
                    MOUSE_EVENTS.add(new MouseEvent(event.code, event.action != Input.RELEASE, 0, 0, 0, x(), y(), nanos));
                    break;
                case InputEvent.CURSOR_POS: {
                    int x = (int) event.x;
                    int y = Lwjgl2Display.height() - 1 - (int) event.y;
                    int eventDx = x - lastX;
                    int eventDy = y - lastY;
                    lastX = x;
                    lastY = y;
                    dx += eventDx;
                    dy += eventDy;
                    MOUSE_EVENTS.add(new MouseEvent(-1, false, eventDx, eventDy, 0, x, y, nanos));
                    break;
                }
                case InputEvent.SCROLL: {
                    int wheel = (int) Math.round(event.y * WHEEL_DELTA);
                    dWheel += wheel;
                    MOUSE_EVENTS.add(new MouseEvent(-1, false, 0, 0, wheel, x(), y(), nanos));
                    break;
                }
                case InputEvent.RESIZE:
                    Lwjgl2Display.resized();
                    break;
                case InputEvent.CLOSE:
                    Lwjgl2Display.requestClose();
                    break;
                default:
                    break;
            }
        }
    }

    private static int x() {
        return (int) Input.cursorX();
    }

    private static int y() {
        return Lwjgl2Display.height() - 1 - (int) Input.cursorY();
    }

    @Patch(owner = KEYBOARD)
    public static synchronized void create() {
        keyboardCreated = true;
    }

    @Patch(owner = KEYBOARD)
    public static synchronized boolean isCreated() {
        return keyboardCreated;
    }

    @Patch(owner = KEYBOARD)
    public static synchronized void destroy() {
        keyboardCreated = false;
    }

    @Patch(owner = KEYBOARD)
    public static void poll() {
        pump();
    }

    @Patch(owner = KEYBOARD)
    public static boolean isKeyDown(int key) {
        return Input.isKeyPressed(Lwjgl2Keys.glfwKey(key));
    }

    @Patch(owner = KEYBOARD)
    public static synchronized int getNumKeyboardEvents() {
        return KEY_EVENTS.size();
    }

    @Patch(owner = KEYBOARD)
    public static synchronized boolean next() {
        KeyEvent next = KEY_EVENTS.poll();
        if (next == null) {
            return false;
        }

        keyEvent = next;
        return true;
    }

    @Patch(owner = KEYBOARD)
    public static synchronized void enableRepeatEvents(boolean enable) {
        repeatEvents = enable;
    }

    @Patch(owner = KEYBOARD)
    public static synchronized boolean areRepeatEventsEnabled() {
        return repeatEvents;
    }

    @Patch(owner = KEYBOARD)
    public static synchronized char getEventCharacter() {
        return keyEvent.character;
    }

    @Patch(owner = KEYBOARD)
    public static synchronized int getEventKey() {
        return keyEvent.key;
    }

    @Patch(owner = KEYBOARD)
    public static synchronized boolean getEventKeyState() {
        return keyEvent.down;
    }

    @Patch(owner = KEYBOARD, name = "getEventNanoseconds")
    public static synchronized long getKeyEventNanoseconds() {
        return keyEvent.nanos;
    }

    @Patch(owner = KEYBOARD)
    public static synchronized boolean isRepeatEvent() {
        return keyEvent.repeat;
    }

    @Patch(owner = MOUSE, name = "create")
    public static synchronized void createMouse() {
        mouseCreated = true;
    }

    @Patch(owner = MOUSE, name = "isCreated")
    public static synchronized boolean isMouseCreated() {
        return mouseCreated;
    }

    @Patch(owner = MOUSE, name = "destroy")
    public static synchronized void destroyMouse() {
        mouseCreated = false;
    }

    @Patch(owner = MOUSE, name = "poll")
    public static void pollMouse() {
        pump();
    }

    @Patch(owner = MOUSE)
    public static boolean isButtonDown(int button) {
        return Input.isButtonPressed(button);
    }

    @Patch(owner = MOUSE, name = "next")
    public static synchronized boolean nextMouseEvent() {
        MouseEvent next = MOUSE_EVENTS.poll();
        if (next == null) {
            return false;
        }

        mouseEvent = next;
        return true;
    }

    @Patch(owner = MOUSE)
    public static synchronized int getEventButton() {
        return mouseEvent.button;
    }

    @Patch(owner = MOUSE)
    public static synchronized boolean getEventButtonState() {
        return mouseEvent.down;
    }

    @Patch(owner = MOUSE)
    public static synchronized int getEventDX() {
        return mouseEvent.dx;
    }

    @Patch(owner = MOUSE)
    public static synchronized int getEventDY() {
        return mouseEvent.dy;
    }

    @Patch(owner = MOUSE)
    public static synchronized int getEventX() {
        return mouseEvent.x;
    }

    @Patch(owner = MOUSE)
    public static synchronized int getEventY() {
        return mouseEvent.y;
    }

    @Patch(owner = MOUSE)
    public static synchronized int getEventDWheel() {
        return mouseEvent.dWheel;
    }

    @Patch(owner = MOUSE, name = "getEventNanoseconds")
    public static synchronized long getMouseEventNanoseconds() {
        return mouseEvent.nanos;
    }

    @Patch(owner = MOUSE)
    public static int getX() {
        return x();
    }

    @Patch(owner = MOUSE)
    public static int getY() {
        return y();
    }

    @Patch(owner = MOUSE)
    public static synchronized int getDX() {
        int result = dx;
        dx = 0;
        return result;
    }

    @Patch(owner = MOUSE)
    public static synchronized int getDY() {
        int result = dy;
        dy = 0;
        return result;
    }

    @Patch(owner = MOUSE)
    public static synchronized int getDWheel() {
        int result = dWheel;
        dWheel = 0;
        return result;
    }

    /**
     * LWJGL 2 names the buttons {@code BUTTON0} to {@code BUTTON15}.
     */
    @Patch(owner = MOUSE)
    public static String getButtonName(int button) {
        return button >= 0 && button < BUTTON_NAMES ? "BUTTON" + button : null;
    }

    @Patch(owner = MOUSE)
    public static int getButtonIndex(String name) {
        if (name != null && name.startsWith("BUTTON")) {
            try {
                int button = Integer.parseInt(name.substring("BUTTON".length()));
                return button < BUTTON_NAMES ? button : -1;
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        return -1;
    }

    @Patch(owner = MOUSE)
    public static int getButtonCount() {
        return 5;
    }

    @Patch(owner = MOUSE)
    public static boolean hasWheel() {
        return true;
    }

    @Patch(owner = MOUSE)
    public static synchronized boolean isGrabbed() {
        return grabbed;
    }

    @Patch(owner = MOUSE)
    public static synchronized void setGrabbed(boolean grab) {
        grabbed = grab;
    }

    @Patch(owner = MOUSE)
    public static synchronized void setCursorPosition(int newX, int newY) {
        lastX = newX;
        lastY = newY;
        Input.setCursor(newX, Lwjgl2Display.height() - 1 - newY);
    }

    @Patch(owner = MOUSE)
    public static boolean isInsideWindow() {
        return true;
    }

    private static final class KeyEvent {
        private final int key;
        private final char character;
        private final boolean down;
        private final boolean repeat;
        private final long nanos;

        private KeyEvent(int key, char character, boolean down, boolean repeat, long nanos) {
            this.key = key;
            this.character = character;
            this.down = down;
            this.repeat = repeat;
            this.nanos = nanos;
        }
    }

    private static final class MouseEvent {
        private final int button;
        private final boolean down;
        private final int dx;
        private final int dy;
        private final int dWheel;
        private final int x;
        private final int y;
        private final long nanos;

        private MouseEvent(int button, boolean down, int dx, int dy, int dWheel, int x, int y, long nanos) {
            this.button = button;
            this.down = down;
            this.dx = dx;
            this.dy = dy;
            this.dWheel = dWheel;
            this.x = x;
            this.y = y;
            this.nanos = nanos;
        }
    }

}
