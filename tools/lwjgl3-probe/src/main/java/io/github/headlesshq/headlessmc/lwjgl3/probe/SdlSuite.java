package io.github.headlesshq.headlessmc.lwjgl3.probe;

import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDLHints;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLPixels;
import org.lwjgl.sdl.SDLPlatform;
import org.lwjgl.sdl.SDLScancode;
import org.lwjgl.sdl.SDLTimer;
import org.lwjgl.sdl.SDLVersion;
import org.lwjgl.sdl.SDLVideo;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.sdl.SDLError.SDL_ClearError;
import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLMouse.SDL_CreateSystemCursor;
import static org.lwjgl.sdl.SDLMouse.SDL_SYSTEM_CURSOR_DEFAULT;
import static org.lwjgl.sdl.SDLVideo.*;

/**
 * SDL 3, the window and input parts Minecraft 26.x uses: a hidden window with a current OpenGL context, the primary
 * display and a cursor. Only video, input, event, timer, hint, error and version functions are probed. Functions that
 * destroy the fixture, block, show dialogs, or install callbacks that later calls would run are denied.
 */
final class SdlSuite extends Suite {
    private final Map<String, Long> handles = new LinkedHashMap<>();
    private final Map<String, Integer> objects = new LinkedHashMap<>();

    @Override
    String name() {
        return "sdl";
    }

    @Override
    void setUp() {
        if (!SDL_Init(SDL_INIT_VIDEO | SDL_INIT_EVENTS)) {
            throw new IllegalStateException("SDL_Init failed: " + SDL_GetError());
        }

        long window = SDL_CreateWindow("HeadlessMc LWJGL probe", 854, 480, SDL_WINDOW_OPENGL | SDL_WINDOW_HIDDEN);
        if (window == 0L) {
            throw new IllegalStateException("SDL_CreateWindow failed: " + SDL_GetError());
        }

        long context = SDL_GL_CreateContext(window);
        if (context == 0L) {
            throw new IllegalStateException("SDL_GL_CreateContext failed: " + SDL_GetError());
        }

        handles.put("SDL_Window *", window);
        handles.put("SDL_GLContext", context);
        handles.put("SDL_Cursor *", SDL_CreateSystemCursor(SDL_SYSTEM_CURSOR_DEFAULT));
        int display = SDL_GetPrimaryDisplay();
        objects.put("displayID", display);
        objects.put("windowID", SDL_GetWindowID(window));
        objects.put("id", SDL_GetWindowID(window));
        SDL_ClearError();
    }

    @Override
    List<Class<?>> classes() {
        return List.of(SDLVideo.class, SDLKeyboard.class, SDLMouse.class, SDLEvents.class, SDLInit.class,
                       SDLTimer.class, SDLHints.class, SDLError.class, SDLVersion.class, SDLPlatform.class);
    }

    @Override
    List<Class<?>> enumClasses() {
        return List.of(SDLVideo.class, SDLKeyboard.class, SDLKeycode.class, SDLScancode.class, SDLMouse.class,
                       SDLEvents.class, SDLInit.class, SDLPixels.class);
    }

    @Override
    List<String> contextEnums() {
        return List.of("SDL_GL_CONTEXT_MAJOR_VERSION", "SDL_GL_CONTEXT_MINOR_VERSION", "SDL_WINDOW_OPENGL",
                       "SDL_SCANCODE_A", "SDL_SYSTEM_CURSOR_DEFAULT");
    }

    @Override
    Map<String, Long> handles() {
        return handles;
    }

    @Override
    Map<String, Integer> objects() {
        return objects;
    }

    @Override
    List<String> strings() {
        return List.of("SDL_VIDEO_DRIVER", "SDL_MOUSE_RELATIVE_MODE_CENTER", "A", "x11");
    }

    /**
     * SDL has no per call error state, so the error message is read and cleared after every call.
     */
    @Override
    String error(Method method) {
        String error = SDL_GetError();
        SDL_ClearError();
        return error == null || error.isEmpty() ? null : error;
    }

    @Override
    Map<String, Object> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("SDL_GetVersion", SDLVersion.SDL_GetVersion());
        info.put("SDL_GetRevision", SDLVersion.SDL_GetRevision());
        info.put("SDL_GetCurrentVideoDriver", SDL_GetCurrentVideoDriver());
        info.put("SDL_GetPlatform", SDLPlatform.SDL_GetPlatform());
        return info;
    }

    @Override
    Object capabilities(String internalName) {
        return null;
    }

    @Override
    Set<String> denied() {
        return Set.of(
            "SDL_Quit", "SDL_QuitSubSystem", "SDL_DestroyWindow", "SDL_GL_DestroyContext", "SDL_GL_UnloadLibrary",
            "SDL_GL_LoadLibrary", "SDL_Vulkan_LoadLibrary", "SDL_Vulkan_UnloadLibrary", "SDL_DestroyCursor",
            "SDL_Delay", "SDL_DelayNS", "SDL_DelayPrecise", "SDL_WaitEvent", "SDL_WaitEventTimeout",
            "SDL_ShowSimpleMessageBox", "SDL_ShowMessageBox", "SDL_ShowWindowSystemMenu",
            "SDL_SetEventFilter", "SDL_AddEventWatch", "SDL_RemoveEventWatch", "SDL_FilterEvents", "SDL_AddTimer",
            "SDL_AddTimerNS", "SDL_RemoveTimer", "SDL_SetWindowHitTest", "SDL_AddHintCallback",
            "SDL_RemoveHintCallback", "SDL_RunOnMainThread", "SDL_SetX11EventHook", "SDL_SetMainReady",
            "SDL_SetAppMetadata", "SDL_SetAppMetadataProperty", "SDL_DestroyWindowSurface",
            "SDL_SetWindowsMessageHook", "SDL_ResetHints", "SDL_ResetHint"
        );
    }

}
