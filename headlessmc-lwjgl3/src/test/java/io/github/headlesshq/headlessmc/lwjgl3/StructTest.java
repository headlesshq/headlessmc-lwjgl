package io.github.headlesshq.headlessmc.lwjgl3;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Structs have real memory: what is written can be read back, while fake addresses stay harmless.
 */
class StructTest {
    @Test
    void fieldsCanBeReadBack() {
        GLFWImage image = GLFWImage.malloc();
        image.width(16).height(32);
        assertEquals(16, image.width());
        assertEquals(32, image.height());

        GLFWImage copy = GLFWImage.calloc();
        assertEquals(0, copy.width());
        copy.set(image);
        assertEquals(32, copy.height());
        image.free();
        copy.free();
    }

    @Test
    void buffersHaveDistinctElements() {
        GLFWImage.Buffer images = GLFWImage.malloc(3);
        assertEquals(3, images.capacity());
        images.get(1).width(7);
        images.get(2).width(9);
        assertEquals(0, images.get(0).width());
        assertEquals(7, images.get(1).width());
        assertEquals(9, images.get(2).width());
    }

    @Test
    void stackAllocatedStructs() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage first = GLFWImage.malloc(stack);
            GLFWImage second = GLFWImage.calloc(stack);
            first.width(1);
            second.width(2);
            assertEquals(1, first.width());
            assertEquals(2, second.width());
        }
    }

    @Test
    void fakeAddressesAreHarmless() {
        long fake = MemoryUtil.nmemAlloc(64);
        MemoryUtil.memPutInt(fake, 5);
        assertEquals(0, MemoryUtil.memGetInt(fake));
        assertEquals(0, GLFWVidMode.create(fake).width());
        MemoryUtil.memCopy(fake, fake + 8, 8);
        MemoryUtil.memSet(fake, 1, 64);
    }

    @Test
    void videoMode() {
        assertTrue(GLFW.glfwInit());
        long monitor = GLFW.glfwGetPrimaryMonitor();
        assertNotEquals(0L, monitor);
        GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
        assertNotNull(mode);
        assertEquals(1920, mode.width());
        assertEquals(1080, mode.height());
        assertEquals(60, mode.refreshRate());
        GLFWVidMode.Buffer modes = GLFW.glfwGetVideoModes(monitor);
        assertNotNull(modes);
        assertEquals(1, modes.limit());
        assertEquals(1920, modes.get(0).width());
    }

}
