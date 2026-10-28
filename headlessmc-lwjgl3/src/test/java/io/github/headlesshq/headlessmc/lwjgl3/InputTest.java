package io.github.headlesshq.headlessmc.lwjgl3;

import io.github.headlesshq.headlessmc.lwjgl.api.Buttons;
import io.github.headlesshq.headlessmc.lwjgl.api.HeadlessLwjgl;
import io.github.headlesshq.headlessmc.lwjgl.api.Keys;
import io.github.headlesshq.headlessmc.lwjgl.api.Modifiers;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Injects input through the API and checks that a GLFW application sees it.
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

}
