package io.github.headlesshq.headlessmc.lwjgl.agent.check2;

import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.openal.AL;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

/**
 * The startup calls of Minecraft 1.12.2 with the original LWJGL 2 jars on the classpath, the agent has to make them
 * work without natives, see the {@code agentCheckLwjgl2} task.
 */
public final class Lwjgl2Check {
    private Lwjgl2Check() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws Exception {
        String location = Display.class.getProtectionDomain().getCodeSource().getLocation().toString();
        check(location.contains("lwjgl-2."), "Display is not loaded from the original jar: " + location);
        check(Class.forName("io.github.headlesshq.headlessmc.lwjgl.api.HeadlessLwjgl") != null, "the API is missing");

        Display.setTitle("Minecraft 1.12.2");
        Display.setDisplayMode(new DisplayMode(854, 480));
        Display.create(new PixelFormat().withDepthBits(24));
        check(Display.isCreated(), "Display.create");
        check(GL11.glGetString(GL11.GL_VERSION) != null, "glGetString");
        check(GL11.glGenTextures() != 0, "glGenTextures");
        Keyboard.create();
        Mouse.create();
        Keyboard.poll();
        Display.update();
        AL.create();
        AL.destroy();
        Display.destroy();
        System.out.println("The agent patched LWJGL " + Sys.getVersion() + " loaded from " + location);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

}
