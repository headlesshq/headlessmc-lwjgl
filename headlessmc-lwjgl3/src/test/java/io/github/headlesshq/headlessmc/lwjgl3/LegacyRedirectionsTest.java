package io.github.headlesshq.headlessmc.lwjgl3;

import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.openal.ALC10;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDLVulkan;
import org.lwjgl.sdl.SDL_DisplayMode;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The results that the redirections of HeadlessMc's transformer based lwjgl patcher guarantee, which applications
 * started with it rely on, e.g. that windows, devices and programs are created and framebuffers are complete.
 */
class LegacyRedirectionsTest {
    @Test
    void glfwWindowsAndMonitors() {
        assertTrue(GLFW.glfwInit());
        assertNotEquals(0L, GLFW.glfwGetPrimaryMonitor());
        long window = GLFW.glfwCreateWindow(854, 480, "Legacy", 0L, 0L);
        try {
            assertNotEquals(0L, window);
            double time = GLFW.glfwGetTime();
            assertTrue(time >= 0.0);
            int[] width = new int[1];
            int[] height = new int[1];
            GLFW.glfwGetFramebufferSize(window, width, height);
            assertTrue(width[0] > 0 && height[0] > 0);
            GLFW.glfwWaitEventsTimeout(0.001);
        } finally {
            GLFW.glfwDestroyWindow(window);
        }
    }

    @Test
    void openGlObjectsAreCreatedAndComplete() {
        assertTrue(GLFW.glfwInit());
        long window = GLFW.glfwCreateWindow(854, 480, "Legacy GL", 0L, 0L);
        try {
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            int shader = GL20C.glCreateShader(GL20C.GL_VERTEX_SHADER);
            assertEquals(GL11.GL_TRUE, GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS));
            assertEquals(GL11.GL_TRUE, GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS));
            int program = GL20C.glCreateProgram();
            assertNotEquals(0, program);
            assertNotEquals(0, GL20.glCreateProgram());
            assertEquals(GL11.GL_TRUE, GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS));
            assertEquals(GL11.GL_TRUE, GL20.glGetProgrami(program, GL20.GL_LINK_STATUS));
            assertEquals(GL30.GL_FRAMEBUFFER_COMPLETE, GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER));
            assertNotEquals(0L, GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0));
            assertNotEquals(0, GL11C.glGenTextures());
            assertTrue(GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE) > 0);
            assertTrue(GL11C.glGetInteger(GL11C.GL_MAX_TEXTURE_SIZE) > 0);

            int texture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 16, 8, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            assertEquals(16, GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH));
            assertEquals(8, GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT));
            int list = GL11.glGenLists(1);
            assertNotEquals(0, list);
        } finally {
            GLFW.glfwDestroyWindow(window);
        }
    }

    @Test
    void memoryIsAllocated() {
        long address = MemoryUtil.nmemAlloc(16);
        assertNotEquals(0L, address);
        MemoryUtil.nmemFree(address);
        address = MemoryUtil.nmemCalloc(4, 4);
        assertNotEquals(0L, address);
        MemoryUtil.nmemFree(address);
        assertNotEquals(0L, MemoryUtil.getAllocator().malloc(16));
        assertNotEquals(0L, MemoryUtil.getAllocator().calloc(4, 4));
        assertNotEquals(0L, MemoryUtil.getAllocator().realloc(0L, 16));
        assertNotEquals(0L, MemoryUtil.getAllocator().aligned_alloc(16, 16));
        ByteBuffer buffer = MemoryUtil.memAlloc(8);
        assertNotEquals(0L, MemoryUtil.memAddress(buffer));
        ByteBuffer grown = MemoryUtil.memRealloc(buffer, 32);
        assertEquals(32, grown.capacity());
        MemoryUtil.memFree(grown);
        assertEquals(4, BufferUtils.createIntBuffer(4).capacity());
    }

    @Test
    void audioDevicesOpen() {
        assertNotEquals(0L, ALC10.alcOpenDevice((CharSequence) null));
    }

    @Test
    void fontsAndImages() {
        STBTTFontinfo info = STBTTFontinfo.malloc();
        assertTrue(STBTruetype.stbtt_InitFont(info, BufferUtils.createByteBuffer(64)));
        info.free();
        // loading never fails, Minecraft 26.3 checks that there is no failure reason
        assertNull(STBImage.stbi_failure_reason());
    }

    @Test
    void sdlWindowsAndDisplays() {
        assertTrue(SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO));
        assertFalse(SDLVulkan.SDL_Vulkan_LoadLibrary((CharSequence) null));
        assertTrue(SDLVideo.SDL_GL_LoadLibrary((CharSequence) null));
        long window = SDLVideo.SDL_CreateWindow("Legacy SDL", 854, 480, SDLVideo.SDL_WINDOW_OPENGL);
        assertNotEquals(0L, window);
        long context = SDLVideo.SDL_GL_CreateContext(window);
        assertNotEquals(0L, context);
        assertTrue(SDLVideo.SDL_GL_MakeCurrent(window, context));
        int display = SDLVideo.SDL_GetPrimaryDisplay();
        assertNotEquals(0, display);
        assertNotEquals(0, SDLVideo.SDL_GetDisplayForWindow(window));
        assertEquals(1.0f, SDLVideo.SDL_GetWindowPixelDensity(window));
        SDL_DisplayMode mode = SDLVideo.SDL_GetDesktopDisplayMode(display);
        assertNotNull(mode);
        assertTrue(mode.w() > 0 && mode.h() > 0 && mode.refresh_rate() > 0);
        assertTrue(SDLVideo.SDL_GL_SwapWindow(window));

        SDL_Event event = SDL_Event.calloc();
        event.window().type(SDLEvents.SDL_EVENT_WINDOW_RESIZED).windowID(SDLVideo.SDL_GetWindowID(window));
        assertEquals(window, SDLEvents.SDL_GetWindowFromEvent(event));
        event.type(SDLEvents.SDL_EVENT_QUIT);
        assertEquals(0L, SDLEvents.SDL_GetWindowFromEvent(event));
        event.free();
        SDLVideo.SDL_DestroyWindow(window);
    }

}
