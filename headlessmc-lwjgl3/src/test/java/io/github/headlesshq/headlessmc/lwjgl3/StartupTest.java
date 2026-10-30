package io.github.headlesshq.headlessmc.lwjgl3;

import io.github.headlesshq.headlessmc.lwjgl.runtime.DummyBuffers;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDLHints;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLScancode;
import org.lwjgl.sdl.SDLTimer;
import org.lwjgl.sdl.SDLVersion;
import org.lwjgl.sdl.SDL_DisplayMode;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.vulkan.VK;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.SharedLibrary;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Calls that every Minecraft version makes during startup.
 */
class StartupTest {
    /**
     * The early loading screens of Forge and NeoForge render text with STB TrueType, and Minecraft frees the
     * framebuffer size callback they set.
     */
    @Test
    void forgeEarlyDisplay() {
        assertTrue(GLFW.glfwInit());
        long window = GLFW.glfwCreateWindow(854, 480, "Early display", 0L, 0L);
        assertEquals(1, GLFW.glfwGetMonitors().remaining());
        assertEquals(GLFW.glfwGetPrimaryMonitor(), GLFW.glfwGetMonitors().get(0));
        assertEquals("a", GLFW.glfwGetKeyName(GLFW.GLFW_KEY_A, 0));
        assertNull(GLFW.glfwGetKeyName(GLFW.GLFW_KEY_ESCAPE, 0));
        GLFW.glfwSetFramebufferSizeCallback(window, (w, width, height) -> { });
        GLFWFramebufferSizeCallback previous = GLFW.glfwSetFramebufferSizeCallback(window, null);
        assertNotNull(previous);
        previous.free();

        ByteBuffer font = BufferUtils.createByteBuffer(16);
        try (STBTTFontinfo info = STBTTFontinfo.create();
             STBTTPackContext context = STBTTPackContext.malloc();
             STBTTPackedchar.Buffer chars = STBTTPackedchar.malloc(96);
             STBTTAlignedQuad quad = STBTTAlignedQuad.malloc()) {
            assertTrue(STBTruetype.stbtt_InitFont(info, font));
            assertTrue(STBTruetype.stbtt_PackBegin(context, BufferUtils.createByteBuffer(512 * 512), 512, 512, 0, 1, 0L));
            assertTrue(STBTruetype.stbtt_PackFontRange(context, font, 0, 16.0f, 32, chars));
            float[] ascent = new float[1];
            float[] descent = new float[1];
            float[] lineGap = new float[1];
            STBTruetype.stbtt_GetScaledFontVMetrics(font, 0, 16.0f, ascent, descent, lineGap);
            assertTrue(ascent[0] > 0.0f);
            assertTrue(descent[0] < 0.0f);
            float[] x = {0.0f};
            float[] y = {0.0f};
            STBTruetype.stbtt_GetPackedQuad(chars, 512, 512, 'A' - 32, x, y, quad, false);
            assertTrue(x[0] > 0.0f);
            assertEquals(x[0], quad.x1());
        }

        GLFW.glfwDestroyWindow(window);
    }

