package io.github.headlesshq.headlessmc.lwjgl2;

import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LWJGL 2's GLU is Java, it is kept instead of being patched.
 */
class GluTest {
    /**
     * Minecraft 1.8 computes the camera position with {@code gluUnProject} in {@code ActiveRenderInfo}.
     */
    @Test
    void unProjectComputesThePosition() {
        FloatBuffer identity = BufferUtils.createFloatBuffer(16);
        identity.put(new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}).flip();
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        viewport.put(new int[]{0, 0, 100, 100}).flip();
        FloatBuffer position = BufferUtils.createFloatBuffer(3);
        assertTrue(GLU.gluUnProject(75, 25, 0.5f, identity, identity, viewport, position));
        assertEquals(0.5f, position.get(0), 1e-6f);
        assertEquals(-0.5f, position.get(1), 1e-6f);
        assertEquals(0.0f, position.get(2), 1e-6f);
    }

    @Test
    void errorStringsAreNamed() {
        assertEquals("Invalid enum", GLU.gluErrorString(GL11.GL_INVALID_ENUM));
    }

    /**
     * Minecraft 1.8 to 1.12 check compiled shaders with {@code glGetObjectParameteriARB} without OpenGL 2.0.
     */
    @Test
    void arbShaderObjectsCompileAndLink() {
        assertEquals(GL11.GL_TRUE, ARBShaderObjects.glGetObjectParameteriARB(1, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB));
        assertEquals(GL11.GL_TRUE, ARBShaderObjects.glGetObjectParameteriARB(1, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB));
    }

}
