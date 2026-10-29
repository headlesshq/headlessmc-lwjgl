package io.github.headlesshq.headlessmc.lwjgl2;

import io.github.headlesshq.headlessmc.lwjgl.api.Buttons;
import io.github.headlesshq.headlessmc.lwjgl.api.HeadlessLwjgl;
import io.github.headlesshq.headlessmc.lwjgl.api.Keys;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Injects input through the API and checks that an LWJGL 2 application sees it.
 */
class InputTest {
    @Test
    void keyboardAndMouse() throws Exception {
        Display.setDisplayMode(new DisplayMode(800, 600));
        Display.create();
        Keyboard.create();
        Mouse.create();
        try {
            HeadlessLwjgl lwjgl = HeadlessLwjgl.get();
            lwjgl.keyboard().press(Keys.W);
            assertTrue(Keyboard.isKeyDown(Keyboard.KEY_W));
            lwjgl.keyboard().release(Keys.W);
            lwjgl.keyboard().type("a");
            lwjgl.mouse().moveTo(10, 20);
            lwjgl.mouse().click(Buttons.RIGHT);
            lwjgl.mouse().scroll(0, 1);
            Display.update();

            List<String> keys = new ArrayList<>();
            while (Keyboard.next()) {
                keys.add(Keyboard.getEventKey() + " " + Keyboard.getEventKeyState() + " " + Keyboard.getEventCharacter());
            }

            assertEquals(Arrays.asList(Keyboard.KEY_W + " true \0", Keyboard.KEY_W + " false \0", "0 true a"), keys);
            List<String> mouse = new ArrayList<>();
            while (Mouse.next()) {
                mouse.add(Mouse.getEventButton() + " " + Mouse.getEventButtonState() + " " + Mouse.getEventX() + " "
                              + Mouse.getEventY() + " " + Mouse.getEventDWheel());
            }

            assertEquals(Arrays.asList("-1 false 10 579 0", "1 true 10 579 0", "1 false 10 579 0", "-1 false 10 579 120"), mouse);
            assertEquals(10, Mouse.getX());
            assertEquals(579, Mouse.getY());

            lwjgl.window().requestClose();
            Display.update();
            assertTrue(Display.isCloseRequested());
        } finally {
            Display.destroy();
        }
    }

}
