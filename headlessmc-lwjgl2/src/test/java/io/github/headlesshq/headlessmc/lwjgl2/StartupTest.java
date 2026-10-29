package io.github.headlesshq.headlessmc.lwjgl2;

import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.SharedDrawable;

import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The LWJGL 2 calls that Minecraft 1.8 to 1.12.2 makes during startup.
 */
class StartupTest {
    /**
     * Forge's splash screen renders on another thread with a drawable that shares the context of the display.
     */
    @Test
    void sharedDrawable() throws Exception {
        Display.create();
        SharedDrawable shared = new SharedDrawable(Display.getDrawable());
        shared.makeCurrent();
        shared.releaseContext();
        Display.getDrawable().makeCurrent();
        shared.destroy();
    }

    @Test
    void display() throws Exception {
        assertEquals(LWJGLUtil.PLATFORM_LINUX, LWJGLUtil.getPlatform());
        Display.setTitle("Minecraft 1.12.2");
        Display.setResizable(true);
        DisplayMode desktop = Display.getDesktopDisplayMode();
        assertEquals(1920, desktop.getWidth());
        assertEquals(1080, desktop.getHeight());
        assertEquals(60, desktop.getFrequency());
        Display.setDisplayMode(new DisplayMode(854, 480));
        Display.create(new PixelFormat().withDepthBits(24));
        assertTrue(Display.isCreated());
        assertEquals("Minecraft 1.12.2", Display.getTitle());
        assertEquals(854, Display.getWidth());
        assertEquals(480, Display.getHeight());
        assertFalse(Display.isCloseRequested());
        assertTrue(Display.isActive());

        ContextCapabilities capabilities = GLContext.getCapabilities();
        assertTrue(capabilities.OpenGL20);
        assertTrue(capabilities.OpenGL32);
        assertEquals(16384, GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE));
        assertTrue(GL11.glGetString(GL11.GL_VERSION).startsWith("4.6"));
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        assertEquals(854, viewport.get(2));
        assertNotEquals(0, GL11.glGenTextures());
        assertEquals(GL30.GL_FRAMEBUFFER_COMPLETE, GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER));
        int shader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        assertEquals(GL11.GL_TRUE, GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS));
        assertEquals(GL11.GL_NO_ERROR, GL11.glGetError());

        Keyboard.create();
        Mouse.create();
        assertTrue(Keyboard.isCreated());
        assertEquals("A", Keyboard.getKeyName(Keyboard.KEY_A));
        assertEquals(Keyboard.KEY_SPACE, Keyboard.getKeyIndex("SPACE"));
        assertEquals("BUTTON0", Mouse.getButtonName(0));

        long time = Sys.getTime();
        assertTrue(time >= 0);
        assertEquals(1000, Sys.getTimerResolution());
        Display.update();
        Display.sync(60);

        AL.create();
        assertTrue(AL.isCreated());
        int source = AL10.alGenSources();
        assertNotEquals(0, source);
        AL10.alSourcePlay(source);
        assertEquals(AL10.AL_STOPPED, AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE));
        Display.destroy();
        assertFalse(Display.isCreated());
    }

}
