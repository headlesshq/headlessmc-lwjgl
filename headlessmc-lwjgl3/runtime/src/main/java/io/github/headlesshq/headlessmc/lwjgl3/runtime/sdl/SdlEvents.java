package io.github.headlesshq.headlessmc.lwjgl3.runtime.sdl;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Input;
import io.github.headlesshq.headlessmc.lwjgl.runtime.InputEvent;
import io.github.headlesshq.headlessmc.lwjgl.runtime.StructMemory;
import org.lwjgl.sdl.SDL_Event;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLMouse.SDL_MOUSEWHEEL_NORMAL;

/**
 * Turns the injected {@link Input} events into SDL events. One input event can become several SDL events, e.g. a
 * resize is reported as {@code SDL_EVENT_WINDOW_RESIZED} and {@code SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED}, so the
 * translated events wait in a queue of their own until the application polls them.
 */
public final class SdlEvents {
    private static final Deque<Consumer<SDL_Event>> PENDING = new ArrayDeque<>();
    private static final long START = System.nanoTime();

    private static ByteBuffer lastText;
    private static double lastX;
    private static double lastY;

    private SdlEvents() {
        throw new AssertionError();
    }

    /**
     * Fills the event with the next pending event.
     *
     * @param event the event to fill, or {@code null} to only check whether there is one.
     * @return {@code true} if there was an event.
     */
    public static synchronized boolean poll(SDL_Event event) {
        translate();
        if (PENDING.isEmpty()) {
            return false;
        } else if (event != null) {
            event.clear();
            PENDING.poll().accept(event);
        }

        return true;
    }

    private static void translate() {
        InputEvent input;
        while (PENDING.isEmpty() && (input = Input.poll()) != null) {
            translate(input);
        }
    }

    private static void translate(InputEvent input) {
        long timestamp = System.nanoTime() - START;
        int window = SdlWindows.MAIN_WINDOW_ID;
        switch (input.type) {
            case InputEvent.KEY: {
                int scancode = SdlKeys.scancode(input.code);
                boolean down = input.action != Input.RELEASE;
                boolean repeat = input.action == Input.REPEAT;
                PENDING.add(event -> event.key()
                    .type(down ? SDL_EVENT_KEY_DOWN : SDL_EVENT_KEY_UP)
                    .timestamp(timestamp)
                    .windowID(window)
                    .scancode(scancode)
                    .key(SdlKeys.keycode(scancode))
                    .mod(SdlKeys.modifiers(input.mods))
                    .down(down)
                    .repeat(repeat));
                break;
            }
            case InputEvent.CHAR: {
                if (!SdlWindows.isTextInputActive()) {
                    break;
                }

                String text = new String(Character.toChars(input.code));
                PENDING.add(event -> {
                    lastText = StructMemory.utf8(text);
                    event.text().type(SDL_EVENT_TEXT_INPUT).timestamp(timestamp).windowID(window).text(lastText);
                });
                break;
            }
            case InputEvent.MOUSE_BUTTON: {
                boolean down = input.action != Input.RELEASE;
                PENDING.add(event -> event.button()
                    .type(down ? SDL_EVENT_MOUSE_BUTTON_DOWN : SDL_EVENT_MOUSE_BUTTON_UP)
                    .timestamp(timestamp)
                    .windowID(window)
                    .button((byte) SdlKeys.button(input.code))
                    .down(down)
                    .clicks((byte) 1)
                    .x((float) Input.cursorX())
                    .y((float) Input.cursorY()));
                break;
            }
            case InputEvent.CURSOR_POS: {
                float xrel = (float) (input.x - lastX);
                float yrel = (float) (input.y - lastY);
                lastX = input.x;
                lastY = input.y;
                SdlWindows.addRelativeMotion(xrel, yrel);
                PENDING.add(event -> event.motion()
                    .type(SDL_EVENT_MOUSE_MOTION)
                    .timestamp(timestamp)
                    .windowID(window)
                    .state(SdlWindows.buttonMask())
                    .x((float) input.x)
                    .y((float) input.y)
                    .xrel(xrel)
                    .yrel(yrel));
                break;
            }
            case InputEvent.SCROLL:
                PENDING.add(event -> event.wheel()
                    .type(SDL_EVENT_MOUSE_WHEEL)
                    .timestamp(timestamp)
                    .windowID(window)
                    .x((float) input.x)
                    .y((float) input.y)
                    .direction(SDL_MOUSEWHEEL_NORMAL)
                    .mouse_x((float) Input.cursorX())
                    .mouse_y((float) Input.cursorY())
                    .integer_x((int) input.x)
                    .integer_y((int) input.y));
                break;
            case InputEvent.RESIZE:
                window(SDL_EVENT_WINDOW_RESIZED, timestamp, input.code, input.action);
                window(SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED, timestamp, input.code, input.action);
                break;
            case InputEvent.FOCUS:
                window(input.code != 0 ? SDL_EVENT_WINDOW_FOCUS_GAINED : SDL_EVENT_WINDOW_FOCUS_LOST, timestamp, 0, 0);
                break;
            case InputEvent.CLOSE:
                window(SDL_EVENT_WINDOW_CLOSE_REQUESTED, timestamp, 0, 0);
                PENDING.add(event -> event.quit().type(SDL_EVENT_QUIT).timestamp(timestamp));
                break;
            default:
                break;
        }
    }

    private static void window(int type, long timestamp, int data1, int data2) {
        PENDING.add(event -> event.window().set(type, timestamp, SdlWindows.MAIN_WINDOW_ID, data1, data2));
    }

}
