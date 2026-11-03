package io.github.headlesshq.headlessmc.lwjgl.agent.check;

import io.github.headlesshq.headlessmc.lwjgl.api.HeadlessLwjgl;
import io.github.headlesshq.headlessmc.lwjgl.api.Keys;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * The startup calls of Minecraft with the original LWJGL 3 jars on the classpath, the agent has to make them work
 * without natives, see the {@code agentCheckLwjgl3*} tasks.
 */
public final class Lwjgl3Check {
    private Lwjgl3Check() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws Exception {
        check("true".equals(System.getProperty("joml.nounsafe")), "joml.nounsafe is not set");
        check(System.getProperty("java.class.path").contains("headlessmc-lwjgl-api.jar"), "the classpath was not rewritten");
        String location = GLFW.class.getProtectionDomain().getCodeSource().getLocation().toString();
        check(location.contains("lwjgl-glfw-3."), "GLFW is not loaded from the original jar: " + location);
        check(GLFW.class.getClassLoader() == ClassLoader.getSystemClassLoader(), "GLFW is not loaded by the system class loader");

        check(GLFW.glfwInit(), "glfwInit");
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        long window = GLFW.glfwCreateWindow(854, 480, "Agent", 0L, 0L);
        check(window != 0L, "glfwCreateWindow");
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        check(GL11.glGetString(GL11.GL_VERSION) != null, "glGetString");
        check(GL11.glGenTextures() != 0, "glGenTextures");

        HeadlessLwjgl.get().keyboard().press(Keys.SPACE);
        GLFW.glfwPollEvents();
        check(GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS, "the API does not reach GLFW");

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(window, width, height);
            check(width.get(0) == 854 && height.get(0) == 480, "glfwGetFramebufferSize");
        }

        ByteBuffer memory = MemoryUtil.memAlloc(1024);
        MemoryUtil.memFree(memory);

        long device = ALC10.alcOpenDevice((ByteBuffer) null);
        check(device != 0L, "alcOpenDevice");
        long context = ALC10.alcCreateContext(device, (IntBuffer) null);
        ALC10.alcMakeContextCurrent(context);
        AL.createCapabilities(ALC.createCapabilities(device));

        // in LWJGL 3.3.3 there is no SDL module, it comes from the patched jars the agent appended
        Class<?> sdl = Class.forName("org.lwjgl.sdl.SDLInit");
        check(sdl.getClassLoader() == ClassLoader.getSystemClassLoader(), "SDL is not loaded by the system class loader");

        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
        System.out.println("The agent patched LWJGL " + Version.getVersion() + " loaded from " + location);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

}
