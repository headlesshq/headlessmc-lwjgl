package io.github.headlesshq.headlessmc.lwjgl3.runtime.sdl;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Ids;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Input;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.StructMemory;
import io.github.headlesshq.headlessmc.lwjgl.runtime.WindowConfig;
import io.github.headlesshq.headlessmc.lwjgl.runtime.opengl.GlInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.sdl.SDL_DisplayMode;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDL_Rect;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.sdl.SDLKeycode.*;
import static org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_XRGB8888;
import static org.lwjgl.sdl.SDLVideo.*;

/**
 * Patches for SDL 3: initialisation, windows, the event queue fed by the input API, the keyboard and mouse state,
 * one display with one mode, and time.
 */
public final class SdlPatches {
    private static final String SDL_INIT = "org/lwjgl/sdl/SDLInit";
    private static final String SDL_VIDEO = "org/lwjgl/sdl/SDLVideo";
    private static final String SDL_EVENTS = "org/lwjgl/sdl/SDLEvents";
    private static final String SDL_KEYBOARD = "org/lwjgl/sdl/SDLKeyboard";
    private static final String SDL_MOUSE = "org/lwjgl/sdl/SDLMouse";
    private static final String SDL_TIMER = "org/lwjgl/sdl/SDLTimer";
    private static final int DISPLAY = 1;
    private static final long WAIT_MILLIS = 10L;
    private static final long START = System.nanoTime();
    private static final SDL_DisplayMode DISPLAY_MODE = displayMode();
    private static final Map<Integer, Integer> GL_ATTRIBUTES = new ConcurrentHashMap<>();

    private static volatile int swapInterval;

    private SdlPatches() {
        throw new AssertionError();
    }

    private static SDL_DisplayMode displayMode() {
        return SDL_DisplayMode.malloc().set(DISPLAY, SDL_PIXELFORMAT_XRGB8888, WindowConfig.MONITOR_WIDTH,
                                            WindowConfig.MONITOR_HEIGHT, 1.0f, WindowConfig.REFRESH_RATE,
                                            WindowConfig.REFRESH_RATE, 1);
    }

    @Patch(owner = SDL_INIT)
    public static boolean SDL_Init(int flags) {
        Input.register();
        return true;
    }

