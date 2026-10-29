package io.github.headlesshq.headlessmc.lwjgl2.runtime;

import io.github.headlesshq.headlessmc.lwjgl.runtime.Dummies;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Input;
import io.github.headlesshq.headlessmc.lwjgl.runtime.Patch;
import io.github.headlesshq.headlessmc.lwjgl.runtime.WindowConfig;
import io.github.headlesshq.headlessmc.lwjgl.runtime.opengl.GlInfo;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.PixelFormatLWJGL;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Patches for LWJGL 2's {@code Display}, {@code Sys}, {@code GLContext} and {@code AL}: a window whose size follows
 * the display mode and the input API, one desktop display mode, a millisecond timer, capabilities for the configured
 * OpenGL version, and a frame limiter for {@code Display.sync}.
 */
public final class Lwjgl2Display {
    private static final String DISPLAY = "org/lwjgl/opengl/Display";
    private static final String SYS = "org/lwjgl/Sys";
    private static final String GL_CONTEXT = "org/lwjgl/opengl/GLContext";
    private static final String AL = "org/lwjgl/openal/AL";
    private static final long START = System.nanoTime();
    private static final DisplayMode DESKTOP_MODE = displayMode(WindowConfig.MONITOR_WIDTH, WindowConfig.MONITOR_HEIGHT,
                                                                 32, WindowConfig.REFRESH_RATE);

    private static boolean created;
    private static boolean closeRequested;
    private static boolean resizedSinceUpdate;
    private static boolean resized;
    private static boolean resizable;
    private static boolean fullscreen;
    private static String title = "";
    private static DisplayMode mode = new DisplayMode(WindowConfig.WIDTH, WindowConfig.HEIGHT);
    private static ContextCapabilities capabilities;
    private static boolean alCreated;
    private static long nextFrame;

    private Lwjgl2Display() {
        throw new AssertionError();
    }

    private static DisplayMode displayMode(int width, int height, int bitsPerPixel, int frequency) {
        try {
            Constructor<DisplayMode> constructor = DisplayMode.class.getDeclaredConstructor(int.class, int.class, int.class, int.class);
            constructor.setAccessible(true);
            return constructor.newInstance(width, height, bitsPerPixel, frequency);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return new DisplayMode(width, height);
        }
    }

    static int height() {
        return Input.height();
    }

    static synchronized void resized() {
        resizedSinceUpdate = true;
    }

    static synchronized void requestClose() {
        closeRequested = true;
    }

    private static synchronized void createDisplay() {
        created = true;
        closeRequested = false;
        Input.setSize(mode.getWidth(), mode.getHeight());
        Input.register();
    }

    @Patch(owner = DISPLAY)
    public static void create() {
        createDisplay();
    }

    @Patch(owner = DISPLAY)
    public static void create(PixelFormat format) {
        createDisplay();
    }

    @Patch(owner = DISPLAY)
    public static void create(PixelFormat format, Drawable shared) {
        createDisplay();
    }

    @Patch(owner = DISPLAY)
    public static void create(PixelFormat format, ContextAttribs attribs) {
        createDisplay();
    }

    @Patch(owner = DISPLAY)
    public static void create(PixelFormat format, Drawable shared, ContextAttribs attribs) {
        createDisplay();
    }

    @Patch(owner = DISPLAY)
    public static void create(PixelFormatLWJGL format) {
        createDisplay();
    }

    @Patch(owner = DISPLAY)
    public static void create(PixelFormatLWJGL format, Drawable shared) {
        createDisplay();
    }

    @Patch(owner = DISPLAY)
    public static void create(PixelFormatLWJGL format, org.lwjgl.opengles.ContextAttribs attribs) {
        createDisplay();
    }

    @Patch(owner = DISPLAY)
    public static void create(PixelFormatLWJGL format, Drawable shared, org.lwjgl.opengles.ContextAttribs attribs) {
        createDisplay();
    }

    @Patch(owner = DISPLAY)
    public static synchronized boolean isCreated() {
        return created;
    }

    @Patch(owner = DISPLAY)
    public static synchronized void destroy() {
        created = false;
    }

    @Patch(owner = DISPLAY)
    public static synchronized boolean isCloseRequested() {
        return closeRequested;
    }

    @Patch(owner = DISPLAY)
    public static void update() {
        update(true);
    }

    @Patch(owner = DISPLAY)
    public static void update(boolean processMessages) {
        if (processMessages) {
            processMessages();
        }

        synchronized (Lwjgl2Display.class) {
            resized = resizedSinceUpdate;
            resizedSinceUpdate = false;
        }
    }

    @Patch(owner = DISPLAY)
    public static void processMessages() {
        Lwjgl2Input.pump();
    }

