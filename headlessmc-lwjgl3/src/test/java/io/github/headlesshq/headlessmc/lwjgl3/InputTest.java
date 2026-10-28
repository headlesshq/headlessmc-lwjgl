package io.github.headlesshq.headlessmc.lwjgl3;

import io.github.headlesshq.headlessmc.lwjgl.api.Buttons;
import io.github.headlesshq.headlessmc.lwjgl.api.HeadlessLwjgl;
import io.github.headlesshq.headlessmc.lwjgl.api.Keys;
import io.github.headlesshq.headlessmc.lwjgl.api.Modifiers;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLScancode;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_DisplayMode;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_KeyboardEvent;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Injects input through the API and checks that GLFW and SDL applications see it.
 */
class InputTest {
    @Test
    void glfw() {
        assertTrue(GLFW.glfwInit());
        long window = GLFW.glfwCreateWindow(1280, 720, "Input", 0L, 0L);
        try {
            List<String> events = new ArrayList<>();
            GLFW.glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> events.add("key " + key + " " + action + " " + mods));
            GLFW.glfwSetCharCallback(window, (w, codepoint) -> events.add("char " + (char) codepoint));
            GLFW.glfwSetMouseButtonCallback(window, (w, button, action, mods) -> events.add("button " + button + " " + action));
            GLFW.glfwSetCursorPosCallback(window, (w, x, y) -> events.add("cursor " + x + " " + y));
            GLFW.glfwSetScrollCallback(window, (w, x, y) -> events.add("scroll " + x + " " + y));
            GLFW.glfwSetFramebufferSizeCallback(window, (w, width, height) -> events.add("framebuffer " + width + "x" + height));
            GLFW.glfwSetWindowFocusCallback(window, (w, focused) -> events.add("focus " + focused));

            HeadlessLwjgl lwjgl = HeadlessLwjgl.get();
            assertEquals(1280, lwjgl.window().width());
            lwjgl.keyboard().press(Keys.W, Modifiers.SHIFT);
            assertEquals(GLFW.GLFW_PRESS, GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W));
            assertTrue(events.isEmpty(), "events are only dispatched when the application polls");

            lwjgl.keyboard().press(Keys.W, Modifiers.SHIFT);
            lwjgl.keyboard().release(Keys.W);
            lwjgl.keyboard().type("hi");
            lwjgl.mouse().moveTo(10.0, 20.0);
            lwjgl.mouse().moveBy(5.0, -5.0);
            lwjgl.mouse().click(Buttons.LEFT);
            lwjgl.mouse().scroll(0.0, 1.0);
            lwjgl.window().resize(800, 600);
            lwjgl.window().setFocused(false);
            GLFW.glfwPollEvents();

            assertEquals(Arrays.asList(
                "key 87 1 1", "key 87 2 1", "key 87 0 0", "char h", "char i", "cursor 10.0 20.0", "cursor 15.0 15.0",
                "button 0 1", "button 0 0", "scroll 0.0 1.0", "framebuffer 800x600", "focus false"), events);
            assertEquals(GLFW.GLFW_RELEASE, GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W));
            double[] x = new double[1];
            double[] y = new double[1];
            GLFW.glfwGetCursorPos(window, x, y);
            assertEquals(15.0, x[0]);
            assertEquals(15.0, y[0]);
            int[] width = new int[1];
            int[] height = new int[1];
            GLFW.glfwGetFramebufferSize(window, width, height);
            assertEquals(800, width[0]);
            assertEquals(600, height[0]);
            assertEquals(GLFW.GLFW_FALSE, GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED));

            assertFalse(GLFW.glfwWindowShouldClose(window));
            lwjgl.window().requestClose();
            GLFW.glfwPollEvents();
            assertTrue(GLFW.glfwWindowShouldClose(window));
        } finally {
            HeadlessLwjgl.get().window().setFocused(true);
            HeadlessLwjgl.get().window().resize(854, 480);
            GLFW.glfwPollEvents();
            GLFW.glfwDestroyWindow(window);
        }
    }

    @Test
    void sdl() {
        assertTrue(SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO));
        long window = SDLVideo.SDL_CreateWindow("Input", 1280, 720, SDLVideo.SDL_WINDOW_OPENGL);
        try {
            assertTrue(SDLKeyboard.SDL_StartTextInput(window));
            HeadlessLwjgl lwjgl = HeadlessLwjgl.get();
            lwjgl.keyboard().press(Keys.W, Modifiers.SHIFT);
            assertEquals(1, SDLKeyboard.SDL_GetKeyboardState().get(SDLScancode.SDL_SCANCODE_W));
            lwjgl.keyboard().type("é");
            lwjgl.mouse().moveTo(10.0, 20.0);
            lwjgl.mouse().press(Buttons.RIGHT);
            lwjgl.mouse().scroll(0.0, -1.0);
            lwjgl.window().requestClose();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                SDL_Event event = SDL_Event.malloc(stack);
                List<String> events = new ArrayList<>();
                while (SDLEvents.SDL_PollEvent(event)) {
                    switch (event.type()) {
                        case SDLEvents.SDL_EVENT_KEY_DOWN:
                            SDL_KeyboardEvent key = event.key();
                            events.add("key " + key.scancode() + " " + Integer.toHexString(key.key()) + " " + key.mod() + " " + key.down());
                            break;
                        case SDLEvents.SDL_EVENT_TEXT_INPUT:
                            events.add("text " + event.text().textString());
                            break;
                        case SDLEvents.SDL_EVENT_MOUSE_MOTION:
                            events.add("motion " + event.motion().x() + " " + event.motion().y() + " " + event.motion().xrel());
                            break;
                        case SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN:
                            events.add("button " + event.button().button() + " " + event.button().down());
                            break;
                        case SDLEvents.SDL_EVENT_MOUSE_WHEEL:
                            events.add("wheel " + event.wheel().y());
                            break;
                        case SDLEvents.SDL_EVENT_WINDOW_CLOSE_REQUESTED:
                            events.add("close " + event.window().windowID());
                            break;
                        case SDLEvents.SDL_EVENT_QUIT:
                            events.add("quit");
                            break;
                        default:
                            events.add("other " + event.type());
                            break;
                    }
                }

                assertEquals(Arrays.asList("key 26 77 1 true", "text é", "motion 10.0 20.0 10.0", "button 3 true",
                                           "wheel -1.0", "close 1", "quit"), events);
            }

            assertEquals(1 << 2, SDLMouse.SDL_GetMouseState(null, null));
            SDL_DisplayMode mode = SDLVideo.SDL_GetCurrentDisplayMode(SDLVideo.SDL_GetPrimaryDisplay());
            assertNotNull(mode);
            assertEquals(1920, mode.w());
            assertEquals(1080, mode.h());
        } finally {
            HeadlessLwjgl.get().keyboard().release(Keys.W);
            HeadlessLwjgl.get().mouse().release(Buttons.RIGHT);
            SDLEvents.SDL_PumpEvents();
            while (SDLEvents.SDL_PollEvent(null)) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    SDLEvents.SDL_PollEvent(SDL_Event.malloc(stack));
                }
            }

            SDLVideo.SDL_DestroyWindow(window);
        }
    }

}
