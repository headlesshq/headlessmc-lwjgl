package io.github.headlesshq.headlessmc.lwjgl3;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * State that an application sets must be returned by the {@code glGet*} queries.
 */
class GlStateTest {
    @Test
    void stateIsQueryable() {
        assertTrue(GLFW.glfwInit());
        long window = GLFW.glfwCreateWindow(1024, 768, "State", 0L, 0L);
        try {
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();

            int[] viewport = new int[4];
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            assertArrayEquals(new int[]{0, 0, 1024, 768}, viewport, "the default viewport is the window size");
            GL11.glViewport(0, 0, 640, 360);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            assertArrayEquals(new int[]{0, 0, 640, 360}, viewport);

            assertFalse(GL11.glIsEnabled(GL11.GL_BLEND));
            GL11.glEnable(GL11.GL_BLEND);
            assertTrue(GL11.glIsEnabled(GL11.GL_BLEND));
            assertTrue(GL11.glGetBoolean(GL11.GL_BLEND));
            GL11.glDisable(GL11.GL_BLEND);
            assertFalse(GL11.glIsEnabled(GL11.GL_BLEND));
            assertTrue(GL11.glIsEnabled(GL11.GL_DITHER), "dithering is enabled initially");

            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 7);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 9);
            assertEquals(9, GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            assertEquals(7, GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
            assertEquals(GL13.GL_TEXTURE0, GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE));

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 3);
            assertEquals(3, GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING));
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 5);
            assertEquals(5, GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING));
            assertEquals(5, GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING));
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL20.glUseProgram(11);
            assertEquals(11, GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
            GL20.glUseProgram(0);

            assertEquals(GL11.GL_ONE, GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB));
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            assertEquals(GL11.GL_SRC_ALPHA, GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA));
            assertEquals(GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.glGetInteger(GL14.GL_BLEND_DST_RGB));

            GL11.glClearColor(0.25f, 0.5f, 0.75f, 1.0f);
            float[] clear = new float[4];
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clear);
            assertArrayEquals(new float[]{0.25f, 0.5f, 0.75f, 1.0f}, clear);
            assertEquals(GL11.GL_LESS, GL11.glGetInteger(GL11.GL_DEPTH_FUNC));

            int shader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            int program = GL20.glCreateProgram();
            GL20.glAttachShader(program, shader);
            assertEquals(GL20.GL_FRAGMENT_SHADER, GL20.glGetShaderi(shader, GL20.GL_SHADER_TYPE));
            assertEquals(GL11.GL_TRUE, GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS));
            assertEquals(0, GL20.glGetShaderi(shader, GL20.GL_INFO_LOG_LENGTH));
            assertEquals(GL11.GL_TRUE, GL20.glGetProgrami(program, GL20.GL_LINK_STATUS));
            assertEquals(1, GL20.glGetProgrami(program, GL20.GL_ATTACHED_SHADERS));
            assertEquals(0, GL20.glGetProgrami(program, GL20.GL_ACTIVE_UNIFORMS));

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 42);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 256, 128, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            assertEquals(256, GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH));
            assertEquals(128, GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT));
            assertEquals(GL11.GL_RGBA8, GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT));
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 43);
            assertEquals(0, GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH));
        } finally {
            GL11.glViewport(0, 0, 854, 480);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GLFW.glfwDestroyWindow(window);
        }
    }

    @Test
    void textureParametersFramebuffersAndFormats() {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        assertEquals(GL11.GL_REPEAT, GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        assertEquals(GL11.GL_NEAREST, GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER));
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, GL11.glGenTextures());
        assertEquals(GL11.GL_NEAREST_MIPMAP_LINEAR, GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER));

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        assertEquals(GL30.GL_FRAMEBUFFER_DEFAULT, GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL11.GL_BACK,
                                                                                            GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE));
        int framebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);
        assertEquals(GL11.GL_TEXTURE, GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                                                                                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE));
        assertEquals(texture, GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                                                                        GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME));
        assertEquals(GL11.GL_NONE, GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
                                                                             GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE));
        assertEquals(GL30.GL_FRAMEBUFFER_COMPLETE, GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER));

        int renderbuffer = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, renderbuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, 854, 480);
        assertEquals(854, GL30.glGetRenderbufferParameteri(GL30.GL_RENDERBUFFER, GL30.GL_RENDERBUFFER_WIDTH));
        assertEquals(GL30.GL_DEPTH24_STENCIL8, GL30.glGetRenderbufferParameteri(GL30.GL_RENDERBUFFER, GL30.GL_RENDERBUFFER_INTERNAL_FORMAT));
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        assertEquals(GL11.GL_TRUE, GL42.glGetInternalformati(GL11.GL_TEXTURE_2D, GL11.GL_RGBA8, GL43.GL_INTERNALFORMAT_SUPPORTED));
        assertTrue(GL42.glGetInternalformati(GL30.GL_RENDERBUFFER, GL11.GL_RGBA8, GL13.GL_SAMPLES) >= 4);
    }

    @Test
    void indexedQueries() {
        int buffer = GL15.glGenBuffers();
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 1, buffer);
        assertEquals(buffer, GL30.glGetIntegeri(GL31.GL_UNIFORM_BUFFER_BINDING, 1));
        assertEquals(0, GL30.glGetIntegeri(GL31.GL_UNIFORM_BUFFER_BINDING, 2));
        assertEquals(buffer, GL11.glGetInteger(GL31.GL_UNIFORM_BUFFER_BINDING));
        GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, 2, buffer, 256L, 64L);
        assertEquals(256L, GL32.glGetInteger64i(GL31.GL_UNIFORM_BUFFER_START, 2));
        assertEquals(64L, GL32.glGetInteger64i(GL31.GL_UNIFORM_BUFFER_SIZE, 2));
        assertEquals(64, GL30.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 2));
        assertTrue(GL30.glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0) >= 65535);
    }

}