    /**
     * Minecraft 26.3 checks each native library when it starts and prefers Vulkan if a Vulkan loader exists.
     */
    @Test
    void minecraft26LibraryChecks() {
        assertNull(STBImage.stbi_failure_reason());
        assertInstanceOf(SharedLibrary.class, GL.getFunctionProvider());
        assertInstanceOf(SharedLibrary.class, ALC.getFunctionProvider());
        assertEquals(SDLVideo.SDL_GL_GetProcAddress("glGetError"), GL.getFunctionProvider().getFunctionAddress("glGetError"));
        assertNotEquals(0L, SDLVideo.SDL_GL_GetProcAddress("glGetError"));
        assertThrows(IllegalStateException.class, VK::create);
        assertThrows(IllegalStateException.class, VK::getFunctionProvider);
        assertTrue(SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO | SDLInit.SDL_INIT_EVENTS));
        assertEquals(SDLInit.SDL_INIT_VIDEO, SDLInit.SDL_WasInit(SDLInit.SDL_INIT_VIDEO));
        assertNull(SDLHints.SDL_GetHint("HMC_TEST_HINT"));
        assertTrue(SDLHints.SDL_SetHint("HMC_TEST_HINT", "1"));
        assertEquals("1", SDLHints.SDL_GetHint("HMC_TEST_HINT"));
        assertTrue(SDLHints.SDL_GetHintBoolean("HMC_TEST_HINT", false));
        assertEquals(1_000_000L, SDLTimer.SDL_MS_TO_NS(1L));
        assertEquals(3_002_001, SDLVersion.SDL_VERSIONNUM(3, 2, 1));
        assertEquals(SDLKeycode.SDLK_A, SDLKeyboard.SDL_GetKeyFromName("A"));
        assertEquals("A", SDLKeyboard.SDL_GetKeyName(SDLKeycode.SDLK_A));
        assertEquals(SDLScancode.SDL_SCANCODE_UNKNOWN, SDLKeyboard.SDL_GetScancodeFromKey(SDLKeycode.SDLK_UNKNOWN, null));
        assertTrue(SDLMouse.SDL_HideCursor());
        assertFalse(SDLMouse.SDL_CursorVisible());
        assertTrue(SDLMouse.SDL_ShowCursor());
        assertNotEquals(0L, SDLMouse.SDL_GetCursor());
        PointerBuffer modes = SDLVideo.SDL_GetFullscreenDisplayModes(SDLVideo.SDL_GetPrimaryDisplay());
        assertNotNull(modes);
        assertEquals(1, modes.limit());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer major = stack.callocInt(1);
            IntBuffer minor = stack.callocInt(1);
            assertTrue(SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_CONTEXT_MAJOR_VERSION, 3));
            assertTrue(SDLVideo.SDL_GL_GetAttribute(SDLVideo.SDL_GL_CONTEXT_MAJOR_VERSION, major));
            assertTrue(SDLVideo.SDL_GL_GetAttribute(SDLVideo.SDL_GL_CONTEXT_MINOR_VERSION, minor));
            assertTrue(major.get(0) * 10 + minor.get(0) >= 33);
        }

        SDL_DisplayMode mode = SDL_DisplayMode.create(modes.get(0));
        assertEquals(1920, mode.w());
        assertEquals(1080, mode.h());
    }

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
        // uses the OpenGL runtime embedded into another module, which fails if the SDL module does not read it
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer major = stack.mallocInt(1);
            assertTrue(SDLVideo.SDL_GL_GetAttribute(SDLVideo.SDL_GL_CONTEXT_MAJOR_VERSION, major));
            assertTrue(major.get(0) > 0);
        }
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

        assertTrue(ALC10.alcGetInteger(device, ALC11.ALC_MONO_SOURCES) > 0);
        assertNotNull(ALC10.alcGetString(device, ALC10.ALC_DEVICE_SPECIFIER));
        int buffer = AL10.alGenBuffers();
        AL10.alBufferData(buffer, AL10.AL_FORMAT_STEREO16, BufferUtils.createShortBuffer(8), 44100);
        assertEquals(44100, AL10.alGetBufferi(buffer, AL10.AL_FREQUENCY));
        assertEquals(2, AL10.alGetBufferi(buffer, AL10.AL_CHANNELS));
        int source = AL10.alGenSources();
        assertEquals(AL10.AL_INITIAL, AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE));
        AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
        AL10.alSourcef(source, AL10.AL_GAIN, 0.5f);
        assertEquals(0.5f, AL10.alGetSourcef(source, AL10.AL_GAIN));
        AL10.alSourcePlay(source);
        assertEquals(AL10.AL_STOPPED, AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE));

        int stream = AL10.alGenSources();
        AL10.alSourceQueueBuffers(stream, new int[]{buffer, buffer});
        AL10.alSourcePlay(stream);
        assertEquals(2, AL10.alGetSourcei(stream, AL10.AL_BUFFERS_PROCESSED));
        assertEquals(buffer, AL10.alSourceUnqueueBuffers(stream));
        assertEquals(1, AL10.alGetSourcei(stream, AL10.AL_BUFFERS_QUEUED));

        AL10.alSourcei(source, AL10.AL_LOOPING, AL10.AL_TRUE);
        AL10.alSourcePlay(source);
        assertEquals(AL10.AL_PLAYING, AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE));
        AL10.alSourceStop(source);
        assertEquals(AL10.AL_STOPPED, AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE));
    }

}
