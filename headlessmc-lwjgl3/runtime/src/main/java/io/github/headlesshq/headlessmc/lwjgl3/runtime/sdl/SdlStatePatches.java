package io.github.headlesshq.headlessmc.lwjgl3.runtime.sdl;

import io.github.headlesshq.headlessmc.lwjgl.runtime.DummyBuffers;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Ids;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.StructMemory;
import io.github.headlesshq.headlessmc.lwjgl.runtime.opengl.GlInfo;
import org.lwjgl.PointerBuffer;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_USER;
import static org.lwjgl.sdl.SDLEvents.SDL_EVENT_LAST;
import static org.lwjgl.sdl.SDLKeycode.*;
import static org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_XRGB8888;
import static org.lwjgl.sdl.SDLVideo.SDL_ORIENTATION_LANDSCAPE;
import static org.lwjgl.sdl.SDLVideo.SDL_SYSTEM_THEME_UNKNOWN;

/**
 * Patches for the SDL state that applications set and query back: initialized subsystems, hints, enabled events,
 * registered event types, the keyboard and mouse devices, the cursor, key names, the current GL context and window
 * properties like title, position, grabs and opacity. The video driver is called {@code headlessmc}.
 */
public final class SdlStatePatches {
    private static final String SDL_HINTS = "org/lwjgl/sdl/SDLHints";
    private static final String SDL_INIT = "org/lwjgl/sdl/SDLInit";
    private static final String SDL_EVENTS = "org/lwjgl/sdl/SDLEvents";
    private static final String SDL_KEYBOARD = "org/lwjgl/sdl/SDLKeyboard";
    private static final String SDL_MOUSE = "org/lwjgl/sdl/SDLMouse";
    private static final String SDL_VIDEO = "org/lwjgl/sdl/SDLVideo";
    private static final String VIDEO_DRIVER = "headlessmc";
    private static final int KEYBOARD_ID = 1;
    private static final int MOUSE_ID = 1;
    private static final Map<String, String> HINTS = new ConcurrentHashMap<>();
    private static final Map<Integer, Boolean> DISABLED_EVENTS = new ConcurrentHashMap<>();
    private static final Map<Long, String> TITLES = new ConcurrentHashMap<>();
    private static final Map<Long, int[]> POSITIONS = new ConcurrentHashMap<>();
    private static final Map<Long, Float> OPACITIES = new ConcurrentHashMap<>();
    private static final Map<Long, Boolean> KEYBOARD_GRABS = new ConcurrentHashMap<>();
    private static final Map<Long, Boolean> MOUSE_GRABS = new ConcurrentHashMap<>();
    private static final Map<Integer, String> KEY_NAMES = new HashMap<>();
    private static final Map<String, Integer> KEYS_BY_NAME = new HashMap<>();
    private static final AtomicInteger NEXT_EVENT = new AtomicInteger(SDL_EVENT_USER);
    private static final long DEFAULT_CURSOR = Ids.nextLong();

    private static volatile int initialized;
    private static volatile Thread mainThread;
    private static volatile boolean cursorVisible = true;
    private static volatile long cursor = DEFAULT_CURSOR;
    private static volatile long currentWindow;
    private static volatile long currentContext;

    static {
        name(SDLK_RETURN, "Return");
        name(SDLK_ESCAPE, "Escape");
        name(SDLK_BACKSPACE, "Backspace");
        name(SDLK_TAB, "Tab");
        name(SDLK_SPACE, "Space");
        name(SDLK_DELETE, "Delete");
        name(SDLK_LEFT, "Left");
        name(SDLK_RIGHT, "Right");
        name(SDLK_UP, "Up");
        name(SDLK_DOWN, "Down");
        name(SDLK_LSHIFT, "Left Shift");
        name(SDLK_RSHIFT, "Right Shift");
        name(SDLK_LCTRL, "Left Ctrl");
        name(SDLK_RCTRL, "Right Ctrl");
        name(SDLK_LALT, "Left Alt");
        name(SDLK_RALT, "Right Alt");
        int[] functionKeys = {SDLK_F1, SDLK_F2, SDLK_F3, SDLK_F4, SDLK_F5, SDLK_F6, SDLK_F7, SDLK_F8, SDLK_F9,
            SDLK_F10, SDLK_F11, SDLK_F12};
        for (int i = 0; i < functionKeys.length; i++) {
            name(functionKeys[i], "F" + (i + 1));
        }
    }