    @Patch(owner = SDL_INIT)
    public static boolean SDL_InitSubSystem(int flags) {
        Input.register();
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static long SDL_CreateWindow(CharSequence title, int w, int h, long flags) {
        return createWindow(w, h);
    }

    @Patch(owner = SDL_VIDEO)
    public static long SDL_CreateWindow(ByteBuffer title, int w, int h, long flags) {
        return createWindow(w, h);
    }

    private static long createWindow(int width, int height) {
        long window = Ids.nextLong();
        SdlWindows.register(window);
        Input.setSize(width, height);
        Input.register();
        return window;
    }

    @Patch(owner = SDL_VIDEO)
    public static void SDL_DestroyWindow(long window) {
        SdlWindows.remove(window);
    }

    @Patch(owner = SDL_VIDEO)
    public static int SDL_GetWindowID(long window) {
        return SdlWindows.id(window);
    }

    @Patch(owner = SDL_VIDEO)
    public static long SDL_GetWindowFromID(int id) {
        return SdlWindows.window(id);
    }

    @Patch(owner = SDL_VIDEO)
    public static long SDL_GetWindowFlags(long window) {
        long flags = SDL_WINDOW_OPENGL | SDL_WINDOW_RESIZABLE;
        if (Input.isFocused()) {
            flags |= SDL_WINDOW_INPUT_FOCUS | SDL_WINDOW_MOUSE_FOCUS;
        }

        if (SdlWindows.isRelativeMode(window)) {
            flags |= SDL_WINDOW_MOUSE_RELATIVE_MODE;
        }

        return flags;
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_GetWindowSize(long window, IntBuffer w, IntBuffer h) {
        put(w, Input.width());
        put(h, Input.height());
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_GetWindowSizeInPixels(long window, IntBuffer w, IntBuffer h) {
        return SDL_GetWindowSize(window, w, h);
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_SetWindowSize(long window, int w, int h) {
        Input.resize(w, h);
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static float SDL_GetWindowPixelDensity(long window) {
        return 1.0f;
    }

    @Patch(owner = SDL_VIDEO)
    public static float SDL_GetWindowDisplayScale(long window) {
        return 1.0f;
    }

    @Patch(owner = SDL_VIDEO)
    public static IntBuffer SDL_GetDisplays() {
        return IntBuffer.wrap(new int[]{DISPLAY});
    }

    @Patch(owner = SDL_VIDEO)
    public static int SDL_GetPrimaryDisplay() {
        return DISPLAY;
    }

    @Patch(owner = SDL_VIDEO)
    public static int SDL_GetDisplayForWindow(long window) {
        return DISPLAY;
    }

    @Patch(owner = SDL_VIDEO)
    public static String SDL_GetDisplayName(int displayID) {
        return "HeadlessMc";
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_GetDisplayBounds(int displayID, SDL_Rect rect) {
        rect.set(0, 0, WindowConfig.MONITOR_WIDTH, WindowConfig.MONITOR_HEIGHT);
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static float SDL_GetDisplayContentScale(int displayID) {
        return 1.0f;
    }

    @Patch(owner = SDL_VIDEO)
    public static SDL_DisplayMode SDL_GetCurrentDisplayMode(int displayID) {
        return DISPLAY_MODE;
    }

    /**
     * The desktop display mode is the only fullscreen display mode.
     */
    @Patch(owner = SDL_VIDEO)
    public static PointerBuffer SDL_GetFullscreenDisplayModes(int displayID) {
        PointerBuffer modes = PointerBuffer.create(StructMemory.allocate(Long.BYTES));
        modes.put(0, DISPLAY_MODE.address());
        return modes;
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_GetClosestFullscreenDisplayMode(int displayID, int w, int h, float refresh_rate,
                                                              boolean include_high_density_modes,
                                                              SDL_DisplayMode closest) {
        closest.set(DISPLAY_MODE);
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static SDL_DisplayMode SDL_GetDesktopDisplayMode(int displayID) {
        return DISPLAY_MODE;
    }

    @Patch(owner = SDL_VIDEO)
    public static void SDL_GL_ResetAttributes() {
        GL_ATTRIBUTES.clear();
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_GL_SetAttribute(int attr, int value) {
        GL_ATTRIBUTES.put(attr, value);
        return true;
    }

    /**
     * The context has the configured OpenGL version, whatever was requested, 8 bits per color channel, a 24 bit
     * depth buffer and an 8 bit stencil buffer, other attributes are what was set.
     */
    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_GL_GetAttribute(int attr, IntBuffer value) {
        int result;
        switch (attr) {
            case SDL_GL_CONTEXT_MAJOR_VERSION:
                result = GlInfo.majorVersion();
                break;
            case SDL_GL_CONTEXT_MINOR_VERSION:
                result = GlInfo.minorVersion();
                break;
            case SDL_GL_RED_SIZE:
            case SDL_GL_GREEN_SIZE:
            case SDL_GL_BLUE_SIZE:
            case SDL_GL_ALPHA_SIZE:
            case SDL_GL_STENCIL_SIZE:
                result = GL_ATTRIBUTES.getOrDefault(attr, 8);
                break;
            case SDL_GL_DEPTH_SIZE:
                result = GL_ATTRIBUTES.getOrDefault(attr, 24);
                break;
            case SDL_GL_DOUBLEBUFFER:
            case SDL_GL_ACCELERATED_VISUAL:
                result = GL_ATTRIBUTES.getOrDefault(attr, 1);
                break;
            case SDL_GL_CONTEXT_PROFILE_MASK:
                result = GL_ATTRIBUTES.getOrDefault(attr, SDL_GL_CONTEXT_PROFILE_CORE);
                break;
            default:
                result = GL_ATTRIBUTES.getOrDefault(attr, 0);
                break;
        }

        value.put(value.position(), result);
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_GL_SetSwapInterval(int interval) {
        swapInterval = interval;
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_GL_GetSwapInterval(IntBuffer interval) {
        interval.put(interval.position(), swapInterval);
        return true;
    }

    @Patch(owner = SDL_EVENTS)
    public static void SDL_PumpEvents() {
        SdlEvents.poll(null);
    }

    @Patch(owner = SDL_EVENTS)
    public static boolean SDL_PollEvent(SDL_Event event) {
        return SdlEvents.poll(event);
    }

    @Patch(owner = SDL_EVENTS)
    public static boolean SDL_WaitEvent(SDL_Event event) {
        while (!SdlEvents.poll(event)) {
            if (!sleep(WAIT_MILLIS)) {
                return false;
            }
        }

        return true;
    }

    @Patch(owner = SDL_EVENTS)
    public static boolean SDL_WaitEventTimeout(SDL_Event event, int timeoutMS) {
        if (SdlEvents.poll(event)) {
            return true;
        }

        sleep(timeoutMS < 0 ? WAIT_MILLIS : Math.min(WAIT_MILLIS, timeoutMS));
        return SdlEvents.poll(event);
    }

    private static boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Patch(owner = SDL_KEYBOARD)
    public static ByteBuffer SDL_GetKeyboardState() {
        return SdlWindows.keyboardState();
    }

    @Patch(owner = SDL_KEYBOARD)
    public static short SDL_GetModState() {
        int mods = 0;
        if (Input.isKeyPressed(GLFW_KEY_LEFT_SHIFT) || Input.isKeyPressed(GLFW_KEY_RIGHT_SHIFT)) {
            mods |= GLFW_MOD_SHIFT;
        }

        if (Input.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || Input.isKeyPressed(GLFW_KEY_RIGHT_CONTROL)) {
            mods |= GLFW_MOD_CONTROL;
        }

        if (Input.isKeyPressed(GLFW_KEY_LEFT_ALT) || Input.isKeyPressed(GLFW_KEY_RIGHT_ALT)) {
            mods |= GLFW_MOD_ALT;
        }

        if (Input.isKeyPressed(GLFW_KEY_LEFT_SUPER) || Input.isKeyPressed(GLFW_KEY_RIGHT_SUPER)) {
            mods |= GLFW_MOD_SUPER;
        }

        return SdlKeys.modifiers(mods);
    }

    @Patch(owner = SDL_KEYBOARD)
    public static int SDL_GetKeyFromScancode(int scancode, short modstate, boolean key_event) {
        return SdlKeys.keycode(scancode);
    }

    @Patch(owner = SDL_KEYBOARD)
    public static int SDL_GetScancodeFromKey(int key, ShortBuffer modstate) {
        if (modstate != null) {
            modstate.put(modstate.position(), (short) SDL_KMOD_NONE);
        }

        return SdlKeys.scancodeOfKey(key);
    }

    @Patch(owner = SDL_KEYBOARD)
    public static long SDL_GetKeyboardFocus() {
        return Input.isFocused() ? SdlWindows.mainWindow() : 0L;
    }

    @Patch(owner = SDL_KEYBOARD)
    public static boolean SDL_StartTextInput(long window) {
        SdlWindows.setTextInputActive(true);
        return true;
    }

    @Patch(owner = SDL_KEYBOARD)
    public static boolean SDL_StopTextInput(long window) {
        SdlWindows.setTextInputActive(false);
        return true;
    }

    @Patch(owner = SDL_KEYBOARD)
    public static boolean SDL_TextInputActive(long window) {
        return SdlWindows.isTextInputActive();
    }

    @Patch(owner = SDL_MOUSE)
    public static long SDL_GetMouseFocus() {
        return Input.isFocused() ? SdlWindows.mainWindow() : 0L;
    }

    @Patch(owner = SDL_MOUSE)
    public static int SDL_GetMouseState(FloatBuffer x, FloatBuffer y) {
        put(x, (float) Input.cursorX());
        put(y, (float) Input.cursorY());
        return SdlWindows.buttonMask();
    }

    @Patch(owner = SDL_MOUSE)
    public static int SDL_GetGlobalMouseState(FloatBuffer x, FloatBuffer y) {
        return SDL_GetMouseState(x, y);
    }

    @Patch(owner = SDL_MOUSE)
    public static int SDL_GetRelativeMouseState(FloatBuffer x, FloatBuffer y) {
        float[] motion = SdlWindows.takeRelativeMotion();
        put(x, motion[0]);
        put(y, motion[1]);
        return SdlWindows.buttonMask();
    }

    @Patch(owner = SDL_MOUSE)
    public static void SDL_WarpMouseInWindow(long window, float x, float y) {
        Input.setCursor(x, y);
    }

    @Patch(owner = SDL_MOUSE)
    public static boolean SDL_SetWindowRelativeMouseMode(long window, boolean enabled) {
        SdlWindows.setRelativeMode(window, enabled);
        return true;
    }

    @Patch(owner = SDL_MOUSE)
    public static boolean SDL_GetWindowRelativeMouseMode(long window) {
        return SdlWindows.isRelativeMode(window);
    }

    @Patch(owner = SDL_TIMER)
    public static long SDL_GetTicks() {
        return (System.nanoTime() - START) / 1_000_000L;
    }

    @Patch(owner = SDL_TIMER)
    public static long SDL_GetTicksNS() {
        return System.nanoTime() - START;
    }

    @Patch(owner = SDL_TIMER)
    public static long SDL_GetPerformanceCounter() {
        return System.nanoTime() - START;
    }

    @Patch(owner = SDL_TIMER)
    public static long SDL_GetPerformanceFrequency() {
        return 1_000_000_000L;
    }

    private static void put(IntBuffer buffer, int value) {
        if (buffer != null) {
            buffer.put(buffer.position(), value);
        }
    }

    private static void put(FloatBuffer buffer, float value) {
        if (buffer != null) {
            buffer.put(buffer.position(), value);
        }
    }

}
