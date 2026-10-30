package io.github.headlesshq.headlessmc.lwjgl2;

import org.junit.jupiter.api.Test;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The results that the redirections of HeadlessMc's transformer based lwjgl patcher guarantee for LWJGL 2, which
 * Minecraft 1.8 to 1.12 started with it rely on.
 */
class LegacyRedirectionsTest {
    @Test
    void displayIsActiveWithASize() throws LWJGLException {
        Display.create();
        try {
            assertTrue(Display.isActive());
            assertTrue(Display.getWidth() > 0 && Display.getHeight() > 0);
            assertFalse(Display.isFullscreen());
            DisplayMode mode = Display.getDesktopDisplayMode();
            assertTrue(mode.getWidth() > 0 && mode.getHeight() > 0);
            assertTrue(mode.getFrequency() > 0 && mode.getBitsPerPixel() > 0);
            Display.update();
        } finally {
            Display.destroy();
        }
    }

    @Test
    void sysReportsVersionAndTime() {
        assertNotNull(Sys.getVersion());
        assertTrue(Sys.getTimerResolution() > 0);
        assertTrue(Sys.getTime() >= 0);
    }

    @Test
    void openGlObjectsAreCreatedAndComplete() throws LWJGLException {
        Display.create();
        try {
            int shader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            assertEquals(GL11.GL_TRUE, GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS));
            int program = GL20.glCreateProgram();
            assertNotEquals(0, program);
            assertEquals(GL11.GL_TRUE, GL20.glGetProgrami(program, GL20.GL_LINK_STATUS));
            assertEquals(GL30.GL_FRAMEBUFFER_COMPLETE, GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER));
            assertTrue(GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE) > 0);
            int texture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 16, 8, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            assertEquals(16, GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH));
            assertNotEquals(0, GL11.glGenLists(1));
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            assertEquals(GL11.GL_NEAREST, GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER));
            assertEquals(GL11.GL_LINEAR, GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER));
        } finally {
            Display.destroy();
        }
    }

}