    private SdlStatePatches() {
        throw new AssertionError();
    }

    private static void name(int key, String name) {
        KEY_NAMES.put(key, name);
        KEYS_BY_NAME.put(name.toLowerCase(Locale.ROOT), key);
    }

    static void initialized(int flags) {
        initialized |= flags;
        if (mainThread == null) {
            mainThread = Thread.currentThread();
        }
    }

    @Patch(owner = SDL_INIT)
    public static int SDL_WasInit(int flags) {
        return flags == 0 ? initialized : initialized & flags;
    }

    @Patch(owner = SDL_INIT)
    public static void SDL_QuitSubSystem(int flags) {
        initialized &= ~flags;
    }

    @Patch(owner = SDL_INIT)
    public static void SDL_Quit() {
        initialized = 0;
    }

    /**
     * The main thread is the thread that initialized SDL, or any thread before that.
     */
    @Patch(owner = SDL_INIT)
    public static boolean SDL_IsMainThread() {
        Thread thread = mainThread;
        return thread == null || thread == Thread.currentThread();
    }

    @Patch(owner = SDL_HINTS)
    public static boolean SDL_SetHint(CharSequence name, CharSequence value) {
        if (name == null) {
            return false;
        }

        if (value == null) {
            HINTS.remove(name.toString());
        } else {
            HINTS.put(name.toString(), value.toString());
        }

        return true;
    }

    @Patch(owner = SDL_HINTS)
    public static boolean SDL_SetHintWithPriority(CharSequence name, CharSequence value, int priority) {
        return SDL_SetHint(name, value);
    }

    @Patch(owner = SDL_HINTS)
    public static boolean SDL_ResetHint(CharSequence name) {
        if (name != null) {
            HINTS.remove(name.toString());
        }

        return true;
    }

    @Patch(owner = SDL_HINTS)
    public static void SDL_ResetHints() {
        HINTS.clear();
    }

    @Patch(owner = SDL_HINTS)
    public static String SDL_GetHint(CharSequence name) {
        return name == null ? null : HINTS.get(name.toString());
    }

    /**
     * Like SDL, {@code "0"} and {@code "false"} are false, every other value is true.
     */
    @Patch(owner = SDL_HINTS)
    public static boolean SDL_GetHintBoolean(CharSequence name, boolean default_value) {
        String value = SDL_GetHint(name);
        if (value == null || value.isEmpty()) {
            return default_value;
        }

        return !"0".equals(value) && !"false".equalsIgnoreCase(value);
    }

    @Patch(owner = SDL_EVENTS)
    public static void SDL_SetEventEnabled(int type, boolean enabled) {
        if (enabled) {
            DISABLED_EVENTS.remove(type);
        } else {
            DISABLED_EVENTS.put(type, true);
        }
    }

    @Patch(owner = SDL_EVENTS)
    public static boolean SDL_EventEnabled(int type) {
        return !DISABLED_EVENTS.containsKey(type);
    }

    static boolean isEnabled(int type) {
        return !DISABLED_EVENTS.containsKey(type);
    }

    /**
     * @return the first of {@code numevents} new user event types, 0 if there are not enough left.
     */
    @Patch(owner = SDL_EVENTS)
    public static int SDL_RegisterEvents(int numevents) {
        if (numevents <= 0) {
            return 0;
        }

        int first = NEXT_EVENT.getAndAdd(numevents);
        return first + numevents - 1 <= SDL_EVENT_LAST ? first : 0;
    }

    @Patch(owner = SDL_EVENTS)
    public static boolean SDL_GetEventFilter(PointerBuffer filter, PointerBuffer userdata) {
        filter.put(filter.position(), 0L);
        userdata.put(userdata.position(), 0L);
        return false;
    }

    @Patch(owner = SDL_KEYBOARD)
    public static boolean SDL_HasKeyboard() {
        return true;
    }

