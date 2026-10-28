package io.github.headlesshq.headlessmc.lwjgl3;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.system.CallbackI;
import org.lwjgl.system.MemoryStack;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Calls API that LWJGL removed before 3.4.3, but that older Minecraft versions were compiled against.
 */
@SuppressWarnings("deprecation")
class CompatibilityTest {
    @Test
    void removedStructStackAllocation() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            assertNotNull(GLFWImage.mallocStack(stack));
            assertNotNull(GLFWImage.callocStack());
            GLFWImage.Buffer images = GLFWImage.mallocStack(2, stack);
            assertNotNull(images);
            assertEquals(2, images.capacity());
        }
    }

    @Test
    void removedCallbackSuperinterface() {
        GLFWKeyCallbackI callback = (window, key, scancode, action, mods) -> { };
        assertInstanceOf(CallbackI.V.class, callback);
        callback.callback(0L);
    }

}
