package io.github.headlesshq.headlessmc.lwjgl3.runtime;

/**
 * Size and refresh rate of the virtual window and monitor, configured by {@code hmc.lwjgl3.window.*}.
 */
public final class WindowConfig {
    public static final int WIDTH = Config.getInt("window.width", 854);
    public static final int HEIGHT = Config.getInt("window.height", 480);
    public static final int MONITOR_WIDTH = Config.getInt("monitor.width", 1920);
    public static final int MONITOR_HEIGHT = Config.getInt("monitor.height", 1080);
    public static final int REFRESH_RATE = Config.getInt("monitor.refreshRate", 60);

    private WindowConfig() {
        throw new AssertionError();
    }

}