    @Patch(owner = SDL_KEYBOARD)
    public static IntBuffer SDL_GetKeyboards() {
        return IntBuffer.wrap(new int[]{KEYBOARD_ID});
    }

    @Patch(owner = SDL_KEYBOARD)
    public static String SDL_GetKeyboardNameForID(int instance_id) {
        return instance_id == KEYBOARD_ID ? "Keyboard" : null;
    }

    /**
     * Printable keys are named by their upper case character, other keys like SDL, unknown keys have no name.
     */
    @Patch(owner = SDL_KEYBOARD)
    public static String SDL_GetKeyName(int key) {
        String name = KEY_NAMES.get(key);
        if (name != null) {
            return name;
        }

        if (key > 0x20 && key < 0x7F) {
            return String.valueOf((char) key).toUpperCase(Locale.ROOT);
        }

        return "";
    }

    @Patch(owner = SDL_KEYBOARD)
    public static int SDL_GetKeyFromName(CharSequence name) {
        if (name == null || name.length() == 0) {
            return SDLK_UNKNOWN;
        }

        Integer key = KEYS_BY_NAME.get(name.toString().toLowerCase(Locale.ROOT));
        if (key != null) {
            return key;
        }

        if (name.length() == 1 && name.charAt(0) > 0x20 && name.charAt(0) < 0x7F) {
            return Character.toLowerCase(name.charAt(0));
        }

        return SDLK_UNKNOWN;
    }

    @Patch(owner = SDL_KEYBOARD)
    public static String SDL_GetScancodeName(int scancode) {
        return SDL_GetKeyName(SdlKeys.keycode(scancode));
    }

    @Patch(owner = SDL_KEYBOARD)
    public static int SDL_GetScancodeFromName(CharSequence name) {
        return SdlKeys.scancodeOfKey(SDL_GetKeyFromName(name));
    }

    @Patch(owner = SDL_MOUSE)
    public static boolean SDL_HasMouse() {
        return true;
    }

    @Patch(owner = SDL_MOUSE)
    public static IntBuffer SDL_GetMice() {
        return IntBuffer.wrap(new int[]{MOUSE_ID});
    }

    @Patch(owner = SDL_MOUSE)
    public static String SDL_GetMouseNameForID(int instance_id) {
        return instance_id == MOUSE_ID ? "Mouse" : null;
    }

    @Patch(owner = SDL_MOUSE)
    public static long SDL_CreateSystemCursor(int id) {
        return Ids.nextLong();
    }

    @Patch(owner = SDL_MOUSE)
    public static boolean SDL_SetCursor(long cursor) {
        if (cursor != 0L) {
            SdlStatePatches.cursor = cursor;
        }

        return true;
    }

    @Patch(owner = SDL_MOUSE)
    public static long SDL_GetCursor() {
        return cursor;
    }

    @Patch(owner = SDL_MOUSE)
    public static long SDL_GetDefaultCursor() {
        return DEFAULT_CURSOR;
    }

    @Patch(owner = SDL_MOUSE)
    public static boolean SDL_ShowCursor() {
        cursorVisible = true;
        return true;
    }

    @Patch(owner = SDL_MOUSE)
    public static boolean SDL_HideCursor() {
        cursorVisible = false;
        return true;
    }

    @Patch(owner = SDL_MOUSE)
    public static boolean SDL_CursorVisible() {
        return cursorVisible;
    }

    @Patch(owner = SDL_VIDEO)
    public static int SDL_GetNumVideoDrivers() {
        return 1;
    }

    @Patch(owner = SDL_VIDEO)
    public static String SDL_GetVideoDriver(int index) {
        return index == 0 ? VIDEO_DRIVER : null;
    }

    @Patch(owner = SDL_VIDEO)
    public static String SDL_GetCurrentVideoDriver() {
        return (initialized & org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO) != 0 ? VIDEO_DRIVER : null;
    }

    @Patch(owner = SDL_VIDEO)
    public static int SDL_GetSystemTheme() {
        return SDL_SYSTEM_THEME_UNKNOWN;
    }

    @Patch(owner = SDL_VIDEO)
    public static int SDL_GetNaturalDisplayOrientation(int displayID) {
        return SDL_ORIENTATION_LANDSCAPE;
    }