    /**
     * Sleeps until the next frame is due, like LWJGL 2's frame limiter, so that a headless game does not render as
     * fast as it can.
     */
    @Patch(owner = DISPLAY)
    public static synchronized void sync(int fps) {
        if (fps <= 0) {
            return;
        }

        long now = System.nanoTime();
        long frame = TimeUnit.SECONDS.toNanos(1) / fps;
        if (nextFrame > now) {
            try {
                TimeUnit.NANOSECONDS.sleep(nextFrame - now);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            nextFrame += frame;
        } else {
            nextFrame = now + frame;
        }
    }

    @Patch(owner = DISPLAY)
    public static synchronized boolean wasResized() {
        return resized;
    }

    @Patch(owner = DISPLAY)
    public static int getWidth() {
        return Input.width();
    }

    @Patch(owner = DISPLAY)
    public static int getHeight() {
        return Input.height();
    }

    @Patch(owner = DISPLAY)
    public static int getX() {
        return 0;
    }

    @Patch(owner = DISPLAY)
    public static int getY() {
        return 0;
    }

    @Patch(owner = DISPLAY)
    public static float getPixelScaleFactor() {
        return 1.0f;
    }

    @Patch(owner = DISPLAY)
    public static synchronized DisplayMode getDisplayMode() {
        return mode;
    }

    @Patch(owner = DISPLAY)
    public static synchronized void setDisplayMode(DisplayMode displayMode) {
        mode = displayMode;
        if (created) {
            Input.resize(displayMode.getWidth(), displayMode.getHeight());
        } else {
            Input.setSize(displayMode.getWidth(), displayMode.getHeight());
        }
    }

    @Patch(owner = DISPLAY)
    public static void setDisplayModeAndFullscreen(DisplayMode displayMode) {
        setDisplayMode(displayMode);
        setFullscreen(true);
    }

    @Patch(owner = DISPLAY)
    public static DisplayMode getDesktopDisplayMode() {
        return DESKTOP_MODE;
    }

    @Patch(owner = DISPLAY)
    public static DisplayMode[] getAvailableDisplayModes() {
        return new DisplayMode[]{DESKTOP_MODE};
    }

    @Patch(owner = DISPLAY)
    public static synchronized void setFullscreen(boolean enabled) {
        fullscreen = enabled;
    }

    @Patch(owner = DISPLAY)
    public static synchronized boolean isFullscreen() {
        return fullscreen;
    }

    @Patch(owner = DISPLAY)
    public static synchronized void setResizable(boolean enabled) {
        resizable = enabled;
    }

    @Patch(owner = DISPLAY)
    public static synchronized boolean isResizable() {
        return resizable;
    }

    @Patch(owner = DISPLAY)
    public static synchronized void setTitle(String newTitle) {
        title = newTitle == null ? "" : newTitle;
    }

    @Patch(owner = DISPLAY)
    public static synchronized String getTitle() {
        return title;
    }

    @Patch(owner = DISPLAY)
    public static boolean isVisible() {
        return true;
    }

    @Patch(owner = DISPLAY)
    public static boolean isActive() {
        return Input.isFocused();
    }

    @Patch(owner = DISPLAY)
    public static boolean isCurrent() {
        return true;
    }

    @Patch(owner = DISPLAY)
    public static int setIcon(ByteBuffer[] icons) {
        return icons == null ? 0 : icons.length;
    }

    @Patch(owner = DISPLAY)
    public static String getAdapter() {
        return "HeadlessMc";
    }

    @Patch(owner = DISPLAY, name = "getVersion")
    public static String getDisplayVersion() {
        return "HeadlessMc";
    }

    @Patch(owner = SYS)
    public static String getVersion() {
        return "2.9.4";
    }

    @Patch(owner = SYS)
    public static boolean is64Bit() {
        return !"32".equals(System.getProperty("sun.arch.data.model"));
    }

    @Patch(owner = SYS)
    public static long getTimerResolution() {
        return 1000L;
    }

    @Patch(owner = SYS)
    public static long getTime() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - START);
    }

    @Patch(owner = SYS)
    public static String getClipboard() {
        return "";
    }

    @Patch(owner = GL_CONTEXT)
    public static synchronized ContextCapabilities getCapabilities() {
        if (capabilities == null) {
            capabilities = Dummies.fillCapabilities(Dummies.create(ContextCapabilities.class), GlInfo::isSupported);
        }

        return capabilities;
    }

    @Patch(owner = AL, name = "create")
    public static synchronized void createAl() {
        alCreated = true;
    }

    @Patch(owner = AL, name = "create")
    public static void createAl(String deviceArguments, int contextFrequency, int contextRefresh, boolean contextSynchronized) {
        createAl();
    }

    @Patch(owner = AL, name = "create")
    public static void createAl(String deviceArguments, int contextFrequency, int contextRefresh, boolean contextSynchronized,
                                boolean openDevice) {
        createAl();
    }

    @Patch(owner = AL, name = "isCreated")
    public static synchronized boolean isAlCreated() {
        return alCreated;
    }

    @Patch(owner = AL, name = "destroy")
    public static synchronized void destroyAl() {
        alCreated = false;
    }

}
