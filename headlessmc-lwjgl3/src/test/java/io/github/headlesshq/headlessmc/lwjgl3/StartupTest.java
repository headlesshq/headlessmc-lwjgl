package io.github.headlesshq.headlessmc.lwjgl3;

import io.github.headlesshq.headlessmc.lwjgl.runtime.DummyBuffers;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Calls that every Minecraft version makes during startup.
 */
class StartupTest {
    @Test
    void memory() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer value = stack.mallocInt(1);
            value.put(0, 5);
            assertEquals(5, value.get(0));

            IntBuffer values = stack.ints(1, 2, 3);
            assertEquals(3, values.capacity());
            assertEquals(3, values.get(2));
        }

        assertEquals(16, BufferUtils.createByteBuffer(16).capacity());
        ByteBuffer data = MemoryUtil.memAlloc(10_000_000);
        data.put(0, (byte) 7);
        assertEquals(7, MemoryUtil.memRealloc(data, 20_000).get(0), "buffers below 16 MiB keep their contents");
        ByteBuffer large = MemoryUtil.memAlloc(20_000_000);
        assertEquals(20_000_000, large.capacity());
        assertTrue(DummyBuffers.sharedSize() >= 20_000_000, "buffers above 16 MiB share memory");
        MemoryUtil.memFree(large);
        assertNotEquals(0L, MemoryUtil.nmemAlloc(64));
    }

    @Test
    void glfwAndOpenGl() {
        GLFWErrorCallback.createPrint(System.err).set();
        assertTrue(GLFW.glfwInit());
        long window = GLFW.glfwCreateWindow(854, 480, "HeadlessMc", 0L, 0L);
        assertNotEquals(0L, window);
        assertFalse(GLFW.glfwWindowShouldClose(window));
        GLFWVidMode videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        assertNotNull(videoMode);

        int[] width = new int[1];
        int[] height = new int[1];
        GLFW.glfwGetFramebufferSize(window, width, height);
        assertEquals(854, width[0]);
        assertEquals(480, height[0]);

        GLFW.glfwMakeContextCurrent(window);
        GLCapabilities capabilities = GL.createCapabilities();
        assertTrue(capabilities.OpenGL32);
        assertTrue(capabilities.OpenGL46);
        assertSame(capabilities, GL.getCapabilities());

        assertEquals(16384, GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE));
        assertTrue(GL11.glGetString(GL11.GL_VERSION).startsWith("4.6"));
        assertNotEquals(0, GL11.glGenTextures());
        assertEquals(GL30.GL_FRAMEBUFFER_COMPLETE, GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER));
        assertEquals(GL11.GL_NO_ERROR, GL11.glGetError());
        assertTrue(GLFW.glfwGetTime() >= 0.0);

        GLFW.glfwSwapBuffers(window);
        GLFW.glfwPollEvents();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    @Test
    void sdl() {
        assertTrue(SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO));
        assertNotEquals(0L, SDLVideo.SDL_CreateWindow("HeadlessMc", 854, 480, 0L));
    }

    @Test
    void openAl() {
        long device = ALC10.alcOpenDevice((ByteBuffer) null);
        assertNotEquals(0L, device);
        long context = ALC10.alcCreateContext(device, (IntBuffer) null);
        assertNotEquals(0L, context);
        assertTrue(ALC10.alcMakeContextCurrent(context));

        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        assertTrue(alcCapabilities.OpenALC11);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
        assertTrue(alCapabilities.OpenAL11);
    }

}