    @Patch(owner = SDL_VIDEO)
    public static int SDL_GetCurrentDisplayOrientation(int displayID) {
        return SDL_ORIENTATION_LANDSCAPE;
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_ScreenSaverEnabled() {
        return false;
    }

    @Patch(owner = SDL_VIDEO)
    public static PointerBuffer SDL_GetWindows() {
        long[] windows = SdlWindows.windows();
        PointerBuffer result = PointerBuffer.create(StructMemory.allocate(Math.max(1, windows.length) * Long.BYTES));
        for (int i = 0; i < windows.length; i++) {
            result.put(i, windows[i]);
        }

        return result.limit(windows.length);
    }

    @Patch(owner = SDL_VIDEO)
    public static int SDL_GetWindowPixelFormat(long window) {
        return SDL_PIXELFORMAT_XRGB8888;
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_SetWindowTitle(long window, CharSequence title) {
        TITLES.put(window, title == null ? "" : title.toString());
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static String SDL_GetWindowTitle(long window) {
        return TITLES.getOrDefault(window, "");
    }

    static void title(long window, CharSequence title) {
        SDL_SetWindowTitle(window, title);
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_SetWindowPosition(long window, int x, int y) {
        POSITIONS.put(window, new int[]{x, y});
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_GetWindowPosition(long window, IntBuffer x, IntBuffer y) {
        int[] position = POSITIONS.getOrDefault(window, new int[2]);
        put(x, position[0]);
        put(y, position[1]);
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_GetWindowMinimumSize(long window, IntBuffer w, IntBuffer h) {
        put(w, 0);
        put(h, 0);
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_GetWindowMaximumSize(long window, IntBuffer w, IntBuffer h) {
        put(w, 0);
        put(h, 0);
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_SetWindowOpacity(long window, float opacity) {
        OPACITIES.put(window, Math.max(0.0f, Math.min(1.0f, opacity)));
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static float SDL_GetWindowOpacity(long window) {
        return OPACITIES.getOrDefault(window, 1.0f);
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_SetWindowKeyboardGrab(long window, boolean grabbed) {
        KEYBOARD_GRABS.put(window, grabbed);
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_GetWindowKeyboardGrab(long window) {
        return KEYBOARD_GRABS.getOrDefault(window, false);
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_SetWindowMouseGrab(long window, boolean grabbed) {
        MOUSE_GRABS.put(window, grabbed);
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_GetWindowMouseGrab(long window) {
        return MOUSE_GRABS.getOrDefault(window, false);
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_SyncWindow(long window) {
        return true;
    }

    /**
     * Function pointers are never called, but must not be {@code NULL}.
     */
    @Patch(owner = SDL_VIDEO)
    public static long SDL_GL_GetProcAddress(CharSequence proc) {
        return DummyBuffers.FAKE_ADDRESS;
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_GL_ExtensionSupported(CharSequence extension) {
        return extension != null && GlInfo.extensions().contains(extension.toString());
    }

    @Patch(owner = SDL_VIDEO)
    public static long SDL_GL_CreateContext(long window) {
        long context = Ids.nextLong();
        currentWindow = window;
        currentContext = context;
        return context;
    }

    @Patch(owner = SDL_VIDEO)
    public static boolean SDL_GL_MakeCurrent(long window, long context) {
        currentWindow = window;
        currentContext = context;
        return true;
    }

    @Patch(owner = SDL_VIDEO)
    public static long SDL_GL_GetCurrentWindow() {
        return currentWindow;
    }

    @Patch(owner = SDL_VIDEO)
    public static long SDL_GL_GetCurrentContext() {
        return currentContext;
    }

    static void forget(long window) {
        TITLES.remove(window);
        POSITIONS.remove(window);
        OPACITIES.remove(window);
        KEYBOARD_GRABS.remove(window);
        MOUSE_GRABS.remove(window);
        if (currentWindow == window) {
            currentWindow = 0L;
        }
    }

    private static void put(IntBuffer buffer, int value) {
        if (buffer != null) {
            buffer.put(buffer.position(), value);
        }
    }

}
